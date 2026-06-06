package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.User;
import com.chatclow.service.UserService;
import com.chatclow.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginController{

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestParam String username,
                                         @RequestParam String password){

        //1.根据用户名查用户
        User user = userService.getByUsername(username);

        //2.用户不存在
        if (user == null){
            return R.error("用户不存在");
        }

        //3.校验密码：BCrypt 优先，兼容旧版明文
        String storedPw = user.getPassword();
        boolean pwMatch;
        if (storedPw.startsWith("$2a$") || storedPw.startsWith("$2b$")) {
            pwMatch = new BCryptPasswordEncoder().matches(password, storedPw);
        } else {
            pwMatch = password.equals(storedPw);
        }
        if (!pwMatch) {
            return R.error("密码错误");
        }

        //4.生成Token与刷新Token并返回
        String token = JwtUtil.generateToken(user.getId(),user.getUsername());
        String refreshToken = JwtUtil.generateRefreshToken(user.getId(),user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token",token);
        data.put("refreshToken",refreshToken);
        data.put("userId",user.getId());
        data.put("username",user.getUsername());
        data.put("role",user.getRole());

        return R.ok("登陆成功",data);
    }

    /**
     * 刷新 Token
     * POST /api/auth/refresh
     * 用 refreshToken 换取新的 accessToken
     */
    @PostMapping("/refresh")
    public R<Map<String, Object>> refresh(@RequestParam String refreshToken) {
        if (!JwtUtil.validateToken(refreshToken)) {
            return R.error("refreshToken 已过期，请重新登录");
        }
        Long userId = JwtUtil.getUserId(refreshToken);
        String username = JwtUtil.getUsername(refreshToken);

        String newToken = JwtUtil.generateToken(userId, username);
        String newRefreshToken = JwtUtil.generateRefreshToken(userId, username);

        Map<String, Object> data = new HashMap<>();
        data.put("token", newToken);
        data.put("refreshToken", newRefreshToken);
        return R.ok("刷新成功", data);
    }
}

