package com.chatclow.dto;

/**
 * 对话响应 DTO
 * 返回给前端：AI回复 + 会话ID
 */

public class ChatResponse {

    private String reply; //AI回复内容
    private Long conversationId; //会话

    public ChatResponse() {}

    public ChatResponse(String reply, Long conversationId) {
        this.reply = reply;
        this.conversationId = conversationId;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
}
