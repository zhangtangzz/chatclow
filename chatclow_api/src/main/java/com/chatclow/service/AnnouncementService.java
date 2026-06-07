package com.chatclow.service;

import com.chatclow.entity.Announcement;
import java.util.List;

public interface AnnouncementService {

    /**
     * 新增公告
     */
    boolean add(Announcement announcement);

    /**
     * 查询所有公告（管理员用）
     */
    List<Announcement> listAll();

    /**
     * 获取最新一条启用的公告（用户端用）
     */
    Announcement getLatestEnabled();

    /**
     * 根据ID查询公告
     */
    Announcement getById(Long id);

    /**
     * 更新公告
     */
    boolean update(Announcement announcement);

    /**
     * 删除公告
     */
    boolean deleteById(Long id);
}
