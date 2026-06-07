package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.Announcement;
import com.chatclow.mapper.AnnouncementMapper;
import com.chatclow.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Override
    public boolean add(Announcement announcement) {
        if (announcement.getStatus() == null) {
            announcement.setStatus(1);
        }
        return announcementMapper.insert(announcement) > 0;
    }

    @Override
    public List<Announcement> listAll() {
        return announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .orderByDesc(Announcement::getCreatedDt)
        );
    }

    @Override
    public Announcement getLatestEnabled() {
        return announcementMapper.selectOne(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getStatus, 1)
                        .orderByDesc(Announcement::getCreatedDt)
                        .last("LIMIT 1")
        );
    }

    @Override
    public Announcement getById(Long id) {
        return announcementMapper.selectById(id);
    }

    @Override
    public boolean update(Announcement announcement) {
        return announcementMapper.updateById(announcement) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return announcementMapper.deleteById(id) > 0;
    }
}
