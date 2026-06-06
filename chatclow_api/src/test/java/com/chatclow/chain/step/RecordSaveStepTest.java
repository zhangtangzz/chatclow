package com.chatclow.chain.step;

import com.chatclow.context.ChatContext;
import com.chatclow.entity.AgentConversation;
import com.chatclow.mapper.AgentConversationMapper;
import com.chatclow.mapper.AgentConversationRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordSaveStep 单元测试")
class RecordSaveStepTest {

    @Mock private AgentConversationRecordMapper recordMapper;
    @Mock private AgentConversationMapper conversationMapper;

    @InjectMocks
    private RecordSaveStep step;

    private ChatContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new ChatContext();
        AgentConversation conv = new AgentConversation();
        conv.setId(1L);
        conv.setTotalTokens(100);
        ctx.setConversation(conv);
    }

    @Test
    @DisplayName("fullReply 为空时跳过")
    void shouldSkipWhenReplyEmpty() {
        ctx.setFullReply(new StringBuilder());
        assertTrue(step.shouldSkip(ctx));
    }

    @Test
    @DisplayName("fullReply 有内容时不跳过")
    void shouldNotSkipWhenReplyNotEmpty() {
        ctx.setFullReply(new StringBuilder("AI 的回复"));
        assertFalse(step.shouldSkip(ctx));
    }

    @Test
    @DisplayName("保存 assistant 角色记录到数据库")
    void shouldSaveAssistantRecord() {
        ctx.setFullReply(new StringBuilder("AI 的回复内容"));

        step.process(ctx);

        verify(recordMapper).insert(argThat(r ->
                "assistant".equals(r.getRole()) &&
                "AI 的回复内容".equals(r.getContent()) &&
                r.getConversationId().equals(1L)));
    }

    @Test
    @DisplayName("累计 token 消耗")
    void shouldAccumulateTokens() {
        ctx.setFullReply(new StringBuilder("回复"));
        ctx.setTokenUsage(50);

        AgentConversation convInDb = new AgentConversation();
        convInDb.setId(1L);
        convInDb.setTotalTokens(100);
        when(conversationMapper.selectById(1L)).thenReturn(convInDb);

        step.process(ctx);

        verify(conversationMapper).updateById(argThat(c -> c.getTotalTokens() == 150));
    }

    @Test
    @DisplayName("tokenUsage 为 null 时不累计")
    void shouldNotAccumulateWhenTokenNull() {
        ctx.setFullReply(new StringBuilder("回复"));
        ctx.setTokenUsage(null);

        step.process(ctx);

        verify(conversationMapper, never()).selectById(anyLong());
        verify(conversationMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("tokenUsage 为 0 时不累计")
    void shouldNotAccumulateWhenTokenZero() {
        ctx.setFullReply(new StringBuilder("回复"));
        ctx.setTokenUsage(0);

        step.process(ctx);

        verify(conversationMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("token 累加时处理 null → 0 的转换")
    void shouldHandleNullTotalTokens() {
        ctx.setFullReply(new StringBuilder("回复"));
        ctx.setTokenUsage(30);

        AgentConversation convInDb = new AgentConversation();
        convInDb.setId(1L);
        convInDb.setTotalTokens(null);
        when(conversationMapper.selectById(1L)).thenReturn(convInDb);

        step.process(ctx);

        verify(conversationMapper).updateById(argThat(c -> c.getTotalTokens() == 30));
    }
}
