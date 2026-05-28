package com.chatclow.service;

import com.chatclow.entity.RagChunk;

import java.util.List;

/**
 * 向量搜索服务接口
 */
public interface VectorSearchService {

    /**
     * 向量检索：根据用户问题的向量找最相关的切片
     * 擅长：语义理解（"年假"能匹配到"休假制度"）
     */
    List<RagChunk> search(float[] questionVector, Long kbId, int topK);

    /**
     * 向量检索（带相似度阈值）
     * @param questionVector  问题向量
     * @param kbId            知识库ID
     * @param topK            返回条数
     * @param threshold       相似度阈值（低于此值的切片认为不相关）
     */
    List<RagChunk> search(float[] questionVector, Long kbId, int topK, float threshold);

    /**
     * 关键字检索：根据用户输入的关键词做模糊匹配
     * 擅长：精确匹配（"3.2.1条款"、"2024年Q3"）
     */
    List<RagChunk> keywordSearch(String query, Long kbId, int topK);

    /**
     * 双路混合检索：向量 + 关键字两条路一起跑，结果合并去重
     * 取两家之长，效果最好
     */
    List<RagChunk> hybridSearch(float[] questionVector, String query, Long kbId, int topK);

    /**
     * 双路混合检索（带全部配置参数）
     * @param questionVector      问题向量
     * @param query               用户问题原文
     * @param kbId                知识库ID
     * @param topK                检索返回条数
     * @param threshold           向量相似度阈值
     * @param keywordEnabled      是否启用关键字检索（0=仅向量，1=双路）
     */
    List<RagChunk> hybridSearch(float[] questionVector, String query, Long kbId,
                                 int topK, float threshold, int keywordEnabled);
}
