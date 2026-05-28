package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.context.ChatContext;
import com.chatclow.dto.SseEvent;
import com.chatclow.entity.AiModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 第3步（流式路径）：调用 AI 模型 — 流式模式（@Order = 30）
 *
 * <h3>这个 Step 只负责三件事</h3>
 * <ol>
 *   <li>发 conversationId 给前端（让前端知道这次对话的 ID）</li>
 *   <li>从 ctx 取 messages + tools，拼成请求体</li>
 *   <li>用 OkHttp 发起流式调用 — SSE 逐字推给前端</li>
 * </ol>
 *
 * <h3>和 SyncAiCallStep 的关系：互斥</h3>
 * <pre>
 * mustSkip = !isStreamMode
 *
 * 流式请求：ModelCallStep 执行，SyncAiCallStep 跳过
 * 非流请求：ModelCallStep 跳过，SyncAiCallStep 执行
 *
 * 两个类都是 @Order(30)，通过 shouldSkip 实现"二选一"
 * 而不是 if-else 写死在一个类里
 * </pre>
 *
 * <h3>流式调用是怎么工作的？</h3>
 * <p>AI API 支持 SSE（Server-Sent Events）协议。请求头里加 stream=true，
 * 响应会一行一行返回 "data: {...}" 格式的 JSON。不是一次性返回完整结果，
 * 而是逐 token 推送 "delta" 增量。</p>
 *
 * <h3>tool_calls 流式碎片处理</h3>
 * <p>流式模式下 tool_calls 是分片到达的，不像非流式一次性返回完整 JSON。
 * 需要用 StreamToolCall 结构按 index 收集碎片，收集完再写入 ctx 交给
 * FunctionCallingStep 处理。</p>
 *
 * <h3>OkHttpClient 注入</h3>
 * <p>使用 OkHttpConfig 提供的共享 Bean，不再自己 new。所有 Step 和 Service
 * 共用同一个连接池，避免资源浪费。</p>
 *
 * @see SyncAiCallStep       非流式路径，同 Order 互斥
 * @see FunctionCallingStep  检测到 tool_calls 后的下一步
 */
@Component
@Order(30)
public class ModelCallStep implements ChatChainStep {

