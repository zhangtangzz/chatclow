package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.common.ChatRole;
import com.chatclow.context.ChatContext;
import com.chatclow.entity.AgentConversationRecord;
import com.chatclow.entity.AiFunction;
import com.chatclow.entity.UserDocument;
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
        String provider = ctx.getModel().getProvider();
        ctx.setRequestMessages(buildMessages(
                ctx.getAgent().getSystemPrompt(),
                ctx.getHistory(),
                ctx.getMessage(),
                ctx.getRagContext(),
                ctx.getChatFiles(),
                provider));
    }

    /**
     * system prompt（+ RAG 上下文）+ 历史消息 + 当前用户消息（含文件）
     */
    private List<Map<String, Object>> buildMessages(String systemPrompt,
                                                    List<AgentConversationRecord> history,
                                                    String userMessage, String ragContext,
                                                    List<UserDocument> chatFiles,
                                                    String provider) {
        List<Map<String, Object>> messages = new ArrayList<>();

        String finalPrompt = systemPrompt;
        if (ragContext != null && !ragContext.isEmpty()) {
            finalPrompt = systemPrompt
                    + "\n\n【参考知识库内容】\n" + ragContext
                    + "\n\n请基于以上知识库内容回答用户问题。如果知识库中没有相关信息，请如实告知。";
        }
        messages.add(Map.of("role", ChatRole.SYSTEM, "content", finalPrompt));

        // DeepSeek 要求 image_data 必须在 messages 首位，有图片时合并且跳过历史
        boolean mergeForDeepSeekImage = "DeepSeek".equalsIgnoreCase(provider)
                && chatFiles != null && !chatFiles.isEmpty()
                && hasImageFile(chatFiles);

        if (mergeForDeepSeekImage) {
            // 有图片时合并历史到当前文本，满足 DeepSeek image_data 首位要求
            StringBuilder merged = new StringBuilder();
            for (AgentConversationRecord record : history) {
                merged.append("【").append(record.getRole()).append("】\n")
                      .append(record.getContent()).append("\n\n");
            }
            if (merged.length() > 0) {
                merged.append("---\n\n");
            }
            merged.append(userMessage != null ? userMessage : "");
            messages.add(buildFileMessage(merged.toString(), chatFiles, provider));
        } else {
            for (AgentConversationRecord record : history) {
                messages.add(Map.of("role", record.getRole(), "content", record.getContent()));
            }
            if (chatFiles != null && !chatFiles.isEmpty()) {
                messages.add(buildFileMessage(userMessage, chatFiles, provider));
            } else {
                messages.add(Map.of("role", ChatRole.USER, "content", userMessage));
            }
        }

        return messages;
    }

    private boolean hasImageFile(List<UserDocument> files) {
        for (UserDocument f : files) {
            String ft = f.getFileType() != null ? f.getFileType().toLowerCase() : "";
            if (isImageType(ft)) return true;
        }
        return false;
    }

    /**
     * 构建包含文件内容的用户消息
     * 当前 API 均不支持多模态（DeepSeek/MiMo），仅传文本
     */
    private Map<String, Object> buildFileMessage(String userMessage, List<UserDocument> files,
                                                  String provider) {
        StringBuilder text = new StringBuilder(
                userMessage != null && !userMessage.isEmpty() ? userMessage : "");

        for (UserDocument file : files) {
            String fileType = file.getFileType() != null ? file.getFileType().toLowerCase() : "";
            if ("txt".equals(fileType) || "md".equals(fileType)) {
                String content = file.getContent();
                if (content != null && !content.isEmpty()) {
                    text.append("\n\n【文件: ").append(file.getFileName()).append("】\n").append(content);
                }
            } else if (isImageType(fileType)) {
                text.append("\n[用户上传了图片: ").append(file.getFileName()).append("]");
            } else {
                text.append("\n[用户上传了文件: ").append(file.getFileName()).append("]");
            }
        }

        Map<String, Object> message = new HashMap<>();
        message.put("role", ChatRole.USER);
        message.put("content", text.toString());
        return message;
    }

    private boolean isImageType(String fileType) {
        return "png".equals(fileType) || "jpg".equals(fileType) || "jpeg".equals(fileType)
                || "gif".equals(fileType) || "webp".equals(fileType) || "bmp".equals(fileType);
    }

    private String mimeType(String fileType) {
        switch (fileType) {
            case "png": return "image/png";
            case "jpg": case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "webp": return "image/webp";
            case "bmp": return "image/bmp";
            default: return "image/png";
        }
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
