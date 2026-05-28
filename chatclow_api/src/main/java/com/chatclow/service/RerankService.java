package com.chatclow.service;

import java.util.List;

/**
 * Re-rank 重排序服务接口
 * 对检索结果进行重新打分排序，把最相关的推到最前面
 */
public interface RerankService {

    /**
     * 对候选文本列表进行重排序
     * @param query      用户问题
     * @param documents  候选文本列表（双路检索返回的结果）
     * @param topK       期望返回前 topK 条（经阈值过滤后可能更少）
     * @param threshold  相关性阈值：低于此分数的结果会被过滤（0~1）
     * @return 重排序后的文本列表（按相关性降序，已过滤低分）
     */
    List<String> rerank(String query, List<String> documents, int topK, float threshold);
}
