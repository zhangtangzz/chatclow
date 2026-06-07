package com.chatclow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("chatclow_user_document")
public class UserDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;           // 所属用户

    private String fileName;       // 原始文件名

    private String fileType;       // pdf / docx / txt / md

    private Long fileSize;         // 字节数

    private String filePath;       // 服务器路径

    private String content;        // 提取的纯文本正文

    private Integer chunkCount;    // 切片数

    private Integer status;        // 1=解析中 2=处理中 3=完成 4=失败

    private String errorMsg;       // 失败原因

    /** rag=个人文档知识库, chat=对话上传 */
    private String source;

    /** 关联会话ID（source=chat 时使用） */
    private Long conversationId;

    private LocalDateTime createdDt;

    public UserDocument() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public LocalDateTime getCreatedDt() { return createdDt; }
    public void setCreatedDt(LocalDateTime createdDt) { this.createdDt = createdDt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
}
