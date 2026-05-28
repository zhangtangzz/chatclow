package com.chatclow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("chatclow_rag_chunk")
public class RagChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;               // 所属知识库ID

    private Long documentId;         // 所属文档ID

    private Integer chunkIndex;      // 切片序号（第几段）

    private String content;          // 切片内容

    private Integer tokenCount;      // Token数量

    private String vectorData;       // 向量数据（JSON数组格式存储）

    private String contentHash;      // 内容哈希（去重用）

    private LocalDateTime createdDt;

    @TableField(exist = false)
    private float score;  // 向量检索相似度得分（非持久化）

    public RagChunk() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getKbId() { return kbId; }
    public void setKbId(Long kbId) { this.kbId = kbId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }

    public String getVectorData() { return vectorData; }
    public void setVectorData(String vectorData) { this.vectorData = vectorData; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public LocalDateTime getCreatedDt() { return createdDt; }
    public void setCreatedDt(LocalDateTime createdDt) { this.createdDt = createdDt; }

    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }
}
