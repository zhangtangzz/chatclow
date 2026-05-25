# ChatClow 项目状态文档

> **最后更新**: 2026-05-24  
> **项目路径**: `C:\Users\张亮亮\Desktop\chatclow\chatclow_api`  
> **运行端口**: `8080`  
> **读者**: 新接入的 AI 助手或开发同学  
> **目标**: 读完本文档，你能立刻接手继续开发，无需追问背景。

---

## 一、项目终极目标

**ChatClow** 是一个**类 Snail-AI 架构的 AI 智能体平台**，终极目标是：

```
用户 → 选择/创建智能体 → 发起对话
    ↓
智能体调用 AI 模型（DeepSeek）→ 结合知识库（RAG）→ 调用工具（Function Calling）
    ↓
返回高质量、有依据、可操作的回答
```

**核心差异化亮点（简历级）**：
1. **RAG 知识库检索增强** — 向量化 + 余弦相似度搜索，让 AI 基于私有知识回答问题
2. **Function Calling 函数调用** — AI 自主决策调用工具（天气/时间/计算器），扩展 AI 能力边界
3. **多轮对话 + 会话持久化** — 完整的对话历史管理，支持上下文连续对话

---

## 二、项目历程（时间线）

### 📅 2026-05-XX 之前：基础搭建阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| 用户模块 | 注册/登录，JWT 认证 | ✅ 完成 |
| Agent 智能体管理 | CRUD 接口，agent 配置管理 | ✅ 完成 |
| AI 模型配置 | 模型 CRUD，apiKey/apiUrl 管理 | ✅ 完成 |
| 多轮对话 | 会话创建，历史消息持久化 | ✅ 完成 |

---

### 📅 2026-05-XX：RAG 知识库开发（第一阶段核心）

**目标**：让 AI 能基于用户上传的文档回答问题，而不是只靠训练数据胡编。

**开发步骤**：
1. **建表** — `chatclow_rag_kb` / `chatclow_rag_document` / `chatclow_rag_chunk`（3 张表）
2. **Entity + Mapper** — RagKnowledgeBase / RagDocument / RagChunk
3. **Service + Controller** — 知识库 CRUD（6 个接口）+ 文档管理（5 个接口）+ 切片查询（3 个接口）
4. **ChunkSplitUtil** — 文本切片工具（纯 Java，500 字/块，尊重句子边界）
5. **EmbeddingService** — 向量化服务（**硅基流动 SiliconFlow + BAAI/bge-m3 模型**）
6. **VectorSearchService** — 余弦相似度搜索（纯 Java 实现，阈值 0.5，Top-K=3）
7. **ChatService 集成 RAG** — Agent 加 `kbEnabled`/`kbId` 字段，chat() 方法插入 RAG 检索分支

**踩坑记录**：
- ❌ DeepSeek 没有 Embedding API → 切换到硅基流动
- ❌ URL 拼接用 replace() 不可靠 → 改 `java.net.URL` 提取域名
- ❌ Agent 的 `model_id` 为 NULL → SQL UPDATE 设置 `model_id=1`

**结果**：RAG 聊天测试通过 ✅，AI 能基于知识库内容回答问题。

---

### 📅 2026-05-23 ~ 2026-05-24：Function Calling 开发（第二阶段核心）

**目标**：让 AI 能调用外部工具（天气/时间/计算器），扩展 AI 能力边界。

**开发步骤**：
1. **建表** — `chatclow_ai_function`（id/agent_id/name/description/parameters/status）
2. **Entity + Mapper** — AiFunction + AiFunctionMapper
3. **Service + Controller** — 5 个 CRUD 接口（`/api/function/add` 等）
4. **FunctionExecutor** — 接口 + 实现，3 个内置工具：
   - `get_weather(city)` — 模拟天气数据
   - `get_current_time()` — 返回服务器当前时间
   - `calculate(expression)` — 四则运算计算器
5. **ChatServiceImpl 改造** — 集成 Function Calling 全流程：
   - `chat()` 查工具列表 → `callAiApi()` 加入 `tools[]` 参数
   - 响应分支：有 `tool_calls` → `handleToolCalls()` → 最终回答
6. **DeepSeek reasoning_content 兼容** — 第二次请求必须回传 `reasoning_content`，否则 400 报错

**踩坑记录**：
- ❌ 方法嵌套在方法内部 → 编译报错 → AI 重写整个文件修复
- ❌ 提前 return 导致死代码 → `tool_calls` 判断永远不执行 → 调整代码顺序
- ❌ `reasoning_content` 未回传 → 400 Bad Request → 提取并传入 `handleToolCalls()`
- ❌ 工具未触发（AI 幻觉回答）→ description 描述太模糊 → 更新描述加"必须调用"

