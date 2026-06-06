package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.common.ChatRole;
import com.chatclow.context.ChatContext;
import com.chatclow.entity.AiAgent;
import com.chatclow.entity.AiModel;
import com.chatclow.entity.AgentConversation;
import com.chatclow.entity.AgentConversationRecord;
import com.chatclow.mapper.AiAgentMapper;
import com.chatclow.mapper.AiModelMapper;
import com.chatclow.mapper.AgentConversationMapper;
import com.chatclow.mapper.AgentConversationRecordMapper;
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

    @Autowired private AiAgentMapper aiAgentMapper;
    @Autowired private AiModelMapper aiModelMapper;
    @Autowired private AgentConversationMapper conversationMapper;
    @Autowired private AgentConversationRecordMapper recordMapper;

    @Override
    public void process(ChatContext ctx) {
        // ① 加载智能体配置
        AiAgent agent = aiAgentMapper.selectById(ctx.getAgentId());
        if (agent == null) throw new RuntimeException("智能体不存在，agentId=" + ctx.getAgentId());
        ctx.setAgent(agent);

        // ② 加载模型配置
        AiModel model = aiModelMapper.selectById(agent.getModelId());
        if (model == null) throw new RuntimeException("模型配置不存在，modelId=" + agent.getModelId());
        ctx.setModel(model);

        // ③ 获取或创建会话
        AgentConversation conversation;
        if (ctx.getConversationId() != null) {
            conversation = conversationMapper.selectById(ctx.getConversationId());
            if (conversation == null) throw new RuntimeException("会话不存在，conversationId=" + ctx.getConversationId());
        } else {
            conversation = new AgentConversation();
            conversation.setUserId(ctx.getUserId());
            conversation.setTitle(ctx.getMessage().length() > 20
                    ? ctx.getMessage().substring(0, 20) + "..."
                    : ctx.getMessage());
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
    }

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        return false; // 永远不跳过，这是整条链的起点
    }
}
