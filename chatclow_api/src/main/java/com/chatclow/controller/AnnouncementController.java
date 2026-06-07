package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.Announcement;
import com.chatclow.mapper.UserMapper;
import com.chatclow.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 系统公告控制器
 */
@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private UserMapper userMapper;

    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        com.chatclow.entity.User user = userMapper.selectById(userId);
        return user != null && user.getRole() != null && user.getRole() == 2;
    }

    /**
     * 获取最新一条启用的公告（用户端）
     * GET /api/announcement/latest
     */
    @GetMapping("/latest")
    public R<Announcement> getLatest() {
        Announcement announcement = announcementService.getLatestEnabled();
        return R.ok(announcement);
    }

    /**
     * 查询所有公告（管理员用）
     * GET /api/announcement/admin/list
     */
    @GetMapping("/admin/list")
    public R<List<Announcement>> adminList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!isAdmin(userId)) {
            return R.error(403, "无权限");
        }
        List<Announcement> list = announcementService.listAll();
        return R.ok(list);
    }

    /**
     * 新增公告
     * POST /api/announcement/add
     */
    @PostMapping("/add")
    public R<Void> add(@RequestBody Announcement announcement, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!isAdmin(userId)) {
            return R.error(403, "无权限");
        }
        boolean success = announcementService.add(announcement);
        if (success) {
            return R.ok("公告创建成功", null);
        }
        return R.error("创建失败");
    }

    /**
     * 更新公告
     * PUT /api/announcement/update
     */
    @PutMapping("/update")
    public R<Void> update(@RequestBody Announcement announcement, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!isAdmin(userId)) {
            return R.error(403, "无权限");
        }
        boolean success = announcementService.update(announcement);
        if (success) {
            return R.ok("更新成功", null);
        }
        return R.error("更新失败");
    }

    /**
     * 删除公告
     * DELETE /api/announcement/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!isAdmin(userId)) {
            return R.error(403, "无权限");
        }
        boolean success = announcementService.deleteById(id);
        if (success) {
            return R.ok("删除成功", null);
        }
        return R.error("删除失败");
    }
}
