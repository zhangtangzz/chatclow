package com.chatclow.interceptor;

import com.chatclow.common.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chatclow.util.JwtUtil;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 拦截器
 * 每次（需要登录的）请求都会经过这里检查 Token
 */

public class JwtInterceptor implements HandlerInterceptor  {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception{

        //1. 如果是 OPTIONS 预检请求，直接放行（跨域时会先发一次 OPTIONS）
        if("OPTIONS".equalsIgnoreCase(request.getMethod())){
            return true;
        }

        //2，从请求头获取Token
        String token = request.getHeader("Authorization");

        //3.没有Token-》返回401
        if (token == null || token.isEmpty()){
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            R<?> result=R.error(401,"未登录,请先登录 ");
            response.getWriter().write(new ObjectMapper().writeValueAsString(result));
            return false;
        }

        //4. 去掉可能带的前缀 "Bearer "
        if(token.startsWith("Bearer")){
            token = token.substring(7);
        }

        // 5. 验证 Token 是否有效
        if (!JwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            R<?> result = R.error(401, "Token 已过期或无效");
            response.getWriter().write(new ObjectMapper().writeValueAsString(result));
            return false;
        }

        // 6. Token 有效 → 把用户信息存入 request，方便后续使用
        Long userId = JwtUtil.getUserId(token);
        String username = JwtUtil.getUsername(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);

        // 7. 放行！
        return true;


    }
}
