package com.chatclow.context;

import com.chatclow.entity.AiAgent;
import com.chatclow.entity.AiModel;
import com.chatclow.entity.AgentConversation;
import com.chatclow.entity.AgentConversationRecord;
import com.chatclow.entity.AiFunction;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 聊天责任链上下文对象 — 步骤之间的"公交车"
 *
 * <h3>为什么需要 ChatContext？</h3>
 * <p>
 * 责任链的每个 Step 是独立类，它们需要共享数据。比如：
 * </p>
 * <ul>
 *   <li>ContextAssemblyStep 加载了 Agent，RagStep 需要知道 Agent 的 kbEnabled 配置</li>
 *   <li>MessageBuildStep 构建了 messages，ModelCallStep 需要用它发请求</li>
 *   <li>ModelCallStep 检测到了 tool_calls，FunctionCallingStep 需要执行它们</li>
 * </ul>
 * <p>
 * 解决方案：把所有步骤需要读写的数据都放在同一个 ChatContext 对象里。
 * 执行器把这个对象依次传给每个步骤，步骤像坐公交一样"下车"前把数据留给"下一站"。
 * </p>
 *
 * <h3>数据按"写入方"分类</h3>
 * <pre>
 * ChatServiceImpl 创建时填入：
 *   agentId, userId, message, conversationId, streamMode
 *
 * ContextAssemblyStep 填入：
 *   agent, model, conversation, history
 *
 * RagStep 填入：
 *   ragContext, ragEnabled
 *
 * MessageBuildStep 填入：
 *   functions, toolsJson, requestMessages, functionsEnabled
 *
 * ModelCallStep / SyncAiCallStep 填入：
 *   fullReply, aiResponse, toolCallsDetected, toolCalls, reasoningContent
 *
 * 所有步骤读取：
 *   上面任何一个字段，只要前面步骤写了，后面就能读
 * </pre>
 *
 * <p><b>关键设计：Java 引用传递。</b>所有步骤拿到的都是同一个 ChatContext 实例，
 * 所以一个步骤 setXxx() 之后，后续步骤 getXxx() 就能拿到最新值。不需要返回值、不需要全局变量。</p>
 *
 * @see com.chatclow.chain.ChatChain    责任链执行器
 * @see com.chatclow.chain.ChatChainStep 步骤接口
 */
public class ChatContext {

    // ╔══════════════════════════════════════════════════════════╗
    // ║  第1组：输入参数（ChatServiceImpl 创建时填入）            ║
    // ║  这些是请求一开始就确定的，后续步骤只读不改               ║
    // ╚══════════════════════════════════════════════════════════╝

    /** 智能体ID — 我们要用哪个 Agent 来回答问题 */
    private Long agentId;

    /** 用户ID — 谁在提问 */
    private Long userId;

    /** 用户的消息内容 — 即用户输入了什么 */
    private String message;

    /** 会话ID — null 表示新会话，非空表示续写已有会话 */
    private Long conversationId;

    /** 是否流式模式 — true=SSE 逐字推送，false=一次性返回 */
    private boolean streamMode;

    // ╔══════════════════════════════════════════════════════════╗
    // ║  第2组：核心对象（ContextAssemblyStep 从数据库加载填入） ║
    // ╚══════════════════════════════════════════════════════════╝

    /** 智能体实体 — 包含 systemPrompt、modelId、kbId、知识库配置等 */
    private AiAgent agent;

    /** 模型实体 — apiUrl、apiKey、modelCode 等 */
    private AiModel model;

    /** 会话记录 — 本次对话对应的会话对象 */
    private AgentConversation conversation;

    /** 历史消息列表 — 该会话之前的所有消息（不含当前用户消息） */
    private List<AgentConversationRecord> history;

    // ╔══════════════════════════════════════════════════════════╗
    // ║  第3组：RAG 检索结果（RagStep 填入）                     ║
    // ╚══════════════════════════════════════════════════════════╝

    /** RAG 检索到的知识库文本，拼装好后写入 system prompt 末尾 */
    private String ragContext;

    /** 当前智能体是否开启了知识库（由 RagStep.shouldSkip() 判断后设置） */
    private boolean ragEnabled;

    // ╔══════════════════════════════════════════════════════════╗
    // ║  第4组：Function Calling 相关（MessageBuildStep 填入）   ║
    // ╚══════════════════════════════════════════════════════════╝

    /** 智能体绑定的函数列表（从数据库查出来的 AiFunction 实体） */
    private List<AiFunction> functions;

    /** 发给 AI API 的 tools 参数（JSON 格式的函数定义列表） */
    private List<Map<String, Object>> toolsJson;

    /** 是否开启了 Function Calling（有函数且非空） */
    private boolean functionsEnabled;

    // ╔══════════════════════════════════════════════════════════╗
    // ║  第5组：AI 响应中检测到的工具调用（ModelCallStep 填入）   ║
    // ║  这些字段只在流式模式下有意义                             ║
    // ╚══════════════════════════════════════════════════════════╝

    /** AI 的流式回复中是否检测到了工具调用请求 */
    private boolean toolCallsDetected;

    /** 解析后的工具调用列表（每个元素是一个 ToolCallInfo） */
    private List<ToolCallInfo> toolCalls;

