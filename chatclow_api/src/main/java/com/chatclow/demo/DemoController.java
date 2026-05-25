package com.chatclow.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 演示控制器 - 展示基本 REST API 写法
 *
 * 路径映射：/api/demo/*
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    /**
     * GET 请求示例
     * URL: http://localhost:8080/api/demo/hello
     */
    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "欢迎使用 ChatClow!");
        result.put("data", "Hello World!");
        return result;
    }

    /**
     * 带参数的 GET 请求
     * URL: http://localhost:8080/api/demo/user/1?name=张三
     */
    @GetMapping("/user/{id}")
    public Map<String, Object> getUser(
            @PathVariable("id") Long id,
            @RequestParam(value = "name", defaultValue = "匿名") String name) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("name", name);
        user.put("role", "管理员");
        result.put("data", user);
        return result;
    }
}
