package com.chatclow.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;


/**
 * 聊天记录实体
 * 对应数据库表：chatclow_ai_conversation_record
 */

@Data
@TableName("chatclow_ai_conversation_record")
public class AgentConversationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    //属于哪个会话类
    private Long conversationId;

    //角色：user瀛湖/assistant AI
    private String role;

    //消息内容
    private String content;

    //创建时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdDt;
}
