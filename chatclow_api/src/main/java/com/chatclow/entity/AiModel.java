package com.chatclow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * AI模型配置实体类
 * 对应数据库表：chatclow_ai_model
 */

@Data
@TableName("chatclow_ai_model")
public class AiModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "模型名称不能为空")
    private String name;

    @NotBlank(message = "提供商不能为空")
    private String provider;

    @NotBlank(message = "模型编码不能为空")
    private String modelCode;

    @NotBlank(message = "API地址不能为空")
    private String apiUrl;

    @NotBlank(message = "API密钥不能为空")
    private String apiKey;

    // 状态：0=禁用，1=启用（有默认值，不强制要求前端传）
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdDt;
}