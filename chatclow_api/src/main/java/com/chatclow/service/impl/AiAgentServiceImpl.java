package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.AiAgent;
import com.chatclow.entity.AiFunction;
import com.chatclow.entity.AgentConversation;
import com.chatclow.entity.User;
import com.chatclow.mapper.AiAgentMapper;
import com.chatclow.mapper.AiFunctionMapper;
import com.chatclow.mapper.UserMapper;
import com.chatclow.service.AgentConversationService;
import com.chatclow.service.AiAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiAgentServiceImpl implements AiAgentService {

    @Autowired
    private AiAgentMapper aiAgentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AgentConversationService agentConversationService;

    @Autowired
    private AiFunctionMapper aiFunctionMapper;

    @Override
    public boolean add(AiAgent aiAgent) {
        if (aiAgent.getStatus() == null) {
            aiAgent.setStatus(1);
        }
        return aiAgentMapper.insert(aiAgent) > 0;
    }

    @Override
    public List<AiAgent> listByUserId(Long userId) {
        // 查出所有管理员的用户ID（管理员创建的 agent 对所有用户可见）
        List<Long> adminUserIds = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, 2)
                        .select(User::getId)
        ).stream().map(User::getId).collect(Collectors.toList());

        LambdaQueryWrapper<AiAgent> wrapper = new LambdaQueryWrapper<AiAgent>()
                .eq(AiAgent::getStatus, 1)
                .and(w -> {
                    w.eq(AiAgent::getUserId, userId); // 自己的 agent
                    if (!adminUserIds.isEmpty()) {
                        w.or(in -> in.in(AiAgent::getUserId, adminUserIds)); // 管理员的 agent
                    }
                })
                .orderByDesc(AiAgent::getCreatedDt);
        return aiAgentMapper.selectList(wrapper);
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
    @Transactional
    public boolean deleteById(Long id) {
        // 1. 删除关联的会话（会级联删除聊天记录）
        List<AgentConversation> conversations = agentConversationService.lambdaQuery()
                .eq(AgentConversation::getAgentId, id)
                .list();
        for (AgentConversation conv : conversations) {
            agentConversationService.deleteConversation(conv.getId());
        }

        // 2. 删除智能体自定义的工具（保留全局工具 agentId=-1）
        LambdaQueryWrapper<AiFunction> functionWrapper = new LambdaQueryWrapper<AiFunction>()
                .eq(AiFunction::getAgentId, id);
        aiFunctionMapper.delete(functionWrapper);

        // 3. 删除智能体
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
