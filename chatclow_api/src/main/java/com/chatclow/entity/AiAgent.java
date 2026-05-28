package com.chatclow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * AI智能体实体类
 * 对应数据库表：chatclow_ai_agent
 */
@Data
@TableName("chatclow_ai_agent")
public class AiAgent {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "智能体名称不能为空")
    private String name;

    private String description;

    private String avatar;

    @NotBlank(message = "系统提示词不能为空")
    private String systemPrompt;//驼峰名，MyBatis-Puls会自动映射

    @NotNull(message = "绑定的模型ID不能为空")
    private Long modelId;

    @NotNull(message = "创建者用户ID不能为空")
    private Long userId;

    // 状态：0=禁用，1=启用
    private Integer status;

    // 是否启用知识库 RAG：0=关闭，1=开启
    private Integer kbEnabled;

    // 绑定的知识库ID
    private Long kbId;

    // ===== 搜索增强引擎配置 =====

    // 检索返回条数（默认6）：双路检索取多少条候选
    private Integer ragTopK;

    // 最终上下文条数（默认3）：Re-rank 后保留多少条送给 LLM
    private Integer ragFinalK;

    // 向量相似度阈值（默认0.5）：低于此值的切片认为不相关
    private Float ragSimilarityThreshold;

    // 是否启用关键字检索（默认1）：0=仅向量检索，1=双路检索
    private Integer ragKeywordEnabled;

    // 是否启用 Re-rank 重排序（默认1）：0=关闭，1=开启
    private Integer ragRerankEnabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdDt;


}
