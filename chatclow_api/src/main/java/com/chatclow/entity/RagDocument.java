package com.chatclow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("chatclow_rag_document")
public class RagDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;               // 所属知识库ID

    private String name;             // 文件名

    private String fileType;         // 文件类型: pdf/txt/md

    private String content;          // 文档内容（纯文本存储）

    private Integer status;          // 状态: 0=待处理 1=解析中 2=处理中 3=完成 4=失败

    private String errorMsg;         // 错误信息

    private Integer chunkCount;      // 切片数量

    private String contentHash;      // 文件SHA256哈希（去重用）

    private LocalDateTime createdDt;

    private LocalDateTime updatedDt;

    public RagDocument() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getKbId() { return kbId; }
    public void setKbId(Long kbId) { this.kbId = kbId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public LocalDateTime getCreatedDt() { return createdDt; }
    public void setCreatedDt(LocalDateTime createdDt) { this.createdDt = createdDt; }

    public LocalDateTime getUpdatedDt() { return updatedDt; }
    public void setUpdatedDt(LocalDateTime updatedDt) { this.updatedDt = updatedDt; }
}
