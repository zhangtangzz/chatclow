package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.User;
import com.chatclow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 *
 * @RestController = @Controller + @ResponseBody
 * 所有方法返回 JSON
 */

@RestController
@RequestMapping("/api/user")
public class UserController {


    //@Autowired = 自动注入 Service
    @Autowired
    private UserService userService;


    /**
     * 注册接口
     * POST http://localhost:8080/api/user/register
     */


    @PostMapping("/register")
    public R<Void> register(@RequestBody @Valid User user) {
        // 调用 service，参数从 user 对象里拿
        boolean success = userService.register(user.getUsername(), user.getPassword(), user.getEmail());

        if (success){
            return R.ok("注册成功!", null);
        }   else {
            return R.error("用户名已存在！");
        }
    }

    /**
     * 根据用户名查询用户
     * GET http://localhost:8080/api/user/admin
     */
    @GetMapping("/{username}")
    public Map<String,Object>getUser(@PathVariable String username){
        Map<String,Object> result = new HashMap<>();

        User user = userService.getByUsername(username);

        if (user!=null){
            result.put("code",200);
            result.put("data",user);
        }   else {
            result.put("code",200);
            result.put("message","用户不存在");
        }
        return result;
    }




}
