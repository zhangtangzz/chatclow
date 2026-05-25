package com.chatclow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chatclow.entity.User;

/**
 * 用户服务接口
 * 继承 IService<User> 后，自动拥有更多服务方法
 */

public interface UserService extends IService<User>{

    /**
     * 根据用户名查询用户
     */
    User getByUsername(String usernaame);

    /**
     * 注册新用户
     */

    boolean register(String username,String password,String email);



}
