package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.common.ChatRole;
import com.chatclow.context.ChatContext;
import com.chatclow.entity.AgentConversation;
import com.chatclow.entity.AgentConversationRecord;
import com.chatclow.mapper.AgentConversationMapper;
import com.chatclow.mapper.AgentConversationRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 第5步：保存 AI 回复到数据库 + 累计 Token 消耗（@Order = 50）
 *
 * <pre>
 * 输入参数来源：
 *   ModelCallStep / SyncAiCallStep → ctx.fullReply, ctx.tokenUsage, ctx.conversation
 *
 * 本步骤写入：
 *   agent_conversation_record 表 ← role="assistant" 消息
 *   agent_conversation 表       ← total_tokens += ctx.tokenUsage
 *
 * 调用方法：
 *   recordMapper.insert()
 *   conversationMapper.updateById()
 * </pre>
 */
@Component
@Order(50)
public class RecordSaveStep implements ChatChainStep {

    private static final Logger log = LoggerFactory.getLogger(RecordSaveStep.class);

    @Autowired private AgentConversationRecordMapper recordMapper;
    @Autowired private AgentConversationMapper conversationMapper;

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        return ctx.getFullReply().length() == 0;
    }

    @Override
    public void process(ChatContext ctx) {
        // 保存回复
        AgentConversationRecord record = new AgentConversationRecord();
        record.setConversationId(ctx.getConversation().getId());
        record.setRole(ChatRole.ASSISTANT);
        record.setContent(ctx.getFullReplyText());
        record.setResponseTime(ctx.getResponseTime());
        recordMapper.insert(record);

        // 累计 token 消耗
        if (ctx.getTokenUsage() != null && ctx.getTokenUsage() > 0) {
            AgentConversation conv = conversationMapper.selectById(ctx.getConversation().getId());
            int current = conv.getTotalTokens() != null ? conv.getTotalTokens() : 0;
            conv.setTotalTokens(current + ctx.getTokenUsage());
            conversationMapper.updateById(conv);
            log.info("[RecordSave] 累计 token: +{} (总: {})", ctx.getTokenUsage(), conv.getTotalTokens());
        }
    }
}
