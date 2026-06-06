package com.chatclow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chatclow.entity.AgentConversation;

import java.util.List;

/**
 * 对话会话服务接口
 */

public interface AgentConversationService extends IService<AgentConversation>{
    /**
     * 创建新会话
     * @param userId 用户ID
     * @param title  对话标题
     */
    AgentConversation createConversation(Long userId,String title);

    /**
     * 查询用户的所有会话
     * @param userId 用户ID
     */
    List<AgentConversation> listByUserId(Long userId);

    /**
     * 删除会话及其所有聊天记录
     * @param id 会话ID
     */
    void deleteConversation(Long id);

    /**
     * 清除会话记忆（删除聊天记录，保留会话）
     * @param convId 会话ID
     */
    void clearMemory(Long convId);

}
