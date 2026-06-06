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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
     *
     * <h3>并行化优化</h3>
     * <p>当输入超过 batchSize 条时，将多个批次并行发送到 API，而非串行等待。
     * 例如 150 段文本 → 3 批并行，总耗时 ≈ max(单批耗时) 而不是 sum(3批耗时)。</p>
     *
     * <h3>并发控制</h3>
     * <p>利用 ForkJoinPool.commonPool() 并行发送所有批次。
     * 如需限制并发数，可注入自定义 Executor 替换 supplyAsync 的默认线程池。</p>
     */
    private List<float[]> doEmbed(List<String> inputs) {
        int batchSize = 50;                     // SiliconFlow API 单次调用上限

        // 计算需要多少批
        int totalBatches = (int) Math.ceil((double) inputs.size() / batchSize);

        // 构建每条批次的独立任务
        List<CompletableFuture<List<float[]>>> futures = IntStream.range(0, totalBatches)
                .mapToObj(batchIndex -> CompletableFuture.supplyAsync(() -> {
                    int start = batchIndex * batchSize;
                    int end = Math.min(start + batchSize, inputs.size());
                    List<String> batch = inputs.subList(start, end);
                    return callEmbeddingApi(batch, batchIndex + 1, totalBatches);
                }))
                .collect(Collectors.toList());

        // 等待所有批次完成，按顺序合并结果
        List<float[]> allEmbeddings = futures.stream()
                .map(CompletableFuture::join)     // join 保持提交顺序
                .flatMap(List::stream)
                .collect(Collectors.toList());

        System.out.println("[Embedding] 全部向量化成功！共 " + allEmbeddings.size()
                + " 条向量，维度: " + (allEmbeddings.isEmpty() ? 0 : allEmbeddings.get(0).length));
        return allEmbeddings;
    }

    /**
     * 调用一次 API，处理一批文本，返回这批文本的向量列表。
     * RestTemplate 是线程安全的（构造后只读），多线程并发调用 exchange() 不会有问题。
     */
    private List<float[]> callEmbeddingApi(List<String> batch, int batchNum, int totalBatches) {
        try {
            System.out.println("[Embedding] 批次 " + batchNum + "/" + totalBatches
                    + "：开始处理 " + batch.size() + " 条（并行模式）");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", batch);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, requestEntity, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode dataArray = rootNode.path("data");

            List<float[]> embeddings = new ArrayList<>();
            for (JsonNode item : dataArray) {
                JsonNode embeddingArray = item.path("embedding");
                float[] vector = new float[embeddingArray.size()];
                for (int i = 0; i < embeddingArray.size(); i++) {
                    vector[i] = (float) embeddingArray.get(i).asDouble();
                }
                embeddings.add(vector);
            }

            System.out.println("[Embedding] 批次 " + batchNum + "/" + totalBatches
                    + "：完成，返回 " + embeddings.size() + " 条向量");
            return embeddings;

        } catch (Exception e) {
            throw new RuntimeException("向量化失败（批次" + batchNum + "/" + totalBatches + "）：" + e.getMessage(), e);
        }
    }
}
