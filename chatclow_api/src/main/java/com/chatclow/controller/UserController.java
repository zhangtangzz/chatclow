package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.User;
import com.chatclow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
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
     * GET http://localhost:8080/api/user/{username}
     */
    @GetMapping("/{username}")
    public Map<String,Object> getUser(@PathVariable String username){
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

    /**
     * ========= 管理员接口 =========
     * 所有接口需要前端在 Header 里带 token，后端通过 JWT 解析 role 进行鉴权
     * 为简化演示，这里先直接实现，正式项目用拦截器统一鉴权
     */

    /**
     * 管理员查看所有用户列表
     * GET /api/user/admin/list
     */
    @GetMapping("/admin/list")
    public R<List<User>> adminListUsers() {
        List<User> users = userService.list();  // MyBatis-Plus 方法名是 list()
        // 密码脱敏
        users.forEach(u -> u.setPassword("****"));
        return R.ok("查询成功", users);
    }

    /**
     * 管理员新建用户
     * POST /api/user/admin/create
     */
    @PostMapping("/admin/create")
    public R<Void> adminCreateUser(@RequestBody User user) {
        boolean success = userService.register(
                user.getUsername(), user.getPassword(), user.getEmail());
        if (success) {
            return R.ok("创建成功", null);
        } else {
            return R.error("用户名已存在");
        }
    }

    /**
     * 管理员修改用户（重置密码 / 修改角色）
     * PUT /api/user/admin/update/{id}
     */
    @PutMapping("/admin/update/{id}")
    public R<Void> adminUpdateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);           // 先把 id 设进实体
        userService.updateById(user);  // MyBatis-Plus 只接收一个实体参数
        return R.ok("修改成功", null);
    }

    /**
     * 管理员删除用户
     * DELETE /api/user/admin/delete/{id}
     */
    @DeleteMapping("/admin/delete/{id}")
    public R<Void> adminDeleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return R.ok("删除成功", null);
    }

    /**
     * 管理员查看指定用户的历史对话列表
     * GET /api/user/admin/conversations/{userId}
     */
    @GetMapping("/admin/conversations/{userId}")
    public R<List<Object>> adminGetUserConversations(@PathVariable Long userId) {
        // TODO: 注入 ConversationService 后实现
        // 这里先返回成功（空数据），避免编译报错
        return R.ok("查询成功", null);
    }

}
