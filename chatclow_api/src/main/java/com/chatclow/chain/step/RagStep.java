package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.context.ChatContext;
import com.chatclow.entity.AiAgent;
import com.chatclow.entity.RagChunk;
import com.chatclow.service.EmbeddingService;
import com.chatclow.service.RerankService;
import com.chatclow.storage.vector.ChatClowVectorStore;
import com.chatclow.storage.vector.VectorStoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 第2步：RAG 检索增强（@Order = 20）
 *
 * <pre>
 * 输入参数来源：
 *   ContextAssemblyStep → ctx.agent（kbId, ragTopK, ragThreshold 等配置）, ctx.message
 *
 * 流水线（全程在本 Step 内完成）：
 *   ① 向量化 — EmbeddingService.embed(query)         → float[] 问题向量
 *   ② 双路检索 — vectorStore.search() + keywordSearch() → 合并去重
 *   ③ 重排序 — RerankService.rerank()                   → 最终 N 条上下文
 *   ④ 拼装 → 写入 ctx.ragContext（MessageBuildStep 拼入 system prompt）
 *
 * 调用方法：
 *   embeddingService.embed()          → 文本向量化（SiliconFlow API）
 *   vectorStoreFactory.getVectorStore() → 获取对应后端的向量存储
 *   vectorStore.search()              → 向量检索
 *   vectorStore.keywordSearch()       → 关键字检索
 *   rerankService.rerank()            → 重排序
 * </pre>
 */
@Component
@Order(20)
public class RagStep implements ChatChainStep {

    @Autowired private EmbeddingService embeddingService;
    @Autowired private VectorStoreFactory vectorStoreFactory;
    @Autowired private RerankService rerankService;

    @Override
    public void process(ChatContext ctx) {
        AiAgent agent = ctx.getAgent();

        int ragTopK = agent.getRagTopK() != null ? agent.getRagTopK() : 6;
        int ragFinalK = agent.getRagFinalK() != null ? agent.getRagFinalK() : 3;
        float ragThreshold = agent.getRagSimilarityThreshold() != null
                ? agent.getRagSimilarityThreshold() : 0.5f;
        int ragKeywordEnabled = agent.getRagKeywordEnabled() != null
                ? agent.getRagKeywordEnabled() : 1;
        int ragRerankEnabled = agent.getRagRerankEnabled() != null
                ? agent.getRagRerankEnabled() : 1;

        String query = ctx.getMessage();
        Long kbId = agent.getKbId();
        ChatClowVectorStore vectorStore = vectorStoreFactory.getVectorStore(kbId);

        // ①② 并行：向量化 + 关键字检索
        CompletableFuture<float[]> embedFuture =
                CompletableFuture.supplyAsync(() -> embeddingService.embed(query));

        CompletableFuture<List<RagChunk>> keywordFuture;
        if (ragKeywordEnabled == 1) {
            keywordFuture = CompletableFuture.supplyAsync(
                    () -> vectorStore.keywordSearch(query, kbId, ragTopK));
        } else {
            keywordFuture = CompletableFuture.completedFuture(new ArrayList<>());
        }

        float[] questionVector = embedFuture.join();
        List<RagChunk> keywordResults = keywordFuture.join();

        // ③ 向量检索
        List<RagChunk> vectorResults = vectorStore.search(questionVector, kbId, ragTopK, ragThreshold);

        // ④ 合并去重
        Map<Long, RagChunk> merged = new LinkedHashMap<>();
        for (RagChunk chunk : vectorResults) merged.put(chunk.getId(), chunk);
        for (RagChunk chunk : keywordResults) merged.putIfAbsent(chunk.getId(), chunk);
        List<RagChunk> relatedChunks = new ArrayList<>(merged.values());

        String ragContext = "";
        if (!relatedChunks.isEmpty()) {
            List<String> chunkTexts = relatedChunks.stream()
                    .map(RagChunk::getContent).collect(Collectors.toList());

            List<String> finalTexts;
            if (ragRerankEnabled == 1) {
                finalTexts = rerankService.rerank(ctx.getMessage(), chunkTexts, ragFinalK, ragThreshold);
            } else {
                finalTexts = chunkTexts.stream().limit(ragFinalK).collect(Collectors.toList());
            }

            ragContext = String.join("\n\n", finalTexts);
        }

        ctx.setRagContext(ragContext);
    }

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        AiAgent agent = ctx.getAgent();
        return agent == null
                || agent.getKbEnabled() == null
                || agent.getKbEnabled() != 1
                || agent.getKbId() == null;
    }
}