**结果**：Function Calling 联调测试通过 ✅，4 个场景全部正常。

---

## 三、当前进度（截至 2026-05-24）

### ✅ 已完成的功能

| 模块 | 功能 | 接口路径 | 状态 |
|------|------|---------|------|
| **用户** | 注册/登录 | `/api/user/register`, `/api/user/login` | ✅ |
| **JWT** | Token 认证 | 全局拦截器 | ✅ |
| **Agent** | 智能体 CRUD | `/api/agent/**` | ✅ |
| **模型** | AI 模型配置 | `/api/model/**` | ✅ |
| **对话** | 多轮对话 | `POST /api/chat/send` | ✅ |
| **RAG** | 知识库管理 | `/api/rag/**` | ✅ |
| **RAG** | 文档上传/切片 | `POST /api/document/process/{id}` | ✅ |
| **RAG** | 向量搜索 | `VectorSearchService.search()` | ✅ |
| **FC** | 工具管理 | `/api/function/**` | ✅ |
| **FC** | 工具执行 | `FunctionExecutorImpl` | ✅ |
| **FC** | 聊天集成 | `ChatServiceImpl.callAiApi()` | ✅ |

### ⚠️ 已知问题 / 待优化

| 问题 | 影响 | 优先级 |
|------|------|--------|
| **SSE 流式输出未实现** | 前端需等待完整回复，体验差 | 🔴 高 |
| **文件上传只支持手动触发** | 用户上传文档后需手动调用 process 接口 | 🟡 中 |
| **工具结果无缓存** | 相同参数重复调用，浪费资源 | 🟢 低 |
| **没有前端页面** | 只能用 Apipost 测试，无法演示 | 🟡 中 |

### 📋 待开发的功能（下一步）

| 功能 | 描述 | 难度 | 简历含金量 |
|------|------|------|-----------|
| **SSE 流式输出** | 实现 `text/event-stream`，AI 回复逐字推送 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **文件上传优化** | 支持 MultipartFile 自动触发切片向量化 | ⭐⭐ | ⭐⭐⭐ |
| **多 Agent 协作** | 一个 Agent 调度另一个 Agent | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **前端页面** | Vue/React 管理后台 | ⭐⭐⭐ | ⭐⭐⭐ |

---

## 四、核心架构与关键决策

### 4.1 技术栈

```
后端: Spring Boot 2.6.13 + Java 17 + MyBatis-Plus 3.5.3
数据库: MySQL 8.0
认证: JWT (jjwt 0.9.1)
AI 聊天: DeepSeek API (Chat Completions)
AI 向量化: 硅基流动 SiliconFlow + BAAI/bge-m3
向量搜索: 纯 Java 余弦相似度实现
```

### 4.2 数据库表关系（核心 8 张表）

```
ai_agent (智能体)
  ├── id (PK)
  ├── model_id (→ ai_model.id)
  ├── kb_id (→ rag_kb.id, 可为空)
  └── kb_enabled (是否启用 RAG)

ai_model (AI 模型配置)
  └── id (PK)

chatclow_rag_kb (知识库)
  └── id (PK)

chatclow_rag_document (文档)
  └── kb_id (→ chatclow_rag_kb.id)

chatclow_rag_chunk (文档切片)
  └── doc_id (→ chatclow_rag_document.id)

chatclow_ai_function (工具函数)
  └── agent_id (→ ai_agent.id)

agent_conversation (对话会话)
  └── user_id (→ user.id)

agent_conversation_record (对话记录)
  └── conversation_id (→ agent_conversation.id)
```

### 4.3 关键设计决策

| 决策 | 原因 | 影响 |
|------|------|------|
| **DeepSeek 只做聊天，不做向量化** | DeepSeek 无 Embedding API | Embedding 用硅基流动 |
| **向量搜索用纯 Java 实现** | 避免引入 Elasticsearch/ Milvus 复杂度 | 性能够用，部署简单 |
| **Function 定义放数据库，实现放 Java** | 定义给 AI 看（description），实现给系统跑 | 解耦，AI 换模型不影响代码 |
| **reasoning_content 必须回传** | DeepSeek 思维链机制 | 第二次请求必须带此字段 |

---

## 五、如何运行和测试

### 5.1 启动项目

```bash
cd C:\Users\张亮亮\Desktop\chatclow\chatclow_api
mvn spring-boot:run
```

### 5.2 测试工具（Apipost）

#### ① 添加工具（给 agentId=1 绑定工具）

