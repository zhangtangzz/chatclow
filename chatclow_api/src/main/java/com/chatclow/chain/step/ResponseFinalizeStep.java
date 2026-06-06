package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.context.ChatContext;
import com.chatclow.dto.SseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 第6步：发送 DONE 事件，关闭 SSE 连接（@Order = 60，链的终点）
 *
 * <pre>
 * 输入参数来源：
 *   RecordSaveStep 之后 → ctx.emitter
 *
 * 调用方法：
 *   emitter.send()    → 推送 SseEvent.done() 告知前端流结束
 *   emitter.complete() → 关闭 HTTP 长连接
 *
 * 仅流式模式执行，非流式直接跳过。
 * </pre>
 */
@Component
@Order(60)
public class ResponseFinalizeStep implements ChatChainStep {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        return !ctx.isStreamMode();
    }

    @Override
    public void process(ChatContext ctx) {
        try {
            ctx.getEmitter().send(
                    org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .data(objectMapper.writeValueAsString(SseEvent.done())));
            ctx.getEmitter().complete();
        } catch (Exception e) {
            ctx.getEmitter().complete();
        }
    }
}
