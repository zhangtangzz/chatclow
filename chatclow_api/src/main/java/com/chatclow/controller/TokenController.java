package com.chatclow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.common.R;
import com.chatclow.entity.AgentConversation;
import com.chatclow.entity.User;
import com.chatclow.mapper.AgentConversationMapper;
import com.chatclow.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Token 用量统计接口
 */
@RestController
@RequestMapping("/api")
public class TokenController {

    @Autowired private AgentConversationMapper conversationMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private com.chatclow.mapper.AgentConversationRecordMapper recordMapper;

    /**
     * 用户查看自己的 token 消耗明细（按会话维度）
     * GET /api/user/token-stats?userId=1
     */
    @GetMapping("/user/token-stats")
    public R<List<Map<String, Object>>> userTokenStats(@RequestParam Long userId) {
        LambdaQueryWrapper<AgentConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConversation::getUserId, userId);
        wrapper.orderByDesc(AgentConversation::getCreatedDt);
        List<AgentConversation> conversations = conversationMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentConversation conv : conversations) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("conversationId", conv.getId());
            item.put("title", conv.getTitle());
            item.put("createdDt", conv.getCreatedDt() != null ? conv.getCreatedDt().toString() : "");
            item.put("totalTokens", conv.getTotalTokens() != null ? conv.getTotalTokens() : 0);
            result.add(item);
        }
        return R.ok(result);
    }

    /**
     * 管理员查看所有用户的 token 汇总
     * GET /api/admin/token-summary
     */
    @GetMapping("/admin/token-summary")
    public R<List<Map<String, Object>>> adminTokenSummary() {
        List<User> users = userMapper.selectList(null);

        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            // 汇总该用户所有会话的 token
            LambdaQueryWrapper<AgentConversation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AgentConversation::getUserId, user.getId());
            List<AgentConversation> conversations = conversationMapper.selectList(wrapper);
            int totalTokens = conversations.stream()
                    .mapToInt(c -> c.getTotalTokens() != null ? c.getTotalTokens() : 0)
                    .sum();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", user.getId());
            item.put("username", user.getUsername());
            item.put("email", user.getEmail());
            item.put("totalTokens", totalTokens);
            result.add(item);
        }

        result.sort((a, b) -> Integer.compare((int) b.get("totalTokens"), (int) a.get("totalTokens")));
        return R.ok(result);
    }

    /**
     * 管理员查看所有对话的平均响应时长
     * GET /api/admin/avg-response-time
     */
    @GetMapping("/admin/avg-response-time")
    public R<Map<String, Object>> avgResponseTime() {
        Long avgMs = recordMapper.avgResponseTime();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avgMs", avgMs != null ? avgMs : 0);
        result.put("avgSec", avgMs != null ? Math.round(avgMs / 10.0) / 100.0 : 0);
        return R.ok(result);
    }
}
