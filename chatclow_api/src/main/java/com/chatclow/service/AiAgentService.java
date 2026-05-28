package com.chatclow.service;

import com.chatclow.entity.AiAgent;
import java.util.List;

public interface AiAgentService {

    /**
     * 新增智能体
     */
    boolean add(AiAgent aiAgent);

    /**
     * 根据用户ID查询智能体列表
     */
    List<AiAgent> listByUserId(Long userId);

    /**
     * 查询所有智能体（管理员用，含禁用）
     */
    List<AiAgent> listAll();

    /**
     * 查询所有启用的智能体（管理员用）
     */
    List<AiAgent> listAllEnabled();

    /**
     * 根据ID查询智能体
     */
    AiAgent getById(Long id);

    /**
     * 更新智能体
     */
    boolean update(AiAgent aiAgent);

    /**
     * 删除智能体
     */
    boolean deleteById(Long id);

    /**
     * 切换启用/禁用状态
     */
    Integer toggleStatus(Long id);
}
