package com.chatclow.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * OkHttp 统一配置
 * 所有 AI API 调用共用同一个 OkHttpClient 实例，复用连接池，避免资源浪费
 *
 * 适用范围：
 * - ModelCallStep（流式 AI 调用）
 * - FunctionCallingStep（工具调用后二次 AI 请求）
 * - ChatServiceImpl（同步 AI 调用 + Function Calling）
 * - RerankServiceImpl（Re-rank API 调用）
 */
@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .build();
    }
}
