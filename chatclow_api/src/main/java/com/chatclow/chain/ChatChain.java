package com.chatclow.chain;

import com.chatclow.context.ChatContext;

/**
 * 责任链执行器接口 — 聊天请求处理的"总调度员"
 *
 * <h3>什么是责任链模式？</h3>
 * <p>
 * 一个聊天请求从进入系统到返回结果，需要经过很多步骤：
 * 加载上下文 → RAG检索 → 构建消息 → 调AI模型 → 执行工具 → 保存记录 → 关闭连接。
 * </p>
 * <p>
 * 如果把这些逻辑全部写在一个方法里（重构前的 ChatServiceImpl），
 * 结果就是一个几百行的"上帝方法"，维护起来很头疼。
 * </p>
 * <p>
 * 责任链模式的核心思想：<b>把一个大流程拆成多个独立的小步骤，
 * 让它们像流水线一样依次执行</b>。每个步骤只关心自己的活儿，
 * 做完就把"接力棒"（ChatContext）传给下一步。
 * </p>
 *
 * <h3>和 Snail AI 的对比</h3>
 * <p>
 * Snail AI 用的是 {@code AgentChatHandler.handle(ctx)}，
 * ChatClow 用的是 {@code ChatChainStep.process(ctx) + shouldSkip(ctx)}。
 * 我们多了一个 shouldSkip()，让每个步骤可以自己决定"这事儿不归我管，跳过"。
 * </p>
 *
 * <h3>调用方只需要一行代码</h3>
 * <pre>{@code
 *   chatChain.execute(ctx);
 * }</pre>
 * 流式和非流式，都是这一行。
 *
 * @see ChatChainImpl   执行器的具体实现
 * @see ChatChainStep   步骤接口
 * @see ChatContext     在步骤之间流转的共享数据
 */
public interface ChatChain {

    /**
     * 启动责任链，按 @Order 顺序执行所有步骤
     *
     * @param ctx 聊天上下文，包含了这次请求的所有输入和中间数据
     *            所有步骤通过它来读写数据
     */
    void execute(ChatContext ctx);
}
