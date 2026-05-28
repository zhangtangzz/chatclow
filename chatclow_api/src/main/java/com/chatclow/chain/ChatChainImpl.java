package com.chatclow.chain;

import com.chatclow.context.ChatContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 责任链执行器实现 — 整个链的"发动机"
 *
 * <h3>核心机制：Spring 构造注入 + @Order 自动排序</h3>
 *
 * <pre>
 * 【为什么不需要手动注册 Step？】
 *
 * 这是我们重构的关键改进。旧版本中，ChatServiceImpl 需要：
 *   1. 手动 new 每一个 Step
 *   2. 手动按顺序加到链里
 *   3. 每增加一个 Step 都要改 ChatServiceImpl
 *
 * 现在只需要：
 *   1. Step 类上加 @Component + @Order(n)
 *   2. ChatChainImpl 构造参数写 List&lt;ChatChainStep&gt;
 *   3. Spring 自动找到所有实现类，按 @Order 排序后注入
 *
 * 新增一个 Step？写类加注解就行，ChatChainImpl 完全不用改。
 * </pre>
 *
 * <h3>双向路由：流式 vs 非流式</h3>
 *
 * <pre>
 * 整条链的步骤顺序：
 *   [10] ContextAssembly  ← 永远执行（加载用户/Agent/会话/历史）
 *   [20] Rag              ← 有知识库才执行
 *   [25] MessageBuild     ← 永远执行（构建消息和工具列表）
 *   [30] ModelCall        ← 仅流式  ┐
 *   [30] SyncAiCall       ← 仅非流式 ├ 两个 Step 同 Order，通过 shouldSkip 互斥
 *   [40] FunctionCalling  ← 仅流式且有 tool_calls
 *   [50] RecordSave       ← 有回复内容才执行
 *   [60] Finalize         ← 仅流式（关闭 SSE 连接）
 *
 * 走到 execute() 之后，ChatServiceImpl 什么都不用管了。
 * </pre>
 *
 * <h3>和 Snail AI 的对比</h3>
 * <p>
 * Snail 的 AgentChatChainService 也是 for 循环驱动，但：
 * - 没有 shouldSkip，所有 Handler 都会被调用
 * - 只支持一种模式（gRPC 流式）
 * - 每个 Handler 内部要做"我该不该执行"的判断
 *
 * 我们的设计更干净：是否执行是执行器的职责，怎么执行是 Step 的职责。
 * </p>
 */
@Component
public class ChatChainImpl implements ChatChain {

    /**
     * 所有步骤的列表，由 Spring 按 @Order 排序后注入
     *
     * <p>Spring 容器的魔法：构造参数只要写 List&lt;ChatChainStep&gt;，
     * Spring 会自动找到项目中所有实现了 ChatChainStep 接口并标注了 @Component 的 Bean，
     * 按它们的 @Order 值从小到大排序，然后注入到这个 List 里。</p>
     *
     * <p>这意味着我们完全不需要手动维护步骤顺序，@Order(n) 就是顺序。</p>
     */
    private final List<ChatChainStep> steps;

    /**
     * Spring 构造注入
     *
     * @param steps Spring 自动收集的所有 ChatChainStep 实现类（已按 @Order 排序）
     */
    public ChatChainImpl(List<ChatChainStep> steps) {
        this.steps = steps;
    }

    /**
     * 按顺序执行所有步骤
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>遍历每个步骤</li>
     *   <li>先调用 shouldSkip(ctx) — "这步要跳过吗？"</li>
     *   <li>如果跳过，打印日志并 continue</li>
     *   <li>否则调用 process(ctx) — "干活！"</li>
     *   <li>ctx 在步骤之间传递，前一步的产出（如 RagStep 写入的 ragContext）
     *       会自然被后一步（如 MessageBuildStep）读取到</li>
     * </ol>
     *
     * <p>之所以 ctx 能在步骤间传递数据，是因为所有步骤操作的是
     * <b>同一个 ChatContext 对象</b>（Java 引用传递）。</p>
     *
     * @param ctx 共享的聊天上下文，整条链围绕它工作
     */
    @Override
    public void execute(ChatContext ctx) {
        for (ChatChainStep step : steps) {
            // ──── 第1步：检查是否需要跳过 ────
            if (step.shouldSkip(ctx)) {
                System.out.println("[Chain] 跳过步骤：" + step.getClass().getSimpleName());
                continue;
            }
            // ──── 第2步：执行 ────
            System.out.println("[Chain] 执行步骤：" + step.getClass().getSimpleName());
            step.process(ctx);
        }
    }
}
