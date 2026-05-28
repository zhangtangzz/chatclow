# ChatClow RAG 知识库企业级特性实现总结

**项目**：chatclow — Java Spring Boot AI 对话系统  
**完成时间**：2026-05-27  
**实现人**：张亮亮  

---

## 目录

1. [已完成的 4 个企业级特性](#1-已完成的-4-个企业级特性)
2. [特性一：文档去重策略（SHA-256 哈希）](#特性一文档去重策略sha-256-哈希)
3. [特性二：双路检索（向量 + 关键字）](#特性二双路检索向量--关键字)
4. [特性三：Re-rank 模型集成](#特性三re-rank-模型集成)
5. [特性四：搜索增强引擎配置](#特性四搜索增强引擎配置)
6. [涉及的文件清单](#涉及的文件清单)
7. [数据库变更](#数据库变更)
8. [Maven 依赖变更](#maven-依赖变更)
9. [关键踩坑记录](#关键踩坑记录)

---

## 1. 已完成的 4 个企业级特性

| # | 特性 | 状态 | 验证方式 |
|---|------|------|----------|
| 1 | 文档去重策略（SHA-256 哈希） | ✅ 完成 | 上传相同内容文档时返回重复提示 |
| 2 | 双路检索（向量 + 关键字） | ✅ 完成 | 日志显示 `向量检索: 8 条，关键字检索: 9 条` |
| 3 | Re-rank 模型集成 | ✅ 完成 | 日志显示 `重排序完成，返回 3 条` |
| 4 | 搜索增强引擎配置 | ✅ 完成 | 日志显示参数全部来自数据库 |

---

## 特性一：文档去重策略（SHA-256 哈希）

### 目标
上传文档时，按内容计算 SHA-256 哈希，如果数据库中已存在相同哈希的 chunk，则跳过，实现文档级去重。

### 实现位置
`DocumentServiceImpl.java` — `saveDocument()` 方法

### 核心代码逻辑

```java
// 1. 对文档全文计算 SHA-256
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
StringBuilder sb = new StringBuilder();
for (byte b : hashBytes) {
    sb.append(String.format("%02x", b));
}
String contentHash = sb.toString();  // 64 位十六进制字符串

// 2. 查询数据库是否已存在该哈希
LambdaQueryWrapper<RagChunk> existsWrapper = new LambdaQueryWrapper<>();
existsWrapper.eq(RagChunk::getContentHash, contentHash)
             .eq(RagChunk::getKbId, kbId)
             .last("LIMIT 1");
RagChunk existing = ragChunkMapper.selectOne(existsWrapper);

// 3. 如果存在，跳过（不报错，静默去重）
if (existing != null) {
    System.out.println("[Dedup] 内容已存在，跳过: " + fileName);
    continue;  // 跳过当前文件，继续处理下一个
}
```

### 数据库字段
`rag_chunk` 表新增字段：
```sql
ALTER TABLE rag_chunk ADD COLUMN content_hash VARCHAR(64) NULL COMMENT '内容SHA-256哈希';
CREATE INDEX idx_content_hash ON rag_chunk(content_hash);
```

### 关键知识点
- SHA-256 输出固定 64 位十六进制字符串
- 去重粒度是 **chunk 级别**（同一文档的每个 chunk 共享同一个 `docHash`，但各自有独立的 `contentHash`）
- `last("LIMIT 1")` 是 MyBatis-Plus 的性能优化，找到一条就停

---

## 特性二：双路检索（向量 + 关键字）

### 目标
同时走两条检索路径，提高召回率：
- **向量检索**：语义相似，用余弦相似度
- **关键字检索**：精确匹配，用 SQL `LIKE`

两条路的结果合并去重，返回 Top-K。

### 实现位置
`VectorSearchServiceImpl.java` — `hybridSearch()` 方法

### 核心代码逻辑

```java
@Override
public List<RagChunk> hybridSearch(float[] questionVector, String query,
                                   Long kbId, int topK, float threshold, int keywordEnabled) {
    // 1. 向量检索（原有逻辑）
    List<RagChunk> vectorResults = search(questionVector, kbId, topK, threshold);

    // 2. 关键字检索（新增，受 keywordEnabled 控制）
    List<RagChunk> keywordResults = new ArrayList<>();
    if (keywordEnabled == 1) {
        keywordResults = keywordSearch(query, kbId, topK);
    }

    // 3. 合并去重（LinkedHashMap 保持顺序）
    Map<Long, RagChunk> merged = new LinkedHashMap<>();
    for (RagChunk chunk : vectorResults) {
        merged.put(chunk.getId(), chunk);
    }
    for (RagChunk chunk : keywordResults) {
        merged.putIfAbsent(chunk.getId(), chunk);  // 已在 vector 里的不会被覆盖
    }

    // 4. 截断到 topK
    return merged.values().stream().limit(topK).collect(Collectors.toList());
}
```

### 关键字检索实现

```java
@Override
public List<RagChunk> keywordSearch(String query, Long kbId, int topK) {
    // 1. 提取关键词（见下方详解）
    List<String> keywords = extractKeywords(query);

    // 2. 逐词 LIKE 查询
    LambdaQueryWrapper<RagChunk> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(RagChunk::getKbId, kbId);
    for (String kw : keywords) {
        wrapper.like(RagChunk::getContent, kw);  // 每个关键词都要匹配（AND 关系）
    }
    wrapper.orderByDesc(RagChunk::getId).last("LIMIT " + topK);

    return ragChunkMapper.selectList(wrapper);
}
```

### 关键词提取算法（踩坑 3 次才正确）

```java
private List<String> extractKeywords(String query) {
    // Step 1: 按空格/标点拆分
    String[] rawTokens = query.split("[\\s\\p{Punct}]+");

    List<String> keywords = new ArrayList<>();
    for (String token : rawTokens) {
        if (token.isEmpty()) continue;

        // Step 2: 中英文混合的 token，按语言边界拆分
        // 例如 "Java基础" → ["Java", "基础"]
        List<String> subtokens = splitByLanguageBoundary(token);

        for (String subtoken : subtokens) {
            // Step 3: 按停用词再拆分
            // 例如 "一下标题" → ["一下", "标题"] → 去掉停用词 "一下" → ["标题"]
            List<String> finalTokens = splitByStopWords(subtoken);

            for (String kw : finalTokens) {
                // Step 4: 去掉无意义的边缘字符（填充词）
                // 例如 "下标题" → 去掉 "下" → "标题"
                kw = stripFillerChars(kw);

                // Step 5: 长度过滤（1~50 字符）
                if (kw.length() >= 1 && kw.length() <= 50) {
                    keywords.add(kw);
                }
            }
        }
    }
    return keywords;
}
```

**关键辅助方法：**

```java
// 判断是否是中文字符
private boolean isChineseChar(char c) {
    return Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
}

// 判断是否是填充词（无实际语义）
private boolean isFillerChar(char c) {
    String fillers = "下了上在从被把被和就也还这那的了的啊哦嗯呢嘛喔呀哇";
    return fillers.indexOf(c) >= 0;
}
```

### 关键知识点
- `LinkedHashMap` 用于合并去重且保持向量结果优先的顺序
- 关键字检索用 `AND` 关系（每个关键词都要匹配），提高精度
- 中文关键词提取的难点：中英文边界 + 停用词 + 填充词，三步处理才准确

---

## 特性三：Re-rank 模型集成

### 目标
用专门的 Re-rank 模型对候选文档做精排，提升最终上下文的相关度。

Re-rank 模型比向量模型更精准，因为它能看到 **完整的 query + document 对**，而不是只看 document 的向量。

### 实现位置
- `RerankService.java`（接口）
- `RerankServiceImpl.java`（实现，调用 SiliconFlow API）

### 核心代码逻辑

```java
@Override
public List<String> rerank(String query, List<String> documents, int topK) {
    try {
        // 1. 构造 SiliconFlow /v1/rerank 请求体
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", "BAAI/bge-reranker-v2-m3");
        requestBody.put("query", query);
        ArrayNode docsArray = requestBody.putArray("documents");
        for (String doc : documents) {
            docsArray.add(doc);
        }
        requestBody.put("top_n", topK);

        // 2. 发送 HTTP 请求
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://api.siliconflow.cn/v1/rerank")
                .post(okhttp3.RequestBody.create(
                    objectMapper.writeValueAsString(requestBody),
                    okhttp3.MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (okhttp3.Response response = httpClient.newCall(request).execute()) {
            // 3. 解析返回结果
            JsonNode root = objectMapper.readTree(response.body().string());
            JsonNode results = root.path("results");

            // 4. 按 relevance_score 排序
            List<JsonNode> sorted = new ArrayList<>();
            results.forEach(sorted::add);
            sorted.sort((a, b) -> Double.compare(
                b.path("relevance_score").asDouble(0),
                a.path("relevance_score").asDouble(0)
            ));

            // 5. 返回 topK 文档内容
            List<String> reranked = new ArrayList<>();
            for (int i = 0; i < Math.min(topK, sorted.size()); i++) {
                int index = sorted.get(i).path("index").asInt();
                reranked.add(documents.get(index));
            }
            return reranked;
        }
    } catch (Exception e) {
        System.err.println("[RAG-Rerank] 失败，降级为原顺序: " + e.getMessage());
        return documents.stream().limit(topK).collect(Collectors.toList());
    }
}
```

### 集成到 RAG 流程

在 `ChatServiceImpl.java` 的 `chat()` 和 `chatStream()` 方法中：

```java
// 1. 混合检索得到候选文档
List<RagChunk> candidates = vectorSearchService.hybridSearch(
    questionVector, message, kbId, topK, threshold, keywordEnabled);

// 2. Re-rank 精排（受 rerankEnabled 控制）
List<RagChunk> finalChunks;
if (rerankEnabled == 1) {
    List<String> docs = candidates.stream()
        .map(RagChunk::getContent).collect(Collectors.toList());
    List<String> reranked = rerankService.rerank(message, docs, finalK);
    // 按 rerank 后顺序重新取 chunk
    // ...（详见源码）
} else {
    finalChunks = candidates.stream().limit(finalK).collect(Collectors.toList());
}
```

### 配置（application.yml）

```yaml
rag:
  embedding:
    api-key: sk-xxx   # SiliconFlow API Key（Re-rank 和 Embedding 共用）
```

### 关键知识点
- Re-rank API 的 `top_n` 参数控制返回几条
- 失败时要**降级处理**（返回原顺序），不能让整个 RAG 流程崩溃
- `@Value("${rag.embedding.api-key}")` 注入配置，注意 key 名称要和 yml 里一致

---

## 特性四：搜索增强引擎配置

### 目标
把 RAG 检索的参数（topK、阈值、是否启用关键字/Re-rank 等）从**硬编码**改为**数据库可配置**，每个 Agent 可以有不同的配置。

### 实现位置
- `AiAgent.java`（实体类，新增 5 个字段）
- `ChatServiceImpl.java`（读取 Agent 配置后传给检索方法）

### 数据库变更

```sql
ALTER TABLE chatclow_ai_agent
  ADD COLUMN rag_top_k INT NULL DEFAULT 6 COMMENT 'RAG检索返回条数',
  ADD COLUMN rag_final_k INT NULL DEFAULT 3 COMMENT 'RAG最终上下文条数',
  ADD COLUMN rag_similarity_threshold FLOAT NULL DEFAULT 0.5 COMMENT '向量相似度阈值',
  ADD COLUMN rag_keyword_enabled INT NULL DEFAULT 1 COMMENT '是否启用关键字检索 0/1',
  ADD COLUMN rag_rerank_enabled INT NULL DEFAULT 1 COMMENT '是否启用Re-rank 0/1';
```

### 实体类变更（AiAgent.java）

```java
@Data
public class AiAgent {
    private Long id;
    private String name;
    private String systemPrompt;
    private Long modelId;
    private Integer status;

    // === RAG 搜索增强配置（新增）===
    private Integer ragTopK;              // 检索返回条数，默认 6
    private Integer ragFinalK;            // 最终上下文条数，默认 3
    private Float ragSimilarityThreshold; // 向量相似度阈值，默认 0.5
    private Integer ragKeywordEnabled;    // 是否启用关键字检索，默认 1
    private Integer ragRerankEnabled;     // 是否启用 Re-rank，默认 1
}
```

### ChatServiceImpl 中的使用

```java
// 1. 从数据库读取配置（带默认值）
AiAgent agent = aiAgentMapper.selectById(agentId);
int topK = agent.getRagTopK() != null ? agent.getRagTopK() : 6;
int finalK = agent.getRagFinalK() != null ? agent.getRagFinalK() : 3;
float threshold = agent.getRagSimilarityThreshold() != null
    ? agent.getRagSimilarityThreshold() : 0.5f;
int keywordEnabled = agent.getRagKeywordEnabled() != null
    ? agent.getRagKeywordEnabled() : 1;
int rerankEnabled = agent.getRagRerankEnabled() != null
    ? agent.getRagRerankEnabled() : 1;

// 2. 传给检索方法
List<RagChunk> candidates = vectorSearchService.hybridSearch(
    questionVector, message, kbId, topK, threshold, keywordEnabled);
```

### 关键知识点
- 数据库字段用 `NULL DEFAULT X`，这样老数据不用全部迁移，新数据有默认值
- Java 读取时用 `!= null ? x : 默认值` 做兜底，防止数据库里是 NULL
- 改数据库比改代码快得多，这是**企业级配置管理**的基本思路

---

## 涉及的文件清单

| 文件 | 变更内容 |
|------|----------|
| `DocumentServiceImpl.java` | 新增 SHA-256 去重逻辑 |
| `VectorSearchService.java` | 接口新增 `hybridSearch()`、`keywordSearch()` 方法 |
| `VectorSearchServiceImpl.java` | 实现双路检索 + 关键词提取算法 |
| `RerankService.java` | 新建，Re-rank 接口 |
| `RerankServiceImpl.java` | 新建，调用 SiliconFlow API |
| `ChatServiceImpl.java` | 集成 Re-rank + 读取数据库配置 + Markdown 清洗 |
| `AiAgent.java` | 新增 5 个 RAG 配置字段 |
| `application.yml` | 确认 `rag.embedding.api-key` 配置 |

---

## 数据库变更

### rag_chunk 表
```sql
ALTER TABLE rag_chunk ADD COLUMN content_hash VARCHAR(64) NULL COMMENT '内容SHA-256哈希';
CREATE INDEX idx_content_hash ON rag_chunk(content_hash);
```

### chatclow_ai_agent 表
```sql
ALTER TABLE chatclow_ai_agent
  ADD COLUMN rag_top_k INT NULL DEFAULT 6,
  ADD COLUMN rag_final_k INT NULL DEFAULT 3,
  ADD COLUMN rag_similarity_threshold FLOAT NULL DEFAULT 0.5,
  ADD COLUMN rag_keyword_enabled INT NULL DEFAULT 1,
  ADD COLUMN rag_rerank_enabled INT NULL DEFAULT 1;
```

---

## Maven 依赖变更

**无新增依赖**。  
Re-rank 实现用的是项目已有的 `okhttp3` 和 `jackson-databind`，不需要额外加依赖。

（最初尝试用 `org.json`，后来发现 pom.xml 里没有，改为用 Jackson 实现。）

---

## 关键踩坑记录

### 坑 1：关键词提取 — 整句被当成一个词
**现象**：查询"Java基础有哪些内容"，关键词只有 1 个（整个句子）。  
**原因**：`split(" ")` 只按空格拆分，中文没有空格。  
**修复**：加入 `splitByLanguageBoundary()` 按中英文边界拆分。

### 坑 2：关键词提取 — 出现碎片词
**现象**：关键词出现"va基础"、"下标题"这样的碎片。  
**原因**：中英文边界拆分后，还有停用词（"一下"）和填充词（"下"）未处理。  
**修复**：加入 `splitByStopWords()` 和 `stripFillerChars()` 两步清洗。

### 坑 3：`org.json` 包不存在
**现象**：编译报错 `程序包org.json不存在`。  
**原因**：pom.xml 里没有 `org.json` 依赖。  
**修复**：改为用 Jackson（`ObjectMapper`）实现 JSON 解析，项目已有此依赖。

### 坑 4：`Could not resolve placeholder 'siliconflow.api-key'`
**现象**：启动报错，找不到配置。  
**原因**：`@Value("${siliconflow.api-key}")` 但 yml 里的 key 是 `rag.embedding.api-key`。  
**修复**：改为 `@Value("${rag.embedding.api-key}")`。

### 坑 5：APIPost 返回内容有很多 `\n` 和 `**`
**现象**：AI 返回的 Markdown 格式在 APIPost 里显示为原始字符串。  
**修复**：在 `ChatServiceImpl` 里加入 `stripMarkdown()` 方法，返回前清洗格式。  
（注：此修复仅对同步接口生效；流式接口需前端处理。）

---

## 验证方式

启动项目后，发送包含知识库问题的消息，观察控制台日志：

```
[RAG-Keyword] 关键字检索命中 X 条，查询词: xxx, 提取关键词: [...]
[RAG-Hybrid] 向量检索: X 条，关键字检索: X 条（keywordEnabled=X）
[RAG-Rerank] 开始重排序，候选文档数: X，查询: xxx
[RAG-Rerank] 重排序完成，返回 X 条
[RAG] 检索完成（topK=X, finalK=X, threshold=X, keyword=X, rerank=X）
```

如果看到以上日志，说明 4 个特性全部正常工作。

---

*文档生成时间：2026-05-27*
*项目路径：C:\Users\张亮亮\Desktop\chatclow*