```http
POST http://localhost:8080/api/function/add
Content-Type: application/json

{
  "agentId": 1,
  "name": "get_weather",
  "description": "获取指定城市的天气信息。参数 city 为城市名称，如北京、上海、广州、深圳",
  "parameters": "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\",\"description\":\"城市名称，如北京、上海\"}},\"required\":[\"city\"]}",
  "status": 1
}
```

#### ② 触发 Function Calling（聊天）

```http
POST http://localhost:8080/api/chat/send
Content-Type: application/json

{
  "agentId": 1,
  "userId": 1,
  "message": "北京今天天气怎么样？"
}
```

**预期行为**：
- 后端日志打印：`[Function Calling] 执行工具: get_weather, 参数: {"city":"北京"}`
- 响应内容是 AI 基于天气数据说的自然语言

#### ③ 测试 RAG（需先有知识库和数据）

```http
POST http://localhost:8080/api/chat/send
Content-Type: application/json

{
  "agentId": 1,
  "userId": 1,
  "message": "知识库里有什么内容？"
}
```

---

## 六、关键文件速查

| 文件路径 | 作用 | 何时需要修改 |
|---------|------|------------|
| `src/main/java/com/chatclow/service/impl/ChatServiceImpl.java` | 核心聊天逻辑，RAG + FC 集成点 | 修改聊天流程时 |
| `src/main/java/com/chatclow/service/impl/FunctionExecutorImpl.java` | 工具执行逻辑（3 个内置工具） | 新增工具时 |
| `src/main/java/com/chatclow/service/impl/VectorSearchServiceImpl.java` | 向量搜索（余弦相似度） | 优化搜索算法时 |
| `src/main/java/com/chatclow/util/ChunkSplitUtil.java` | 文本切片工具 | 调整切片策略时 |
| `src/main/resources/application.yml` | 配置文件（DB/AI API Key） | 更换 API Key 或数据库时 |
| `src/main/java/com/chatclow/entity/AiAgent.java` | Agent 实体（含 kbEnabled/kbId） | 给 Agent 加字段时 |

---

## 七、给新 AI 助手的快速上手指南

**如果你是完全新的 AI 助手，读完这里就可以开始干活了：**

1. **项目在哪？**  
   `C:\Users\张亮亮\Desktop\chatclow\chatclow_api`

2. **现在做到哪了？**  
   RAG + Function Calling 两个核心功能都已开发完成并通过测试。下一步建议做 **SSE 流式输出**（简历含金量最高）。

3. **用户是谁？**  
   张亮亮，Java 开发学生，把 AI 当编程导师。偏好分步骤教学，每步测试确认后再继续。重视架构理解，不满足于复制代码。

4. **沟通风格？**  
   中文交流。用户会直接说"你告诉我如何做，让我来做"。如果代码太复杂，用户会说"太复杂了你给我改吧"。

5. **关键技术坑？**  
   - DeepSeek 第二次请求必须回传 `reasoning_content`
   - HTTP 方法要严格按照接口定义（POST/PUT/GET/DELETE）
   - 向量化用硅基流动，不是 DeepSeek

6. **如何验证我的改动没问题？**  
   改完代码后运行 `mvn compile -q`，零错误才算通过。然后用 Apipost 按第五章的接口测试。

---

## 八、项目文档资源

| 文档 | 路径 | 用途 |
|------|------|------|
| **项目开发文档（HTML）** | `C:\Users\张亮亮\Desktop\chatclow\ChatClow项目开发文档.html` | 完整技术文档，含架构图/ER 图/API 清单 |
| **本文件** | `C:\Users\张亮亮\Desktop\chatclow\PROJECT_STATUS.md` | 项目状态交接文档（你正在读的这个） |
| **工作日志** | `C:\Users\张亮亮\WorkBuddy\2026-05-20-22-38-37\.workbuddy\memory\2026-05-24.md` | 每日开发记录 |

---

## 九、联系方式与后续支持

**如果用户问"现在该做什么"，你应该建议：**

1. **优先级最高**：实现 SSE 流式输出（改动小，简历亮点大）
2. **优先级次之**：优化文件上传流程（支持 MultipartFile 自动触发）
3. **优先级最低**：写前端页面（Vue/React）

**如果用户问"这个功能怎么做"，你应该：**

1. 先用 `@skill:code-mentor` 调取教学计划
2. 分步骤讲解，每步让用户自己写代码
3. 如果用户说"太复杂"，你直接改代码

---

**文档结束。祝你接手顺利！ 🚀**

---

*本文档由 WorkBuddy AI 助手于 2026-05-24 生成，作为 ChatClow 项目交接文档。任何新的 AI 助手或开发人员请先读此文档再开始工作。*
