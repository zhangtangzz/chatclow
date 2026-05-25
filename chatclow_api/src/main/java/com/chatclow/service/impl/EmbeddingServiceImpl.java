package com.chatclow.service.impl;

import com.chatclow.service.EmbeddingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 硅基流动 SiliconFlow Embedding 向量化服务实现
 * 使用 BAAI/bge-m3 模型，支持多语言（含中文）
 */
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    @Value("${rag.embedding.api-url}")
    private String apiUrl;

    @Value("${rag.embedding.api-key}")
    private String apiKey;

    @Value("${rag.embedding.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public float[] embed(String text) {
        List<float[]> result = doEmbed(List.of(text));
        return result.get(0);
    }

    @Override
    public List<float[]> batchEmbed(List<String> texts) {
        return doEmbed(texts);
    }

    /**
     * 核心方法：调用硅基流动 Embedding API（OpenAI 兼容格式）
     * 分批调用，每批最多 50 条，避免超过 API 限制
     */
    private List<float[]> doEmbed(List<String> inputs) {
        List<float[]> allEmbeddings = new ArrayList<>();
        int batchSize = 50; // SiliconFlow 批次上限，保守设置

        try {
            for (int start = 0; start < inputs.size(); start += batchSize) {
                int end = Math.min(start + batchSize, inputs.size());
                List<String> batch = inputs.subList(start, end);

                System.out.println("[Embedding] 批次 " + (start / batchSize + 1)
                        + "：第 " + (start + 1) + "~" + end + " 条，共 " + inputs.size() + " 条");

                // 1. 构造请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                // 2. 构造请求体（OpenAI 兼容格式）
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", model);
                requestBody.put("input", batch);

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

                // 3. 发送 POST 请求
                ResponseEntity<String> response = restTemplate.exchange(
                        apiUrl, HttpMethod.POST, requestEntity, String.class);

                // 4. 解析响应，提取 embedding 数组
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode dataArray = rootNode.path("data");

                for (JsonNode item : dataArray) {
                    JsonNode embeddingArray = item.path("embedding");
                    float[] vector = new float[embeddingArray.size()];
                    for (int i = 0; i < embeddingArray.size(); i++) {
                        vector[i] = (float) embeddingArray.get(i).asDouble();
                    }
                    allEmbeddings.add(vector);
                }

                // 批次间稍作停顿，避免触发限流
                if (end < inputs.size()) {
                    Thread.sleep(200);
                }
            }

            System.out.println("[Embedding] 全部向量化成功！共 " + allEmbeddings.size() + " 条向量，维度: "
                    + (allEmbeddings.isEmpty() ? 0 : allEmbeddings.get(0).length));
            return allEmbeddings;

        } catch (Exception e) {
            throw new RuntimeException("向量化失败：" + e.getMessage(), e);
        }
    }
}
