package com.chatclow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告实体类
 * 对应数据库表：chatclow_announcement
 */
@Data
@TableName("chatclow_announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    // 状态：0=禁用，1=启用
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdDt;

    private LocalDateTime updatedDt;
}
