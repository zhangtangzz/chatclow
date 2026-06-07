package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.context.ChatContext;
import com.chatclow.dto.SseEvent;
import com.chatclow.entity.AiModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 第3步（流式路径）：SSE 流式调用 AI 模型（@Order = 30）
 *
 * <pre>
 * 与 SyncAiCallStep 互斥（同 @Order(30)，通过 shouldSkip 二选一）：
 *   流式  → 本 Step 执行，SyncAiCallStep 跳过
 *   非流式 → 本 Step 跳过，SyncAiCallStep 执行
 *
 * 输入参数来源：
 *   MessageBuildStep → ctx.requestMessages, ctx.toolsJson, ctx.model
 *
 * 本步骤产出：
 *   ctx.fullReply        — 逐字累积 AI 回复
 *   ctx.toolCallsDetected — 检测到 tool_calls 则 set，给 FunctionCallingStep 用
 *   ctx.toolCalls         — 工具调用碎片拼装结果
 *   ctx.reasoningContent  — deepseek-reasoner 推理内容
 *   ctx.tokenUsage        — API 返回的 token 消耗
 *   SSE emitter           — 逐字推送给前端
 *
 * 调用方法：
 *   httpClient.newCall()  → OkHttp 流式 SSE 请求
 *   逐行解析 SSE 数据      → 普通文本追加到 fullReply + 推送前端
 *                        → tool_calls 碎片按 index 拼装到 StreamToolCall
 * </pre>
 */
@Component
@Order(30)
public class ModelCallStep implements ChatChainStep {

    private static final Logger log = LoggerFactory.getLogger(ModelCallStep.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModelCallStep(OkHttpClient okHttpClient) {
        this.httpClient = okHttpClient;
    }

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        return !ctx.isStreamMode();
    }

    @Override
    public void process(ChatContext ctx) {
        ctx.setStartTime(System.currentTimeMillis());
        try {
            // ① 先推 conversationId 给前端
            ctx.getEmitter().send(
                    SseEmitterEvent(SseEvent.convId(ctx.getConversation().getId().toString())));

            // ② 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ctx.getModel().getModelCode());
            requestBody.put("stream", true);
            requestBody.put("messages", ctx.getRequestMessages());
            putModelParams(requestBody, ctx.getModel());
            if (ctx.getToolsJson() != null && !ctx.getToolsJson().isEmpty()) {
                requestBody.put("tools", ctx.getToolsJson());
            }

            // ③ 流式调用
            streamWithOkHttp(ctx, objectMapper.writeValueAsString(requestBody));

        } catch (Exception e) {
            log.error("[ModelCallStep] 异常", e);
            ctx.getEmitter().complete();
        } finally {
            ctx.setResponseTime(System.currentTimeMillis() - ctx.getStartTime());
        }
    }

    private void streamWithOkHttp(ChatContext ctx, String jsonBody) {
        try {
            Request request = new Request.Builder()
                    .url(ctx.getModel().getApiUrl())
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + ctx.getModel().getApiKey())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("[SSE] API 错误: {}", response.code());
                    ctx.getEmitter().complete();
                    return;
                }

                boolean hasToolCalls = false;
                Map<Integer, StreamToolCall> toolCallMap = new TreeMap<>();
                StringBuilder reasoningContent = new StringBuilder();

                try (BufferedSource source = response.body().source()) {
                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null) break;
                        if (!line.startsWith("data: ")) continue;
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;

                        JsonNode node = objectMapper.readTree(data);

                        // 捕获 token 消耗（流式模式下 usage 通常在最后的 chunk 中）
                        int chunkTokens = node.path("usage").path("total_tokens").asInt(0);
                        if (chunkTokens > 0) ctx.setTokenUsage(chunkTokens);

                        JsonNode choices = node.path("choices");
                        if (choices.size() > 0) {
                            JsonNode delta = choices.get(0).path("delta");

                            // deepseek-reasoner 推理内容
                            String rc = delta.path("reasoning_content").asText(null);
                            if (rc != null) reasoningContent.append(rc);

                            // tool_calls 碎片收集（按 index 拼装）
                            JsonNode toolCalls = delta.path("tool_calls");
                            if (toolCalls.isArray() && toolCalls.size() > 0) {
                                hasToolCalls = true;
                                for (JsonNode tc : toolCalls) {
                                    int idx = tc.path("index").asInt();
                                    StreamToolCall stc = toolCallMap.computeIfAbsent(idx, k -> new StreamToolCall());
                                    if (tc.has("id") && !tc.path("id").asText().isEmpty())
                                        stc.id = tc.path("id").asText();
                                    if (tc.has("type") && !tc.path("type").asText().isEmpty())
                                        stc.type = tc.path("type").asText();
                                    JsonNode fn = tc.path("function");
                                    if (fn.path("name").isTextual())
                                        stc.funcName += fn.path("name").asText();
                                    if (fn.path("arguments").isTextual())
                                        stc.funcArgs.append(fn.path("arguments").asText());
                                }
                                continue;
                            }

                            // 普通文本 → 逐字推送
                            String content = delta.path("content").asText(null);
                            if (content != null && !content.isEmpty()) {
                                ctx.getFullReply().append(content);
                                try {
                                    ctx.getEmitter().send(SseEmitterEvent(SseEvent.content(content)));
                                } catch (Exception ex) {
                                    break; // 客户端断开
                                }
                            }
                        }
                    }
                }

                if (hasToolCalls) {
                    ctx.setReasoningContent(reasoningContent.toString());
                    ctx.setToolCallsDetected(true);
                    List<ChatContext.ToolCallInfo> tciList = new ArrayList<>();
                    for (StreamToolCall stc : toolCallMap.values()) {
                        tciList.add(new ChatContext.ToolCallInfo(stc.id, stc.funcName, stc.funcArgs.toString()));
                    }
                    ctx.setToolCalls(tciList);
                }

            }
        } catch (Exception e) {
            log.error("[SSE] OkHttp 流读取异常", e);
            ctx.getEmitter().complete();
        }
    }

    private org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder
            SseEmitterEvent(SseEvent event) throws Exception {
        return org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                .data(objectMapper.writeValueAsString(event));
    }

    /** 流式 tool_calls 碎片收集器（按 index 把 name/arguments 分片拼成完整值） */
    private static class StreamToolCall {
        String id;
        String type;
        String funcName = "";
        StringBuilder funcArgs = new StringBuilder();
    }

    private void putModelParams(Map<String, Object> body, AiModel model) {
        if (model.getTemperature() != null) body.put("temperature", model.getTemperature());
        if (model.getMaxTokens() != null) body.put("max_tokens", model.getMaxTokens());
        if (model.getTopP() != null) body.put("top_p", model.getTopP());
    }
}
