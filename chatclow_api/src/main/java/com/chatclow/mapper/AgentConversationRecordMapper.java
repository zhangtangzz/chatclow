package com.chatclow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatclow.entity.AgentConversationRecord;
import org.apache.ibatis.annotations.Mapper;


/**
 * 聊天记录 Mapper
 */

@Mapper
public interface AgentConversationRecordMapper extends BaseMapper<AgentConversationRecord>{
    //依旧是继承BaseMapper
}
