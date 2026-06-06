package com.chatclow.chain.step;

import com.chatclow.context.ChatContext;
import com.chatclow.entity.*;
import com.chatclow.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ContextAssemblyStep 单元测试")
class ContextAssemblyStepTest {

    @Mock private AiAgentMapper agentMapper;
    @Mock private AiModelMapper modelMapper;
    @Mock private AgentConversationMapper conversationMapper;
    @Mock private AgentConversationRecordMapper recordMapper;

    @InjectMocks
    private ContextAssemblyStep step;

    private ChatContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new ChatContext(1L, 100L, "你好，帮我查天气", null, true);
        ctx.setMemoryEnabled(true);

        AiAgent agent = new AiAgent();
        agent.setId(1L);
        agent.setName("测试智能体");
        agent.setModelId(10L);
        agent.setSystemPrompt("你是一个助手");

        AiModel model = new AiModel();
        model.setId(10L);
        model.setModelCode("test-model");

        when(agentMapper.selectById(1L)).thenReturn(agent);
        when(modelMapper.selectById(10L)).thenReturn(model);
    }

    @Test
    @DisplayName("shouldSkip 永远返回 false")
    void shouldNeverSkip() {
        assertFalse(step.shouldSkip(ctx));
    }

    @Test
    @DisplayName("正常加载 Agent 和 Model 到 ctx")
    void shouldLoadAgentAndModel() {
        step.process(ctx);
        assertNotNull(ctx.getAgent());
        assertEquals("测试智能体", ctx.getAgent().getName());
        assertNotNull(ctx.getModel());
        assertEquals("test-model", ctx.getModel().getModelCode());
    }

    @Test
    @DisplayName("conversationId 为 null 时新建会话，标题取自消息前20字")
    void shouldCreateNewConversationWhenIdIsNull() {
        when(conversationMapper.insert(any(AgentConversation.class))).thenReturn(1);
        when(recordMapper.selectList(any())).thenReturn(java.util.List.of());

        step.process(ctx);

        verify(conversationMapper).insert(any(AgentConversation.class));
        verify(conversationMapper, never()).selectById(anyLong());
        assertEquals("你好，帮我查天气", ctx.getConversation().getTitle());
    }

    @Test
    @DisplayName("conversationId 不为 null 时加载已有会话")
    void shouldLoadExistingConversation() {
        ctx = new ChatContext(1L, 100L, "你好", 5L, true);
        ctx.setMemoryEnabled(true);

        AgentConversation existing = new AgentConversation();
        existing.setId(5L);
        existing.setTitle("已有会话");

        when(agentMapper.selectById(1L)).thenReturn(ctx.getAgent());
        // need to re-setup since ctx changed
        AiAgent agent = new AiAgent();
        agent.setId(1L);
        agent.setModelId(10L);
        when(agentMapper.selectById(1L)).thenReturn(agent);
        when(modelMapper.selectById(10L)).thenReturn(new AiModel());
        when(conversationMapper.selectById(5L)).thenReturn(existing);
        when(recordMapper.selectList(any())).thenReturn(java.util.List.of());

        step.process(ctx);

        verify(conversationMapper).selectById(5L);
        assertEquals(5L, ctx.getConversation().getId());
    }

    @Test
    @DisplayName("memoryEnabled=false 时不加载历史消息")
    void shouldSkipHistoryWhenMemoryDisabled() {
        ctx.setMemoryEnabled(false);
        when(conversationMapper.insert(any(AgentConversation.class))).thenReturn(1);

        step.process(ctx);

        verify(recordMapper, never()).selectList(any());
        assertTrue(ctx.getHistory().isEmpty());
    }

    @Test
    @DisplayName("正常加载历史消息")
    void shouldLoadHistoryWhenMemoryEnabled() {
        when(conversationMapper.insert(any(AgentConversation.class))).thenReturn(1);
        when(recordMapper.selectList(any())).thenReturn(java.util.List.of());

        step.process(ctx);

        verify(recordMapper).selectList(any());
        assertNotNull(ctx.getHistory());
    }

    @Test
    @DisplayName("保存当前用户消息到 record 表")
    void shouldSaveUserMessage() {
        when(conversationMapper.insert(any(AgentConversation.class))).thenReturn(1);
        when(recordMapper.insert(any(AgentConversationRecord.class))).thenReturn(1);
        when(recordMapper.selectList(any())).thenReturn(java.util.List.of());

        step.process(ctx);

        verify(recordMapper).insert(argThat(r ->
                "user".equals(r.getRole()) && "你好，帮我查天气".equals(r.getContent())));
    }

    @Test
    @DisplayName("Agent 不存在时抛异常")
    void shouldThrowWhenAgentNotFound() {
        when(agentMapper.selectById(1L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> step.process(ctx));
        assertTrue(ex.getMessage().contains("智能体不存在"));
    }

    @Test
    @DisplayName("模型不存在时抛异常")
    void shouldThrowWhenModelNotFound() {
        when(modelMapper.selectById(10L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> step.process(ctx));
        assertTrue(ex.getMessage().contains("模型配置不存在"));
    }
}
