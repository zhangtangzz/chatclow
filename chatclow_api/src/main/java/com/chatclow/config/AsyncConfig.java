package com.chatclow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("sseExecutor")
    public Executor sseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);       // 常驻5个线程
        executor.setMaxPoolSize(20);        // 最多20个线程
        executor.setQueueCapacity(100);     // 排队100个任务
        executor.setThreadNamePrefix("chatclow-sse-");  // 线程名前缀
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 排队也满了？由调用者线程自己跑，不丢弃任务
        executor.initialize();
        return executor;
    }
}
