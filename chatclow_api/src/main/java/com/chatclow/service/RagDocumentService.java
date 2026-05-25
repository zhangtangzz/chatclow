package com.chatclow.service;

import com.chatclow.entity.RagDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RagDocumentService {

    /**
     * 新增文档记录
     */
    boolean add(RagDocument document);

    /**
     * 按知识库ID查文档列表
     */
    List<RagDocument> listByKbId(Long kbId);

    /**
     * 按ID查询
     */
    RagDocument getById(Long id);

    /**
     * 更新文档状态和信息
     */
    boolean update(RagDocument document);

    /**
     * 删除文档
     */
    boolean deleteById(Long id);

    /**
     * 处理文档：切片 + 向量化 + 存储（RAG 全链路）
     */
    void processDocument(Long documentId);


    /**
     * 上传文件并异步处理（切片+向量化）
     * @return 文档ID（立即返回，处理在后台进行）
     */
    Long uploadAndProcess(MultipartFile file, Long kbId);
}
