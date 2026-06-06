package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.common.ChatRole;
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
 * <pre>
 * 输入参数来源：
 *   ContextAssemblyStep → ctx.agent.systemPrompt, ctx.history, ctx.message
 *   RagStep              → ctx.ragContext（可能为空）
 *
 * 本步骤产出（写入 ctx，给 ModelCallStep / SyncAiCallStep 使用）：
 *   ctx.functions        — 查询 ai_function 表
 *   ctx.toolsJson         — OpenAI tools 格式
 *   ctx.requestMessages   — system prompt + 历史 + RAG 上下文 + 用户消息
 *
 * 调用方法：
 *   aiFunctionService.listByAgentId()  → 查函数列表
 *   buildTools()                       → 转成 OpenAI tools JSON
 *   buildMessages()                    → 拼装消息列表
 * </pre>
 */
@Component
@Order(25)
public class MessageBuildStep implements ChatChainStep {

    @Autowired
    private AiFunctionService aiFunctionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        return false; // 永远需要构建消息
    }

    @Override
    public void process(ChatContext ctx) {
        // ① 查询工具列表
        List<AiFunction> functions = aiFunctionService.listByAgentId(ctx.getAgentId());
        ctx.setFunctions(functions);
        ctx.setFunctionsEnabled(functions != null && !functions.isEmpty());
        if (ctx.isFunctionsEnabled()) {
            ctx.setToolsJson(buildTools(functions));
        }

        // ② 构建消息列表
        ctx.setRequestMessages(buildMessages(
                ctx.getAgent().getSystemPrompt(),
                ctx.getHistory(),
                ctx.getMessage(),
                ctx.getRagContext()));
    }

    /**
     * system prompt（+ RAG 上下文）+ 历史消息 + 当前用户消息
     */
    private List<Map<String, String>> buildMessages(String systemPrompt,
                                                    List<AgentConversationRecord> history,
                                                    String userMessage, String ragContext) {
        List<Map<String, String>> messages = new ArrayList<>();

        String finalPrompt = systemPrompt;
        if (ragContext != null && !ragContext.isEmpty()) {
            finalPrompt = systemPrompt
                    + "\n\n【参考知识库内容】\n" + ragContext
                    + "\n\n请基于以上知识库内容回答用户问题。如果知识库中没有相关信息，请如实告知。";
        }
        messages.add(Map.of("role", ChatRole.SYSTEM, "content", finalPrompt));

        for (AgentConversationRecord record : history) {
            messages.add(Map.of("role", record.getRole(), "content", record.getContent()));
        }

        messages.add(Map.of("role", ChatRole.USER, "content", userMessage));
        return messages;
    }

    /**
     * AiFunction 实体列表 → OpenAI tools 参数格式
     */
    private List<Map<String, Object>> buildTools(List<AiFunction> functions) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (AiFunction func : functions) {
            Map<String, Object> funcDef = new HashMap<>();
            funcDef.put("name", func.getName());
            funcDef.put("description", func.getDescription());
            if (func.getParameters() != null && !func.getParameters().isEmpty()) {
                try {
                    funcDef.put("parameters", objectMapper.readTree(func.getParameters()));
                } catch (Exception e) {
                    funcDef.put("parameters", Map.of("type", "object", "properties", new HashMap<>()));
                }
            } else {
                funcDef.put("parameters", Map.of("type", "object", "properties", new HashMap<>()));
            }

            Map<String, Object> tool = new HashMap<>();
            tool.put("type", "function");
            tool.put("function", funcDef);
            tools.add(tool);
        }
        return tools;
    }
}
