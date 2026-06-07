package com.chatclow.context;

import com.chatclow.entity.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 聊天责任链的共享数据对象 — 所有 Step 通过它传递数据（Java 引用传递）
 *
 * <pre>
 * 数据按"写入方"分类：
 *   ChatServiceImpl 创建时填入 → agentId, userId, message, conversationId, streamMode, emitter
 *   ContextAssemblyStep        → agent, model, conversation, history
 *   RagStep                    → ragContext
 *   MessageBuildStep           → functions, toolsJson, requestMessages
 *   ModelCallStep              → fullReply(逐字累积), toolCallsDetected, toolCalls, reasoningContent, tokenUsage
 *   SyncAiCallStep             → fullReply(一次性), tokenUsage
 *   FunctionCallingStep        → fullReply(追加)
 * </pre>
 *
 * @see com.chatclow.chain.ChatChain     责任链执行器
 * @see com.chatclow.chain.ChatChainStep 步骤接口
 */
public class ChatContext {

    // ──── 输入参数（ChatServiceImpl 填入）────

    private Long agentId;
    private Long userId;
    private String message;
    private Long conversationId;
    private boolean streamMode;
    private boolean memoryEnabled = true;
    private java.util.List<Long> fileIds;

    // ──── 核心对象（ContextAssemblyStep 从数据库加载）────

    private AiAgent agent;
    private AiModel model;
    private AgentConversation conversation;
    private List<AgentConversationRecord> history;

    // ──── RAG（RagStep 填入）────

    /** RAG 检索到的知识库文本，拼入 system prompt 末尾 */
    private String ragContext;
    private boolean ragEnabled;

    // ──── Function Calling（MessageBuildStep 填入）────

    private List<AiFunction> functions;
    private List<Map<String, Object>> toolsJson;
    private boolean functionsEnabled;

    // ──── 请求消息（MessageBuildStep 构建）────

    /** 原始消息列表，Function Calling 二次请求时基于此重建 */
    private List<Map<String, Object>> requestMessages;

    /** 对话上传的文件（source=chat） */
    private List<UserDocument> chatFiles;

    // ──── 工具调用检测（ModelCallStep 从 SSE 流中解析）────

    private boolean toolCallsDetected;
    private List<ToolCallInfo> toolCalls;
    private String reasoningContent;

    // ──── 流式输出 ────

    private SseEmitter emitter;
    private StringBuilder fullReply;
    private String aiResponse;

    // ──── Token 消耗（ModelCallStep / SyncAiCallStep 从 API 响应中解析）────

    private Integer tokenUsage;

    // ──── 响应时间（ModelCallStep / SyncAiCallStep 在 process() 中记录）────

    /** AI 调用开始时间（毫秒时间戳） */
    private Long startTime;
    /** AI 调用耗时（毫秒） */
    private Long responseTime;

    // ======================== 构造函数 ========================

    public ChatContext() {}

    public ChatContext(Long agentId, Long userId, String message,
                       Long conversationId, boolean streamMode) {
        this.agentId = agentId;
        this.userId = userId;
        this.message = message;
        this.conversationId = conversationId;
        this.streamMode = streamMode;
        this.fullReply = new StringBuilder();
    }

    /** 兼容流式（fullReply）和非流式（aiResponse）的回复文本 */
    public String getFullReplyText() {
        if (fullReply != null && fullReply.length() > 0) return fullReply.toString();
        return aiResponse;
    }

    // ======================== Getter / Setter ========================

    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public boolean isStreamMode() { return streamMode; }
    public void setStreamMode(boolean streamMode) { this.streamMode = streamMode; }
    public boolean isMemoryEnabled() { return memoryEnabled; }
    public void setMemoryEnabled(boolean memoryEnabled) { this.memoryEnabled = memoryEnabled; }
    public java.util.List<Long> getFileIds() { return fileIds; }
    public void setFileIds(java.util.List<Long> fileIds) { this.fileIds = fileIds; }
    public AiAgent getAgent() { return agent; }
    public void setAgent(AiAgent agent) { this.agent = agent; }
    public AiModel getModel() { return model; }
    public void setModel(AiModel model) { this.model = model; }
    public AgentConversation getConversation() { return conversation; }
    public void setConversation(AgentConversation conversation) { this.conversation = conversation; }
    public List<AgentConversationRecord> getHistory() { return history; }
    public void setHistory(List<AgentConversationRecord> history) { this.history = history; }
    public String getRagContext() { return ragContext; }
    public void setRagContext(String ragContext) { this.ragContext = ragContext; }
    public boolean isRagEnabled() { return ragEnabled; }
    public void setRagEnabled(boolean ragEnabled) { this.ragEnabled = ragEnabled; }
    public List<AiFunction> getFunctions() { return functions; }
    public void setFunctions(List<AiFunction> functions) { this.functions = functions; }
    public List<Map<String, Object>> getToolsJson() { return toolsJson; }
    public void setToolsJson(List<Map<String, Object>> toolsJson) { this.toolsJson = toolsJson; }
    public boolean isFunctionsEnabled() { return functionsEnabled; }
    public void setFunctionsEnabled(boolean functionsEnabled) { this.functionsEnabled = functionsEnabled; }
    public boolean isToolCallsDetected() { return toolCallsDetected; }
    public void setToolCallsDetected(boolean toolCallsDetected) { this.toolCallsDetected = toolCallsDetected; }
    public List<ToolCallInfo> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCallInfo> toolCalls) { this.toolCalls = toolCalls; }
    public String getReasoningContent() { return reasoningContent; }
    public void setReasoningContent(String reasoningContent) { this.reasoningContent = reasoningContent; }
    public List<Map<String, Object>> getRequestMessages() { return requestMessages; }
    public void setRequestMessages(List<Map<String, Object>> requestMessages) { this.requestMessages = requestMessages; }
    public List<UserDocument> getChatFiles() { return chatFiles; }
    public void setChatFiles(List<UserDocument> chatFiles) { this.chatFiles = chatFiles; }
    public SseEmitter getEmitter() { return emitter; }
    public void setEmitter(SseEmitter emitter) { this.emitter = emitter; }
    public StringBuilder getFullReply() { return fullReply; }
    public void setFullReply(StringBuilder fullReply) { this.fullReply = fullReply; }
    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }
    public Integer getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(Integer tokenUsage) { this.tokenUsage = tokenUsage; }
    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public Long getResponseTime() { return responseTime; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }

    // ======================== 内部类 ========================

    /** AI 回复中的单条工具调用信息 */
    public static class ToolCallInfo {
        private String id;
        private String functionName;
        private String arguments;

        public ToolCallInfo() {}
        public ToolCallInfo(String id, String functionName, String arguments) {
            this.id = id;
            this.functionName = functionName;
            this.arguments = arguments;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }
        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
    }
}
