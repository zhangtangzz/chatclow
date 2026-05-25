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
}
