package com.chatclow.storage.vector;

import com.chatclow.entity.RagChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SPI 向量存储接口
 * 所有向量存储后端（MySQL / Redis / Milvus）都必须实现这个接口
 */
public interface ChatClowVectorStore {

    /**
     * 添加单个切片向量
     */
    boolean add(RagChunk chunk);

    /**
     * 批量添加切片向量
     */
    boolean addBatch(List<RagChunk> chunks);

    /**
     * 向量检索（核心方法）
     * @param queryVector 问题向量
     * @param kbId       知识库ID（必须，因为向量是按知识库隔离的）
     * @param topK        返回条数
     * @param threshold   相似度阈值
     * @return 相关切片列表
     */
    List<RagChunk> search(float[] queryVector, Long kbId, int topK, double threshold);

    /**
     * 关键字检索（可选实现，不支持则返回空列表）
     */
    default List<RagChunk> keywordSearch(String query, Long kbId, int topK) {
        return new ArrayList<>();
    }

    /**
     * 混合检索：向量 + 关键字
     */
    default List<RagChunk> hybridSearch(float[] queryVector, String query, Long kbId, int topK, double threshold) {
        List<RagChunk> vectorResults = search(queryVector, kbId, topK, threshold);
        List<RagChunk> keywordResults = keywordSearch(query, kbId, topK);

        // 简单合并去重
        Map<Long, RagChunk> merged = new LinkedHashMap<>();
        for (RagChunk chunk : vectorResults) {
            merged.put(chunk.getId(), chunk);
        }
        for (RagChunk chunk : keywordResults) {
            merged.putIfAbsent(chunk.getId(), chunk);
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * 删除指定文档的所有切片向量
     */
    boolean deleteByDocument(Long documentId);

    /**
     * 测试连接
     */
    boolean test();

    /**
     * 检查切片内容是否已存在（去重用）
     * 默认返回 false（不重复），MySQL 后端需要覆盖此方法查询 chatclow_rag_chunk 表
     * @param kbId        知识库ID
     * @param contentHash 内容的 SHA-256 哈希
     * @return true=已存在（重复），false=不存在
     */
    default boolean existsByHash(Long kbId, String contentHash) {
        return false;
    }
}

