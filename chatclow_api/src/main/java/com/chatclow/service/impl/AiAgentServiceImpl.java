package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.AiAgent;
import com.chatclow.mapper.AiAgentMapper;
import com.chatclow.service.AiAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiAgentServiceImpl implements AiAgentService {

    @Autowired
    private AiAgentMapper aiAgentMapper;

    @Override
    public boolean add(AiAgent aiAgent) {
        if (aiAgent.getStatus() == null) {
            aiAgent.setStatus(1);
        }
        return aiAgentMapper.insert(aiAgent) > 0;
    }

    @Override
    public List<AiAgent> listByUserId(Long userId) {
        return aiAgentMapper.selectList(
                new LambdaQueryWrapper<AiAgent>()
                        .eq(AiAgent::getUserId, userId)
                        .eq(AiAgent::getStatus, 1)
                        .orderByDesc(AiAgent::getCreatedDt)
        );
    }

    @Override
    public List<AiAgent> listAll() {
        return aiAgentMapper.selectList(
                new LambdaQueryWrapper<AiAgent>()
                        .orderByDesc(AiAgent::getCreatedDt)
        );
    }

    @Override
    public List<AiAgent> listAllEnabled() {
        return aiAgentMapper.selectList(
                new LambdaQueryWrapper<AiAgent>()
                        .eq(AiAgent::getStatus, 1)
                        .orderByDesc(AiAgent::getCreatedDt)
        );
    }

    @Override
    public AiAgent getById(Long id) {
        return aiAgentMapper.selectById(id);
    }

    @Override
    public boolean update(AiAgent aiAgent) {
        return aiAgentMapper.updateById(aiAgent) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return aiAgentMapper.deleteById(id) > 0;
    }

    @Override
    public Integer toggleStatus(Long id) {
        AiAgent agent = aiAgentMapper.selectById(id);
        if (agent == null) {
            throw new RuntimeException("智能体不存在");
        }
        int newStatus = agent.getStatus() == 1 ? 0 : 1;
        agent.setStatus(newStatus);
        aiAgentMapper.updateById(agent);
        return newStatus;
    }
}
