package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.AiAgent;
import com.chatclow.entity.User;
import com.chatclow.mapper.UserMapper;
import com.chatclow.service.AiAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI智能体控制器
 */
@RestController
@RequestMapping("/api/agent")
public class AiAgentController {

    @Autowired
    private AiAgentService aiAgentService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 新增智能体
     * POST /api/agent/add
     */
    @PostMapping("/add")
    public R<Void> add(@RequestBody @Valid AiAgent aiAgent) {
        boolean success = aiAgentService.add(aiAgent);
        if (success) {
            return R.ok("智能体创建成功!", null);
        }
        return R.error("创建失败");
    }

    /**
     * 查询用户的智能体列表
     * GET /api/agent/list/{userId}
     * 管理员(role=2)返回所有智能体，普通用户只返回自己的
     */
    @GetMapping("/list/{userId}")
    public R<List<AiAgent>> listByUserId(@PathVariable Long userId) {
        User user = userMapper.selectById(userId);
        List<AiAgent> list;
        if (user != null && user.getRole() != null && user.getRole() == 2) {
            // 管理员：返回所有启用的智能体
            list = aiAgentService.listAllEnabled();
        } else {
            // 普通用户：只返回自己的
            list = aiAgentService.listByUserId(userId);
        }
        return R.ok("查询成功", list);
    }

    /**
     * 根据ID查询智能体
     * GET /api/agent/{id}
     */
    @GetMapping("/{id}")
    public R<AiAgent> getById(@PathVariable Long id) {
        AiAgent agent = aiAgentService.getById(id);
        if (agent != null) {
            return R.ok("查询成功", agent);
        }
        return R.error("智能体不存在");
    }

    /**
     * 更新智能体
     * PUT /api/agent/update
     */
    @PutMapping("/update")
    public R<Void> update(@RequestBody @Valid AiAgent aiAgent) {
        boolean success = aiAgentService.update(aiAgent);
        if (success) {
            return R.ok("更新成功!", null);
        }
        return R.error("更新失败");
    }

    /**
     * 删除智能体
     * DELETE /api/agent/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Long id) {
        boolean success = aiAgentService.deleteById(id);
        if (success) {
            return R.ok("删除成功!", null);
        }
        return R.error("删除失败");
    }

    /**
     * 切换启用/禁用状态
     * PUT /api/agent/status/{id}
     */
    @PutMapping("/status/{id}")
    public R<Map<String, Object>> toggleStatus(@PathVariable Long id) {
        Integer newStatus = aiAgentService.toggleStatus(id);
        Map<String, Object> data = new HashMap<>();
        data.put("newStatus", newStatus);
        data.put("statusText", newStatus == 1 ? "已启用" : "已禁用");
        return R.ok("状态切换成功", data);
    }
}
