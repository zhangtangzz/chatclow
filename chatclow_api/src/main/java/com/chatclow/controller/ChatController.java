package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import com.chatclow.dto.ChatRequest;
import com.chatclow.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.Executor;

/**
 * AI 对话控制器
 * 接收用户消息，调用 AI 获取回复
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    @Qualifier("sseExecutor")
    private Executor sseExecutor;

    /**
     * 发送消息，获取AI回复（支持多轮对话）
     * POST /api/chat/send
     */
    @PostMapping("/send")
    public R<ChatResponse> send(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(
                request.getAgentId(),
                request.getUserId(),
                request.getMessage(),
                request.getConversationId(),
                request.isMemoryEnabled()
        );
        return R.ok(response);
    }

    /**
     * SSE 流式对话 — 前端用 EventSource 连接此端点
     */
    @PostMapping("/send-stream")
    public SseEmitter sendStream(@RequestBody ChatRequest request, HttpServletResponse response) {
        // 禁用响应缓冲，确保每个 token 立即推送到客户端
        response.setBufferSize(0);

        SseEmitter emitter = new SseEmitter(120000L); // 120秒超时

        // 在另一个线程中执行，避免阻塞主线程
        sseExecutor.execute(() -> {
            chatService.chatStream(
                    request.getAgentId(),
                    request.getUserId(),
                    request.getMessage(),
                    request.getConversationId(),
                    request.isMemoryEnabled(),
                    emitter
            );
        });

        return emitter; // 立即返回 emitter，数据后续通过它推送
    }
}
