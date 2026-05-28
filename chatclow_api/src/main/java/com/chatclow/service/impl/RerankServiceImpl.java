package com.chatclow.service.impl;

import com.chatclow.service.RerankService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Re-rank 重排序服务实现
 * 调用 SiliconFlow /v1/rerank 接口对检索结果重新打分排序
 *
 * 为什么需要 Re-rank？
 * 双路检索（向量+关键字）返回的结果，排在前面的不一定是最相关的。
 * Re-rank 模型会对"用户问题 + 每条候选文本"做深度语义理解，重新打分排序。
 */
@Service
public class RerankServiceImpl implements RerankService {

    // 从 application.yml 读取 SiliconFlow API Key（和 Embedding 共用同一个 key）
    @Value("${rag.embedding.api-key}")
    private String apiKey;

    // Re-rank 接口地址
    private static final String RERANK_URL = "https://api.siliconflow.cn/v1/rerank";

    // 使用的 Re-rank 模型（BAAI/bge-reranker-v2-m3 中文效果好）
    private static final String RERANK_MODEL = "BAAI/bge-reranker-v2-m3";

    private final OkHttpClient httpClient;

    public RerankServiceImpl(OkHttpClient okHttpClient) {
        this.httpClient = okHttpClient;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<String> rerank(String query, List<String> documents, int topK, float threshold) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        System.out.println("[RAG-Rerank] 开始重排序，候选文档数: " + documents.size() + "，查询: " + query);

        try {
            // 1. 构建请求体（JSON 格式）
            // {"model":"BAAI/bge-reranker-v2-m3","query":"...","documents":["..."],"top_n":3}
            List<Map<String, Object>> body = new ArrayList<>();
            // 用 Map 构建 JSON，更简洁
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", RERANK_MODEL);
            requestMap.put("query", query);
            requestMap.put("documents", documents);
            requestMap.put("top_n", topK);

            String jsonBody = objectMapper.writeValueAsString(requestMap);

            // 2. 发送 HTTP POST 请求
            Request request = new Request.Builder()
                    .url(RERANK_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "无响应体";
                    System.err.println("[RAG-Rerank] 请求失败: HTTP " + response.code() + "，body: " + errorBody);
                    return fallback(documents, topK);  // 失败兜底
                }

                String responseBody = response.body().string();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                // 3. 解析返回结果
                // SiliconFlow 返回格式：
                // {"results": [{"index":2,"relevance_score":0.98}, {"index":0,"relevance_score":0.85}, ...]}
                JsonNode results = jsonResponse.get("results");
                if (results == null || !results.isArray()) {
                    System.err.println("[RAG-Rerank] 返回格式异常，无 results 字段");
                    return fallback(documents, topK);
                }

                // 4. 按返回顺序，取出对应原文（过滤低于阈值的）
                List<String> reranked = new ArrayList<>();
                for (int i = 0; i < results.size(); i++) {
                    JsonNode result = results.get(i);
                    int index = result.get("index").asInt();
                    double score = result.get("relevance_score").asDouble();
                    if (score < threshold) {
                        System.out.println("[RAG-Rerank] 跳过低分: 原索引=" + index + ", 分数=" + score + " < 阈值=" + threshold);
                        continue;
                    }
                    System.out.println("[RAG-Rerank] 重排序结果 " + i + ": 原索引=" + index + ", 分数=" + score);
                    reranked.add(documents.get(index));
                }

                // 如果 Re-rank 返回的数量不够 topK，用原顺序补全
                if (reranked.size() < topK && documents.size() > reranked.size()) {
                    for (int i = 0; i < documents.size() && reranked.size() < topK; i++) {
                        if (!reranked.contains(documents.get(i))) {
                            reranked.add(documents.get(i));
                        }
                    }
                }

                System.out.println("[RAG-Rerank] 重排序完成，返回 " + reranked.size() + " 条");
                return reranked;
            }

        } catch (IOException e) {
            System.err.println("[RAG-Rerank] 请求异常: " + e.getMessage());
            return fallback(documents, topK);  // 异常兜底
        }
    }

    /** 兜底策略：直接返回前 topK 条（原顺序） */
    private List<String> fallback(List<String> documents, int topK) {
        int limit = Math.min(topK, documents.size());
        System.out.println("[RAG-Rerank] 使用兜底策略，返回前 " + limit + " 条");
        return new ArrayList<>(documents.subList(0, limit));
    }
}
