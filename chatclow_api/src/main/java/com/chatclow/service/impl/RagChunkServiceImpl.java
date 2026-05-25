package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.RagChunk;
import com.chatclow.mapper.RagChunkMapper;
import com.chatclow.service.RagChunkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagChunkServiceImpl implements RagChunkService {

    @Autowired
    private RagChunkMapper ragChunkMapper;

    @Override
    public boolean add(RagChunk chunk) {
        return ragChunkMapper.insert(chunk) > 0;
    }

    @Override
    public List<RagChunk> listByKbId(Long kbId) {
        LambdaQueryWrapper<RagChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagChunk::getKbId, kbId);
        wrapper.orderByAsc(RagChunk::getChunkIndex);  // 按顺序排列
        return ragChunkMapper.selectList(wrapper);
    }

    @Override
    public List<RagChunk> listByDocumentId(Long documentId) {
        LambdaQueryWrapper<RagChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagChunk::getDocumentId, documentId);
        wrapper.orderByAsc(RagChunk::getChunkIndex);
        return ragChunkMapper.selectList(wrapper);
    }

    @Override
    public int batchInsert(List<RagChunk> chunks) {
        // MyBatis-Plus 的 saveBatch 需要 IService，这里用循环逐条插入
        int count = 0;
        for (RagChunk chunk : chunks) {
            count += ragChunkMapper.insert(chunk);
        }
        return count;
    }

    @Override
    public RagChunk getById(Long id) {
        return ragChunkMapper.selectById(id);
    }

    @Override
    public int deleteByDocumentId(Long documentId) {
        LambdaQueryWrapper<RagChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagChunk::getDocumentId, documentId);
        return ragChunkMapper.delete(wrapper);
    }
}
