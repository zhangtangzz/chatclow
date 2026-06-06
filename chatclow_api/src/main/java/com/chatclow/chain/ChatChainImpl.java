package com.chatclow.chain;

import com.chatclow.context.ChatContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 责任链执行器实现
 *
 * <p>Spring 自动收集所有 ChatChainStep Bean，按 @Order 排序注入。
 * 新增 Step 只需写类加 @Component + @Order(n)，无需修改本类。</p>
 *
 * <p>步骤顺序（@Order 值）：</p>
 * <pre>
 *   [10] ContextAssembly  → 加载 Agent/Model/会话/历史
 *   [20] Rag              → 向量检索 + 关键字检索 + Re-rank
 *   [25] MessageBuild     → 构建 messages + tools JSON
 *   [30] ModelCall        → 仅流式 ┐ 同 Order，通过 shouldSkip 互斥
 *   [30] SyncAiCall       → 仅非流式 ┘
 *   [40] FunctionCalling  → 仅流式且检测到 tool_calls
 *   [50] RecordSave       → 保存 AI 回复到数据库
 *   [60] Finalize         → 发送 DONE，关闭 SSE 连接
 * </pre>
 */
@Component
public class ChatChainImpl implements ChatChain {

    private final List<ChatChainStep> steps;

    public ChatChainImpl(List<ChatChainStep> steps) {
        this.steps = steps;
    }

    @Override
    public void execute(ChatContext ctx) {
        for (ChatChainStep step : steps) {
            if (step.shouldSkip(ctx)) {
                System.out.println("[Chain] 跳过: " + step.getClass().getSimpleName());
                continue;
            }
            System.out.println("[Chain] 执行: " + step.getClass().getSimpleName());
            step.process(ctx);
        }
    }
}
