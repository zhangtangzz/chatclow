package com.chatclow.config;

import com.chatclow.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 作用：
 *   1. 注册 JWT 拦截器
 *   2. 配置哪些接口需要登录、哪些不用
 *   3. 解决跨域问题
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor())
                // 拦截所有请求
                .addPathPatterns("/**")
                // 排除以下路径（不需要登录就能访问）
                .excludePathPatterns(
                        // 登录 & 注册（没登录怎么登录呢？）
                        "/api/auth/login",
                        "/api/user/register",
                        // 静态资源
                        "/*.html",
                        "/*.js",
                        "/*.css"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}