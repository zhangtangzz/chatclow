package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chatclow.entity.AiAgent;
import com.chatclow.entity.AiModel;
import com.chatclow.mapper.AiAgentMapper;
import com.chatclow.mapper.AiModelMapper;
import com.chatclow.service.AiModelService;
import com.chatclow.util.AesUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiModelServiceImpl implements AiModelService {

    private static final Logger log = LoggerFactory.getLogger(AiModelServiceImpl.class);

    @Autowired
    private AiModelMapper aiModelMapper;

    @Autowired
    private AiAgentMapper aiAgentMapper;

    @Autowired
    private OkHttpClient httpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean add(AiModel aiModel) {
        if (aiModel.getStatus() == null) {
            aiModel.setStatus(1);
        }
        if (aiModel.getApiKey() != null && !aiModel.getApiKey().isEmpty()) {
            aiModel.setApiKey(AesUtil.encrypt(aiModel.getApiKey()));
        }
        return aiModelMapper.insert(aiModel) > 0;
    }

    @Override
    public List<AiModel> listEnabled() {
        List<AiModel> list = aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getStatus, 1)
                        .orderByDesc(AiModel::getCreatedDt)
        );
        list.forEach(m -> m.setApiKey(AesUtil.mask(m.getApiKey())));
        return list;
    }

    @Override
    public List<AiModel> listAll() {
        List<AiModel> list = aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .orderByDesc(AiModel::getCreatedDt)
        );
        list.forEach(m -> m.setApiKey(AesUtil.mask(m.getApiKey())));
        return list;
    }

    @Override
    public AiModel getById(Long id) {
        AiModel model = aiModelMapper.selectById(id);
        if (model != null) {
            model.setApiKey(AesUtil.mask(model.getApiKey()));
        }
        return model;
    }

    @Override
    public boolean update(AiModel aiModel) {
        // 如果 apiKey 是脱敏后的值（含 ****），说明用户没修改，保持原值
        if (AesUtil.isMasked(aiModel.getApiKey())) {
            aiModel.setApiKey(null); // 不更新此字段
        } else if (aiModel.getApiKey() != null && !aiModel.getApiKey().isEmpty()) {
            aiModel.setApiKey(AesUtil.encrypt(aiModel.getApiKey()));
        }
        return aiModelMapper.updateById(aiModel) > 0;
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        // 1. 查出另一个可用的模型作为替补
        List<AiModel> available = aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .ne(AiModel::getId, id)
                        .eq(AiModel::getStatus, 1)
                        .last("LIMIT 1")
        );
        Long fallbackModelId = available.isEmpty() ? null : available.get(0).getId();

        // 2. 将所有引用该模型的智能体切换到替补模型
        LambdaUpdateWrapper<AiAgent> updateWrapper = new LambdaUpdateWrapper<AiAgent>()
                .eq(AiAgent::getModelId, id);
        AiAgent updateEntity = new AiAgent();
        updateEntity.setModelId(fallbackModelId);
        aiAgentMapper.update(updateEntity, updateWrapper);

        // 3. 删除模型
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

    @Override
    public boolean testConnection(AiModel model) {
        try {
            // 如果传进来的 key 是脱敏的，从数据库取出真实密钥
            String apiKey = model.getApiKey();
            if (AesUtil.isMasked(apiKey)) {
                AiModel dbModel = aiModelMapper.selectById(model.getId());
                if (dbModel != null) {
                    apiKey = AesUtil.decrypt(dbModel.getApiKey());
                }
            } else if (apiKey != null && !apiKey.isEmpty()) {
                // 新填的密钥，解密前先解密（其实是明文）
                // 测试前不需要加密，直接用明文测试
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", model.getModelCode());
            List<Map<String, String>> msgs = new ArrayList<>();
            Map<String, String> msg = new HashMap<>();
            msg.put("role", "user");
            msg.put("content", "Hi");
            msgs.add(msg);
            body.put("messages", msgs);
            body.put("max_tokens", 5);
            body.put("stream", false);

            String json = objectMapper.writeValueAsString(body);
            log.info("[测试连接] 请求 URL: {}, body: {}", model.getApiUrl(), json);

            Request request = new Request.Builder()
                    .url(model.getApiUrl())
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "(空)";
                log.info("[测试连接] 响应状态码: {}, body: {}", response.code(), respBody);
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.error("模型连接测试失败", e);
            return false;
        }
    }
}
