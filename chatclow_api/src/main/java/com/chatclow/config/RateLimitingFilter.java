package com.chatclow.config;

import com.chatclow.common.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 请求限流过滤器
 * 基于 IP + 接口路径，防止滥用
 * 对话接口 30次/分钟，其余接口 60次/分钟
 */
@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int CHAT_LIMIT = 30;
    private static final int DEFAULT_LIMIT = 60;
    private static final long WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(1);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI();

        // 只限流 API 接口
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getIp(req);
        String key = ip + ":" + path;
        int limit = path.contains("/chat/") ? CHAT_LIMIT : DEFAULT_LIMIT;

        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(limit));

        synchronized (bucket) {
            long now = System.currentTimeMillis();
            if (now - bucket.windowStart > WINDOW_MILLIS) {
                bucket.count = 0;
                bucket.windowStart = now;
            }
            if (bucket.count >= limit) {
                resp.setStatus(429);
                resp.setContentType("application/json;charset=UTF-8");
                R<?> result = R.error(429, "请求太频繁，请稍后再试");
                resp.getWriter().write(objectMapper.writeValueAsString(result));
                return;
            }
            bucket.count++;
        }

        chain.doFilter(request, response);
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }

    private static class Bucket {
        int count;
        long windowStart;

        Bucket(int count) {
            this.count = 0;
            this.windowStart = System.currentTimeMillis();
        }
    }
}
