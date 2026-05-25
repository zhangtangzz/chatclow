package com.chatclow.service;

import com.chatclow.entity.AiAgent;
import com.chatclow.entity.AiModel;
import com.chatclow.entity.AgentConversation;
import com.chatclow.entity.AgentConversationRecord;
import com.chatclow.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务接口
 */
public interface ChatService {

    /**
     * 发送消息并获取AI回复（支持多轮对话）
     *
     * @param agentId         智能体ID（必须）
     * @param userId          用户ID（必须）
     * @param message         用户消息内容（必须）
     * @param conversationId  会话ID（可选，传null则新建会话）
     * @return AI回复内容
     */
    ChatResponse chat(Long agentId, Long userId, String message,Long conversationId);


    /**
     * SSE 流式对话 — 逐块推送 AI 回复到前端
     *
     * @param agentId         智能体ID
     * @param userId          用户ID
     * @param message         用户消息
     * @param conversationId  会话ID（可选）
     * @param emitter         SSE 发射器，用于逐块推送数据
     */
    void chatStream(Long agentId, Long userId, String message,
                    Long conversationId, SseEmitter emitter);

}
