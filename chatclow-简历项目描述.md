# ChatClow 简历项目描述

> 生成日期：2026-05-27 | 基于项目源码分析

---

## 简历项目经历描述

**【ChatClow — AI 智能体对话平台】**

- **背景**：从零搭建一个基于 DeepSeek 大模型的 AI 智能体对话平台，支持 SSE 流式输出、Function Calling 工具调用、RAG 知识库检索增强等多模态交互能力。
- **技术**：Spring Boot 2.6 + MyBatis-Plus + MySQL 8.0 + OkHttp + JWT（后端），Vue 3 + Vite + Element Plus + Pinia（前端），集成 SiliconFlow Embedding API（BAAI/bge-m3）与 Re-rank API（BAAI/bge-reranker-v2-m3）。
- **贡献**：
  - 设计并实现**责任链模式**的对话处理管线（7 个 Step），将智能体加载、RAG 检索、上下文组装、模型调用、Function Calling、回复清洗、记录保存等环节解耦为独立可编排的步骤。
  - 实现**企业级 RAG 知识库模块**：包含 SHA-256 文档去重、双路混合检索（向量余弦相似度 + 中文关键字提取）、Re-rank 精排，以及 5 个可配置参数（top_k、final_k、similarity_threshold、keyword_enabled、rerank_enabled）。
  - 自研**中文关键字提取算法**：通过中英语言边界拆分 → 停用词分割 → 填充词清洗三步处理，解决中文无空格分词难题。
  - 实现**SSE 流式输出 + Function Calling 多轮工具调用**：OkHttp 逐行读取 SSE 数据流，支持流式场景下 tool_calls 碎片拼接与二次请求注入。
- **成果**：完成完整的前后端全栈开发，支持多智能体管理、多轮对话、工具调用、私有知识库问答等核心能力，RAG 模块达到企业级可配置水平。

---

## 个人技能描述

**熟练掌握：**
- Java 17、Spring Boot 2.x、MyBatis-Plus、MySQL，能独立完成后端项目的架构设计与功能开发
- RESTful API 设计，统一响应封装与全局异常处理
- JWT 认证鉴权与拦截器模式的权限控制

**熟悉：**
- SSE（Server-Sent Events）流式推送原理及 OkHttp 流式读取实现
- RAG（检索增强生成）完整链路：文档解析（PDFBox / POI）、文本切片、向量化（SiliconFlow BAAI/bge-m3）、余弦相似度检索、Re-rank 重排序
- Function Calling 工具调用机制：工具 Schema 构造、多轮工具调用、流式 tool_calls 拼接
- Vue 3 + Element Plus 前端开发，SSE 流式数据消费（fetch + ReadableStream）
- 责任链（Chain of Responsibility）设计模式在实际业务中的落地

**了解：**
- 中文 NLP 基础：关键词提取、停用词过滤、中英文字符边界识别
- DeepSeek API 与 OpenAI 兼容格式的适配
- Apache PDFBox / POI 文档解析
- 多线程编程（自定义 ThreadPoolTaskExecutor 处理 SSE 异步推送）

Chatclow主要核心功能及实现：

一、企业级RAG知识库实现
1、独立设计并实现RAG知识库全链路模块：PDF/Word解析（PDFBox+POI）→ 文本智能切片（句子边界感知、500字符窗口、回溯搜索6种分隔符），结合硅基流动BAAI/bge-m3 Embedding大模型的向量化存储（1024维）→ 存储（逗号分隔字符串）。
2、研究并实现 RAG 知识库模块的企业级特性升级四大模块：
文档去重策略：基于内容 SHA-256 哈希，上传时计算全文哈希并与数据库 idx_content_hash 索引比对，哈希冲突概率 < 2^-256；重复文档静默跳过，避免向量库冗余存储
双路检索：研究并实现向量 + 关键字的双路混合检索，通过 LinkedHashMap 合并去重提升召回率；自行设计中文关键词提取算法（语言边界拆分 → 停用词过滤 → 填充词清洗）。
Re-rank重排序模型集成：集成 硅基流动BAAI/bge-reranker-v2-m3 重排序模型，对候选文档进行精排（top_n 截断），失败时自动降级保证可用性。
搜索增强引擎配置：将检索参数（如：Top-K、相似度阈值、关键字/Re-rank 开关）下沉至数据库配置层，实现每个 Agent 独立调优，无需修改代码。
二、企业级设计模式和机构：
独立设计对话处理责任链架构，讲AI对话核心流程从700+行冗余代码重构为7个独立的Step组件，引入shouldSkip（）条件跳过机制实现流式/非流式双路径自由选择。
实现 Function Calling 工具调用机制，AI 可自主决定调用工具（天气/计算器/时间）。
支持流式场景下的工具调用片段累加和二次请求，工具结果自动注入对话上下文。往后新增步骤仅需实现接口即可无入侵拓展。并且统一了OkHttpClient连接池管理，消除多服务独立创建HTTP客户端的资源浪费。
三、企业级存储模式
构建实现了SPI可插拔存储，支持 MySQL + MongoDB + Milvus + Elasticsearch + PostgreSQL 多后端切换。
四、基础功能
独立实现 JWT 认证体系（jjwt + 拦截器 + 白名单机制），支持 Token 生成/解析/验证及管理员权限分离。
· 开发 Vue3 前端界面（Composition API + Pinia 状态管理 + Element Plus），实现智能体管理、知识库上传、Markdown 渲染及流式事件分发的完整前端交互