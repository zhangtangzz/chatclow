package com.chatclow.service;

import com.chatclow.entity.AiFunction;
import java.util.List;

public interface AiFunctionService {

    /** 新增工具 */
    void add(AiFunction aiFunction);

    /** 修改工具 */
    void update(AiFunction aiFunction);

    /** 删除工具 */
    void delete(Long id);

    /** 按ID查询详情 */
    AiFunction getById(Long id);

    /**
     * 按智能体ID查询工具列表
     * ⭐ 这个最关键！聊天时用它查某个智能体绑定了哪些工具
     */
    List<AiFunction> listByAgentId(Long agentId);
    // → 组装成 tools[] 发给 AI
}
