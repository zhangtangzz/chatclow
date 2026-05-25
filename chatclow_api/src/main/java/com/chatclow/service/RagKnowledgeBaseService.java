package com.chatclow.service;

import com.chatclow.entity.RagKnowledgeBase;
import java.util.List;

public interface RagKnowledgeBaseService {

    /**
     * 新建知识库
     */
    boolean add(RagKnowledgeBase kb);

    /**
     * 查询所有知识库列表
     */
    List<RagKnowledgeBase> list();

    /**
     * 按ID查询
     */
    RagKnowledgeBase getById(Long id);

    /**
     * 更新知识库
     */
    boolean update(RagKnowledgeBase kb);

    /**
     * 删除知识库
     */
    boolean deleteById(Long id);

    /**
     * 切换启用/禁用状态
     */
    Integer toggleStatus(Long id);
}
