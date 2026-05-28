package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.context.ChatContext;
import com.chatclow.dto.SseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 第6步：发送完成事件，关闭 SSE 连接（@Order = 60）
 *
 * <h3>整条链的终点站</h3>
 * <p>
 * 这是流式模式的最后一步。前面的步骤已经把 AI 回复推完了、记录保存好了，
 * 现在需要告诉前端"说完了"并关闭连接。
 * </p>
 *
 * <h3>为什么独立成 Step？</h3>
 * <p>
 * 重构前，关闭 emitter 的逻辑嵌在 ChatServiceImpl 的 chatStream() 方法末尾。
 * 如果中间出了异常，可能跳过了关闭逻辑，前端一直等。
 * 独立成 Step 后，即使前面步骤异常，执行器也会继续执行后续步骤（除非步骤内部直接中断了链）。
 * </p>
 *
 * <h3>SSE 协议约定</h3>
 * <pre>
 *   SseEvent.done() → {"type":"done"}
 *   emitter.complete() → 关闭 HTTP 长连接
 * </pre>
 * <p>前端收到 type=done 事件后知道流结束了，可以做收尾处理。
 * complete() 释放服务器端的连接资源，避免内存泄漏。</p>
 *
 * <h3>非流式模式</h3>
 * <p>
 * 非流式没有 SseEmitter，不需要发送 DONE 事件，直接跳过。
 * ChatServiceImpl 的 chat() 方法返回 ChatResponse 对象，前端通过正常的 HTTP 响应知道请求结束。
 * </p>
 */
@Component
@Order(60)  // 数字最大 = 最后一个执行 = 链的收尾
public class ResponseFinalizeStep implements ChatChainStep {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        // 非流式没有 emitter，自然不需要发送 DONE 事件和关闭连接
        return !ctx.isStreamMode();
    }

    @Override
    public void process(ChatContext ctx) {
        try {
            // ── ① 发送 DONE 事件：告知前端流已结束 ──
            ctx.getEmitter().send(
                    org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .data(objectMapper.writeValueAsString(SseEvent.done()))
            );
            // ── ② 关闭 SSE 连接 ──
            ctx.getEmitter().complete();
        } catch (Exception e) {
            // 即使发送失败（比如前端已断开），也要确保 close
            ctx.getEmitter().complete();
        }
    }
}
