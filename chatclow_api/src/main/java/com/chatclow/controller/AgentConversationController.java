package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.AgentConversation;
import com.chatclow.service.AgentConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话会话控制器
 */
@RestController
@RequestMapping("/api/conversation")
public class AgentConversationController {

    @Autowired
    private AgentConversationService agentConversationService;

    /**
     * 创建新会话
     * POST /api/conversation/create?userId=1&title=新对话
     */
    @PostMapping("/create")
    public R<AgentConversation> create(@RequestParam Long userId,
                                        @RequestParam String title) {
        AgentConversation conversation = agentConversationService.createConversation(userId, title);
        return R.ok(conversation);
    }

    /**
     * 查询用户的所有会话
     * GET /api/conversation/list?userId=1
     */
    @GetMapping("/list")
    public R<List<AgentConversation>> list(@RequestParam Long userId) {
        List<AgentConversation> list = agentConversationService.listByUserId(userId);
        return R.ok(list);
    }

    /**
     * 删除会话（含所有聊天记录）
     * DELETE /api/conversation/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Long id) {
        agentConversationService.deleteConversation(id);
        return R.ok("删除成功", null);
    }

    /**
     * 清除会话记忆（删除聊天记录，保留会话）
     * DELETE /api/conversation/memory/{convId}
     */
    @DeleteMapping("/memory/{convId}")
    public R<Void> clearMemory(@PathVariable Long convId) {
        agentConversationService.clearMemory(convId);
        return R.ok("记忆已清除", null);
    }
}
