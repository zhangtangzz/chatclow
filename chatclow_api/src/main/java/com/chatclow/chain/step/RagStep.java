package com.chatclow.chain.step;

import com.chatclow.chain.ChatChainStep;
import com.chatclow.context.ChatContext;
import com.chatclow.entity.AiAgent;
import com.chatclow.entity.RagChunk;
import com.chatclow.service.EmbeddingService;
import com.chatclow.service.RerankService;
import com.chatclow.storage.vector.VectorStoreFactory;  // ← 新增导入
import com.chatclow.storage.vector.ChatClowVectorStore;   // ← 新增导入
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 第2步：RAG 检索增强（@Order = 20）
 *
 * <h3>RAG 是什么？</h3>
 * <p>Retrieval Augmented Generation，检索增强生成。
 * 简单说就是：AI 回答之前，先去知识库里找相关内容，然后带着这些内容去回答。</p>
 *
 * <h3>这个 Step 的完整流水线</h3>
 * <pre>
 * 用户问题
 *   │
 *   ▼
 * ① 向量化（EmbeddingService.embed）        ← 把文字转成 1024 维向量
 *   │
 *   ▼
 * ② 双路检索（通过 VectorStoreFactory 获取 ChatClowVectorStore）
 *   ├── 向量检索：cosine 相似度 > threshold   ← 语义匹配
 *   └── 关键词检索：MySQL LIKE 匹配           ← 精确匹配
 *   │
 *   ├── 合并 + 去重（SHA-256 哈希）
 *   │
 *   ▼
 * ③ Re-rank 重排序（RerankService.rerank）    ← 用专用模型重新打分
 *   │  SiliconFlow BAAI/bge-reranker-v2-m3
 *   │  relevance_score < threshold → 过滤掉
 *   │
 *   ▼
 * ④ 拼装上下文 → 写入 ctx.ragContext
 *    后续 MessageBuildStep 会把它塞进 system prompt
 * </pre>
 *
 * <h3>SPI 改造说明</h3>
 * <p>原来直接注入 VectorSearchService，现在改为通过 VectorStoreFactory 获取 ChatClowVectorStore。
 * 这样可以在不修改代码的情况下，切换不同的向量存储后端（MySQL / Redis / Milvus）。</p>
 */
@Component
@Order(20)
public class RagStep implements ChatChainStep {

    @Autowired
    private EmbeddingService embeddingService;    // 文本 → 向量（调用 SiliconFlow embedding API）

    @Autowired
    private VectorStoreFactory vectorStoreFactory; // ← 改成注入工厂（原来的是 VectorSearchService）

    @Autowired
    private RerankService rerankService;              // 重排序（BAAI/bge-reranker-v2-m3）

    @Override
    public void process(ChatContext ctx) {
        AiAgent agent = ctx.getAgent();

        // ─────── 从 Agent 配置读取参数，没配就用默认值 ───────
        int ragTopK = agent.getRagTopK() != null ? agent.getRagTopK() : 6;
        int ragFinalK = agent.getRagFinalK() != null ? agent.getRagFinalK() : 3;
        float ragThreshold = agent.getRagSimilarityThreshold() != null
                ? agent.getRagSimilarityThreshold() : 0.5f;
        int ragKeywordEnabled = agent.getRagKeywordEnabled() != null
                ? agent.getRagKeywordEnabled() : 1;
        int ragRerankEnabled = agent.getRagRerankEnabled() != null
                ? agent.getRagRerankEnabled() : 1;

        // ─────── ① 向量化：把用户问题转成向量 ───────
        float[] questionVector = embeddingService.embed(ctx.getMessage());

        // ─────── ② 通过工厂获取向量存储实例 ───────
        // SPI 核心：不再直接依赖 VectorSearchService，而是面向接口编程
        ChatClowVectorStore vectorStore = vectorStoreFactory.getVectorStore(agent.getKbId());

        // 双路检索（向量 + 关键词）
        List<RagChunk> relatedChunks = vectorStore.hybridSearch(
                questionVector,           // 问题向量
                ctx.getMessage(),         // 原始问题文本（关键词检索用）
                agent.getKbId(),          // 知识库 ID
                ragTopK,                  // 检索多少条候选
                ragThreshold             // 向量相似度门槛
        );

        String ragContext = "";
        if (!relatedChunks.isEmpty()) {
            // ─────── ③ 提取文本内容 ───────
            List<String> chunkTexts = relatedChunks.stream()
                    .map(RagChunk::getContent)
                    .collect(Collectors.toList());

            List<String> finalTexts;
            if (ragRerankEnabled == 1) {
                // 开启 Re-rank：重新打分，truncate 到 ragFinalK 条
                // threshold 参数确保低分结果（比如 relevance=0.013）被过滤掉
                finalTexts = rerankService.rerank(
                        ctx.getMessage(), chunkTexts, ragFinalK, ragThreshold);
            } else {
                // 关闭 Re-rank：直接截取前 ragFinalK 条
                finalTexts = chunkTexts.stream()
                        .limit(ragFinalK)
                        .collect(Collectors.toList());
            }

            // ─────── ④ 拼装上下文 ───────
            // 多条文本用双换行分隔，后续 MessageBuildStep 会加到 system prompt 末尾
            ragContext = String.join("\n\n", finalTexts);
            System.out.println("[RAG Step] 检索完成（topK=" + ragTopK
                    + ", finalK=" + ragFinalK + ", threshold=" + ragThreshold
                    + ", keyword=" + ragKeywordEnabled + ", rerank="
                    + ragRerankEnabled + "），最终上下文 " + finalTexts.size() + " 条");
        }

        // 写入 ctx，下一步 MessageBuildStep 会用
        ctx.setRagContext(ragContext);
    }

    @Override
    public boolean shouldSkip(ChatContext ctx) {
        // 只有开启了知识库的 Agent 才需要执行 RAG
        AiAgent agent = ctx.getAgent();
        return agent == null                         // 没加载到 Agent（异常情况）
                || agent.getKbEnabled() == null      // 没配置（视为不开启）
                || agent.getKbEnabled() != 1         // 明确没开启
                || agent.getKbId() == null;          // 开启了但没绑定知识库
    }
}
