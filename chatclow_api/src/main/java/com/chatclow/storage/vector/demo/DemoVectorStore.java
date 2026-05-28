package com.chatclow.storage.vector.demo;

import com.chatclow.entity.RagChunk;
import com.chatclow.storage.vector.ChatClowVectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 演示用向量存储实现
 * 用于验证 SPI 热插拔是否工作正常
 */
@Component
public class DemoVectorStore implements ChatClowVectorStore {

    @Override
    public boolean add(RagChunk chunk) {
        System.out.println("[DemoVectorStore] 添加切片 ID=" + chunk.getId() + "（演示模式，未真正存储）");
        return true;
    }

    @Override
    public boolean addBatch(List<RagChunk> chunks) {
        System.out.println("[DemoVectorStore] 批量添加 " + chunks.size() + " 条切片（演示模式）");
        return true;
    }

    @Override
    public List<RagChunk> search(float[] queryVector, Long kbId, int topK, double threshold) {
        System.out.println("[DemoVectorStore] 向量检索（演示模式），kbId=" + kbId + "，topK=" + topK);
        // 返回空列表（演示用，不返回真实数据）
        return new ArrayList<>();
    }

    @Override
    public List<RagChunk> keywordSearch(String query, Long kbId, int topK) {
        System.out.println("[DemoVectorStore] 关键字检索（演示模式），query=" + query);
        return new ArrayList<>();
    }

    @Override
    public boolean deleteByDocument(Long documentId) {
        System.out.println("[DemoVectorStore] 删除文档 ID=" + documentId + "（演示模式）");
        return true;
    }

    @Override
    public boolean test() {
        System.out.println("[DemoVectorStore] 测试连接（演示模式，总是返回 true）");
        return true;
    }
}
