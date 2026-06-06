package com.chatclow.controller;

import com.chatclow.entity.AgentConversation;
import com.chatclow.entity.User;
import com.chatclow.mapper.AgentConversationMapper;
import com.chatclow.mapper.UserMapper;
import com.chatclow.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("TokenController 接口测试")
class TokenControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserMapper userMapper;
    @Autowired private AgentConversationMapper conversationMapper;

    private String token;
    private Long userId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("toketest");
        user.setPassword("123456");
        user.setRole(1);
        userMapper.insert(user);
        userId = user.getId();
        token = JwtUtil.generateToken(userId, "toketest");

        AgentConversation conv = new AgentConversation();
        conv.setUserId(userId);
        conv.setTitle("测试会话");
        conv.setTotalTokens(500);
        conversationMapper.insert(conv);
    }

    @Test
    @DisplayName("用户 token 统计返回正确格式")
    void shouldReturnUserTokenStats() throws Exception {
        mockMvc.perform(get("/api/user/token-stats")
                        .param("userId", userId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("测试会话"))
                .andExpect(jsonPath("$.data[0].totalTokens").value(500));
    }

    @Test
    @DisplayName("管理员 token 汇总按消耗降序排列")
    void shouldReturnAdminTokenSummary() throws Exception {
        // 第二个用户，token 更多
        User user2 = new User();
        user2.setUsername("heavyuser");
        user2.setPassword("123456");
        user2.setRole(1);
        userMapper.insert(user2);

        AgentConversation conv2 = new AgentConversation();
        conv2.setUserId(user2.getId());
        conv2.setTitle("大量消耗");
        conv2.setTotalTokens(2000);
        conversationMapper.insert(conv2);

        mockMvc.perform(get("/api/admin/token-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data[0].totalTokens").value(2000));
    }
}
