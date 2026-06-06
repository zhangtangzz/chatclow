package com.chatclow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;


@TableName("chatclow_rag_kb")
public class RagKnowledgeBase {

    /** 向量存储实例ID（外键关联 store_instance.id） */
    @TableField("store_instance_id")
    private Long storeInstanceId;

    /** 向量存储类型（冗余字段，方便查询） */
    private String vectorStoreType;


    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;              // 知识库名称

    private String description;       // 描述

    private String icon;              // 图标

    private Long embeddingModelId;    // 绑定的Embedding模型ID（向量化用）

    private String ragEnhancement;    // RAG增强提示词

    private Integer status;           // 状态: 0=禁用 1=启用

    /** 创建者用户ID */
    private Long userId;

    private LocalDateTime createdDt;

    private LocalDateTime updatedDt;

    // 无参构造器
    public RagKnowledgeBase() {}

    // Getter 和 Setter（IDE生成或手写）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Long getEmbeddingModelId() { return embeddingModelId; }
    public void setEmbeddingModelId(Long embeddingModelId) { this.embeddingModelId = embeddingModelId; }

    public String getRagEnhancement() { return ragEnhancement; }
    public void setRagEnhancement(String ragEnhancement) { this.ragEnhancement = ragEnhancement; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getCreatedDt() { return createdDt; }
    public void setCreatedDt(LocalDateTime createdDt) { this.createdDt = createdDt; }

    public LocalDateTime getUpdatedDt() { return updatedDt; }
    public void setUpdatedDt(LocalDateTime updatedDt) { this.updatedDt = updatedDt; }

    public Long getStoreInstanceId() { return storeInstanceId; }
    public void setStoreInstanceId(Long storeInstanceId) { this.storeInstanceId = storeInstanceId; }

    public String getVectorStoreType() { return vectorStoreType; }
    public void setVectorStoreType(String vectorStoreType) { this.vectorStoreType = vectorStoreType; }



}
