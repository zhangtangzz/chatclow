package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.context.ChatContext;
import com.chatclow.entity.AgentConversationRecord;
import com.chatclow.entity.AiFunction;
import com.chatclow.service.AiFunctionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 第2.5步：构建消息和工具列表（@Order = 25）
 *
 * <h3>为什么要从 ModelCallStep 里拆出来？</h3>
 * <p>
 * 重构前，ModelCallStep 既负责构建消息、又负责调 AI API，
 * 一个类干了两个完全不同的事，不符合单一职责原则。
 * 拆出来后 ModelCallStep 只管"拿到做好的食材 → 下锅"，不再管"备菜"。
 * </p>
 *
 * <h3>这个 Step 做三件事</h3>
 * <ol>
 *   <li><b>查函数列表</b> — 从 ai_function 表查出这个 Agent 绑定了哪些工具</li>
 *   <li><b>构建 tools JSON</b> — 转成 AI API 要求的 tools 参数格式</li>
 *   <li><b>构建 messages</b> — system prompt + 历史消息 + RAG 上下文 + 用户消息</li>
 * </ol>
 *
 * <h3>对应 Snail AI 中哪些 Handler？</h3>
 * <p>
 * Snail 把这些职责拆成了三个 Handler：
 * SystemPromptHandler（构建 system prompt）+ ContextCollectorHandler（收集历史）
 * + 函数加载逻辑。ChatClow 把它们合并在一个 Step 里，因为它们都围绕一个目标：
 * <b>"为 AI 调用准备好所有输入数据"</b>。
 * </p>
 *
 * <h3>system prompt 的组装逻辑</h3>
 * <pre>
 * finalSystemPrompt = 原始 systemPrompt
 *   + "\n\n【参考知识库内容】\n"
 *   + ragContext
 *   + "\n\n请基于以上知识库内容回答用户问题..."
 * </pre>
 * <p>RAG 上下文直接拼接在 system prompt 末尾，而不是作为 user 消息的一部分。
 * 这样设计是因为 system 角色有更高的"指令权重"，AI 更容易遵循。</p>
 *
 * <h3>为什么永远不跳过？</h3>
 * <p>不管什么模式（流式/非流式、有无 RAG），都需要构建 messages 发给 AI。
 * 所以 shouldSkip() 返回 false，类似 ContextAssemblyStep。</p>
 */
@Component
@Order(25)  // 在 ContextAssembly(10) 和 Rag(20) 之后，ModelCall(30) 之前
public class MessageBuildStep implements ChatChainStep {

    @Autowired
    private AiFunctionService aiFunctionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        return false; // 永远需要构建消息 —— 没消息 AI 咋回答？
    }

    @Override
    public void process(ChatContext ctx) {
        // ──────── ① 查询工具列表 ────────
        // 从 ai_function 表查出该 Agent 绑了哪些工具
        // 结果写入 ctx，给 ModelCallStep / SyncAiCallStep 使用
        List<AiFunction> functions = aiFunctionService.listByAgentId(ctx.getAgentId());
        ctx.setFunctions(functions);
        ctx.setFunctionsEnabled(functions != null && !functions.isEmpty());

        // ──────── ② 构建 tools JSON ────────
        // 只有当 Agent 确实有工具时才构建
        // 转成 AI API 要求的格式：[{"type":"function", "function":{...}}, ...]
        if (ctx.isFunctionsEnabled()) {
            ctx.setToolsJson(buildTools(functions));
        }

        // ──────── ③ 构建消息列表 ────────
        // system prompt(含RAG上下文) + 历史消息 + 用户消息
        List<Map<String, String>> messages = buildMessages(
                ctx.getAgent().getSystemPrompt(),   // Agent 的系统提示词
                ctx.getHistory(),                    // 历史消息（不含当前用户消息）
                ctx.getMessage(),                    // 当前用户输入
                ctx.getRagContext()                  // RAG 检索上下文（可能为空）
        );
        ctx.setRequestMessages(messages);

        System.out.println("[MessageBuild] 消息构建完成, 函数数量=" + functions.size());
    }

    // ════════════════════ private 辅助方法 ════════════════════

    /**
     * 构建发给 AI 的消息列表
     *
     * <h3>消息顺序（很重要！）</h3>
     * <ol>
     *   <li>system — 系统提示词 + RAG 上下文</li>
     *   <li>user / assistant / ...  — 历史消息（按时间升序）</li>
     *   <li>user — 当前用户输入</li>
     * </ol>
     *
     * <p>设计说明：RAG 上下文放在 system prompt 里，而不是 user 消息里。
     * 因为 system 角色的指令权重更高，AI 更容易认真对待知识库内容。</p>
     *
     * @param systemPrompt Agent 配置的系统提示词
     * @param history      历史消息（不含当前用户消息，因为 ContextAssemblyStep 先查后存）
     * @param userMessage  用户当前的输入
     * @param ragContext   知识库检索到的上下文（空字符串 = 没有 RAG）
     * @return OpenAI/DeepSeek 兼容格式的 messages 列表
     */
    private List<Map<String, String>> buildMessages(String systemPrompt,
                                                    List<AgentConversationRecord> history,
                                                    String userMessage, String ragContext) {
        List<Map<String, String>> messages = new ArrayList<>();

        // system prompt：如果有 RAG 上下文就拼上去
        String finalSystemPrompt = systemPrompt;
        if (ragContext != null && !ragContext.isEmpty()) {
            finalSystemPrompt = systemPrompt
                    + "\n\n【参考知识库内容】\n"
                    + ragContext
                    + "\n\n请基于以上知识库内容回答用户问题。如果知识库中没有相关信息，请如实告知。";
        }
        messages.add(Map.of("role", "system", "content", finalSystemPrompt));

        // 历史消息：之前 user 和 assistant 的对话
        for (AgentConversationRecord record : history) {
            messages.add(Map.of("role", record.getRole(), "content", record.getContent()));
        }

        // 当前用户消息：本次问题的内容
        messages.add(Map.of("role", "user", "content", userMessage));

        return messages;
    }

    /**
     * 把 AiFunction 实体列表转成 OpenAI tools 参数格式
     *
     * <h3>输出格式示例</h3>
     * <pre>
     * [{
     *   "type": "function",
     *   "function": {
     *     "name": "get_weather",
     *     "description": "获取指定城市的天气",
     *     "parameters": {
     *       "type": "object",
     *       "properties": {
     *         "city": {"type": "string", "description": "城市名称"}
     *       },
     *       "required": ["city"]
     *     }
     *   }
     * }]
     * </pre>
     *
     * @param functions 数据库中的函数实体列表
     * @return OpenAI tools 格式的 JSON 数组
     */
    private List<Map<String, Object>> buildTools(List<AiFunction> functions) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (AiFunction func : functions) {
            Map<String, Object> tool = new HashMap<>();
            tool.put("type", "function");

            Map<String, Object> funcDef = new HashMap<>();
            funcDef.put("name", func.getName());
            funcDef.put("description", func.getDescription());

            // 函数参数 JSON — 直接解析后放入，保持嵌套结构
            if (func.getParameters() != null && !func.getParameters().isEmpty()) {
                try {
                    JsonNode paramsNode = objectMapper.readTree(func.getParameters());
                    funcDef.put("parameters", paramsNode);
                } catch (Exception e) {
                    // JSON 解析失败，给个空的 parameters，避免请求失败
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
}
