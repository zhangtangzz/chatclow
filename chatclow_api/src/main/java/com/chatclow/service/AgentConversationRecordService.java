package com.chatclow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chatclow.entity.AgentConversationRecord;

import java.util.List;

/**
 * 聊天记录服务接口
 */

public interface AgentConversationRecordService extends IService<AgentConversationRecord>   {

    /**
     * 方法
     * 保存一条聊天记录
     * @param conversationId 会话ID
     * @param role           角色（user/assistant）
     * @param content        消息内容
     */

    void saveRecord(Long conversationId, String role, String content);

    /**
     * 查询会话的所有聊天记录
     * @param conversationId 会话ID
     */
    List<AgentConversationRecord> listByConversationId(Long conversationId);
}
