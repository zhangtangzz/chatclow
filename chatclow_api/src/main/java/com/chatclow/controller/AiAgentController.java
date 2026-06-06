package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.AiAgent;
import com.chatclow.mapper.UserMapper;
import com.chatclow.service.AiAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
     * 检查当前用户是否是管理员
     */
    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        com.chatclow.entity.User user = userMapper.selectById(userId);
        return user != null && user.getRole() != null && user.getRole() == 2;
    }

    /**
     * 检查是否有权限操作该智能体（管理员或创建者）
     */
    private boolean canManage(AiAgent agent, Long userId) {
        if (userId == null) return false;
        return isAdmin(userId) || userId.equals(agent.getUserId());
    }

    /**
     * 新增智能体
     * POST /api/agent/add
     */
    @PostMapping("/add")
    public R<Void> add(@RequestBody @Valid AiAgent aiAgent, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        aiAgent.setUserId(userId);
        boolean success = aiAgentService.add(aiAgent);
        if (success) {
            return R.ok("智能体创建成功!", null);
        }
        return R.error("创建失败");
    }

    /**
     * 查询智能体列表（每人只能看到自己创建的）
     * GET /api/agent/list/{userId}
     */
    @GetMapping("/list/{userId}")
    public R<List<AiAgent>> listByUserId(@PathVariable Long userId) {
        List<AiAgent> list = aiAgentService.listByUserId(userId);
        return R.ok("查询成功", list);
    }

    /**
     * 查询所有智能体（管理员用，含禁用）
     * GET /api/agent/admin/list
     */
    @GetMapping("/admin/list")
    public R<List<AiAgent>> adminListAll() {
        List<AiAgent> list = aiAgentService.listAll();
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
    public R<Void> update(@RequestBody @Valid AiAgent aiAgent, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        AiAgent existing = aiAgentService.getById(aiAgent.getId());
        if (existing == null) {
            return R.error("智能体不存在");
        }
        if (!canManage(existing, userId)) {
            return R.error(403, "无权修改该智能体");
        }
        // 保留原始创建者
        aiAgent.setUserId(existing.getUserId());
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
    public R<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        AiAgent existing = aiAgentService.getById(id);
        if (existing == null) {
            return R.error("智能体不存在");
        }
        if (!canManage(existing, userId)) {
            return R.error(403, "无权删除该智能体");
        }
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
    public R<Map<String, Object>> toggleStatus(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        AiAgent existing = aiAgentService.getById(id);
        if (existing == null) {
            return R.error("智能体不存在");
        }
        if (!canManage(existing, userId)) {
            return R.error(403, "无权修改该智能体");
        }
        Integer newStatus = aiAgentService.toggleStatus(id);
        Map<String, Object> data = new HashMap<>();
        data.put("newStatus", newStatus);
        data.put("statusText", newStatus == 1 ? "已启用" : "已禁用");
        return R.ok("状态切换成功", data);
    }
}
