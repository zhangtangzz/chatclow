package com.chatclow.chain.step;

import com.chatclow.common.ChatRole;
import com.chatclow.context.ChatContext;
import com.chatclow.service.FunctionExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * FC（Function Calling）公共逻辑提取
 *
 * <p>原来 FunctionCallingStep（流式）和 SyncAiCallStep（非流式）中有 ~80 行完全一样的代码：
 * 消息类型转换、构建 assistant tool_calls 消息、执行工具、构建 tool 结果消息。
 * 提取到这里，两边共用。</p>
 */
@Component
public class ToolCallHelper {

    private final FunctionExecutor functionExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolCallHelper(FunctionExecutor functionExecutor) {
        this.functionExecutor = functionExecutor;
    }

    /**
     * 将原始消息列表转成可变版本，并追加 assistant（含 tool_calls）消息
     *
     * @param originalMessages MessageBuildStep 构建的原始消息（String,String 不可变）
     * @param toolCalls        ModelCallStep 从 SSE 流中解析出的工具调用列表
     * @param reasoningContent deepseek-reasoner 推理内容（可为 null）
     * @return 可变消息列表（已包含 assistant tool_calls 消息）
     */
    public List<Map<String, Object>> prepareMessages(
            List<Map<String, Object>> originalMessages,
            List<ChatContext.ToolCallInfo> toolCalls,
            String reasoningContent) {

        List<Map<String, Object>> messages = new ArrayList<>();
        for (Map<String, Object> msg : originalMessages) {
            messages.add(new HashMap<>(msg));
        }

        // 构建 assistant 消息（含 tool_calls）
        Map<String, Object> assistantMsg = new HashMap<>();
        assistantMsg.put("role", ChatRole.ASSISTANT);
        assistantMsg.put("content", null);
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            assistantMsg.put("reasoning_content", reasoningContent);
        }

        List<Map<String, Object>> tcList = new ArrayList<>();
        for (ChatContext.ToolCallInfo tci : toolCalls) {
            Map<String, Object> tc = new HashMap<>();
            tc.put("id", tci.getId());
            tc.put("type", "function");
            Map<String, Object> funcMap = new HashMap<>();
            funcMap.put("name", tci.getFunctionName());
            funcMap.put("arguments", tci.getArguments());
            tc.put("function", funcMap);
            tcList.add(tc);
        }
        assistantMsg.put("tool_calls", tcList);
        messages.add(assistantMsg);

        return messages;
    }

    /**
     * 执行单个工具调用并构建 tool 角色消息
     *
     * @param tci 工具调用信息（id + 函数名 + JSON 参数）
     * @return role=tool 的消息 Map（含 tool_call_id、name、content）
     */
    public Map<String, Object> executeTool(ChatContext.ToolCallInfo tci) {
        @SuppressWarnings("unchecked")
        Map<String, Object> args;
        try {
            args = objectMapper.readValue(tci.getArguments(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("解析工具参数失败: " + tci.getFunctionName(), e);
        }
        String result = functionExecutor.execute(tci.getFunctionName(), args);

        Map<String, Object> toolResult = new HashMap<>();
        toolResult.put("role", ChatRole.TOOL);
        toolResult.put("tool_call_id", tci.getId());
        toolResult.put("name", tci.getFunctionName());
        toolResult.put("content", result);
        return toolResult;
    }
}
