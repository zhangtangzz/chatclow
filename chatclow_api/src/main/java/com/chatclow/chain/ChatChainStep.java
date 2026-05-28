package com.chatclow.chain;

import com.chatclow.context.ChatContext;

/**
 * 责任链步骤接口 — 所有 Step 的统一"身份证"
 *
 * <h3>每个 Step 要实现什么？</h3>
 * <ol>
 *   <li><b>process(ctx)</b> — 执行这个步骤的核心逻辑</li>
 *   <li><b>shouldSkip(ctx)</b> — 告诉执行器"这步要不要跳过"（默认不跳过）</li>
 * </ol>
 *
 * <h3>shouldSkip() 的设计用意</h3>
 * <p>
 * 这是 ChatClow 相比 Snail AI 的一个改进。Snail 的 Handler 没有跳过机制，
 * 即使 RAG 没开启，RagHandler 也会被调用，需要自己在 handle() 里判断"我啥也不干直接返回"。
 * </p>
 * <p>
 * 我们把"要不要干"和"怎么干"分开：
 * </p>
 * <ul>
 *   <li>shouldSkip() — 判断"该不该我干"</li>
 *   <li>process()   — 只管"怎么干"，不用再判断前提条件</li>
 * </ul>
 *
 * <p><b>举例：</b></p>
 * <pre>{@code
 * // RagStep — 智能体没开知识库？跳过
 * public boolean shouldSkip(ctx) {
 *     return agent.getKbEnabled() != 1;
 * }
 *
 * // ModelCallStep — 非流式请求？跳过（交给 SyncAiCallStep）
 * public boolean shouldSkip(ctx) {
 *     return !ctx.isStreamMode();
 * }
 * }</pre>
 *
 * <h3>Spring 自动装配机制</h3>
 * <p>
 * 实现类只需加 {@code @Component} 和 {@code @Order(n)}，Spring 就会自动发现并注入到
 * {@link ChatChainImpl} 中，按数字从小到大排序执行。
 * 不需要手动注册，新增一个 Step 只要写一个类加这两个注解就行，零配置。
 * </p>
 *
 * @see ChatChain       责任链执行器
 * @see ChatChainImpl   执行器实现（Spring 自动收集所有 ChatChainStep Bean）
 */
public interface ChatChainStep {

    /**
     * 执行当前步骤的核心逻辑
     *
     * @param ctx 共享的聊天上下文，本步骤可以从中读取前面步骤写入的数据，
     *            也可以写入数据供后续步骤使用
     */
    void process(ChatContext ctx);

    /**
     * 是否跳过当前步骤（默认不跳过）
     *
     * <p>子类可以重写此方法来实现条件跳过。执行器在调用 process() 之前
     * 会先调用这个方法，返回 true 则直接跳过。</p>
     *
     * @param ctx 聊天上下文，用于判断当前是否满足执行条件
     * @return true 表示跳过，false 表示需要执行
     */
    default boolean shouldSkip(ChatContext ctx) {
        return false;
    }
}
