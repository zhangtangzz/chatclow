package com.chatclow.service;

import com.chatclow.entity.AiModel;
import java.util.List;

public interface AiModelService {

    /**
     * 新增模型配置
     */
    boolean add(AiModel aiModel);

    /**
     * 查询所有启用的模型（status=1）
     */
    List<AiModel> listEnabled();

    /**
     * 根据ID查询模型
     */
    AiModel getById(Long id);

    /**
     * 更新模型配置
     */
    boolean update(AiModel aiModel);

    /**
     * 删除模型
     */
    boolean deleteById(Long id);

    /**
     * 切换状态：启用↔禁用
     * 传回切换后的状态值
     */
    Integer toggleStatus(Long id);
}
