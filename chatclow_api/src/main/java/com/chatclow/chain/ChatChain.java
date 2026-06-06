package com.chatclow.chain;

import com.chatclow.context.ChatContext;

/**
 * 责任链执行器接口
 *
 * <p>把一次对话请求拆成多个独立 Step，像流水线一样依次执行。
 * 流式和非流式共用同一条链，区别仅在于 ctx.streamMode 和各 Step 的 shouldSkip() 判断。</p>
 *
 * <p>调用方只需要一行：{@code chatChain.execute(ctx);}</p>
 */
public interface ChatChain {

    /**
     * 启动责任链，按 @Order 顺序执行所有 ChatChainStep
     *
     * @param ctx 聊天上下文，所有步骤通过它读写数据（Java 引用传递）
     */
    void execute(ChatContext ctx);
}
