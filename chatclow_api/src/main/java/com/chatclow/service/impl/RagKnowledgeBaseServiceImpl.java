package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.RagDocument;
import com.chatclow.entity.RagKnowledgeBase;
import com.chatclow.mapper.RagKnowledgeBaseMapper;
import com.chatclow.service.RagDocumentService;
import com.chatclow.service.RagKnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RagKnowledgeBaseServiceImpl implements RagKnowledgeBaseService {

    @Autowired
    private RagKnowledgeBaseMapper ragKnowledgeBaseMapper;

    @Autowired
    private RagDocumentService ragDocumentService;

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
    @Transactional
    public boolean deleteById(Long id) {
        // 1. 查出该知识库下的所有文档
        List<RagDocument> docs = ragDocumentService.listByKbId(id);

        // 2. 逐个删除文档（ragDocumentService.deleteById 会级联清理向量存储）
        for (RagDocument doc : docs) {
            ragDocumentService.deleteById(doc.getId());
        }

        // 3. 删除知识库
        return ragKnowledgeBaseMapper.deleteById(id) > 0;
    }

    @Override
    public Integer toggleStatus(Long id) {
        RagKnowledgeBase kb = ragKnowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new RuntimeException("知识库不存在");
        }
        Integer newStatus = kb.getStatus() == 1 ? 0 : 1;
        kb.setStatus(newStatus);
        ragKnowledgeBaseMapper.updateById(kb);
        return newStatus;
    }
}
