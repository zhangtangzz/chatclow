package com.chatclow.service.impl;

import com.chatclow.entity.AgentConversationRecord;
import com.chatclow.mapper.AgentConversationRecordMapper;
import com.chatclow.service.AgentConversationRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Mapper
@Service
public class AgentConversationRecordServiceImpl
        extends ServiceImpl<AgentConversationRecordMapper, AgentConversationRecord>
        implements AgentConversationRecordService {

    @Override
    public void saveRecord(Long conversationId, String role, String content) {
        AgentConversationRecord record = new AgentConversationRecord();
        record.setConversationId(conversationId);
        record.setRole(role);
        record.setContent(content);
        this.save(record);
    }

    @Override
    public List<AgentConversationRecord> listByConversationId(Long conversationId) {
        return this.lambdaQuery()
                .eq(AgentConversationRecord::getConversationId, conversationId)
                .list();
    }
}
