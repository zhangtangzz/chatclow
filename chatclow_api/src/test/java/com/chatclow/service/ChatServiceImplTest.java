package com.chatclow.service;

import com.chatclow.chain.ChatChain;
import com.chatclow.context.ChatContext;
import com.chatclow.dto.ChatResponse;
import com.chatclow.entity.AgentConversation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ChatServiceImpl 集成测试")
class ChatServiceImplTest {

    @Autowired private ChatService chatService;

    @MockBean private ChatChain chatChain;

    @Test
    @DisplayName("非流式 chat() 构建正确的 ChatContext")
    void shouldBuildContextForNonStream() {
        doAnswer(inv -> {
            ChatContext ctx = inv.getArgument(0);
            ctx.getFullReply().append("AI 回复");
            ctx.setTokenUsage(100);
            AgentConversation conv = new AgentConversation();
            conv.setId(1L);
            ctx.setConversation(conv);
            return null;
        }).when(chatChain).execute(any(ChatContext.class));

        ChatResponse resp = chatService.chat(1L, 100L, "你好", null, true);

        assertNotNull(resp);
        assertEquals("AI 回复", resp.getReply());
        verify(chatChain).execute(any(ChatContext.class));
    }

    @Test
    @DisplayName("流式 chatStream() memoryEnabled=false 时 context 正确")
    void shouldBuildContextForStream() {
        doAnswer(inv -> {
            ChatContext ctx = inv.getArgument(0);
            ctx.getFullReply().append("流式回复");
            return null;
        }).when(chatChain).execute(any(ChatContext.class));

        SseEmitter emitter = new SseEmitter();
        chatService.chatStream(1L, 100L, "你好", 5L, false, emitter);

        verify(chatChain).execute(argThat(ctx ->
                ctx.isStreamMode() && !ctx.isMemoryEnabled()));
    }
}