    /** 共享的 OkHttpClient（OkHttpConfig Bean 注入） */
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModelCallStep(OkHttpClient okHttpClient) {
        this.httpClient = okHttpClient;
    }

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        // 非流式请求？跳过！让 SyncAiCallStep 来处理
        return !ctx.isStreamMode();
    }

    @Override
    public void process(ChatContext ctx) {
        try {
            // ──────── ① 先把 conversationId 推给前端 ────────
            // 前端需要知道这次对话的 ID，后面发送消息时才能续写到同一个会话里
            ctx.getEmitter().send(
                    org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().data(
                            objectMapper.writeValueAsString(
                                    SseEvent.convId(ctx.getConversation().getId().toString()))
                    )
            );

            // ──────── ② 从 ctx 读取 MessageBuildStep 准备好的数据 ────────
            List<Map<String, String>> messages = ctx.getRequestMessages();
            List<Map<String, Object>> toolsJson = ctx.getToolsJson();

            // ──────── ③ 构建请求体 ────────
            // 格式参考 OpenAI API: {"model":"...", "stream":true, "messages":[...], "tools":[...]}
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ctx.getModel().getModelCode());
            requestBody.put("stream", true);    // 关键参数：开启流式
            requestBody.put("messages", messages);
            if (toolsJson != null && !toolsJson.isEmpty()) {
                requestBody.put("tools", toolsJson);
            }

            // ──────── ④ 发起流式调用 ────────
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            streamWithOkHttp(ctx.getModel(), jsonBody, ctx);

        } catch (Exception e) {
            System.err.println("[ModelCallStep] 异常: " + e.getMessage());
            e.printStackTrace();
            ctx.getEmitter().complete();  // 异常时也要关闭 emitter，否则前端一直等
        }
    }

    // ════════════════════ 流式调用核心逻辑 ════════════════════

    /**
     * 用 OkHttp 发起流式 SSE 请求，逐行解析 AI 返回的数据
     *
     * <h3>SSE 数据格式</h3>
     * <pre>
     * data: {"choices":[{"delta":{"content":"你好"}}]}
     * data: {"choices":[{"delta":{"content":"，今天"}}]}
     * data: [DONE]
     * </pre>
     *
     * <h3>tool_calls 流式特征</h3>
     * <p>tool_calls 在流式模式下是分片到达的：</p>
     * <pre>
     * data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_xxx","type":"function"}]}}]}
     * data: {"choices":[{"delta":{"tool_calls":[{"index":0,"function":{"name":"get_weather"}}]}}]}
     * data: {"choices":[{"delta":{"tool_calls":[{"index":0,"function":{"arguments":"{\"city\":"}}]}}]}
     * data: {"choices":[{"delta":{"tool_calls":[{"index":0,"function":{"arguments":"\"北京\"}"}}]}}]}
     * </pre>
     * <p>需要通过 index 把同一个工具调用的碎片拼起来。</p>
     */
    private void streamWithOkHttp(AiModel model, String jsonBody, ChatContext ctx) {
        try {
            // 构建 HTTP 请求
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(model.getApiUrl())
                    .post(okhttp3.RequestBody.create(jsonBody,
                            okhttp3.MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + model.getApiKey())
                    .build();

            okhttp3.Response response = httpClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                System.err.println("[SSE] API 错误: " + response.code());
                ctx.getEmitter().complete();
                return;
            }

            // ══ 流式解析开始 ══
            boolean hasToolCalls = false;                         // 这次回复是要调工具还是纯文字？
            Map<Integer, StreamToolCall> toolCallMap = new TreeMap<>();  // index → 工具调用碎片
            StringBuilder reasoningContent = new StringBuilder();        // deepseek-reasoner 推理内容

            try (okio.BufferedSource source = response.body().source()) {
                // 逐行读取 SSE 流
                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    if (line == null) break;

                    // SSE 协议：每行以 "data: " 开头
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();

                    // "[DONE]" 表示流结束
                    if ("[DONE]".equals(data)) break;

                    JsonNode node = objectMapper.readTree(data);
                    JsonNode choices = node.path("choices");
                    if (choices.size() > 0) {
                        JsonNode delta = choices.get(0).path("delta");

                        // ── 处理 deepseek-reasoner 的 reasoning_content ──
                        String rc = delta.path("reasoning_content").asText(null);
                        if (rc != null) reasoningContent.append(rc);

                        // ── 处理 tool_calls 分片 ──
                        // 通过 index 分组，同一个 index 的碎片拼到同一个 StreamToolCall
                        JsonNode toolCalls = delta.path("tool_calls");
                        if (toolCalls.isArray() && toolCalls.size() > 0) {
                            hasToolCalls = true;
                            for (JsonNode tc : toolCalls) {
                                int idx = tc.path("index").asInt();
                                // computeIfAbsent: 第一次出现这个 index → 创建新的 StreamToolCall
                                StreamToolCall stc = toolCallMap.computeIfAbsent(
                                        idx, k -> new StreamToolCall());
                                if (tc.has("id") && !tc.path("id").asText().isEmpty())
                                    stc.id = tc.path("id").asText();
                                if (tc.has("type") && !tc.path("type").asText().isEmpty())
                                    stc.type = tc.path("type").asText();
                                JsonNode fn = tc.path("function");
                                if (fn.has("name") && !fn.path("name").asText().isEmpty())
                                    stc.funcName += fn.path("name").asText();
                                if (fn.has("arguments") && !fn.path("arguments").asText().isEmpty())
                                    stc.funcArgs.append(fn.path("arguments").asText());
                            }
                            continue; // tool_calls 行没有普通 content，跳过
                        }

                        // ── 处理普通文本内容 ──
                        String content = delta.path("content").asText(null);
                        if (content != null && !content.isEmpty()) {
                            // 追加到 fullReply（后面 RecordSaveStep 需要完整的回复文本）
                            ctx.getFullReply().append(content);
                            try {
                                // 逐字推给前端：发送 SSE 事件
                                ctx.getEmitter().send(
                                        org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                                .data(objectMapper.writeValueAsString(
                                                        SseEvent.content(content)))
                                );
                            } catch (Exception ex) {
                                break; // 客户端断开了，停止推送
                            }
                        }
                    }
                }
            }

            // ══ 流解析结束。如果检测到工具调用，写入 ctx 交给 FunctionCallingStep ══
            if (hasToolCalls) {
                ctx.setReasoningContent(reasoningContent.toString());
                ctx.setToolCallsDetected(true);

                // StreamToolCall → ToolCallInfo
                List<ChatContext.ToolCallInfo> tciList = new ArrayList<>();
                for (StreamToolCall stc : toolCallMap.values()) {
                    tciList.add(new ChatContext.ToolCallInfo(
                            stc.id, stc.funcName, stc.funcArgs.toString()));
                }
                ctx.setToolCalls(tciList);
                System.out.println("[SSE] 检测到 " + tciList.size()
                        + " 个工具调用，交给 FunctionCallingStep");
            }

        } catch (Exception e) {
            System.err.println("[SSE] OkHttp 流读取异常: " + e.getMessage());
            ctx.getEmitter().complete();
        }
    }

    /**
     * 流式 tool_calls 的碎片收集器
     *
     * <p>为什么叫 StreamToolCall？因为流式模式下 tool_calls 不是一次性到达的。
     * 例如 "get_weather" 可能分两片：{"name":"get"} 和 {"name":"_weather"}，
     * 需要通过这个类把碎片拼成完整的名称。</p>
     */
    private static class StreamToolCall {
        String id;
        String type;
        String funcName = "";
        StringBuilder funcArgs = new StringBuilder();
    }
}
