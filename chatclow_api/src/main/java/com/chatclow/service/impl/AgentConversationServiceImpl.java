package com.chatclow.service.impl;

import com.chatclow.entity.AgentConversation;
import com.chatclow.entity.AgentConversationRecord;
import com.chatclow.mapper.AgentConversationMapper;
import com.chatclow.mapper.AgentConversationRecordMapper;
import com.chatclow.service.AgentConversationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgentConversationServiceImpl
        extends ServiceImpl<AgentConversationMapper, AgentConversation>
        implements AgentConversationService {

    @Autowired
    private AgentConversationRecordMapper recordMapper;

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

    @Override
    @Transactional
    public void deleteConversation(Long id) {
        recordMapper.delete(new LambdaQueryWrapper<AgentConversationRecord>()
                .eq(AgentConversationRecord::getConversationId, id));
        this.removeById(id);
    }

    @Override
    @Transactional
    public void clearMemory(Long convId) {
        recordMapper.delete(new LambdaQueryWrapper<AgentConversationRecord>()
                .eq(AgentConversationRecord::getConversationId, convId));
    }
}
