package com.chatclow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

/**
 * ChatClow 应用启动类
 *
 * 作用：这个类的 main 方法是整个项目的入口
 * 运行这个类，就会启动 Spring Boot 应用
 */
@SpringBootApplication(exclude = {MongoAutoConfiguration.class})
@MapperScan("com.chatclow.mapper")  // 扫描 Mapper 接口
public class ChatclowApplication {

    public static void main(String[] args) {
        // SpringApplication.run() 会启动整个 Spring 容器
        SpringApplication.run(ChatclowApplication.class, args);
    }
}
