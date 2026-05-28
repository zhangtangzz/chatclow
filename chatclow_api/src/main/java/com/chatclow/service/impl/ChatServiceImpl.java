package com.chatclow.service.impl;

import com.chatclow.chain.ChatChain;
import com.chatclow.context.ChatContext;
import com.chatclow.dto.ChatResponse;
import com.chatclow.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务实现类
 * 流式（chatStream）和非流式（chat）统一走责任链 ChatChain
 */
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatChain chatChain;

    // ========== 非流式对话 ==========

    @Override
    public ChatResponse chat(Long agentId, Long userId, String message, Long conversationId) {
        ChatContext ctx = new ChatContext(agentId, userId, message, conversationId, false);
        ctx.setFullReply(new StringBuilder());
        chatChain.execute(ctx);
        return new ChatResponse(ctx.getFullReply().toString(), ctx.getConversation().getId());
    }

    // ========== 流式对话（SSE） ==========

    @Override
    public void chatStream(Long agentId, Long userId, String message,
                           Long conversationId, SseEmitter emitter) {
        try {
            ChatContext ctx = new ChatContext(agentId, userId, message, conversationId, true);
            ctx.setEmitter(emitter);
            ctx.setFullReply(new StringBuilder());

            chatChain.execute(ctx);

            System.out.println("[SSE] 流式对话完成，回复长度: " + ctx.getFullReply().length());
        } catch (Exception e) {
            System.err.println("[SSE] 流式对话异常: " + e.getMessage());
            emitter.complete();
        }
    }
}
