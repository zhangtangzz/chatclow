package com.chatclow.chain;

import com.chatclow.context.ChatContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatChainImpl implements ChatChain {

    private static final Logger log = LoggerFactory.getLogger(ChatChainImpl.class);

    private final List<ChatChainStep> steps;

    public ChatChainImpl(List<ChatChainStep> steps) {
        this.steps = steps;
    }

    @Override
    public void execute(ChatContext ctx) {
        for (ChatChainStep step : steps) {
            if (step.shouldSkip(ctx)) {
                log.info("[Chain] 跳过: " + step.getClass().getSimpleName());
                continue;
            }
            log.info("[Chain] 执行: " + step.getClass().getSimpleName());
            step.process(ctx);
        }
    }
}
