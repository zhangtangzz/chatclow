package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.RagKnowledgeBase;
import com.chatclow.mapper.UserMapper;
import com.chatclow.service.RagKnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/kb")
public class RagKnowledgeBaseController {

    @Autowired
    private RagKnowledgeBaseService ragKnowledgeBaseService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 从请求中获取当前用户ID（JWT 拦截器已注入）
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /**
     * 检查当前用户是否是管理员
     */
    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        com.chatclow.entity.User user = userMapper.selectById(userId);
        return user != null && user.getRole() != null && user.getRole() == 2;
    }

    /**
     * 检查是否有权限操作该知识库（管理员或创建者）
     */
    private boolean canManage(Long kbId, Long userId) {
        if (userId == null) return false;
        if (isAdmin(userId)) return true;
        RagKnowledgeBase kb = ragKnowledgeBaseService.getById(kbId);
        return kb != null && userId.equals(kb.getUserId());
    }

    /**
     * 新建知识库
     * POST /api/kb/add
     */
    @PostMapping("/add")
    public R<String> add(@RequestBody RagKnowledgeBase kb, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        kb.setUserId(userId);
        ragKnowledgeBaseService.add(kb);
        return R.ok("知识库创建成功");
    }

    /**
     * 查询所有知识库
     * GET /api/kb/list
     */
    @GetMapping("/list")
    public R<List<RagKnowledgeBase>> list() {
        List<RagKnowledgeBase> list = ragKnowledgeBaseService.list();
        return R.ok(list);
    }

    /**
     * 按ID查询
     * GET /api/kb/{id}
     */
    @GetMapping("/{id}")
    public R<RagKnowledgeBase> getById(@PathVariable Long id) {
        RagKnowledgeBase kb = ragKnowledgeBaseService.getById(id);
        return R.ok(kb);
    }

    /**
     * 更新知识库
     * PUT /api/kb/update/{id}
     */
    @PutMapping("/update/{id}")
    public R<String> update(@PathVariable Long id, @RequestBody RagKnowledgeBase kb, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!canManage(id, userId)) {
            return R.error(403, "无权修改该知识库");
        }
        kb.setId(id);
        ragKnowledgeBaseService.update(kb);
        return R.ok("更新成功");
    }

    /**
     * 删除知识库
     * DELETE /api/kb/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!canManage(id, userId)) {
            return R.error(403, "无权删除该知识库");
        }
        ragKnowledgeBaseService.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 切换启用/禁用状态
     * PUT /api/kb/status/{id}
     */
    @PutMapping("/status/{id}")
    public R<Integer> toggleStatus(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!canManage(id, userId)) {
            return R.error(403, "无权修改该知识库");
        }
        Integer newStatus = ragKnowledgeBaseService.toggleStatus(id);
        return R.ok(newStatus);
    }
}
