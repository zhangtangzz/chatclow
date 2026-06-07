package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.context.ChatContext;
import com.chatclow.entity.AiModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 第3步（非流式路径）：同步调用 AI + Function Calling（@Order = 30）
 *
 * <pre>
 * 与 ModelCallStep 互斥（同 @Order(30)，通过 shouldSkip 二选一）：
 *   流式  → ModelCallStep 执行，本 Step 跳过
 *   非流式 → 本 Step 执行，ModelCallStep 跳过
 *
 * 上下文传递：
 *   [上一步] MessageBuildStep → ctx.requestMessages, ctx.toolsJson
 *   [本步骤] 同步调 AI → 有 tool_calls 则执行后二次请求 → ctx.fullReply
 *   [下一步] RecordSaveStep → 读取 ctx.fullReply
 * </pre>
 */
@Component
@Order(30)
public class SyncAiCallStep implements ChatChainStep {

    private static final Logger log = LoggerFactory.getLogger(SyncAiCallStep.class);

    private final OkHttpClient httpClient;
    private final ToolCallHelper toolCallHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SyncAiCallStep(OkHttpClient okHttpClient, ToolCallHelper toolCallHelper) {
        this.httpClient = okHttpClient;
        this.toolCallHelper = toolCallHelper;
    }

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        return ctx.isStreamMode();
    }

    @Override
    public void process(ChatContext ctx) {
        ctx.setStartTime(System.currentTimeMillis());
        try {
            AiModel model = ctx.getModel();
            List<Map<String, Object>> messages = ctx.getRequestMessages();
            List<Map<String, Object>> toolsJson = ctx.getToolsJson();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model.getModelCode());
            requestBody.put("messages", messages);
            putModelParams(requestBody, model);
            if (toolsJson != null && !toolsJson.isEmpty()) {
                requestBody.put("tools", toolsJson);
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            String reply = syncCallAi(ctx, model, jsonBody, messages);
            ctx.setFullReply(new StringBuilder(reply));

        } catch (Exception e) {
            log.error("[SyncAiCall] 异常", e);
            ctx.setFullReply(new StringBuilder());
        } finally {
            ctx.setResponseTime(System.currentTimeMillis() - ctx.getStartTime());
        }
    }

    /**
     * 同步调用 AI，自动处理 tool_calls（一次或两次请求）
     *
     * @param model            模型配置
     * @param jsonBody         请求 JSON
     * @param originalMessages 原始消息列表（FC 二次请求需要基于此重建）
     */
    private String syncCallAi(ChatContext ctx, AiModel model, String jsonBody,
                               List<Map<String, Object>> originalMessages) throws Exception {
        Request request = new Request.Builder()
                .url(model.getApiUrl())
                .post(RequestBody.create(jsonBody,
                        MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + model.getApiKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("API 返回错误码: " + response.code());
            }

            JsonNode rootNode = objectMapper.readTree(response.body().string());
            JsonNode assistantMsg = rootNode.path("choices").get(0).path("message");

            // 捕获第一次请求的 token 消耗
            int firstTokens = rootNode.path("usage").path("total_tokens").asInt();

            if (assistantMsg.has("tool_calls") && !assistantMsg.get("tool_calls").isEmpty()) {
                String reasoningContent = assistantMsg.path("reasoning_content").asText(null);
                return handleToolCalls(ctx, model, originalMessages,
                        assistantMsg.get("tool_calls"), reasoningContent, firstTokens);
            }

            ctx.setTokenUsage(firstTokens);
            return assistantMsg.path("content").asText("");
        }
    }

    /**
     * 工具调用闭环：解析 → 执行 → 二次请求
     *
     * <p>ToolCallHelper 负责消息构建和工具执行，本方法只负责编排流程。</p>
     */
    private String handleToolCalls(ChatContext ctx, AiModel model,
                                    List<Map<String, Object>> originalMessages,
                                    JsonNode toolCallsNode, String reasoningContent,
                                    int firstTokens) throws Exception {
        // ① 从 OpenAI 格式转成内部 ToolCallInfo 列表
        List<ChatContext.ToolCallInfo> tciList = new ArrayList<>();
        for (JsonNode tc : toolCallsNode) {
            tciList.add(new ChatContext.ToolCallInfo(
                    tc.path("id").asText(),
                    tc.path("function").path("name").asText(),
                    tc.path("function").path("arguments").asText()));
        }

        // ② 用 ToolCallHelper 准备可变消息 + 执行所有工具
        List<Map<String, Object>> messages = toolCallHelper.prepareMessages(
                originalMessages, tciList, reasoningContent);

        for (ChatContext.ToolCallInfo tci : tciList) {
            messages.add(toolCallHelper.executeTool(tci));
        }

        // ③ 第二次请求（不带 tools）
        Map<String, Object> secondBody = new HashMap<>();
        secondBody.put("model", model.getModelCode());
        secondBody.put("messages", messages);
        putModelParams(secondBody, model);

        String jsonBody = objectMapper.writeValueAsString(secondBody);
        Request secondRequest = new Request.Builder()
                .url(model.getApiUrl())
                .post(RequestBody.create(jsonBody,
                        MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + model.getApiKey())
                .build();

        try (Response response2 = httpClient.newCall(secondRequest).execute()) {
            if (!response2.isSuccessful()) {
                throw new RuntimeException("FC 第二轮 API 错误: " + response2.code());
            }
            JsonNode finalRoot = objectMapper.readTree(response2.body().string());
            int secondTokens = finalRoot.path("usage").path("total_tokens").asInt();
            ctx.setTokenUsage(firstTokens + secondTokens);
            return finalRoot.path("choices").get(0).path("message").path("content").asText("");
        }
    }

    private void putModelParams(Map<String, Object> body, AiModel model) {
        if (model.getTemperature() != null) body.put("temperature", model.getTemperature());
        if (model.getMaxTokens() != null) body.put("max_tokens", model.getMaxTokens());
        if (model.getTopP() != null) body.put("top_p", model.getTopP());
    }
}
