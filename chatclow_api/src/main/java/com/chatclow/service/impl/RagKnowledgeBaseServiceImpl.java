package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.RagKnowledgeBase;
import com.chatclow.mapper.RagKnowledgeBaseMapper;
import com.chatclow.service.RagKnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagKnowledgeBaseServiceImpl implements RagKnowledgeBaseService {

    @Autowired
    private RagKnowledgeBaseMapper ragKnowledgeBaseMapper;

    @Override
    public boolean add(RagKnowledgeBase kb) {
        return ragKnowledgeBaseMapper.insert(kb) > 0;
    }

    @Override
    public List<RagKnowledgeBase> list() {
        return ragKnowledgeBaseMapper.selectList(null);
    }

    @Override
    public RagKnowledgeBase getById(Long id) {
        return ragKnowledgeBaseMapper.selectById(id);
    }

    @Override
    public boolean update(RagKnowledgeBase kb) {
        return ragKnowledgeBaseMapper.updateById(kb) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return ragKnowledgeBaseMapper.deleteById(id) > 0;
    }

    @Override
    public Integer toggleStatus(Long id) {
        // 查出当前状态
        RagKnowledgeBase kb = ragKnowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new RuntimeException("知识库不存在");
        }
        // 反转状态：1→0，0→1
        Integer newStatus = kb.getStatus() == 1 ? 0 : 1;
        kb.setStatus(newStatus);
        ragKnowledgeBaseMapper.updateById(kb);
        return newStatus;
    }
}
