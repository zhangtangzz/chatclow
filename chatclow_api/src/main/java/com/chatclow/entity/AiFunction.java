package com.chatclow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI工具函数定义 实体类
 * 对应表: chatclow_ai_function
 */
@Data
@TableName("chatclow_ai_function")
public class AiFunction {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属智能体ID */
    private Long agentId;

    /** 工具名称（如 get_weather）*/
    private String name;

    /** 工具描述（AI据此判断是否需要调用）*/
    private String description;

    /** 参数定义（JSON Schema 格式）*/
    private String parameters;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdDt;
}
