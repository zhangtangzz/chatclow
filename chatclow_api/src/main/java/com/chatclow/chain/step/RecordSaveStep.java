package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.context.ChatContext;
import com.chatclow.entity.AgentConversationRecord;
import com.chatclow.mapper.AgentConversationRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 第5步：保存 AI 回复到数据库（@Order = 50）
 *
 * <h3>这个 Step 干什么？</h3>
 * <p>
 * 把 AI 生成的回复作为一条 "assistant" 角色消息写入 agent_conversation_record 表。
 * 这样下次同一个会话再来请求时，ContextAssemblyStep 加载的历史里就包含这条回复了。
 * </p>
 *
 * <h3>为什么独立成一个 Step？</h3>
 * <p>
 * 重构前，保存逻辑分散在 ChatServiceImpl 的 saveRecord() 私有方法里。
 * 独立成 Step 后有几个好处：
 * </p>
 * <ul>
 *   <li><b>职责单一</b>：只负责存数据库，不管别的</li>
 *   <li><b>可控跳过</b>：AI 返回空内容就不存（shouldSkip）</li>
 *   <li><b>位置固定</b>：在 AI 调用之后、关闭连接之前执行，保证一定被保存</li>
 * </ul>
 *
 * <h3>fullReply vs aiResponse</h3>
 * <p>
 * 流式模式：fullReply 由 ModelCallStep（和可能的 FunctionCallingStep）逐字拼接累积
 * 非流模式：SyncAiCallStep 一次性 setFullReply
 * 用 ctx.getFullReplyText() 兼容两种模式
 * </p>
 *
 * <h3>和 Snail AI 的对比</h3>
 * <p>
 * Snail 把保存回复的逻辑放在 LlmCallHandler（最后一个 Handler）里附带处理，
 * ChatClow 拆成了 RecordSaveStep（存数据库）和 ResponseFinalizeStep（关连接）两个独立步骤。
 * 分离的好处：如果以后不想发 DONE 事件但还是要存记录，跳过 Finalize 就行，互相不影响。
 * </p>
 */
@Component
@Order(50)
public class RecordSaveStep implements ChatChainStep {

    @Autowired
    private AgentConversationRecordMapper recordMapper;

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        // 空的回复没有保存价值（AI 出错或流中断时可能发生）
        return ctx.getFullReply().length() == 0;
    }

    @Override
    public void process(ChatContext ctx) {
        // 构建一条 "assistant" 角色消息
        AgentConversationRecord record = new AgentConversationRecord();
        record.setConversationId(ctx.getConversation().getId());
        record.setRole("assistant");
        // getFullReplyText() 兼容流式（fullReply）和非流式（aiResponse）
        record.setContent(ctx.getFullReplyText());
        recordMapper.insert(record);
        System.out.println("[RecordSaveStep] 已保存回复，长度: " + ctx.getFullReply().length());
    }
}
