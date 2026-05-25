package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.service.ChatService;
import com.chatclow.util.ChunkSplitUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import com.chatclow.dto.ChatRequest;
import com.chatclow.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
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
                request.getConversationId()
        );
        return R.ok(response);
    }

    /**
     * SSE 流式对话 — 前端用 EventSource 连接此端点
     */
    @PostMapping("/send-stream")
    public SseEmitter sendStream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120000L); // 120秒超时

        // 在另一个线程中执行，避免阻塞主线程
        sseExecutor.execute(() -> {
            chatService.chatStream(
                    request.getAgentId(),
                    request.getUserId(),
                    request.getMessage(),
                    request.getConversationId(),
                    emitter
            );
        });

        return emitter; // 立即返回 emitter，数据后续通过它推送
    }


    // 临时测试接口，测完删掉
    @PostMapping("/test-chunk")
    public R<List<String>> testChunk(@RequestBody String text) {
        List<String> chunks = ChunkSplitUtil.split(text);
        return R.ok(chunks);
    }

}
