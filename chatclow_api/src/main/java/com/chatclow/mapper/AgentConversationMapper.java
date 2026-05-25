package com.chatclow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatclow.entity.AgentConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话会话 Mapper
 */
@Mapper
public interface AgentConversationMapper extends BaseMapper<AgentConversation>{
    //继承BaseMapper,自动拥有增删改查
}
