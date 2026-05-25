package com.chatclow.service;

import java.util.List;

/**
 * 文本向量化服务接口
 */
public interface EmbeddingService {

    /**
     * 将单条文本转为向量
     * @param text 输入文本
     * @return 浮点数向量数组
     */
    float[] embed(String text);

    /**
     * 批量将多条文本转为向量
     * @param texts 输入文本列表
     * @return 向量列表，每条对应输入的一个向量
     */
    List<float[]> batchEmbed(List<String> texts);
}
