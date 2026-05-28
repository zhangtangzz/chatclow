package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.context.ChatContext;
import com.chatclow.context.ChatContext.ToolCallInfo;
import com.chatclow.dto.SseEvent;
import com.chatclow.service.FunctionExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第4步：Function Calling 工具执行（流式模式专用，@Order = 40）
 *
 * <h3>这个 Step 什么时候执行？</h3>
 * <pre>
 * 条件（全部满足才执行）：
 *   1. 流式模式（isStreamMode = true）
 *   2. AI 第一次回复检测到了 tool_calls（toolCallsDetected = true）
 *
 * 跳过场景：
 *   - 非流式模式 → SyncAiCallStep 内部处理了 FC，不需要这个 Step
 *   - AI 没要求调工具 → 纯文本问答，跳过
 * </pre>
 *
 * <h3>和 SyncAiCallStep 中 FC 处理的区别</h3>
 * <table>
 *   <tr><th></th><th>FunctionCallingStep（流式）</th><th>SyncAiCallStep（非流式）</th></tr>
 *   <tr><td>工具执行</td><td>通过 FunctionExecutor 执行</td><td>通过 FunctionExecutor 执行（相同）</td></tr>
 *   <tr><td>通知前端</td><td>✅ SSE 推送 tool_call 和 tool_result 事件</td><td>❌ 无需通知前端</td></tr>
 *   <tr><td>第二次请求</td><td>流式，逐字推答案</td><td>同步，一次性拿答案</td></tr>
 *   <tr><td>回复累积</td><td>追加到 fullReply</td><td>直接 setFullReply</td></tr>
 * </table>
 *
 * <h3>tool_calls 数据来源</h3>
 * <p>由 ModelCallStep 在 SSE 流解析阶段收集并写入 ctx：</p>
 * <ul>
 *   <li>ctx.toolCallsDetected — "有工具调用"标志位</li>
 *   <li>ctx.toolCalls — List&lt;ToolCallInfo&gt;，每条包含 id、functionName、arguments</li>
 *   <li>ctx.reasoningContent — deepseek-reasoner 的推理内容（FC 二次请求必须回传）</li>
 * </ul>
 *
 * <h3>为什么要独立成 Step？</h3>
 * <p>重构前，FC 逻辑是 ChatServiceImpl 的私有方法 handleStreamToolCalls()。
 * 它依赖 emitter、fullReply、modelCode 等多个字段，逻辑纠缠不清。
 * 独立成 Step 后职责单一：只负责"拿到工具调用信息 → 执行 → 二次请求"。</p>
 */
@Component
@Order(40)
public class FunctionCallingStep implements ChatChainStep {

    @Autowired
    private FunctionExecutor functionExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 共享的 OkHttpClient（OkHttpConfig Bean，不是本地 new） */
    private final OkHttpClient httpClient;

    public FunctionCallingStep(OkHttpClient okHttpClient) {
        this.httpClient = okHttpClient;
    }

