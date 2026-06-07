package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chatclow.entity.User;
import com.chatclow.mapper.UserMapper;
import com.chatclow.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 *
 * @Service = 标记为 Spring 的 Bean，可以被其他类引用
 * extends ServiceImpl<UserMapper, User> = 继承后自动获得 CRUD 方法
 * implements UserService = 实现我们刚才定义的接口
 */

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements UserService {

    @Override
    public User getByUsername(String username){
    //QueryWrapper = 条件构造器
        //eq = equal,等于
        QueryWrapper<User>wrapper = new QueryWrapper<>();
        wrapper.eq("username",username);//WHERE username = ?
        return this.getOne(wrapper);//返回一条记录
    }

    @Override
    public boolean register(String username, String password,String email){
        //检查用户名是否存在
        User existUser = getByUsername(username);
        if (existUser!=null){
            return false;//用户已存在
        }

        //创建新用户
        User user = new User();
        user.setUsername(username);
        // BCrypt 加密密码
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        user.setEmail(email);
        user.setRole(1);

        //保存到数据库
        return this.save(user);

    }



}