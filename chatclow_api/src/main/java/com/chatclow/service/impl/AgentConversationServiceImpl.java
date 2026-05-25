package com.chatclow.service.impl;

import com.chatclow.entity.AgentConversation;
import com.chatclow.mapper.AgentConversationMapper;
import com.chatclow.service.AgentConversationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentConversationServiceImpl
        extends ServiceImpl<AgentConversationMapper, AgentConversation>
        implements AgentConversationService {

    @Override
    public AgentConversation createConversation(Long userId, String title) {
        AgentConversation conversation = new AgentConversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        this.save(conversation);
        return conversation;
    }

    @Override
    public List<AgentConversation> listByUserId(Long userId) {
        return this.lambdaQuery()
                .eq(AgentConversation::getUserId, userId)
                .orderByDesc(AgentConversation::getCreatedDt)
                .list();
    }
}
