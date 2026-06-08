package com.chatclow.service.impl;

import com.chatclow.chain.ChatChain;
import com.chatclow.context.ChatContext;
import com.chatclow.dto.ChatResponse;
import com.chatclow.dto.SseEvent;
import com.chatclow.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务实现类
 *
 * <p>流式（chatStream）和非流式（chat）统一走责任链 ChatChain，
 * 区别仅在于 ChatContext 的 streamMode 标志位。</p>
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Autowired
    private ChatChain chatChain;
    @Override
    public ChatResponse chat(Long agentId, Long userId, String message,
                              Long conversationId, boolean memoryEnabled, java.util.List<Long> fileIds) {
        ChatContext ctx = new ChatContext(agentId, userId, message, conversationId, false);
        ctx.setMemoryEnabled(memoryEnabled);
        ctx.setFileIds(fileIds);
        chatChain.execute(ctx);
        return new ChatResponse(ctx.getFullReply().toString(), ctx.getConversation().getId());
    }

    @Override
    public void chatStream(Long agentId, Long userId, String message,
                           Long conversationId, boolean memoryEnabled, java.util.List<Long> fileIds, SseEmitter emitter) {
        try {
            ChatContext ctx = new ChatContext(agentId, userId, message, conversationId, true);
            ctx.setMemoryEnabled(memoryEnabled);
            ctx.setFileIds(fileIds);
            ctx.setEmitter(emitter);
            chatChain.execute(ctx);
            log.info("[SSE] 流式对话完成，回复长度: {}", ctx.getFullReply().length());
        } catch (Exception e) {
            log.error("[SSE] 流式对话异常: {}", e.getMessage());
            try {
                emitter.send(SseEmitter.event().data(
                        new ObjectMapper().writeValueAsString(SseEvent.error(e.getMessage()))
                ));
            } catch (Exception ignored) {}
            emitter.complete();
        }
    }
}
