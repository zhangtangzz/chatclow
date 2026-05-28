package com.chatclow.storage.vector.pgvector;

import com.chatclow.entity.RagChunk;
import com.chatclow.storage.vector.ChatClowVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL + pgvector 向量存储实现
 * 使用 VECTOR(1024) 类型 + HNSW 索引，余弦距离算子 <=>
 */
@Component
@ConditionalOnProperty(name = "pgvector.enabled", havingValue = "true")
public class PgVectorStore implements ChatClowVectorStore {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStore.class);

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Override
    public boolean add(RagChunk chunk) {
        String embStr = parseVectorString(chunk.getVectorData());
        String sql = "INSERT INTO rag_vectors (embedding, doc_id, content, kb_id, chunk_index) " +
                     "VALUES (?::vector, ?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql,
                    embStr,
                    chunk.getDocumentId(),
                    chunk.getContent(),
                    chunk.getKbId(),
                    chunk.getChunkIndex()
            );
            return true;
        } catch (Exception e) {
            log.error("[PgVectorStore] 添加向量失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean addBatch(List<RagChunk> chunks) {
        String sql = "INSERT INTO rag_vectors (embedding, doc_id, content, kb_id, chunk_index) " +
                     "VALUES (?::vector, ?, ?, ?, ?)";
        List<Object[]> params = new ArrayList<>();
        for (RagChunk chunk : chunks) {
            params.add(new Object[]{
                    parseVectorString(chunk.getVectorData()),
                    chunk.getDocumentId(),
                    chunk.getContent(),
                    chunk.getKbId(),
                    chunk.getChunkIndex()
            });
        }
        try {
            jdbcTemplate.batchUpdate(sql, params);
            return true;
        } catch (Exception e) {
            log.error("[PgVectorStore] 批量添加向量失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<RagChunk> search(float[] queryVector, Long kbId, int topK, double threshold) {
        String queryStr = floatArrayToPgVector(queryVector);
        String sql = "SELECT id, doc_id, content, chunk_index, " +
                     "1 - (embedding <=> ?::vector) AS score " +
                     "FROM rag_vectors WHERE kb_id = ? " +
                     "AND 1 - (embedding <=> ?::vector) >= ? " +
                     "ORDER BY embedding <=> ?::vector LIMIT ?";
        try {
            return jdbcTemplate.query(sql,
                    new Object[]{queryStr, kbId, queryStr, threshold, queryStr, topK},
                    (rs, rowNum) -> {
                        RagChunk v = new RagChunk();
                        v.setId(rs.getLong("id"));
                        v.setDocumentId(rs.getLong("doc_id"));
                        v.setContent(rs.getString("content"));
                        v.setChunkIndex(rs.getInt("chunk_index"));
                        v.setScore(rs.getFloat("score"));
                        return v;
                    }
            );
        } catch (Exception e) {
            log.error("[PgVectorStore] 向量检索失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deleteByDocument(Long documentId) {
        String sql = "DELETE FROM rag_vectors WHERE doc_id = ?";
        try {
            jdbcTemplate.update(sql, documentId);
            return true;
        } catch (Exception e) {
            log.error("[PgVectorStore] 删除向量失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean test() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rag_vectors LIMIT 1", Integer.class);
            return count != null;
        } catch (Exception e) {
            log.error("[PgVectorStore] 测试连接失败: {}", e.getMessage());
            return false;
        }
    }

    // ========== 工具方法 ==========

    /**
     * 将 float[] 转为 pgvector 接受的字符串格式 "[0.1,0.2,0.3]"
     */
    private String floatArrayToPgVector(float[] arr) {
        if (arr == null || arr.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 将 JSON 数组字符串 "[0.1,0.2,0.3]" 解析为 pgvector 字符串格式
     */
    private String parseVectorString(String vectorData) {
        if (vectorData == null || vectorData.isEmpty()) return "[]";
        String trimmed = vectorData.trim();
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return "[" + trimmed + "]";
    }
}
