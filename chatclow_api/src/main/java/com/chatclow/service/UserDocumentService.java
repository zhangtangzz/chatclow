package com.chatclow.service;

import com.chatclow.entity.UserDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserDocumentService {

    /**
     * 上传文件并自动处理（提取文本 → 切片 → 向量化）
     */
    Long upload(MultipartFile file, Long userId);

    /**
     * 获取用户的文档列表
     */
    List<UserDocument> listByUserId(Long userId);

    /**
     * 删除文档及其切片
     */
    void delete(Long id, Long userId);

    /**
     * 搜索用户的个人 RAG
     */
    List<String> search(Long userId, String query, int topK);
}
