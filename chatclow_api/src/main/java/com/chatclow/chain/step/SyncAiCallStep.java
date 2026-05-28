package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.context.ChatContext;
import com.chatclow.entity.AiModel;
import com.chatclow.service.FunctionExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 第3步（非流式路径）：同步调用 AI 模型 + Function Calling（@Order = 30）
 *
 * <h3>和 ModelCallStep 的关系：互斥</h3>
 * <pre>
 * shouldSkip = isStreamMode()
 *
 *   流式请求 → SyncAiCallStep 跳过，ModelCallStep 执行
 *   非流请求 → SyncAiCallStep 执行，ModelCallStep 跳过
 * </pre>
 *
 * <h3>非流式和流式的本质区别</h3>
 * <table>
 *   <tr><th></th><th>流式（ModelCallStep）</th><th>非流式（SyncAiCallStep）</th></tr>
 *   <tr><td>请求方式</td><td>OkHttp SSE stream=true</td><td>OkHttp 普通同步请求</td></tr>
 *   <tr><td>响应</td><td>逐 token 推送</td><td>一次性返回完整 JSON</td></tr>
 *   <tr><td>FC 处理</td><td>独立 Step（FunctionCallingStep）</td><td>本类内部闭环</td></tr>
 *   <tr><td>SSE emitter</td><td>需要</td><td>不需要（直接返回字符串）</td></tr>
 * </table>
 *
 * <h3>Function Calling 全流程</h3>
 * <pre>
 * 第一次请求 AI（带 tools）
 *   │
 *   ├── AI 返回普通文本 → 直接作为回复返回
 *   │
 *   └── AI 返回 tool_calls →
 *         ├── 执行每个工具（FunctionExecutor）
 *         ├── 构建含 tool 结果的消息列表
 *         └── 第二次请求 AI（不带 tools，避免死循环）→ 最终文本回答
 * </pre>
 *
 * <p><b>注意：非流式 FC 在同一个 Step 内闭环，不需要 FunctionCallingStep。</b>
 * 因为非流式没有 SSE emitter，不需要"推工具事件给前端 → 推文字给前端"的流程，
 * 直接内部搞定更简单。</p>
 */
@Component
@Order(30)  // 和 ModelCallStep 同 Order，通过 shouldSkip 互斥
public class SyncAiCallStep implements ChatChainStep {

    /** 共享的 OkHttpClient（OkHttpConfig Bean 注入） */
    private final OkHttpClient httpClient;

