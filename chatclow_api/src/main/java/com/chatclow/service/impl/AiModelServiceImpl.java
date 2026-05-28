package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.AiModel;
import com.chatclow.mapper.AiModelMapper;
import com.chatclow.service.AiModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiModelServiceImpl implements AiModelService {

    @Autowired
    private AiModelMapper aiModelMapper;

    @Override
    public boolean add(AiModel aiModel) {
        // 新增时默认设为启用状态
        if (aiModel.getStatus() == null) {
            aiModel.setStatus(1);
        }
        return aiModelMapper.insert(aiModel) > 0;
    }

    @Override
    public List<AiModel> listEnabled() {
        // 查询 status=1 的所有记录
        return aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getStatus, 1)
                        .orderByDesc(AiModel::getCreatedDt)
        );
    }

    @Override
    public List<AiModel> listAll() {
        return aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .orderByDesc(AiModel::getCreatedDt)
        );
    }

    @Override
    public AiModel getById(Long id) {
        return aiModelMapper.selectById(id);
    }

    @Override
    public boolean update(AiModel aiModel) {
        return aiModelMapper.updateById(aiModel) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return aiModelMapper.deleteById(id) > 0;
    }

    @Override
    public Integer toggleStatus(Long id) {
        // 1. 先查出当前模型
        AiModel model = aiModelMapper.selectById(id);
        if (model == null) {
            throw new RuntimeException("模型不存在");
        }

        // 2. 切换状态：1→0 或 0→1
        int newStatus = model.getStatus() == 1 ? 0 : 1;
        model.setStatus(newStatus);
        aiModelMapper.updateById(model);

        return newStatus;
    }
}
