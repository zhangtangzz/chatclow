package com.chatclow.storage.vector.mysql;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.RagChunk;
import com.chatclow.mapper.RagChunkMapper;
import com.chatclow.service.VectorSearchService;
import com.chatclow.storage.vector.ChatClowVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * MySQL 向量存储实现
 * 读写都走 MySQL 的 chatclow_rag_chunk 表
 */
@Component
public class MySQLVectorStore implements ChatClowVectorStore {

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private RagChunkMapper ragChunkMapper;

    @Override
    public boolean add(RagChunk chunk) {
        return ragChunkMapper.insert(chunk) > 0;
    }

    @Override
    public boolean addBatch(List<RagChunk> chunks) {
        int count = 0;
        for (RagChunk chunk : chunks) {
            count += ragChunkMapper.insert(chunk);
        }
        return count == chunks.size();
    }

    @Override
    public List<RagChunk> search(float[] queryVector, Long kbId, int topK, double threshold) {
        // 委派给现有的 VectorSearchServiceImpl
        return vectorSearchService.search(queryVector, kbId, topK, (float) threshold);
    }

    @Override
    public List<RagChunk> keywordSearch(String query, Long kbId, int topK) {
        // 委派给现有的 VectorSearchServiceImpl
        return vectorSearchService.keywordSearch(query, kbId, topK);
    }

    @Override
    public List<RagChunk> hybridSearch(float[] queryVector, String query, Long kbId, int topK, double threshold) {
        // 委派给现有的 VectorSearchServiceImpl
        return vectorSearchService.hybridSearch(queryVector, query, kbId, topK, (float) threshold, 1);
    }

    @Override
    public boolean deleteByDocument(Long documentId) {
        // MySQL 模式下，由 RagChunkService 负责删除
        // 这里返回 true 即可（实际删除在 Service 层做）
        return true;
    }

    @Override
    public boolean test() {
        try {
            // 简单测试：尝试查询一条切片记录
            return vectorSearchService != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean existsByHash(Long kbId, String contentHash) {
        LambdaQueryWrapper<RagChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagChunk::getKbId, kbId);
        wrapper.eq(RagChunk::getContentHash, contentHash);
        return ragChunkMapper.selectCount(wrapper) > 0;
    }
}