    /** 工具执行器 — 负责实际调用 Java 函数 */
    private final FunctionExecutor functionExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SyncAiCallStep(OkHttpClient okHttpClient, FunctionExecutor functionExecutor) {
        this.httpClient = okHttpClient;
        this.functionExecutor = functionExecutor;
    }

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        // 流式请求？跳过！让 ModelCallStep 来处理
        return ctx.isStreamMode();
    }

    @Override
    public void process(ChatContext ctx) {
        try {
            AiModel model = ctx.getModel();
            List<Map<String, String>> messages = ctx.getRequestMessages();
            List<Map<String, Object>> toolsJson = ctx.getToolsJson();

            // 构建请求体（stream=false 或不传 stream 参数）
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model.getModelCode());
            requestBody.put("messages", messages);
            if (toolsJson != null && !toolsJson.isEmpty()) {
                requestBody.put("tools", toolsJson);
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 发起同步调用（内部自动处理 FC）
            String reply = syncCallAi(model, jsonBody, messages);

            // 写入 fullReply → RecordSaveStep 会用它保存到数据库
            ctx.setFullReply(new StringBuilder(reply));
            System.out.println("[SyncAiCall] 同步调用完成，回复长度: " + reply.length());

        } catch (Exception e) {
            System.err.println("[SyncAiCall] 异常: " + e.getMessage());
            e.printStackTrace();
            ctx.setFullReply(new StringBuilder());  // 出错给空回复
        }
    }

    // ════════════════════ 同步 AI 调用 ════════════════════

    /**
     * 同步调用 AI API
     *
     * <p>和流式调用的关键区别：
     * <ul>
     *   <li>不传 stream=true</li>
     *   <li>一次拿到完整 JSON 响应</li>
     *   <li>tool_calls 是完整结构，不需要碎片收集</li>
     * </ul>
     *
     * @param model            模型配置（URL、Key、modelCode）
     * @param jsonBody         请求体的 JSON 字符串
     * @param originalMessages 原始消息列表（FC 二次请求时需要基于此构建）
     * @return AI 回复的文本内容
     */
    private String syncCallAi(AiModel model, String jsonBody,
                               List<Map<String, String>> originalMessages) throws Exception {
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(model.getApiUrl())
                .post(okhttp3.RequestBody.create(jsonBody,
                        okhttp3.MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + model.getApiKey())
                .build();

        try (okhttp3.Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("API 返回错误码: " + response.code());
            }

            // 反序列化完整响应
            String responseBody = response.body().string();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode assistantMsg = rootNode.path("choices").get(0).path("message");

            // ──────── 分支A：AI 要求调用工具？────────
            // 非流式的 tool_calls 是完整 JSON，不需要碎片拼接
            if (assistantMsg.has("tool_calls") && !assistantMsg.get("tool_calls").isEmpty()) {
                String reasoningContent = assistantMsg.path("reasoning_content").asText(null);
                return handleToolCalls(model, originalMessages,
                        assistantMsg.get("tool_calls"), reasoningContent);
            }

            // ──────── 分支B：普通回答 ────────
            // content 字段就是 AI 的回复文本
            return assistantMsg.path("content").asText("");
        }
    }

    // ════════════════════ Function Calling 处理 ════════════════════

    /**
     * 处理工具调用：执行 → 组装 → 二次请求 → 返回最终回答
     *
     * <h3>消息构建过程</h3>
     * <pre>
     * 原始 messages（List&lt;Map&lt;String,String&gt;&gt;）:
     *   [{role:system, content:"你是一个助手"},
     *    {role:user, content:"明天北京天气怎么样？"}]
     *
     * ↓ 转成可变的 List&lt;Map&lt;String,Object&gt;&gt;（因为 FC 消息的 content 可以是 null）
     *
     * ↓ 追加 assistant 消息（含 tool_calls）:
     *   {role:assistant, content:null, tool_calls:[{id:"call_1", function:{name:"get_weather", arguments:"..."}}]}
     *
     * ↓ 追加 tool 消息（工具执行结果）:
     *   {role:tool, tool_call_id:"call_1", name:"get_weather", content:"28°C，晴"}
     *
     * ↓ 第二次请求（不带 tools，避免无限循环调用工具）
     *
     * ↓ 返回 AI 最终自然语言回答
     * </pre>
     */
    private String handleToolCalls(AiModel model, List<Map<String, String>> messages,
                                    JsonNode toolCalls, String reasoningContent) throws Exception {
        // ──── ① 转成可变版本 ────
        // FC 需要追加 assistant 消息（content=null）和 tool 消息，原始的 String, String 装不下
        List<Map<String, Object>> mutableMessages = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            mutableMessages.add(new HashMap<>(msg));
        }

        // ──── ② 加入 assistant 的 tool_calls 消息 ────
        Map<String, Object> assistantMsgMap = new HashMap<>();
        assistantMsgMap.put("role", "assistant");
        assistantMsgMap.put("content", null);
        if (reasoningContent != null) {
            assistantMsgMap.put("reasoning_content", reasoningContent);
        }

        List<Map<String, Object>> tcList = new ArrayList<>();
        for (JsonNode tc : toolCalls) {
            Map<String, Object> tcMap = new HashMap<>();
            tcMap.put("id", tc.path("id").asText());
            tcMap.put("type", tc.path("type").asText());
            Map<String, Object> funcMap = new HashMap<>();
            funcMap.put("name", tc.path("function").path("name").asText());
            funcMap.put("arguments", tc.path("function").path("arguments").asText());
            tcMap.put("function", funcMap);
            tcList.add(tcMap);
        }
        assistantMsgMap.put("tool_calls", tcList);
        mutableMessages.add(assistantMsgMap);

        // ──── ③ 逐一执行工具 ────
        for (JsonNode toolCall : toolCalls) {
            String toolName = toolCall.path("function").path("name").asText();
            String argsJson = toolCall.path("function").path("arguments").asText();
            String callId = toolCall.path("id").asText();

            // 解析参数 → 执行函数 → 拿到结果
            @SuppressWarnings("unchecked")
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            System.out.println("[SyncAiCall FC] 执行工具: " + toolName);
            String toolResult = functionExecutor.execute(toolName, args);

            // 结果以 "tool" 角色消息加入，tool_call_id 关联到上面的 assistant 消息
            Map<String, Object> toolResultMsg = new HashMap<>();
            toolResultMsg.put("role", "tool");
            toolResultMsg.put("tool_call_id", callId);
            toolResultMsg.put("name", toolName);
            toolResultMsg.put("content", toolResult);
            mutableMessages.add(toolResultMsg);
        }

        // ──── ④ 第二次请求（不带 tools 参数）────────
        // 注意：不带 tools！否则 AI 可能再次返回 tool_calls，造成死循环
        Map<String, Object> secondBody = new HashMap<>();
        secondBody.put("model", model.getModelCode());
        secondBody.put("messages", mutableMessages);
        // 不传 "tools" 字段 = AI 只能返回普通文本

        String jsonBody = objectMapper.writeValueAsString(secondBody);
        okhttp3.Request secondRequest = new okhttp3.Request.Builder()
                .url(model.getApiUrl())
                .post(okhttp3.RequestBody.create(jsonBody,
                        okhttp3.MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + model.getApiKey())
                .build();

        try (okhttp3.Response response2 = httpClient.newCall(secondRequest).execute()) {
            if (!response2.isSuccessful()) {
                throw new RuntimeException("FC 第二轮 API 错误: " + response2.code());
            }
            JsonNode finalRoot = objectMapper.readTree(response2.body().string());
            String finalAnswer = finalRoot.path("choices").get(0)
                    .path("message").path("content").asText("");
            System.out.println("[SyncAiCall FC] 最终回答: "
                    + finalAnswer.substring(0, Math.min(100, finalAnswer.length())) + "...");
            return finalAnswer;
        }
    }
}
