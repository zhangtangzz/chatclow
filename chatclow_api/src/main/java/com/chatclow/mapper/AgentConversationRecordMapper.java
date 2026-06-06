package com.chatclow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatclow.entity.AgentConversationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;


/**
 * 聊天记录 Mapper
 */

@Mapper
public interface AgentConversationRecordMapper extends BaseMapper<AgentConversationRecord>{

    /** 查询所有 AI 回复的平均响应时长（毫秒） */
    @Select("SELECT AVG(response_time) FROM chatclow_ai_conversation_record WHERE role = 'assistant' AND response_time > 0")
    Long avgResponseTime();
}
