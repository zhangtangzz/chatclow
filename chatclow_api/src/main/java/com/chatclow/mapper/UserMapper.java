package com.chatclow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatclow.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 * 继承 BaseMapper<User> 后，就自动拥有了这些方法：
 * - selectById(id)     根据ID查询
 * - insert(user)        新增
 * - updateById(user)   更新
 * - deleteById(id)     删除
 * - selectList(wrapper) 条件查询
 */



public interface UserMapper extends BaseMapper<User> {
}
