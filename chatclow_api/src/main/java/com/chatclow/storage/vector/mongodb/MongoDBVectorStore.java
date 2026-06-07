package com.chatclow.storage.vector.mongodb;

import com.chatclow.entity.RagChunk;
import com.chatclow.storage.vector.ChatClowVectorStore;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MongoDB 向量存储实现
 * 向量存 JSON Array，Java 计算余弦相似度
 */
@Component
@ConditionalOnProperty(name = "mongodb.enabled", havingValue = "true")
public class MongoDBVectorStore implements ChatClowVectorStore {

    private static final Logger log = LoggerFactory.getLogger(MongoDBVectorStore.class);
    private static final String COLLECTION_NAME = "rag_vectors";

    @Autowired
    private MongoDatabase mongoDatabase;

    @Override
    public boolean add(RagChunk chunk) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(COLLECTION_NAME);

        Document doc = new Document()
                .append("chunk_id", chunk.getId())
                .append("kb_id", chunk.getKbId())
                .append("doc_id", chunk.getDocumentId())
                .append("content", chunk.getContent())
                .append("vector", parseVectorString(chunk.getVectorData()));

        collection.insertOne(doc);
        log.info("[MongoDBVectorStore] 插入向量 ID=" + chunk.getId());
        return true;
    }

    @Override
    public boolean addBatch(List<RagChunk> chunks) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(COLLECTION_NAME);

        List<Document> docs = new ArrayList<>();
        for (RagChunk chunk : chunks) {
            docs.add(new Document()
                    .append("chunk_id", chunk.getId())
                    .append("kb_id", chunk.getKbId())
                    .append("doc_id", chunk.getDocumentId())
                    .append("content", chunk.getContent())
                    .append("vector", parseVectorString(chunk.getVectorData())));
        }

        collection.insertMany(docs);
        log.info("[MongoDBVectorStore] 批量插入 " + chunks.size() + " 条");
        return true;
    }

    @Override
    public List<RagChunk> search(float[] queryVector, Long kbId, int topK, double threshold) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(COLLECTION_NAME);

        // 1. 查出该知识库的所有向量
        Bson filter = Filters.eq("kb_id", kbId);
        FindIterable<Document> iterable = collection.find(filter);

        // 2. 计算余弦相似度
        List<ScoreDoc> scored = new ArrayList<>();
        for (Document doc : iterable) {
            @SuppressWarnings("unchecked")
            List<Double> vectorList = (List<Double>) doc.get("vector");
            float[] docVector = listToFloatArray(vectorList);

            double score = cosineSimilarity(queryVector, docVector);
            if (score >= threshold) {
                scored.add(new ScoreDoc(
                        (Long) doc.get("chunk_id"),
                        (String) doc.get("content"),
                        score
                ));
            }
        }

        // 3. 按相似度降序排序
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // 4. 取 topK
        List<RagChunk> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            ScoreDoc sd = scored.get(i);
            RagChunk chunk = new RagChunk();
            chunk.setId(sd.id);
            chunk.setContent(sd.content);
            results.add(chunk);
        }

        log.info("[MongoDBVectorStore] 检索完成，返回 " + results.size() + " 条");
        return results;
    }

    @Override
    public boolean deleteByDocument(Long documentId) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(COLLECTION_NAME);
        collection.deleteMany(Filters.eq("doc_id", documentId));
        log.info("[MongoDBVectorStore] 删除文档 ID=" + documentId);
        return true;
    }

    @Override
    public boolean test() {
        try {
            mongoDatabase.listCollectionNames().first();
            return true;
        } catch (Exception e) {
            log.warn("[MongoDBVectorStore] 连接失败: {}", e.getMessage());
            return false;
        }
    }

    // ========== 工具方法 ==========

    /**
     * 将 JSON 数组字符串 "[0.1,0.2,0.3]" 解析为 List<Double>
     */
    private List<Double> parseVectorString(String vectorData) {
        List<Double> list = new ArrayList<>();
        String trimmed = vectorData.trim();
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        String[] parts = trimmed.split(",");
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                list.add(Double.parseDouble(part));
            }
        }
        return list;
    }

    private float[] listToFloatArray(List<Double> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i).floatValue();
        return array;
    }

    private double cosineSimilarity(float[] v1, float[] v2) {
        double dot = 0, norm1 = 0, norm2 = 0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private static class ScoreDoc {
        Long id;
        String content;
        double score;

        ScoreDoc(Long id, String content, double score) {
            this.id = id;
            this.content = content;
            this.score = score;
        }
    }
}
