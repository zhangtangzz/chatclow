package com.chatclow.controller;

import com.chatclow.entity.User;
import com.chatclow.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("LoginController 接口测试")
class LoginControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(new BCryptPasswordEncoder().encode("123456"));
        user.setRole(1);
        userMapper.insert(user);
    }

    @Test
    @DisplayName("登录成功返回 token")
    void shouldLoginSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "testuser")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("密码错误返回错误")
    void shouldFailOnWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "testuser")
                        .param("password", "wrongpass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("密码错误"));
    }

    @Test
    @DisplayName("用户不存在返回错误")
    void shouldFailOnUserNotFound() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "nobody")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("用户不存在"));
    }
}