    /** deepseek-reasoner 模型的推理内容（FC 二次请求需要回传） */
    private String reasoningContent;

    // ╔══════════════════════════════════════════════════════════╗
    // ║  第6组：请求消息列表（MessageBuildStep 构建，ModelCallStep/SyncAiCallStep 使用） ║
    // ╚══════════════════════════════════════════════════════════╝

    /**
     * 发给 AI 的 messages 数组（可变，多轮 FC 时会不断追加消息）
     *
     * <p>注意：这是 {@code List<Map<String, Object>>} 而非 {@code List<Map<String, String>>}，
     * 因为 Function Calling 场景下 assistant 消息的 content 可能是 null，
     * tool_calls 字段的值是数组而非字符串。</p>
     */
    private List<Map<String, Object>> messages;

    /**
     * 第一次请求的 messages（不可变版本，Function Calling 二次请求需要基于它来构建）
     *
     * <p>为什么需要两份？因为 messages 是可变的（FC 过程中会 add assistant、tool 消息），
     * 但第二次请求前需要回到原始 messages 再重新构建。用 requestMessages 保留一份原始版。</p>
     */
    private List<Map<String, String>> requestMessages;

    // ╔══════════════════════════════════════════════════════════╗
    // ║  第7组：流式输出相关（ChatServiceImpl 创建时设置 emitter）║
    // ╚══════════════════════════════════════════════════════════╝

    /** SSE 发射器 — 流式模式下给前端推消息的通道（非流式为 null） */
    private SseEmitter emitter;

    /** 流式/非流式累积的完整 AI 回复文本 */
    private StringBuilder fullReply;

    // ╔══════════════════════════════════════════════════════════╗
    // ║  第8组：AI 响应（SyncAiCallStep 填入）                   ║
    // ╚══════════════════════════════════════════════════════════╝

    /** AI 的非流式原始响应文本（流式模式用 fullReply 累积） */
    private String aiResponse;

    // ======================== 构造函数 ========================

    public ChatContext() {
    }

    /**
     * 带输入参数的构造 — ChatServiceImpl 创建 ctx 时调用
     *
     * @param agentId        智能体 ID
     * @param userId         用户 ID
     * @param message        用户消息
     * @param conversationId 会话 ID（null = 新会话）
     * @param streamMode     是否流式
     */
    public ChatContext(Long agentId, Long userId, String message,
                       Long conversationId, boolean streamMode) {
        this.agentId = agentId;
        this.userId = userId;
        this.message = message;
        this.conversationId = conversationId;
        this.streamMode = streamMode;
        // fullReply 初始化很重要！否则后续 append 会 NPE
        this.fullReply = new StringBuilder();
    }

    // ======================== 便捷方法 ========================

    /**
     * 获取完整回复文本（兼容流式和非流式两种模式）
     *
     * <p>流式模式：fullReply 由 ModelCallStep 和 FunctionCallingStep 逐字 append</p>
     * <p>非流式模式：aiResponse 由 SyncAiCallStep 一次性写入</p>
     *
     * @return 完整的 AI 回复文本
     */
    public String getFullReplyText() {
        if (fullReply != null && fullReply.length() > 0) {
            return fullReply.toString();
        }
        return aiResponse;
    }

    /**
     * 向 messages 列表追加一条消息
     *
     * <p>用于 Function Calling 场景中动态添加 assistant（含 tool_calls）消息
     * 和 tool（执行结果）消息。</p>
     *
     * @param role    角色：system / user / assistant / tool
     * @param content 消息内容
     */
    public void addMessage(String role, String content) {
        if (messages == null) {
            messages = new java.util.ArrayList<>();
        }
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        messages.add(msg);
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

    public List<Map<String, Object>> getMessages() { return messages; }
    public void setMessages(List<Map<String, Object>> messages) { this.messages = messages; }

    public List<Map<String, String>> getRequestMessages() { return requestMessages; }
    public void setRequestMessages(List<Map<String, String>> requestMessages) {
        this.requestMessages = requestMessages;
    }

    public SseEmitter getEmitter() { return emitter; }
    public void setEmitter(SseEmitter emitter) { this.emitter = emitter; }

    public StringBuilder getFullReply() { return fullReply; }
    public void setFullReply(StringBuilder fullReply) { this.fullReply = fullReply; }

    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }

    // ======================== 内部类 ========================

    /**
     * AI 回复中的单条工具调用信息
     *
     * <p>当 AI 模型（如 deepseek）决定"我需要调用一个函数"时，
     * 会在回复中返回 tool_calls 数组，每条记录包含：
     * - id: 调用 ID（后续 tool 回复需要用它关联）
     * - functionName: 函数名（如 get_weather）
     * - arguments: JSON 字符串格式的参数</p>
     *
     * <p>这个内部类把原来 Map&lt;String, Object&gt; 的松散结构
     * 改成了类型安全的 POJO，减少手滑写错 key 的风险。</p>
     */
    public static class ToolCallInfo {
        /** 工具调用 ID（AI 生成，关联 tool 角色消息用） */
        private String id;

        /** 函数名 */
        private String functionName;

        /** 函数参数（JSON 字符串，执行时再解析） */
        private String arguments;

        public ToolCallInfo() {
        }

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
