package com.chatclow.service;

import com.chatclow.entity.AgentConversationRecord;
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
@DisplayName("AgentConversationRecordServiceImpl 集成测试")
class AgentConversationRecordServiceImplTest {

    @Autowired private AgentConversationRecordService service;

    @Test
    @DisplayName("保存聊天记录")
    void shouldSaveRecord() {
        service.saveRecord(1L, "user", "你好");

        List<AgentConversationRecord> records = service.listByConversationId(1L);
        assertEquals(1, records.size());
        assertEquals("user", records.get(0).getRole());
        assertEquals("你好", records.get(0).getContent());
    }

    @Test
    @DisplayName("按会话ID查询聊天记录")
    void shouldListByConversationId() {
        service.saveRecord(10L, "user", "问题1");
        service.saveRecord(10L, "assistant", "回答1");
        service.saveRecord(20L, "user", "另一个会话的消息");

        List<AgentConversationRecord> list = service.listByConversationId(10L);
        assertEquals(2, list.size());
        assertTrue(list.stream().allMatch(r -> r.getConversationId().equals(10L)));
    }

    @Test
    @DisplayName("空会话查询返回空列表")
    void shouldReturnEmptyForNoRecords() {
        List<AgentConversationRecord> list = service.listByConversationId(9999L);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }
}
