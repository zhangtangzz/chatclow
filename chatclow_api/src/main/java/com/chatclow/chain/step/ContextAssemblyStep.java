package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.common.ChatRole;
import com.chatclow.context.ChatContext;
import com.chatclow.entity.*;
import com.chatclow.mapper.*;
import com.chatclow.util.AesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 第1步：上下文组装（@Order = 10）
 *
 * <pre>
 * 输入参数来源：
 *   ChatServiceImpl → ctx.agentId, ctx.userId, ctx.message, ctx.conversationId
 *
 * 本步骤产出（写入 ctx，给后续所有 Step 使用）：
 *   ctx.agent         — 查询 ai_agent 表
 *   ctx.model          — 查询 ai_model 表
 *   ctx.conversation   — 获取或新建会话（agent_conversation 表）
 *   ctx.history        — 加载历史消息（agent_conversation_record 表）
 *
 * 调用方法：
 *   aiAgentMapper.selectById()             → 加载智能体
 *   aiModelMapper.selectById()             → 加载模型
 *   conversationMapper.selectById/insert()  → 获取/创建会话
 *   recordMapper.selectList()              → 加载历史（⚠ 在保存当前消息之前）
 *   recordMapper.insert()                  → 保存当前用户消息
 *
 * ⚠ 历史加载必须在保存当前消息之前，否则历史会包含自己，FC 二次请求时消息会重复。
 * </pre>
 */
@Component
@Order(10)
public class ContextAssemblyStep implements ChatChainStep {

    private static final Logger log = LoggerFactory.getLogger(ContextAssemblyStep.class);

    @Autowired private AiAgentMapper aiAgentMapper;
    @Autowired private AiModelMapper aiModelMapper;
    @Autowired private AgentConversationMapper conversationMapper;
    @Autowired private AgentConversationRecordMapper recordMapper;
    @Autowired private UserDocumentMapper userDocumentMapper;

    @Override
    public void process(ChatContext ctx) {
        // ① 加载智能体配置
        AiAgent agent = aiAgentMapper.selectById(ctx.getAgentId());
        if (agent == null) throw new RuntimeException("智能体不存在，agentId=" + ctx.getAgentId());
        ctx.setAgent(agent);

        // ② 加载模型配置
        AiModel model = aiModelMapper.selectById(agent.getModelId());
        if (model == null) {
            // 兜底：绑定的模型不存在时，取第一个可用模型
            List<AiModel> available = aiModelMapper.selectList(
                    new LambdaQueryWrapper<AiModel>().eq(AiModel::getStatus, 1).last("LIMIT 1"));
            model = available.isEmpty() ? null : available.get(0);
        }
        if (model == null) throw new RuntimeException("模型配置不存在，modelId=" + agent.getModelId() + "，且无可用模型替代");
        // 解密 API 密钥（数据库中加密存储，使用前解密）
        if (model.getApiKey() != null) {
            try {
                model.setApiKey(AesUtil.decrypt(model.getApiKey()));
            } catch (Exception e) {
                log.warn("API 密钥解密失败（可能是旧数据未加密），使用原值: {}", e.getMessage());
            }
        }
        ctx.setModel(model);

        // ③ 获取或创建会话
        AgentConversation conversation;
        if (ctx.getConversationId() != null) {
            conversation = conversationMapper.selectById(ctx.getConversationId());
            if (conversation == null) throw new RuntimeException("会话不存在，conversationId=" + ctx.getConversationId());
            // 已有会话但没有模型信息时补充
            if (conversation.getModelId() == null) {
                conversation.setModelId(model.getId());
                conversation.setModelName(model.getName());
                conversationMapper.updateById(conversation);
            }
        } else {
            conversation = new AgentConversation();
            conversation.setUserId(ctx.getUserId());
            conversation.setTitle(ctx.getMessage().length() > 20
                    ? ctx.getMessage().substring(0, 20) + "..."
                    : ctx.getMessage());
            conversation.setModelId(model.getId());
            conversation.setModelName(model.getName());
            conversation.setAgentId(agent.getId());
            conversation.setCreatedDt(LocalDateTime.now());
            conversationMapper.insert(conversation);
        }
        ctx.setConversation(conversation);

        // ④ 加载历史消息（⚠ 必须在保存当前消息之前！记忆关闭时跳过）
        if (ctx.isMemoryEnabled()) {
            LambdaQueryWrapper<AgentConversationRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AgentConversationRecord::getConversationId, conversation.getId());
            wrapper.orderByAsc(AgentConversationRecord::getCreatedDt);
            ctx.setHistory(recordMapper.selectList(wrapper));
        } else {
            ctx.setHistory(List.of());
        }

        // ⑤ 保存当前用户消息
        AgentConversationRecord userRecord = new AgentConversationRecord();
        userRecord.setConversationId(conversation.getId());
        userRecord.setRole(ChatRole.USER);
        userRecord.setContent(ctx.getMessage());
        userRecord.setCreatedDt(LocalDateTime.now());
        recordMapper.insert(userRecord);

        // ⑥ 加载对话上传的文件
        if (ctx.getFileIds() != null && !ctx.getFileIds().isEmpty()) {
            List<UserDocument> files = userDocumentMapper.selectBatchIds(ctx.getFileIds());
            ctx.setChatFiles(files);
        }
    }

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        return false; // 永远不跳过，这是整条链的起点
    }
}
