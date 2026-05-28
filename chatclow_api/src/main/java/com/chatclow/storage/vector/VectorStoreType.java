package com.chatclow.storage.vector;

/**
 * 向量存储类型枚举
 */
public enum VectorStoreType {

    MYSQL("MYSQL", "MySQL 余弦相似度存储"),
    REDIS("REDIS", "Redis Stack 向量索引存储"),
    MILVUS("MILVUS", "Milvus 向量数据库");

    private final String code;
    private final String desc;

    VectorStoreType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static VectorStoreType fromCode(String code) {
        for (VectorStoreType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        return MYSQL; // 默认MySQL
    }
}
