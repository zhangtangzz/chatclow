package com.chatclow.service;

import com.chatclow.entity.RagChunk;

import java.util.List;

/**
 * 向量搜索服务接口
 */
public interface VectorSearchService {

    /**
     * 根据用户问题搜索最相关的知识库切片
     * @param questionVector 用户问题的向量
     * @param kbId 知识库ID
     * @param topK 返回前几条（默认3）
     * @return 按相似度排序的切片列表
     */
    List<RagChunk> search(float[] questionVector, Long kbId, int topK);
}
