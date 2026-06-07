package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.UserDocument;
import com.chatclow.mapper.UserDocumentMapper;
import com.chatclow.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import com.chatclow.dto.ChatRequest;
import com.chatclow.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
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

    @Autowired
    private UserDocumentMapper userDocumentMapper;

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
                request.isMemoryEnabled(),
                request.getFileIds()
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
                    request.getFileIds(),
                    emitter
            );
        });

        return emitter; // 立即返回 emitter，数据后续通过它推送
    }

    /**
     * 对话文件上传
     * POST /api/chat/upload
     */
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return R.error("未登录");

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) return R.error("文件名不能为空");

        try {
            // 存文件到 uploads/chat/{userId}/
            String dir = "uploads/chat/" + userId;
            File dirFile = new File(dir);
            if (!dirFile.exists()) dirFile.mkdirs();

            String timestamp = String.valueOf(System.currentTimeMillis());
            String storageName = timestamp + "_" + originalName;
            Path targetPath = Paths.get(dir, storageName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 入库
            UserDocument doc = new UserDocument();
            doc.setUserId(userId);
            doc.setFileName(originalName);
            doc.setFileType(originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase());
            doc.setFileSize(file.getSize());
            doc.setFilePath(targetPath.toString());
            doc.setStatus(3); // 直接完成
            doc.setSource("chat");
            doc.setCreatedDt(LocalDateTime.now());
            userDocumentMapper.insert(doc);

            Map<String, Object> result = new HashMap<>();
            result.put("id", doc.getId());
            result.put("fileName", originalName);
            result.put("fileType", doc.getFileType());
            result.put("fileSize", file.getSize());
            result.put("url", "/uploads/chat/" + userId + "/" + storageName);
            return R.ok(result);
        } catch (IOException e) {
            return R.error("文件上传失败: " + e.getMessage());
        }
    }
}
