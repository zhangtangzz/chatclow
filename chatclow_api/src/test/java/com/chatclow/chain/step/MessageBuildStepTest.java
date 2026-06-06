package com.chatclow.chain.step;

import com.chatclow.common.ChatRole;
import com.chatclow.context.ChatContext;
import com.chatclow.entity.AgentConversationRecord;
import com.chatclow.entity.AiFunction;
import com.chatclow.service.AiFunctionService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageBuildStep 单元测试")
class MessageBuildStepTest {

    @Mock private AiFunctionService functionService;

    @InjectMocks
    private MessageBuildStep step;

    private ChatContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new ChatContext();
        ctx.setAgentId(1L);
        ctx.setAgent(new com.chatclow.entity.AiAgent());
        ctx.getAgent().setSystemPrompt("你是一个AI助手");
        ctx.setMessage("今天的天气怎么样？");
        ctx.setHistory(List.of());
    }

    @Test
    @DisplayName("shouldSkip 永远返回 false")
    void shouldNeverSkip() {
        assertFalse(step.shouldSkip(ctx));
    }

    @Test
    @DisplayName("构建基础消息列表：system + user")
    void shouldBuildBasicMessages() {
        when(functionService.listByAgentId(1L)).thenReturn(List.of());

        step.process(ctx);

        List<Map<String, String>> msgs = ctx.getRequestMessages();
        assertEquals(2, msgs.size());
        assertEquals(ChatRole.SYSTEM, msgs.get(0).get("role"));
        assertTrue(msgs.get(0).get("content").contains("你是一个AI助手"));
        assertEquals(ChatRole.USER, msgs.get(1).get("role"));
        assertEquals("今天的天气怎么样？", msgs.get(1).get("content"));
        assertFalse(ctx.isFunctionsEnabled());
    }

    @Test
    @DisplayName("拼接 RAG 上下文到 system prompt 末尾")
    void shouldAppendRagContext() {
        ctx.setRagContext("知识库内容：北京今天25度");
        when(functionService.listByAgentId(1L)).thenReturn(List.of());

        step.process(ctx);

        String systemContent = ctx.getRequestMessages().get(0).get("content");
        assertTrue(systemContent.contains("【参考知识库内容】"));
        assertTrue(systemContent.contains("知识库内容：北京今天25度"));
    }

    @Test
    @DisplayName("无 RAG 上下文时 system prompt 不包含知识库标记")
    void shouldNotHaveRagMarkerWithoutContext() {
        when(functionService.listByAgentId(1L)).thenReturn(List.of());

        step.process(ctx);

        String systemContent = ctx.getRequestMessages().get(0).get("content");
        assertFalse(systemContent.contains("【参考知识库内容】"));
    }

    @Test
    @DisplayName("包含历史消息时正确排序：system → history → user")
    void shouldIncludeHistoryInOrder() {
        List<AgentConversationRecord> history = List.of(
                createRecord("user", "上次的问题"),
                createRecord("assistant", "上次的回答")
        );
        ctx.setHistory(history);
        when(functionService.listByAgentId(1L)).thenReturn(List.of());

        step.process(ctx);

        List<Map<String, String>> msgs = ctx.getRequestMessages();
        assertEquals(4, msgs.size()); // system + 2 history + user
        assertEquals(ChatRole.SYSTEM, msgs.get(0).get("role"));
        assertEquals("user", msgs.get(1).get("role"));
        assertEquals("上次的问题", msgs.get(1).get("content"));
        assertEquals("assistant", msgs.get(2).get("role"));
        assertEquals("上次的回答", msgs.get(2).get("content"));
        assertEquals(ChatRole.USER, msgs.get(3).get("role"));
    }

    @Test
    @DisplayName("有函数时构建 tools JSON 并设置标志位")
    void shouldBuildToolsJson() {
        AiFunction func = new AiFunction();
        func.setName("get_weather");
        func.setDescription("查询天气");
        func.setParameters("{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}");
        when(functionService.listByAgentId(1L)).thenReturn(List.of(func));

        step.process(ctx);

        assertTrue(ctx.isFunctionsEnabled());
        assertNotNull(ctx.getToolsJson());
        assertEquals(1, ctx.getToolsJson().size());
        assertEquals("function", ctx.getToolsJson().get(0).get("type"));
    }

    @Test
    @DisplayName("无函数时 toolsJson 为空，functionsEnabled 为 false")
    void shouldHandleNoFunctions() {
        when(functionService.listByAgentId(1L)).thenReturn(List.of());

        step.process(ctx);

        assertFalse(ctx.isFunctionsEnabled());
        assertNull(ctx.getToolsJson());
    }

    private AgentConversationRecord createRecord(String role, String content) {
        AgentConversationRecord r = new AgentConversationRecord();
        r.setRole(role);
        r.setContent(content);
        return r;
    }
}
