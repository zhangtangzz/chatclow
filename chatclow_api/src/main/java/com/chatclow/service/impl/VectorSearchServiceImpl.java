package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.RagChunk;
import com.chatclow.mapper.RagChunkMapper;
import com.chatclow.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于余弦相似度的向量搜索服务实现
 */
@Service
public class VectorSearchServiceImpl implements VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchServiceImpl.class);

    @Autowired
    private RagChunkMapper ragChunkMapper;

    /** 默认返回 Top 3 */
    private static final int DEFAULT_TOP_K = 3;

    /** 相似度阈值：低于这个值认为不相关 */
    private static final float SIMILARITY_THRESHOLD = 0.5f;

    @Override
    public List<RagChunk> search(float[] questionVector, Long kbId, int topK) {
        // 默认阈值调用
        return search(questionVector, kbId, topK, SIMILARITY_THRESHOLD);
    }

    @Override
    public List<RagChunk> search(float[] questionVector, Long kbId, int topK, float threshold) {
        // 1. 查出该知识库下所有有向量的切片
        LambdaQueryWrapper<RagChunk> wrapper = new LambdaQueryWrapper<>();
        if (kbId != null) {
            wrapper.eq(RagChunk::getKbId, kbId);
        }
        wrapper.isNotNull(RagChunk::getVectorData);  // 只查有向量的
        List<RagChunk> allChunks = ragChunkMapper.selectList(wrapper);

        if (allChunks.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 计算每条切片与问题的相似度
        log.info("[Vector-Diag] 问题向量维度=" + questionVector.length + "，知识库切片总数=" + allChunks.size() + "，阈值=" + threshold);

        List<ScoredChunk> scoredChunks = new ArrayList<>();
        int parseFailCount = 0;
        int dimMismatchCount = 0;
        int belowThresholdCount = 0;
        Float maxSimilarity = null;

        for (int i = 0; i < allChunks.size(); i++) {
            RagChunk chunk = allChunks.get(i);
            float[] chunkVector = parseVector(chunk.getVectorData());
            if (chunkVector == null || chunkVector.length == 0) {
                parseFailCount++;
                continue;
            }
            // 维度不匹配诊断（只打印前3条）
            if (chunkVector.length != questionVector.length) {
                if (dimMismatchCount < 3) {
                    log.info("[Vector-Diag] 维度不匹配！问题=" + questionVector.length + "维，切片(id=" + chunk.getId() + ")=" + chunkVector.length + "维");
                }
                dimMismatchCount++;
                continue;
            }
            float similarity = cosineSimilarity(questionVector, chunkVector);
            // 收集最高分和前5个分数样本
            if (maxSimilarity == null || similarity > maxSimilarity) maxSimilarity = similarity;
            if (i < 5) {
                log.info("[Vector-Diag] 切片(id=" + chunk.getId() + ") 相似度=" + String.format("%.6f", similarity));
            }
            if (similarity >= threshold) {
                scoredChunks.add(new ScoredChunk(chunk, similarity));
            } else {
                belowThresholdCount++;
            }
        }

        log.info("[Vector-Diag] 统计：解析失败=" + parseFailCount + "，维度不匹配=" + dimMismatchCount
                + "，低于阈值=" + belowThresholdCount + "，通过=" + scoredChunks.size()
                + "，最高相似度=" + (maxSimilarity != null ? String.format("%.6f", maxSimilarity) : "N/A"));

        // 3. 按相似度降序排列，取 topK
        List<RagChunk> result = scoredChunks.stream()
                .sorted((a, b) -> Float.compare(b.score, a.score))  // 降序
                .limit(topK)
                .map(sc -> sc.chunk)
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 关键字检索：对切片内容做 LIKE 模糊匹配
     * 擅长精确匹配：条款编号、年份数字、标准号等
     * 支持中文：按停用词分割 + 滑动窗口提取关键词
     */
    @Override
    public List<RagChunk> keywordSearch(String query, Long kbId, int topK) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 从用户问题中提取关键词
        List<String> keywords = extractKeywords(query.trim());

        if (keywords.isEmpty()) {
            log.info("[RAG-Keyword] 未提取到有效关键词，查询词: " + query);
            return new ArrayList<>();
        }

        // 2. 构建查询条件：内容包含任一关键词（kbId 不为 null 时按知识库过滤）
        LambdaQueryWrapper<RagChunk> wrapper = new LambdaQueryWrapper<>();
        if (kbId != null) {
            wrapper.eq(RagChunk::getKbId, kbId);
        }

        // .and() 里面的逻辑：关键词之间用 OR 连接（匹配到任何一个就算命中）
        wrapper.and(w -> {
            boolean first = true;
            for (String keyword : keywords) {
                if (first) {
                    w.like(RagChunk::getContent, keyword);
                    first = false;
                } else {
                    w.or().like(RagChunk::getContent, keyword);
                }
            }
        });

        // 3. 限制返回数量（多取一些，后面可能去重）
        wrapper.last("LIMIT " + (topK * 3));
        List<RagChunk> results = ragChunkMapper.selectList(wrapper);

        log.info("[RAG-Keyword] 关键字检索命中 " + results.size() + " 条，查询词: " + query + "，提取关键词: " + keywords);

        return results.stream().limit(topK).collect(Collectors.toList());
    }

    /**
     * 从用户问题中提取关键词（支持中英混合）
     * 1. 先按空格和标点切分
     * 2. 按中英文字符边界拆分（"Java基础" → "Java" + "基础"）
     * 3. 按中文停用词再分割
     * 4. 过滤太短和无意义的词
     */
    private List<String> extractKeywords(String query) {
        List<String> result = new ArrayList<>();

        // 中文常见停用词：把没有检索价值的词去掉，同时作为分界点
        String stopWords = "的|了|是|在|有|和|与|或|不|这|那|个|一|些|什么|怎么|哪些|如何|可以|能够|应该|吗|呢|吧|啊|呀|哪|几|多少|内容|包括|介绍|告诉|一下|简单|就行|只要";

        // 第一步：按空格和标点粗切分
        String[] parts = query.split("[\\s，。！？、,.!?]+");

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            // 第二步：按中英文边界拆分
            // "Java基础" → ["Java", "基础"]
            // "2024年Q3财报" → ["2024年", "Q3", "财报"]
            List<String> tokens = splitByLanguageBoundary(part);

            for (String token : tokens) {
                token = token.trim();
                if (token.isEmpty()) continue;

                // 第三步：按停用词再切分
                // "有哪些" → 用"有"切 → ["", "些"] → "些"太短跳过
                String[] subParts = token.split(stopWords);

                for (String sub : subParts) {
                    sub = sub.trim();
                    if (sub.isEmpty()) continue;

                    // 第四步：剥掉首尾的单字（切分残留的无意义字）
                    // 比如 "下标题" → 去掉首字"下" → "标题"
                    //      "基础类" → "类"是单字但粘在后面 → "基础类" 保留（3字有意义）
                    while (sub.length() > 2 && isChineseChar(sub.charAt(0)) && !isChineseChar(sub.charAt(1))) {
                        sub = sub.substring(1);  // 首字是中文单字+后面英文，去掉首字
                    }
                    // 首字是中文且整体>2字：如果首字本身没意义就剥掉
                    while (sub.length() > 2) {
                        char first = sub.charAt(0);
                        char last = sub.charAt(sub.length() - 1);
                        boolean trimmed = false;
                        // 首字是常见无意义单字，剥掉
                        if (isFillerChar(first)) {
                            sub = sub.substring(1);
                            trimmed = true;
                        }
                        // 尾字是常见无意义单字，剥掉
                        if (!trimmed && isFillerChar(last)) {
                            sub = sub.substring(0, sub.length() - 1);
                            trimmed = true;
                        }
                        if (!trimmed) break;  // 两头都没得剥了，停止
                    }

                    if (sub.isEmpty()) continue;

                    // 第五步：长度过滤
                    if (sub.length() >= 2 && sub.length() <= 6) {
                        result.add(sub);
                    } else if (sub.length() > 6) {
                        result.add(sub.substring(0, 4));
                        result.add(sub.substring(sub.length() - 4));
                    }
                }
            }
        }

        // 去重
        return result.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 按中英文字符边界拆分字符串
     * 连续的英文字母/数字归为一组，连续的中文字符归为一组
     * 例如："Java基础有哪些" → ["Java", "基础有哪些"]
     *       "2024年Q3财报" → ["2024年", "Q3", "财报"]
     */
    private List<String> splitByLanguageBoundary(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;

        StringBuilder current = new StringBuilder();
        Boolean currentIsEnglish = null;  // null=未定, true=英文/数字, false=中文

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isEnglishChar = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');

            if (currentIsEnglish == null) {
                // 第一个字符，初始化
                currentIsEnglish = isEnglishChar;
                current.append(c);
            } else if (isEnglishChar == currentIsEnglish) {
                // 同类型，继续拼接
                current.append(c);
            } else {
                // 类型切换，保存当前段，开始新段
                result.add(current.toString());
                current = new StringBuilder();
                current.append(c);
                currentIsEnglish = isEnglishChar;
            }
        }

        // 别忘了最后一段
        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    /** 判断字符是否为中文字符 */
    private boolean isChineseChar(char c) {
        return c >= '\u4e00' && c <= '\u9fff';
    }

    /** 判断字符是否为"填充词"（切分后粘在词首/词尾的无意义单字） */
    private boolean isFillerChar(char c) {
        if (!isChineseChar(c)) return false;
        String fillers = "的下上了里中后前起来出过又还被让给到从向着往";
        return fillers.indexOf(c) >= 0;
    }

    /**
     * 双路混合检索：向量 + 关键字两条路一起跑，结果合并去重
     */
    @Override
    public List<RagChunk> hybridSearch(float[] questionVector, String query, Long kbId, int topK) {
        // 用默认配置调用
        return hybridSearch(questionVector, query, kbId, topK, SIMILARITY_THRESHOLD, 1);
    }

    /**
     * 双路混合检索（带全部配置参数）
     * @param questionVector      问题向量
     * @param query               用户问题原文
     * @param kbId                知识库ID
     * @param topK                检索返回条数
     * @param threshold           向量相似度阈值
     * @param keywordEnabled      是否启用关键字检索（0=仅向量，1=双路）
     */
    @Override
    public List<RagChunk> hybridSearch(float[] questionVector, String query, Long kbId,
                                        int topK, float threshold, int keywordEnabled) {
        // 1. 向量检索（必走）
        List<RagChunk> vectorResults = search(questionVector, kbId, topK, threshold);

        // 2. 关键字检索（可配置开关）
        List<RagChunk> keywordResults = new ArrayList<>();
        if (keywordEnabled == 1) {
            keywordResults = keywordSearch(query, kbId, topK);
        }

        log.info("[RAG-Hybrid] 向量检索: " + vectorResults.size() + " 条，关键字检索: " + keywordResults.size() + " 条（keywordEnabled=" + keywordEnabled + "）");

        // 2. 合并去重：用 LinkedHashMap 保持顺序
        //    向量结果排在前面（相关性更强），关键字结果补充在后面
        Map<Long, RagChunk> mergedMap = new LinkedHashMap<>();
        for (RagChunk chunk : vectorResults) {
            mergedMap.put(chunk.getId(), chunk);          // 向量结果：直接放入
        }
        for (RagChunk chunk : keywordResults) {
            mergedMap.putIfAbsent(chunk.getId(), chunk);  // 关键字结果：已有的不覆盖
        }

        // 3. 转成列表，取前 topK 条
        List<RagChunk> merged = new ArrayList<>(mergedMap.values());
        return merged.stream().limit(topK).collect(Collectors.toList());
    }

    /**
     * 余弦相似度计算
     * cos(A,B) = (A·B) / (|A| × |B|)
     */
    private float cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            return 0f;  // 维度不同无法比较
        }

        float dotProduct = 0f;     // 点积 A·B
        float normA = 0f;          // |A| 模长
        float normB = 0f;          // |B| 模长

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0f || normB == 0f) {
            return 0f;  // 零向量无意义
        }

        return dotProduct / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
    }

    /**
     * 将数据库存的字符串格式向量解析为 float[]
     * 存储格式：逗号分隔的数字字符串，如 "0.02,-0.15,0.89,..."
     */
    private float[] parseVector(String vectorData) {
        if (vectorData == null || vectorData.isEmpty()) {
            return null;
        }
        String[] parts = vectorData.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }

    /**
     * 内部类：带分数的切片（用于排序）
     */
    private static class ScoredChunk {
        RagChunk chunk;
        float score;

        ScoredChunk(RagChunk chunk, float score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
