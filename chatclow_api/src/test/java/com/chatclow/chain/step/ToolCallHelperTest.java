package com.chatclow.chain.step;

import com.chatclow.common.ChatRole;
import com.chatclow.context.ChatContext;
import com.chatclow.service.FunctionExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolCallHelper 单元测试")
class ToolCallHelperTest {

    @Mock private FunctionExecutor functionExecutor;

    @InjectMocks
    private ToolCallHelper helper;

    private List<Map<String, String>> originalMessages;

    @BeforeEach
    void setUp() {
        originalMessages = List.of(
                Map.of("role", ChatRole.SYSTEM, "content", "你是一个助手"),
                Map.of("role", ChatRole.USER, "content", "北京天气怎么样")
        );
    }

    @Test
    @DisplayName("prepareMessages 追加 assistant tool_calls 消息")
    void shouldPrepareMessagesWithToolCalls() {
        List<ChatContext.ToolCallInfo> toolCalls = List.of(
                new ChatContext.ToolCallInfo("call_1", "get_weather", "{\"city\":\"北京\"}")
        );

        List<Map<String, Object>> result = helper.prepareMessages(originalMessages, toolCalls, null);

        // 原始2条 + assistant tool_calls 消息
        assertEquals(3, result.size());
        // assistant 消息
        Map<String, Object> asstMsg = result.get(2);
        assertEquals(ChatRole.ASSISTANT, asstMsg.get("role"));
        assertNotNull(asstMsg.get("tool_calls"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tcList = (List<Map<String, Object>>) asstMsg.get("tool_calls");
        assertEquals(1, tcList.size());
        assertEquals("call_1", tcList.get(0).get("id"));
        assertEquals("get_weather", ((Map<String, Object>) tcList.get(0).get("function")).get("name"));
    }

    @Test
    @DisplayName("prepareMessages 附加 reasoning_content")
    void shouldIncludeReasoningContent() {
        List<ChatContext.ToolCallInfo> toolCalls = List.of(
                new ChatContext.ToolCallInfo("call_1", "get_weather", "{}")
        );

        List<Map<String, Object>> result = helper.prepareMessages(originalMessages, toolCalls, "思考过程...");

        Map<String, Object> asstMsg = result.get(2);
        assertEquals("思考过程...", asstMsg.get("reasoning_content"));
    }

    @Test
    @DisplayName("prepareMessages reasoningContent 为 null 时不设置")
    void shouldNotSetNullReasoning() {
        List<ChatContext.ToolCallInfo> toolCalls = List.of(
                new ChatContext.ToolCallInfo("call_1", "get_weather", "{}")
        );

        List<Map<String, Object>> result = helper.prepareMessages(originalMessages, toolCalls, null);

        Map<String, Object> asstMsg = result.get(2);
        assertNull(asstMsg.get("reasoning_content"));
    }

    @Test
    @DisplayName("prepareMessages 多条工具调用")
    void shouldHandleMultipleToolCalls() {
        List<ChatContext.ToolCallInfo> toolCalls = List.of(
                new ChatContext.ToolCallInfo("call_1", "get_weather", "{\"city\":\"北京\"}"),
                new ChatContext.ToolCallInfo("call_2", "get_current_time", "{}")
        );

        List<Map<String, Object>> result = helper.prepareMessages(originalMessages, toolCalls, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tcList =
                (List<Map<String, Object>>) result.get(2).get("tool_calls");
        assertEquals(2, tcList.size());
    }

    @Test
    @DisplayName("executeTool 返回 role=tool 的消息")
    void shouldExecuteTool() {
        when(functionExecutor.execute(anyString(), any())).thenReturn("{\"weather\":\"晴\",\"temperature\":25}");

        ChatContext.ToolCallInfo tci = new ChatContext.ToolCallInfo("call_1", "get_weather", "{\"city\":\"北京\"}");
        Map<String, Object> result = helper.executeTool(tci);

        assertEquals(ChatRole.TOOL, result.get("role"));
        assertEquals("call_1", result.get("tool_call_id"));
        assertEquals("get_weather", result.get("name"));
        assertEquals("{\"weather\":\"晴\",\"temperature\":25}", result.get("content"));
    }

    @Test
    @DisplayName("executeTool 参数解析失败时抛异常")
    void shouldThrowOnInvalidArgs() {
        ChatContext.ToolCallInfo tci = new ChatContext.ToolCallInfo("call_1", "get_weather", "not-json");

        assertThrows(RuntimeException.class, () -> helper.executeTool(tci));
    }
}