    @Override
    public void process(ChatContext ctx) {
        try {
            // ════════ ① 准备消息列表（基于原始 messages 重建） ════════
            // 需要从 String,String 转成 Object，因为 assistant 消息的 content 是 null
            List<Map<String, String>> messagesStr = ctx.getRequestMessages();
            List<Map<String, Object>> messages = new ArrayList<>();
            for (Map<String, String> msg : messagesStr) {
                messages.add(new HashMap<>(msg));
            }

            // ════════ ② 构建 assistant 消息（含 tool_calls） ════════
            Map<String, Object> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", null);  // 工具调用时 content 必须是 null

            // deepseek-reasoner 特殊处理：推理内容必须回传给 API
            String reasoningContent = ctx.getReasoningContent();
            if (reasoningContent != null && !reasoningContent.isEmpty()) {
                assistantMsg.put("reasoning_content", reasoningContent);
            }

            // ToolCallInfo → OpenAI tool_calls 格式
            List<Map<String, Object>> tcList = new ArrayList<>();
            for (ToolCallInfo tci : ctx.getToolCalls()) {
                Map<String, Object> tc = new HashMap<>();
                tc.put("id", tci.getId());
                tc.put("type", "function");
                Map<String, Object> funcMap = new HashMap<>();
                funcMap.put("name", tci.getFunctionName());
                funcMap.put("arguments", tci.getArguments());
                tc.put("function", funcMap);
                tcList.add(tc);
            }
            assistantMsg.put("tool_calls", tcList);
            messages.add(assistantMsg);

            // ════════ ③ 逐一执行工具，结果以 role="tool" 消息加入 ════════
            for (ToolCallInfo tci : ctx.getToolCalls()) {
                System.out.println("[FC Step] 执行工具: " + tci.getFunctionName()
                        + ", 参数: " + tci.getArguments());

                // ── 通知前端：开始调用工具 ──
                ctx.getEmitter().send(SseEmitterEvent(
                        SseEvent.toolCall(tci.getFunctionName(), tci.getArguments())
                ));

                // ── 解析参数 → 执行函数 → 拿到结果 ──
                @SuppressWarnings("unchecked")
                Map<String, Object> args = objectMapper.readValue(
                        tci.getArguments(), Map.class);
                String result = functionExecutor.execute(tci.getFunctionName(), args);

                // ── 通知前端：工具执行完成 ──
                ctx.getEmitter().send(SseEmitterEvent(
                        SseEvent.toolResult(tci.getFunctionName(), result)
                ));

                // ── 构建 tool 角色消息 ──
                Map<String, Object> toolResult = new HashMap<>();
                toolResult.put("role", "tool");
                toolResult.put("tool_call_id", tci.getId());     // 关联到 assistant 消息
                toolResult.put("name", tci.getFunctionName());
                toolResult.put("content", result);
                messages.add(toolResult);

                System.out.println("[FC Step] 工具结果: " + result);
            }

            // ════════ ④ 第二次流式请求（不带 tools，避免死循环） ════════
            Map<String, Object> secondBody = new HashMap<>();
            secondBody.put("model", ctx.getModel().getModelCode());
            secondBody.put("messages", messages);
            secondBody.put("stream", true);     // 流式推最终答案
            // ⚠️ 注意：不传 "tools" 参数 = AI 只能返回自然语言

            System.out.println("[FC Step] 开始第二次流式请求...");
            streamSecondResponse(ctx, secondBody);
            System.out.println("[FC Step] 第二次流式请求完成");

        } catch (Exception e) {
            System.err.println("[FC Step] 工具调用处理失败: " + e.getMessage());
            e.printStackTrace();
            ctx.getEmitter().complete();
        }
    }

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        // 三个条件缺一不可：流式 + 有 tool_calls + tool_calls 非空
        return !ctx.isStreamMode()
                || !ctx.isToolCallsDetected()
                || ctx.getToolCalls() == null
                || ctx.getToolCalls().isEmpty();
    }

    // ════════════════════ 第二次流式请求 ════════════════════

    /**
     * 工具执行完成后的第二次流式请求
     *
     * <p>这次请求不带 tools 参数，AI 会基于工具执行结果给出最终的自然语言回答。
     * 和 ModelCallStep 的流式调用类似，逐字推送给前端。</p>
     *
     * <p>回复累积到 ctx.fullReply，和 ModelCallStep 的第一次回复拼接在一起。</p>
     */
    private void streamSecondResponse(ChatContext ctx, Map<String, Object> requestBodyMap)
            throws Exception {
        String jsonBody = objectMapper.writeValueAsString(requestBodyMap);

        Request request = new Request.Builder()
                .url(ctx.getModel().getApiUrl())
                .post(RequestBody.create(jsonBody,
                        MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + ctx.getModel().getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("[FC Step] API 错误: " + response.code());
                ctx.getEmitter().complete();
                return;
            }

            // 逐行解析 SSE 流（和 ModelCallStep 一样的工作原理）
            try (BufferedSource source = response.body().source()) {
                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    if (line == null) break;
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        JsonNode choices = node.path("choices");
                        if (choices.size() > 0) {
                            String content = choices.get(0).path("delta")
                                    .path("content").asText(null);
                            if (content != null && !content.isEmpty()) {
                                // 追加到 fullReply（紧接第一次回复）
                                ctx.getFullReply().append(content);
                                try {
                                    // 推送单字给前端
                                    ctx.getEmitter().send(
                                            SseEmitterEvent(SseEvent.content(content))
                                    );
                                } catch (Exception ex) {
                                    break; // 客户端已断开
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 坏行跳过，不能因为一行 JSON 解析失败就挂掉整个流程
                    }
                }
            }
        }
    }

    // ════════════════════ 辅助方法 ════════════════════

    /**
     * SseEvent 对象 → SseEmitter 事件格式的便捷转换
     *
     * <p>减少重复代码：每次 push 事件给前端都要 event().data(objectMapper.writeValueAsString(...))
     * 这个快捷方法把这两步合并了。</p>
     */
    private org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder
            SseEmitterEvent(SseEvent event) throws Exception {
        return org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                .data(objectMapper.writeValueAsString(event));
    }
}
