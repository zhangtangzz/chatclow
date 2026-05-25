package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.RagChunk;
import com.chatclow.mapper.RagChunkMapper;
import com.chatclow.service.VectorSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于余弦相似度的向量搜索服务实现
 */
@Service
public class VectorSearchServiceImpl implements VectorSearchService {

    @Autowired
    private RagChunkMapper ragChunkMapper;

    /** 默认返回 Top 3 */
    private static final int DEFAULT_TOP_K = 3;

    /** 相似度阈值：低于这个值认为不相关 */
    private static final float SIMILARITY_THRESHOLD = 0.5f;

    @Override
    public List<RagChunk> search(float[] questionVector, Long kbId, int topK) {
        // 1. 查出该知识库下所有有向量的切片
        LambdaQueryWrapper<RagChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagChunk::getKbId, kbId);
        wrapper.isNotNull(RagChunk::getVectorData);  // 只查有向量的
        List<RagChunk> allChunks = ragChunkMapper.selectList(wrapper);

        if (allChunks.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 计算每条切片与问题的相似度
        List<ScoredChunk> scoredChunks = new ArrayList<>();
        for (RagChunk chunk : allChunks) {
            float[] chunkVector = parseVector(chunk.getVectorData());
            if (chunkVector != null && chunkVector.length > 0) {
                float similarity = cosineSimilarity(questionVector, chunkVector);
                if (similarity >= SIMILARITY_THRESHOLD) {
                    scoredChunks.add(new ScoredChunk(chunk, similarity));
                }
            }
        }

        // 3. 按相似度降序排列，取 topK
        List<RagChunk> result = scoredChunks.stream()
                .sorted((a, b) -> Float.compare(b.score, a.score))  // 降序
                .limit(topK)
                .map(sc -> sc.chunk)
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 余弦相似度计算
     * cos(A,B) = (A·B) / (|A| × |B|)
     */
    private float cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            return 0f;  // 维度不同无法比较
        }

        float dotProduct = 0f;     // 点积 A·B
        float normA = 0f;          // |A| 模长
        float normB = 0f;          // |B| 模长

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0f || normB == 0f) {
            return 0f;  // 零向量无意义
        }

        return dotProduct / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
    }

    /**
     * 将数据库存的字符串格式向量解析为 float[]
     * 存储格式：逗号分隔的数字字符串，如 "0.02,-0.15,0.89,..."
     */
    private float[] parseVector(String vectorData) {
        if (vectorData == null || vectorData.isEmpty()) {
            return null;
        }
        String[] parts = vectorData.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }

    /**
     * 内部类：带分数的切片（用于排序）
     */
    private static class ScoredChunk {
        RagChunk chunk;
        float score;

        ScoredChunk(RagChunk chunk, float score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
