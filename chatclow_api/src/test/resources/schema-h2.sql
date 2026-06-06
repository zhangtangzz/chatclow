-- H2 测试表结构（MySQL 兼容模式）
-- 每次测试启动时自动执行

CREATE TABLE IF NOT EXISTS chatclow_ai_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(200) NOT NULL,
    email VARCHAR(100),
    role INT DEFAULT 1,
    totals INT DEFAULT 100,
    created_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chatclow_ai_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    model_code VARCHAR(200) NOT NULL,
    api_url VARCHAR(500) NOT NULL,
    api_key VARCHAR(500) NOT NULL,
    status INT DEFAULT 1,
    temperature DOUBLE DEFAULT 0.7,
    max_tokens INT DEFAULT 4096,
    top_p DOUBLE DEFAULT 1.0,
    created_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chatclow_ai_agent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    avatar VARCHAR(255),
    system_prompt TEXT NOT NULL,
    model_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status INT DEFAULT 1,
    kb_enabled INT DEFAULT 0,
    kb_id BIGINT,
    rag_top_k INT DEFAULT 6,
    rag_final_k INT DEFAULT 3,
    rag_similarity_threshold FLOAT DEFAULT 0.5,
    rag_keyword_enabled INT DEFAULT 1,
    rag_rerank_enabled INT DEFAULT 1,
    created_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chatclow_ai_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    total_tokens INT DEFAULT 0,
    created_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chatclow_ai_conversation_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    created_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    response_time BIGINT DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS chatclow_ai_function (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    parameters TEXT,
    agent_id BIGINT,
    created_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chatclow_rag_kb (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    icon VARCHAR(255),
    store_instance_id BIGINT DEFAULT 1,
    vector_store_type VARCHAR(50),
    embedding_model_id BIGINT,
    rag_enhancement VARCHAR(500),
    status INT DEFAULT 1,
    user_id BIGINT,
    created_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chatclow_rag_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    name VARCHAR(255),
    file_type VARCHAR(20),
    content TEXT,
    status INT DEFAULT 0,
    error_msg VARCHAR(500),
    chunk_count INT DEFAULT 0,
    content_hash VARCHAR(64),
    created_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chatclow_store_instance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    store_type VARCHAR(50),
    host VARCHAR(255),
    port INT,
    database_name VARCHAR(100),
    username VARCHAR(100),
    password VARCHAR(200),
    status INT DEFAULT 1,
    created_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
