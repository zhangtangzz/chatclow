package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
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
 * <h3>这个 Step 干什么？</h3>
 * <p>从数据库加载一次对话需要的所有基础数据，相当于"做菜前先把所有食材备好"：</p>
 * <ol>
 *   <li>根据 agentId 查出 Agent 配置</li>
 *   <li>根据 Agent 的 modelId 查出模型配置</li>
 *   <li>获取或新建会话</li>
 *   <li>加载历史消息</li>
 *   <li>保存当前用户消息到数据库</li>
 * </ol>
 * <p>做完这些，后续步骤就可以直接从 ctx 取数据，不用再查数据库了。</p>
 *
 * <h3>为什么永远不跳过？</h3>
 * <p>不管是流式还是非流式，有 RAG 还是没有，这些基础数据都是必须要的。
 * 所以 shouldSkip() 直接返回 false。</p>
 *
 * <h3>⚠️ 一个踩过的坑：顺序问题</h3>
 * <p>步骤4（加载历史）必须在步骤5（保存用户消息）之前执行。
 * 一开始是反过来的——先保存再加载历史，导致加载出来的历史里包含了刚插入的当前消息。
 * FC 二次请求时 MessageBuildStep 遍历历史又加了一次，结果 user 消息出现了两份。</p>
 * <p><b>教训：</b>先查询、再插入，保证查询结果不包含自己。</p>
 */
@Component
@Order(10)  // 数字最小 = 第一个执行
public class ContextAssemblyStep implements ChatChainStep {

    // ──── 四个 Mapper：负责从对应的数据库表加载数据 ────

    @Autowired
    private AiAgentMapper aiAgentMapper;                 // ai_agent 表

    @Autowired
    private AiModelMapper aiModelMapper;                  // ai_model 表

    @Autowired
    private AgentConversationMapper conversationMapper;   // agent_conversation 表

    @Autowired
    private AgentConversationRecordMapper recordMapper;   // agent_conversation_record 表

    @Override
    public void process(ChatContext ctx) {

        // ──────── 步骤1：加载智能体配置 ────────
        // agent 表里有 systemPrompt、modelId、kbId、知识库开关等关键配置
        AiAgent agent = aiAgentMapper.selectById(ctx.getAgentId());
        if (agent == null) {
            throw new RuntimeException("智能体不存在，agentId=" + ctx.getAgentId());
        }
        ctx.setAgent(agent);

        // ──────── 步骤2：加载模型配置 ────────
        // model 表里有 apiUrl、apiKey、modelCode
        AiModel model = aiModelMapper.selectById(agent.getModelId());
        if (model == null) {
            throw new RuntimeException("模型配置不存在，modelId=" + agent.getModelId());
        }
        ctx.setModel(model);

        // ──────── 步骤3：获取或创建会话 ────────
        AgentConversation conversation;
        if (ctx.getConversationId() != null) {
            // 续写已有会话：直接从数据库加载
            conversation = conversationMapper.selectById(ctx.getConversationId());
            if (conversation == null) {
                throw new RuntimeException("会话不存在，conversationId=" + ctx.getConversationId());
            }
        } else {
            // 新会话：创建一条记录，标题取用户消息前20个字符
            conversation = new AgentConversation();
            conversation.setUserId(ctx.getUserId());
            conversation.setTitle(
                    ctx.getMessage().length() > 20
                            ? ctx.getMessage().substring(0, 20) + "..."
                            : ctx.getMessage()
            );
            conversation.setCreatedDt(LocalDateTime.now());
            conversationMapper.insert(conversation);
            System.out.println("[Context] 创建新会话，id=" + conversation.getId());
        }
        ctx.setConversation(conversation);

        // ──────── 步骤4：加载历史消息（⚠️ 必须在步骤5之前！）────────
        // 按创建时间升序排列，保证消息顺序正确
        LambdaQueryWrapper<AgentConversationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConversationRecord::getConversationId, conversation.getId());
        wrapper.orderByAsc(AgentConversationRecord::getCreatedDt);
        List<AgentConversationRecord> history = recordMapper.selectList(wrapper);
        ctx.setHistory(history);
        System.out.println("[Context] 加载历史消息 " + history.size() + " 条");

        // ──────── 步骤5：保存当前用户消息（⚠️ 在步骤4之后，防止历史包含自己）────────
        AgentConversationRecord userRecord = new AgentConversationRecord();
        userRecord.setConversationId(conversation.getId());
        userRecord.setRole("user");
        userRecord.setContent(ctx.getMessage());
        userRecord.setCreatedDt(LocalDateTime.now());
        recordMapper.insert(userRecord);
        System.out.println("[Context] 保存用户消息，recordId=" + userRecord.getId());
    }

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        return false;  // 永远不跳过 —— 这是整条链的起点，没有它后面全跑不了
    }
}
