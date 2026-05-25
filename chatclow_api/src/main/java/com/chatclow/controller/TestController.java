package com.chatclow.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器
 *
 * @RestController = @Controller + @ResponseBody
 * 所有方法返回 JSON 数据
 */
@RestController
public class TestController {

    /**
     * 测试接口：访问 http://localhost:8080/hello 会返回这段文字
     */
    @GetMapping("/hello")
    public String hello() {
        return "Hello! ChatClow 项目启动成功！🎉";
    }
}
