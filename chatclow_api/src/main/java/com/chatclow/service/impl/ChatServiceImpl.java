package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.dto.SseEvent;
import com.chatclow.entity.*;
import com.chatclow.mapper.AiAgentMapper;
import com.chatclow.mapper.AiModelMapper;
import com.chatclow.mapper.AgentConversationMapper;
import com.chatclow.mapper.AgentConversationRecordMapper;
import com.chatclow.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.LocalDateTime;
import java.util.*;

import com.chatclow.dto.ChatResponse;

import java.util.stream.Collectors;

/**
 * AI 对话服务实现类
 */
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private AiAgentMapper aiAgentMapper;

    @Autowired
    private AiModelMapper aiModelMapper;

    @Autowired
    private AgentConversationMapper conversationMapper;

    @Autowired
    private AgentConversationRecordMapper recordMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorSearchService vectorSearchService;

    // ⭐ Function Calling 相关
    @Autowired
    private AiFunctionService aiFunctionService;

    @Autowired
    private FunctionExecutor functionExecutor;

    // 新：OkHttp 客户端（连接池 + 超时配置）
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(new okhttp3.ConnectionPool(10, 5, java.util.concurrent.TimeUnit.MINUTES))
            .build();

    // JSON 解析工具
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ChatResponse chat(Long agentId, Long userId, String message, Long conversationId) {
        // 1. 查出智能体配置
        AiAgent agent = aiAgentMapper.selectById(agentId);
        if (agent == null) {
            throw new RuntimeException("智能体不存在");
        }

        // 2. 查出模型配置
        AiModel model = aiModelMapper.selectById(agent.getModelId());
        if (model == null) {
            throw new RuntimeException("模型配置不存在");
        }

        // 3. 获取或复用会话
        AgentConversation conversation;
        if (conversationId != null) {
            conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                throw new RuntimeException("会话不存在");
            }
        } else {
            conversation = createNewConversation(userId);
        }

        // 4. 保存用户消息
        saveRecord(conversation.getId(), "user", message);

        // 5. 构建带历史上下文的 messages 数组
        LambdaQueryWrapper<AgentConversationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConversationRecord::getConversationId, conversation.getId());
        wrapper.orderByAsc(AgentConversationRecord::getCreatedDt);
        List<AgentConversationRecord> history = recordMapper.selectList(wrapper);

        // ===== RAG 检索增强 =====
        String ragContext = "";
        if (agent.getKbEnabled() != null && agent.getKbEnabled() == 1
                && agent.getKbId() != null) {
            float[] questionVector = embeddingService.embed(message);
            List<RagChunk> relatedChunks = vectorSearchService.search(
                    questionVector, agent.getKbId(), 3);
            if (!relatedChunks.isEmpty()) {
                ragContext = relatedChunks.stream()
                        .map(RagChunk::getContent)
                        .collect(Collectors.joining("\n\n"));
                System.out.println("[RAG] 检索到 " + relatedChunks.size() + " 条相关片段");
            }
        }

        // ⭐ 查询该智能体绑定的工具列表
        List<AiFunction> functions = aiFunctionService.listByAgentId(agentId);

        // 6. 调用 AI（带上 RAG 上下文 + 工具列表）
        String aiReply = callAiApi(model, agent.getSystemPrompt(), history, message, ragContext, functions);

        return new ChatResponse(aiReply, conversation.getId());
    }

    @Override
    public void chatStream(Long agentId, Long userId, String message,
                           Long conversationId, SseEmitter emitter) {
        try {
            // 1. 查出智能体配置
            AiAgent agent = aiAgentMapper.selectById(agentId);
            if (agent == null) {
                emitter.completeWithError(new RuntimeException("智能体不存在"));
                return;
            }

            // 2. 查出模型配置
            AiModel model = aiModelMapper.selectById(agent.getModelId());
            if (model == null) {
                emitter.completeWithError(new RuntimeException("模型配置不存在"));
                return;
            }

            // 3. 获取或复用会话
            AgentConversation conversation;
            if (conversationId != null) {
                conversation = conversationMapper.selectById(conversationId);
                if (conversation == null) {
                    emitter.completeWithError(new RuntimeException("会话不存在"));
                    return;
                }
            } else {
                conversation = createNewConversation(userId);
            }

            // ⭐ 先把 conversationId 推给前端
            emitter.send(SseEmitter.event().data(
                    objectMapper.writeValueAsString(SseEvent.convId(conversation.getId().toString()))
            ));

            // 4. 保存用户消息
            saveRecord(conversation.getId(), "user", message);

            // 5. 构建历史上下文
            LambdaQueryWrapper<AgentConversationRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AgentConversationRecord::getConversationId, conversation.getId());
            wrapper.orderByAsc(AgentConversationRecord::getCreatedDt);
            List<AgentConversationRecord> history = recordMapper.selectList(wrapper);

            // ===== RAG 检索增强 =====
            String ragContext = "";
            if (agent.getKbEnabled() != null && agent.getKbEnabled() == 1
                    && agent.getKbId() != null) {
                float[] questionVector = embeddingService.embed(message);
                List<RagChunk> relatedChunks = vectorSearchService.search(
                        questionVector, agent.getKbId(), 3);
                if (!relatedChunks.isEmpty()) {
                    ragContext = relatedChunks.stream()
                            .map(RagChunk::getContent)
                            .collect(Collectors.joining("\n\n"));
                    System.out.println("[RAG-Stream] 检索到 " + relatedChunks.size() + " 条相关片段");
                }
            }

            // ⭐ 查询该智能体绑定的工具列表
            List<AiFunction> functions = aiFunctionService.listByAgentId(agentId);

            // 6. 构建请求体
            JsonNode requestBody = buildStreamRequestBody(model, agent.getSystemPrompt(),
                    history, message, ragContext, functions);

            // 7. 发起流式 HTTP 请求，逐块推送给前端
            StringBuilder fullReply = new StringBuilder();
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            streamWithOkHttp(model, jsonBody, emitter, fullReply, functions, requestBody);

            // 8. 保存 AI 完整回复到数据库
            if (fullReply.length() > 0) {
                saveRecord(conversation.getId(), "assistant", fullReply.toString());
            }

            // 9. 发送完成事件 + 关闭连接
            emitter.send(SseEmitter.event().data(
                    objectMapper.writeValueAsString(SseEvent.done())
            ));
            emitter.complete();
            System.out.println("[SSE] 流式对话完成，回复长度: " + fullReply.length());

        } catch (Exception e) {
            System.err.println("[SSE] 流式对话异常: " + e.getMessage());
            // Apipost 不是真正的 SSE 客户端，连接断后 completeWithError 会触发序列化报错
            emitter.complete();
        }
    }


    /**
     * 新建会话
     */
    private AgentConversation createNewConversation(Long userId) {
        AgentConversation conversation = new AgentConversation();
        conversation.setUserId(userId);
        conversation.setTitle("对话 " + LocalDateTime.now().toString().substring(11, 16));
        conversationMapper.insert(conversation);
        return conversation;
    }

    private void saveRecord(Long conversationId, String role, String content) {
        AgentConversationRecord record = new AgentConversationRecord();
        record.setConversationId(conversationId);
        record.setRole(role);
        record.setContent(content);
        recordMapper.insert(record);
    }

    /**
     * 调用 AI API（支持多轮历史 + RAG + Function Calling）
     */
    private String callAiApi(AiModel model, String systemPrompt,
                             List<AgentConversationRecord> history,
                             String userMessage, String ragContext,
                             List<AiFunction> functions) {
        try {
            // ⭐ 复用 buildMessages()，不再手写消息构建
            List<Map<String, String>> messages = buildMessages(systemPrompt, history, userMessage, ragContext);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model.getModelCode());
            requestBody.put("messages", messages);

            // ⭐ 如果有工具，加入 tools 参数
            if (functions != null && !functions.isEmpty()) {
                requestBody.put("tools", buildTools(functions));
            }

            // 用 OkHttp 发送同步请求
            String jsonBody = objectMapper.writeValueAsString(requestBody);
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
                String responseBody = response.body().string();

                // 解析响应
                JsonNode rootNode = objectMapper.readTree(responseBody);
                JsonNode assistantMsg = rootNode.path("choices").get(0).path("message");

                // ⭐ 关键分支：AI 是否要求调用工具？
                if (assistantMsg.has("tool_calls")
                        && !assistantMsg.get("tool_calls").isEmpty()) {

                    // === 分支A：执行工具调用 ===
                    String reasoningContent = assistantMsg.path("reasoning_content").asText(null);
                    return handleToolCalls(model, functions, messages,
                            assistantMsg.get("tool_calls"), reasoningContent);

                } else {
                    // === 分支B：普通回答，直接返回 ===
                    return assistantMsg.path("content").asText("");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("调用 AI API 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 把 AiFunction 实体列表转成 DeepSeek API 要求的 tools[] JSON 格式
     */
    private List<Map<String, Object>> buildTools(List<AiFunction> functions) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (AiFunction func : functions) {
            Map<String, Object> tool = new HashMap<>();
            tool.put("type", "function");

            Map<String, Object> funcDef = new HashMap<>();
            funcDef.put("name", func.getName());
            funcDef.put("description", func.getDescription());

            if (func.getParameters() != null && !func.getParameters().isEmpty()) {
                try {
                    JsonNode paramsNode = objectMapper.readTree(func.getParameters());
                    funcDef.put("parameters", paramsNode);
                } catch (Exception e) {
                    funcDef.put("parameters", Map.of("type", "object",
                            "properties", new HashMap<>()));
                }
            } else {
                funcDef.put("parameters", Map.of("type", "object",
                        "properties", new HashMap<>()));
            }

            tool.put("function", funcDef);
            tools.add(tool);
        }
        return tools;
    }

    /**
     * 构建消息列表（同步/流式共用）
     */
    private List<Map<String, String>> buildMessages(String systemPrompt,
                                                    List<AgentConversationRecord> history,
                                                    String userMessage, String ragContext) {
        List<Map<String, String>> messages = new ArrayList<>();

        String finalSystemPrompt = systemPrompt;
        if (ragContext != null && !ragContext.isEmpty()) {
            finalSystemPrompt = systemPrompt
                    + "\n\n【参考知识库内容】\n"
                    + ragContext
                    + "\n\n请基于以上知识库内容回答用户问题。如果知识库中没有相关信息，请如实告知。";
        }
        messages.add(Map.of("role", "system", "content", finalSystemPrompt));

        for (AgentConversationRecord record : history) {
            messages.add(Map.of("role", record.getRole(), "content", record.getContent()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        return messages;
    }


    /**
     * ⭐ 核心：处理 AI 的工具调用请求
     * 流程：assistant消息 → 执行工具 → tool结果消息 → 再问AI → 最终回复
     */
    private String handleToolCalls(AiModel model, List<AiFunction> functions,
                                   List<Map<String, String>> messages,
                                   JsonNode toolCalls, String reasoningContent) {
        try {
            // ① 把不可变的 messages 转成可变版本
            List<Map<String, Object>> mutableMessages = new ArrayList<>();
            for (Map<String, String> msg : messages) {
                mutableMessages.add(new HashMap<>(msg));
            }

            // ② 加入 assistant 的 tool_calls 响应消息
            Map<String, Object> assistantMsgMap = new HashMap<>();
            assistantMsgMap.put("role", "assistant");
            assistantMsgMap.put("content", null);
            // ⭐ DeepSeek 要求回传 reasoning_content
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

            // ③ 逐一执行每个工具调用
            for (JsonNode toolCall : toolCalls) {
                String toolName = toolCall.path("function").path("name").asText();
                String argsJson = toolCall.path("function").path("arguments").asText();
                String callId = toolCall.path("id").asText();

                // 解析参数
                Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);


                // ⚡ 执行工具！
                System.out.println("[Function Calling] 执行工具: " + toolName + ", 参数: " + argsJson);
                String toolResult = functionExecutor.execute(toolName, args);

                // ④ 结果作为 role="tool" 消息加入
                Map<String, Object> toolResultMsg = new HashMap<>();
                toolResultMsg.put("role", "tool");
                toolResultMsg.put("tool_call_id", callId);
                toolResultMsg.put("name", toolName);
                toolResultMsg.put("content", toolResult);
                mutableMessages.add(toolResultMsg);
            }

            // ⑤ 再次发给 DeepSeek（带工具执行结果），让 AI 生成最终回答
            Map<String, Object> secondBody = new HashMap<>();
            secondBody.put("model", model.getModelCode());
            secondBody.put("messages", mutableMessages);
            // 第二次请求不再带 tools，避免 AI 反复调用导致死循环

            String jsonBody = objectMapper.writeValueAsString(secondBody);
            okhttp3.Request secondRequest = new okhttp3.Request.Builder()
                    .url(model.getApiUrl())
                    .post(okhttp3.RequestBody.create(jsonBody,
                            okhttp3.MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + model.getApiKey())
                    .build();

            try (okhttp3.Response response2 = httpClient.newCall(secondRequest).execute()) {
                if (!response2.isSuccessful()) {
                    throw new RuntimeException("API 返回错误码: " + response2.code());
                }
                JsonNode finalRoot = objectMapper.readTree(response2.body().string());
                String finalAnswer = finalRoot.path("choices").get(0)
                        .path("message").path("content").asText("");

                System.out.println("[Function Calling] AI 最终回答: " + finalAnswer);
                return finalAnswer;
            }

        } catch (Exception e) {
            throw new RuntimeException("工具调用处理失败：" + e.getMessage(), e);
        }
    }
    /**
     * 构建流式请求体（与 callAiApi 逻辑一致，只是多加 stream:true）
     */
    private JsonNode buildStreamRequestBody(AiModel model, String systemPrompt,
                                            List<AgentConversationRecord> history,
                                            String userMessage, String ragContext,
                                            List<AiFunction> functions) throws Exception {
        // ⭐ 复用 buildMessages()，不再手写消息构建
        List<Map<String, String>> messages = buildMessages(systemPrompt, history, userMessage, ragContext);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model.getModelCode());
        requestBody.put("stream", true);  // ⭐ 开启流式
        requestBody.put("messages", messages);

        // ⭐ 如果有工具，加入 tools 参数
        if (functions != null && !functions.isEmpty()) {
            requestBody.put("tools", buildTools(functions));
        }

        return objectMapper.valueToTree(requestBody);
    }


    private void streamWithOkHttp(AiModel model, String jsonBody,
                                  SseEmitter emitter, StringBuilder fullReply,
                                  List<AiFunction> functions, JsonNode originalRequestBody) {
        try {
            // 1. 构建请求
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(model.getApiUrl())
                    .post(okhttp3.RequestBody.create(jsonBody,
                            okhttp3.MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + model.getApiKey())
                    .build();

            // 2. 发起请求
            okhttp3.Response response = httpClient.newCall(request).execute();

            // 3. 读取流式响应
            if (!response.isSuccessful()) {
                System.err.println("[SSE] API 错误: " + response.code());
                emitter.complete();
                return;
            }

            // 4. 逐行解析 SSE 数据
            boolean hasToolCalls = false;
            Map<Integer, StreamToolCall> toolCallMap = new TreeMap<>();
            StringBuilder reasoningContent = new StringBuilder();

            try (okio.BufferedSource source = response.body().source()) {
                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    if (line == null) break;
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;

                    // --- 下面的解析逻辑跟你原来一模一样 ---
                    JsonNode node = objectMapper.readTree(data);
                    JsonNode choices = node.path("choices");
                    if (choices.size() > 0) {
                        JsonNode delta = choices.get(0).path("delta");

                        // reasoning_content
                        String rc = delta.path("reasoning_content").asText(null);
                        if (rc != null) reasoningContent.append(rc);

                        // tool_calls 碎片
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
                                if (fn.has("name") && !fn.path("name").asText().isEmpty())
                                    stc.funcName += fn.path("name").asText();
                                if (fn.has("arguments") && !fn.path("arguments").asText().isEmpty())
                                    stc.funcArgs.append(fn.path("arguments").asText());
                            }
                            continue;
                        }

                        // 普通文字
                        String content = delta.path("content").asText(null);
                        if (content != null && !content.isEmpty()) {
                            fullReply.append(content);
                            try {
                                emitter.send(SseEmitter.event().data(
                                        objectMapper.writeValueAsString(SseEvent.content(content))
                                ));
                            } catch (Exception ex) {
                                break;
                            }
                        }
                    }
                }
            }

            // 5. 工具调用处理
            if (hasToolCalls) {
                handleStreamToolCalls(model, toolCallMap, reasoningContent.toString(),
                        originalRequestBody, emitter, fullReply);
            }

        } catch (Exception e) {
            System.err.println("[SSE] OkHttp 流读取异常: " + e.getMessage());
            emitter.complete();
        }
    }

    /**
     * ⭐ 流式模式下的工具调用处理：
     * 执行工具 → 构建 tool 消息 → 第二次流式请求 → 逐字输出最终答案
     */
    private void handleStreamToolCalls(AiModel model,
                                       Map<Integer, StreamToolCall> toolCallMap,
                                       String reasoningContent,
                                       JsonNode firstRequestBody,
                                       SseEmitter emitter, StringBuilder fullReply) {
        try {
            // 1. 提取第一次请求的 messages
            JsonNode messagesNode = firstRequestBody.get("messages");
            List<Map<String, Object>> messages = new ArrayList<>();
            for (JsonNode msg : messagesNode) {
                Map<String, Object> m = new HashMap<>();
                msg.fields().forEachRemaining(field -> {
                    JsonNode val = field.getValue();
                    if (val.isTextual()) {
                        m.put(field.getKey(), val.asText());
                    } else {
                        m.put(field.getKey(), objectMapper.convertValue(val, Object.class));
                    }
                });
                messages.add(m);
            }

            // 2. 构建 assistant 消息（含 tool_calls）
            Map<String, Object> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", null);
            // ⭐ 加上 reasoning_content（deepseek-reasoner 模型必须传回）
            if (reasoningContent != null && !reasoningContent.isEmpty()) {
                assistantMsg.put("reasoning_content", reasoningContent);
            }
            List<Map<String, Object>> tcList = new ArrayList<>();
            for (StreamToolCall stc : toolCallMap.values()) {
                Map<String, Object> tc = new HashMap<>();
                tc.put("id", stc.id);
                tc.put("type", stc.type);
                Map<String, Object> funcMap = new HashMap<>();
                funcMap.put("name", stc.funcName);
                funcMap.put("arguments", stc.funcArgs.toString());
                tc.put("function", funcMap);
                tcList.add(tc);
            }
            assistantMsg.put("tool_calls", tcList);
            messages.add(assistantMsg);

            // 3. 逐一执行工具，结果作为 role="tool" 消息加入
            for (StreamToolCall stc : toolCallMap.values()) {
                System.out.println("[SSE-FC] 执行工具: " + stc.funcName
                        + ", 参数: " + stc.funcArgs);

                // ⭐ 通知前端：正在调用工具
                emitter.send(SseEmitter.event().data(
                        objectMapper.writeValueAsString(SseEvent.toolCall(stc.funcName, stc.funcArgs.toString()))
                ));

                Map<String, Object> args = objectMapper.readValue(
                        stc.funcArgs.toString(), Map.class);
                String result = functionExecutor.execute(stc.funcName, args);

                // ⭐ 通知前端：工具执行结果
                emitter.send(SseEmitter.event().data(
                        objectMapper.writeValueAsString(SseEvent.toolResult(stc.funcName, result))
                ));

                Map<String, Object> toolResult = new HashMap<>();
                toolResult.put("role", "tool");
                toolResult.put("tool_call_id", stc.id);
                toolResult.put("name", stc.funcName);
                toolResult.put("content", result);
                messages.add(toolResult);
                System.out.println("[SSE-FC] 工具结果: " + result);
            }

            // 4. 构建第二次请求体（不带 tools，避免死循环）
            Map<String, Object> secondBody = new HashMap<>();
            secondBody.put("model", model.getModelCode());
            secondBody.put("messages", messages);
            secondBody.put("stream", true);

            // 5. 第二次流式请求，逐字推送最终答案
            System.out.println("[SSE-FC] 开始第二次流式请求...");
            streamSecondResponse(model, secondBody, emitter, fullReply);
            System.out.println("[SSE-FC] 第二次流式请求完成");

        } catch (Exception e) {
            System.err.println("[SSE-FC] 工具调用处理失败: " + e.getMessage());
            e.printStackTrace();
            // completeWithError 会导致 Spring 尝试序列化异常 → LinkedHashMap 报错
            // 改用 complete() + 日志记录即可
            emitter.complete();
        }
    }

    /**
     * 第二次流式请求：纯文字输出，不再检测 tool_calls
     * ⭐ 已替换为 OkHttp 实现
     */
    private void streamSecondResponse(AiModel model, Map<String, Object> requestBodyMap,
                                      SseEmitter emitter, StringBuilder fullReply) {
        try {
            String jsonBody = objectMapper.writeValueAsString(requestBodyMap);
            // ⭐ 调试：打印第二次请求的 body（400 时能看到哪里错了）
            System.out.println("[SSE-FC] 第二次请求 body: " + jsonBody);

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(model.getApiUrl())
                    .post(okhttp3.RequestBody.create(jsonBody,
                            okhttp3.MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + model.getApiKey())
                    .build();

            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("[SSE-FC] DeepSeek 错误响应: " + response.code());
                    emitter.complete();
                    return;
                }

                try (okio.BufferedSource source = response.body().source()) {
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
                                    fullReply.append(content);
                                    try {
                                        emitter.send(SseEmitter.event().data(
                                                objectMapper.writeValueAsString(SseEvent.content(content))
                                        ));
                                    } catch (Exception ex) {
                                        break; // 客户端已断开
                                    }
                                }
                            }
                        } catch (Exception e) {}
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SSE-FC] 第二次流式读取异常: " + e.getMessage());
            emitter.complete();
        }
    }

    /**
     * 流式工具调用碎片累加器
     */
    private static class StreamToolCall {
        String id;
        String type;
        String funcName = "";
        StringBuilder funcArgs = new StringBuilder();
    }


}
