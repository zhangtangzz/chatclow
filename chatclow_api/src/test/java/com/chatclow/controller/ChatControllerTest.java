package com.chatclow.controller;

import com.chatclow.dto.ChatResponse;
import com.chatclow.service.ChatService;
import com.chatclow.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ChatController 接口测试")
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ChatService chatService;

    private final String token = JwtUtil.generateToken(1L, "test");

    @Test
    @DisplayName("POST /api/chat/send 返回正确的 ChatResponse 结构")
    void shouldReturnChatResponse() throws Exception {
        when(chatService.chat(anyLong(), anyLong(), anyString(), any(), anyBoolean()))
                .thenReturn(new ChatResponse("AI 回复内容", 10L));

        mockMvc.perform(post("/api/chat/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":1,\"userId\":100,\"message\":\"你好\",\"conversationId\":null,\"memoryEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.reply").value("AI 回复内容"))
                .andExpect(jsonPath("$.data.conversationId").value(10));
    }

    @Test
    @DisplayName("POST /api/chat/send-stream 返回 SseEmitter")
    void shouldReturnSseEmitter() throws Exception {
        mockMvc.perform(post("/api/chat/send-stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":1,\"userId\":100,\"message\":\"你好\",\"memoryEnabled\":false}"))
                .andExpect(status().isOk());
    }
}
