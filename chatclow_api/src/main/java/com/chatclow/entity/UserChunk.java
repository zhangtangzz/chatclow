package com.chatclow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("chatclow_user_chunk")
public class UserChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;             // 所属用户（隔离关键）

    private Long docId;              // 关联 user_document.id

    private Integer chunkIndex;      // 切片序号

    private String content;          // 切片内容

    private Integer tokenCount;      // 字数

    private String vectorData;       // 向量（逗号分隔 float）

    private String contentHash;      // 内容哈希（去重）

    private LocalDateTime createdDt;

    @TableField(exist = false)
    private float score;  // 检索相似度得分（非持久化）

    public UserChunk() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }

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
