package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.AgentConversationRecord;
import com.chatclow.service.AgentConversationRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天记录控制器
 */
@RestController
@RequestMapping("/api/record")
public class AgentConversationRecordController {

    @Autowired
    private AgentConversationRecordService agentConversationRecordService;

    /**
     * 保存聊天记录
     * POST /api/record/save?conversationId=1&role=user&content=你好
     */
    @PostMapping("/save")
    public R<Void> save(@RequestParam Long conversationId,
                        @RequestParam String role,
                        @RequestParam String content) {
        agentConversationRecordService.saveRecord(conversationId, role, content);
        return R.ok();
    }

    /**
     * 查询会话的所有聊天记录
     * GET /api/record/list?conversationId=1
     */
    @GetMapping("/list")
    public R<List<AgentConversationRecord>> list(@RequestParam Long conversationId) {
        List<AgentConversationRecord> list = agentConversationRecordService.listByConversationId(conversationId);
        return R.ok(list);
    }
}
