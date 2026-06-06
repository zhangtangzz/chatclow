package com.chatclow.dto;


/**
 * 对话请求DTO
 * 接受前端发来的聊天参数
 */
public class ChatRequest {

    private Long agentId; //智能体ID
    private Long userId;  //用户ID
    private String message; //用户消息内容
    private Long conversationId; // 会话ID
    private boolean memoryEnabled = true; // 是否启用记忆（默认开启）

    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public boolean isMemoryEnabled() { return memoryEnabled; }
    public void setMemoryEnabled(boolean memoryEnabled) { this.memoryEnabled = memoryEnabled; }

}
