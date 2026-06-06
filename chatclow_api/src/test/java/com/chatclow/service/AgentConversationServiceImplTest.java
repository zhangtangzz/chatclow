package com.chatclow.service;

import com.chatclow.entity.AgentConversation;
import com.chatclow.entity.AgentConversationRecord;
import com.chatclow.mapper.AgentConversationRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AgentConversationServiceImpl 集成测试")
class AgentConversationServiceImplTest {

    @Autowired private AgentConversationService service;
    @Autowired private AgentConversationRecordMapper recordMapper;

    @Test
    @DisplayName("创建会话并生成标题")
    void shouldCreateConversation() {
        AgentConversation conv = service.createConversation(1L, "测试会话");
        assertNotNull(conv.getId());
        assertEquals("测试会话", conv.getTitle());
        assertEquals(1L, conv.getUserId());
    }

    @Test
    @DisplayName("按用户ID查询会话列表，按时间倒序")
    void shouldListByUserId() {
        service.createConversation(1L, "会话A");
        service.createConversation(1L, "会话B");

        List<AgentConversation> list = service.listByUserId(1L);
        assertTrue(list.size() >= 2);
    }

    @Test
    @DisplayName("不同用户的会话隔离")
    void shouldIsolateByUser() {
        service.createConversation(1L, "用户1的会话");
        service.createConversation(2L, "用户2的会话");

        List<AgentConversation> user1List = service.listByUserId(1L);
        List<AgentConversation> user2List = service.listByUserId(2L);
        assertTrue(user1List.stream().allMatch(c -> c.getUserId() == 1L));
        assertTrue(user2List.stream().allMatch(c -> c.getUserId() == 2L));
    }

    @Test
    @DisplayName("删除会话级联删除聊天记录")
    void shouldCascadeDeleteRecords() {
        AgentConversation conv = service.createConversation(1L, "待删除");
        recordMapper.insert(createRecord(conv.getId(), "user", "你好"));
        recordMapper.insert(createRecord(conv.getId(), "assistant", "你好！"));

        service.deleteConversation(conv.getId());

        assertNull(service.getById(conv.getId()));
        List<AgentConversationRecord> records = recordMapper.selectList(null);
        assertTrue(records.stream().noneMatch(r -> r.getConversationId().equals(conv.getId())));
    }

    @Test
    @DisplayName("清除记忆：删记录保留会话")
    void shouldClearMemoryKeepConversation() {
        AgentConversation conv = service.createConversation(1L, "测试记忆");
        recordMapper.insert(createRecord(conv.getId(), "user", "消息1"));
        recordMapper.insert(createRecord(conv.getId(), "assistant", "回复1"));

        service.clearMemory(conv.getId());

        assertNotNull(service.getById(conv.getId())); // 会话还在
        List<AgentConversationRecord> records =
                recordMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentConversationRecord>()
                        .eq(AgentConversationRecord::getConversationId, conv.getId()));
        assertTrue(records.isEmpty()); // 记录没了
    }

    private AgentConversationRecord createRecord(Long convId, String role, String content) {
        AgentConversationRecord r = new AgentConversationRecord();
        r.setConversationId(convId);
        r.setRole(role);
        r.setContent(content);
        return r;
    }
}
