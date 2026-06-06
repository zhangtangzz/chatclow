package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.context.ChatContext;
import com.chatclow.dto.SseEvent;
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
 * 第4步：Function Calling 工具执行（流式，@Order = 40）
 *
 * <pre>
 * 触发条件（全部满足才执行）：
 *   1. 流式模式（streamMode = true）
 *   2. ModelCallStep 检测到 tool_calls 并写入 ctx
 *
 * 上下文传递：
 *   [上一步] ModelCallStep → ctx.toolCalls, ctx.reasoningContent, ctx.requestMessages, ctx.model
 *   [本步骤] 执行工具 → 拼接 tool 结果消息 → 第二次流式请求 → ctx.fullReply 追加最终文本
 *   [下一步] RecordSaveStep → 读取 ctx.fullReply 保存数据库
 * </pre>
 */
@Component
@Order(40)
public class FunctionCallingStep implements ChatChainStep {

    private static final Logger log = LoggerFactory.getLogger(FunctionCallingStep.class);

    private final OkHttpClient httpClient;
    private final ToolCallHelper toolCallHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FunctionCallingStep(OkHttpClient okHttpClient, ToolCallHelper toolCallHelper) {
        this.httpClient = okHttpClient;
        this.toolCallHelper = toolCallHelper;
    }

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        return !ctx.isStreamMode()
                || !ctx.isToolCallsDetected()
                || ctx.getToolCalls() == null
                || ctx.getToolCalls().isEmpty();
    }

    @Override
    public void process(ChatContext ctx) {
        try {
            // ① 准备可变消息列表（含 assistant tool_calls 消息）
            List<Map<String, Object>> messages = toolCallHelper.prepareMessages(
                    ctx.getRequestMessages(), ctx.getToolCalls(), ctx.getReasoningContent());

            // ② 逐一执行工具，推送 SSE 事件给前端
            for (ChatContext.ToolCallInfo tci : ctx.getToolCalls()) {
                sendEvent(ctx, SseEvent.toolCall(tci.getFunctionName(), tci.getArguments()));

                Map<String, Object> toolResult = toolCallHelper.executeTool(tci);
                messages.add(toolResult);

                sendEvent(ctx, SseEvent.toolResult(tci.getFunctionName(),
                        (String) toolResult.get("content")));
            }

            // ③ 第二次流式请求（不带 tools 参数，AI 返回自然语言）
            Map<String, Object> secondBody = new HashMap<>();
            secondBody.put("model", ctx.getModel().getModelCode());
            secondBody.put("messages", messages);
            secondBody.put("stream", true);
            putModelParams(secondBody, ctx.getModel());

            streamSecondResponse(ctx, secondBody);

        } catch (Exception e) {
            log.error("[FC Step] 处理失败", e);
            ctx.getEmitter().complete();
        }
    }

    // ──── 第二次流式请求，逐字推送给前端 ────

    private void streamSecondResponse(ChatContext ctx, Map<String, Object> body) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(body);

        Request request = new Request.Builder()
                .url(ctx.getModel().getApiUrl())
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + ctx.getModel().getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("[FC Step] API 错误: {}", response.code());
                ctx.getEmitter().complete();
                return;
            }

            try (BufferedSource source = response.body().source()) {
                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    if (line == null) break;
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;

                    try {
                        JsonNode node = objectMapper.readTree(data);

                        // 捕获第二次请求的 token 消耗，累加到第一次
                        int chunkTokens = node.path("usage").path("total_tokens").asInt(0);
                        if (chunkTokens > 0) {
                            int firstTokens = ctx.getTokenUsage() != null ? ctx.getTokenUsage() : 0;
                            ctx.setTokenUsage(firstTokens + chunkTokens);
                        }

                        JsonNode choices = node.path("choices");
                        if (choices.size() > 0) {
                            String content = choices.get(0).path("delta").path("content").asText(null);
                            if (content != null && !content.isEmpty()) {
                                ctx.getFullReply().append(content);
                                try {
                                    ctx.getEmitter().send(SseEmitterEvent(SseEvent.content(content)));
                                } catch (Exception ex) {
                                    break; // 客户端断开
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 坏行跳过
                    }
                }
            }
        }
    }

    private void sendEvent(ChatContext ctx, SseEvent event) throws Exception {
        ctx.getEmitter().send(SseEmitterEvent(event));
    }

    private void putModelParams(Map<String, Object> body, com.chatclow.entity.AiModel model) {
        if (model.getTemperature() != null) body.put("temperature", model.getTemperature());
        if (model.getMaxTokens() != null) body.put("max_tokens", model.getMaxTokens());
        if (model.getTopP() != null) body.put("top_p", model.getTopP());
    }

    private org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder
            SseEmitterEvent(SseEvent event) throws Exception {
        return org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                .data(objectMapper.writeValueAsString(event));
    }
}
