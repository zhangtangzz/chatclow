package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.AiFunction;
import com.chatclow.mapper.AiFunctionMapper;
import com.chatclow.service.AiFunctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiFunctionServiceImpl implements AiFunctionService {

    @Autowired
    private AiFunctionMapper aiFunctionMapper;

    @Override
    public void add(AiFunction aiFunction) {
        aiFunctionMapper.insert(aiFunction);
    }

    @Override
    public void update(AiFunction aiFunction) {
        aiFunctionMapper.updateById(aiFunction);
    }

    @Override
    public void delete(Long id) {
        aiFunctionMapper.deleteById(id);
    }

    @Override
    public AiFunction getById(Long id) {
        return aiFunctionMapper.selectById(id);
    }

    @Override
    public List<AiFunction> listByAgentId(Long agentId) {
        LambdaQueryWrapper<AiFunction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiFunction::getAgentId, agentId);   // 按智能体ID筛选
        wrapper.eq(AiFunction::getStatus, 1);             // 只要启用的
        wrapper.orderByAsc(AiFunction::getId);            // 按ID升序
        return aiFunctionMapper.selectList(wrapper);
    }
}
