package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.User;
import com.chatclow.service.UserService;
import com.chatclow.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

        //3.校验密码（明文比较，实际项目要用加密）
        if(!password.equals(user.getPassword())){
            return R.error("密码错误");
        }

        //4.生成Token并返回
        String token = JwtUtil.generateToken(user.getId(),user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token",token);
        data.put("userId",user.getId());
        data.put("username",user.getUsername());

        return R.ok("登陆成功",data);
    }
}

