package com.chatclow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话会话实体
 * 对应数据库表：chatclow_ai_conversation
 */


@Data
@TableName("chatclow_ai_conversation")
public class AgentConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    //用户ID（属于哪个用户）
    private Long userId;

    //对话标题
    private String title;

    //创建时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdDt;

    /** Token 累计消耗量 */
    private Integer totalTokens;

    /** 使用的模型ID */
    private Long modelId;

    /** 使用的模型名称 */
    private String modelName;

    /** 关联的智能体ID */
    private Long agentId;
}
