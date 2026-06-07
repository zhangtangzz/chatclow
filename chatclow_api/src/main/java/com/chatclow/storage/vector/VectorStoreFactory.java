package com.chatclow.storage.vector;

import com.chatclow.entity.RagKnowledgeBase;
import com.chatclow.entity.StoreInstance;
import com.chatclow.mapper.RagKnowledgeBaseMapper;
import com.chatclow.mapper.StoreInstanceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量存储工厂
 * 1. 注册所有 ChatClowVectorStore 实现
 * 2. 根据配置动态获取向量存储实例
 */
@Component
public class VectorStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreFactory.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private StoreInstanceMapper storeInstanceMapper;

    @Autowired
    private RagKnowledgeBaseMapper ragKnowledgeBaseMapper;

    /**
     * 注册表：VectorStoreType → ChatClowVectorStore 实现类
     * 通过 Spring 自动注入所有实现类
     */
    private final Map<String, ChatClowVectorStore> registry = new HashMap<>();

    /**
     * 实例缓存：StoreInstance.id → ChatClowVectorStore 实例
     * 避免重复创建连接（尤其是 Redis/Milvus 等需要网络连接的后端）
     */
    private final Map<Long, ChatClowVectorStore> instanceCache = new HashMap<>();

    /**
     * 初始化：把所有 ChatClowVectorStore 实现类注册进注册表
     */
    @PostConstruct
    public void init() {
        // 从 Spring 容器中取出所有 ChatClowVectorStore 实现类
        Map<String, ChatClowVectorStore> beans = applicationContext.getBeansOfType(ChatClowVectorStore.class);

        for (ChatClowVectorStore store : beans.values()) {
            // 用 instanceof 判断类型（更可靠）
            if (store instanceof com.chatclow.storage.vector.mysql.MySQLVectorStore) {
                registry.put("MYSQL", store);
            } else if (store instanceof com.chatclow.storage.vector.mongodb.MongoDBVectorStore) {
                registry.put("MONGODB", store);

            } else if (store instanceof com.chatclow.storage.vector.pgvector.PgVectorStore) {
                registry.put("PGVECTOR", store);
            } else {
                log.info("[VectorStoreFactory] 警告：未识别的向量存储实现: " + store.getClass().getSimpleName());
            }
        }

        log.info("[VectorStoreFactory] 已注册向量存储实现: " + registry.keySet());
    }


    /**
     * 根据知识库获取向量存储实例
     * @param kbId 知识库ID（会从 RagKnowledgeBase 中查出 storeInstanceId）
     * @return ChatClowVectorStore 实例
     */
    public ChatClowVectorStore getVectorStore(Long kbId) {
        // 1. 查知识库配置
        RagKnowledgeBase kb = ragKnowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new RuntimeException("知识库不存在，ID: " + kbId);
        }

        // 2. 获取绑定的存储实例 ID
        Long storeInstanceId = kb.getStoreInstanceId();
        if (storeInstanceId == null) {
            // 兜底：如果没有配置，返回默认的 MySQL 实现
            log.info("[VectorStoreFactory] 知识库 " + kbId + " 未配置存储实例，使用默认 MySQL");
            return registry.get("MYSQL");
        }

        // 3. 根据存储实例 ID 获取向量存储（带缓存）
        return getVectorStoreByInstanceId(storeInstanceId);
    }

    /**
     * 根据存储实例 ID 获取向量存储实例（带缓存）
     * @param storeInstanceId StoreInstance.id
     * @return ChatClowVectorStore 实例
     */
    public ChatClowVectorStore getVectorStoreByInstanceId(Long storeInstanceId) {
        // 1. 先查缓存
        if (instanceCache.containsKey(storeInstanceId)) {
            return instanceCache.get(storeInstanceId);
        }

        // 2. 缓存未命中，查数据库
        StoreInstance instance = storeInstanceMapper.selectById(storeInstanceId);
        if (instance == null) {
            throw new RuntimeException("存储实例不存在，ID: " + storeInstanceId);
        }

        // 3. 根据类型从注册表获取实现
        ChatClowVectorStore store = registry.get(instance.getType());
        if (store == null) {
            throw new RuntimeException("未找到类型为 " + instance.getType() + " 的向量存储实现");
        }

        // 4. 存入缓存
        instanceCache.put(storeInstanceId, store);

        return store;
    }

    /**
     * 获取默认向量存储实例（不依赖 kbId，搜全部时使用）
     * 优先取第一个启用的知识库的存储后端，兜底 MySQL
     */
    public ChatClowVectorStore getDefaultVectorStore() {
        // 优先取第一个启用的知识库的存储类型
        List<RagKnowledgeBase> kbs = ragKnowledgeBaseMapper.selectList(null);
        for (RagKnowledgeBase kb : kbs) {
            if (kb.getStoreInstanceId() != null) {
                try {
                    return getVectorStoreByInstanceId(kb.getStoreInstanceId());
                } catch (Exception ignored) {
                    // 该实例不可用，尝试下一个
                }
            }
        }
        // 兜底：使用 MySQL 实现
        ChatClowVectorStore mysql = registry.get("MYSQL");
        if (mysql != null) return mysql;
        throw new RuntimeException("没有可用的向量存储实例");
    }

    /**
     * 注册新的向量存储实现（支持运行时动态注册）
     * @param type  存储类型（MYSQL / REDIS / MILVUS）
     * @param store 实现实例
     */
    public void register(String type, ChatClowVectorStore store) {
        registry.put(type, store);
        log.info("[VectorStoreFactory] 动态注册向量存储: " + type);
    }

    /**
     * 测试某个存储实例是否可用
     * @param storeInstanceId 存储实例ID
     * @return true=可用，false=不可用
     */
    public boolean test(Long storeInstanceId) {
        try {
            ChatClowVectorStore store = getVectorStoreByInstanceId(storeInstanceId);
            return store.test();
        } catch (Exception e) {
            log.warn("[VectorStoreFactory] 测试失败: {}", e.getMessage());
            return false;
        }
    }
}
