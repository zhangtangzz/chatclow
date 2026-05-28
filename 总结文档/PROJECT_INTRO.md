# ChatClow 项目简介

> 基于 DeepSeek 的 RAG 聊天应用 —— 支持 SSE 流式输出、Function Calling 工具调用、知识库检索增强

## 一、项目概述

ChatClow 是一个 AI 智能体对话平台，核心能力包括：

- **多智能体管理**：创建不同角色/提示词的 AI 助手，绑定不同模型和知识库
- **SSE 流式输出**：逐字推送 AI 回复，类 ChatGPT 打字效果
- **Function Calling**：AI 自主决定调用工具（天气查询、计算器、时间查询），工具结果自动注入下一轮对话
- **RAG 知识库**：上传文档 → 自动切片 → 向量化 → 相似度检索，让 AI 基于私有知识回答
- **JWT 认证**：登录鉴权 + 管理员权限分离

## 二、技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 2.6.13 |
| 语言 | Java | 17 |
| ORM | MyBatis-Plus | 3.5.3 |
| 数据库 | MySQL | 8.0 |
| HTTP 客户端 | OkHttp | 4.12.0 |
| JWT | jjwt | 0.9.1 |
| PDF 解析 | Apache PDFBox | 2.0.30 |
| Word 解析 | Apache POI | 5.2.5 |
| Embedding | SiliconFlow BAAI/bge-m3 | - |
| 前端框架 | Vue 3 + Vite | - |
| UI 组件库 | Element Plus | - |
| 状态管理 | Pinia | - |
| 路由 | Vue Router | - |
| Markdown | MarkdownIt | - |

## 三、项目结构

### 后端（chatclow_api/）

```
chatclow_api/
├── pom.xml                          # Maven 依赖
├── src/main/java/com/chatclow/
│   ├── ChatclowApplication.java     # 启动类
│   ├── common/
│   │   └── R.java                   # 统一响应封装 {code, msg, data}
│   ├── config/
│   │   ├── AsyncConfig.java         # SSE 线程池配置
│   │   └── WebMvcConfig.java        # JWT 拦截器注册 + 跨域配置
│   ├── controller/
│   │   ├── ChatController.java      # 对话接口（同步 + 流式）
│   │   ├── LoginController.java     # 登录接口
│   │   ├── AiAgentController.java   # 智能体 CRUD
│   │   ├── AiFunctionController.java# 工具函数管理
│   │   ├── AiModelController.java   # 模型管理
│   │   ├── RagKnowledgeBaseController.java  # 知识库 CRUD
│   │   ├── RagDocumentController.java       # 文档上传 + 处理
│   │   ├── RagChunkController.java          # 切片查询
│   │   ├── UserController.java      # 用户管理
│   │   ├── AgentConversationController.java # 会话管理
│   │   └── AgentConversationRecordController.java # 对话记录
│   ├── dto/
│   │   ├── ChatRequest.java         # 对话请求 DTO
│   │   ├── ChatResponse.java        # 对话响应 DTO
│   │   └── SseEvent.java            # SSE 结构化事件
│   ├── entity/
│   │   ├── User.java                # 用户实体
│   │   ├── AiAgent.java             # 智能体实体
│   │   ├── AiModel.java             # AI 模型实体
│   │   ├── AiFunction.java          # 工具函数实体
│   │   ├── RagKnowledgeBase.java    # 知识库实体
│   │   ├── RagDocument.java         # 文档实体
│   │   ├── RagChunk.java            # 文档切片实体
│   │   ├── AgentConversation.java   # 会话实体
│   │   └── AgentConversationRecord.java # 对话记录实体
│   ├── interceptor/
│   │   └── JwtInterceptor.java      # JWT 认证拦截器
│   ├── mapper/                      # MyBatis-Plus Mapper 接口
│   ├── service/                     # Service 接口
│   │   └── impl/                    # Service 实现
│   │       ├── ChatServiceImpl.java         # ⭐ 核心对话服务
│   │       ├── FunctionExecutorImpl.java    # ⭐ 工具执行器
│   │       ├── EmbeddingServiceImpl.java    # ⭐ 向量化服务
│   │       ├── VectorSearchServiceImpl.java # ⭐ 向量搜索服务
│   │       ├── RagDocumentServiceImpl.java  # 文档处理（上传+切片+向量化）
│   │       ├── RagChunkServiceImpl.java     # 切片管理
│   │       ├── RagKnowledgeBaseServiceImpl.java # 知识库管理
│   │       ├── AiAgentServiceImpl.java      # 智能体管理
│   │       └── UserServiceImpl.java         # 用户管理
│   └── util/
│       ├── JwtUtil.java             # JWT 工具类
│       └── ChunkSplitUtil.java      # ⭐ 文本切片工具
├── src/main/resources/
│   └── application.yml              # 应用配置
└── uploads/                         # 文件上传目录
```

### 前端（chatclow-web/）

```
chatclow-web/
├── index.html
├── package.json
├── vite.config.js                   # Vite 配置（代理 /api → localhost:8080）
├── public/
└── src/
    ├── main.js                      # 入口（Pinia + Router + ElementPlus）
    ├── App.vue                      # 根组件
    ├── router/
    │   └── index.js                 # 路由守卫（未登录跳转 /login）
    ├── stores/
    │   └── user.js                  # Pinia 用户状态（token/userId）
    ├── api/
    │   ├── request.js               # Axios 实例（baseURL + JWT Header）
    │   ├── auth.js                  # 登录 API
    │   ├── chat.js                  # ⭐ SSE 流式聊天 API
    │   ├── agent.js                 # 智能体 CRUD API
    │   ├── model.js                 # 模型列表 API
    │   └── knowledge.js             # 知识库 + 文档上传 API
    └── views/
        ├── Login.vue                # 登录页
        ├── Chat.vue                 # ⭐ 聊天主页
        └── Knowledge.vue            # 知识库管理页
```

## 四、核心功能模块详解

### 4.1 AI 对话（同步 + 流式）

**入口**：`ChatController.java` → `ChatServiceImpl.java`

| 接口 | 方式 | 说明 |
|------|------|------|
| `POST /api/chat/send` | 同步 | 一次性返回完整回复 |
| `POST /api/chat/send-stream` | SSE 流式 | 逐字推送，类 ChatGPT 体验 |

**核心流程**：

```
用户消息 → 查智能体配置 → 查模型配置 → 获取/创建会话
→ 保存用户消息 → 构建历史上下文 → RAG检索(可选)
→ 查工具列表(可选) → 调用 AI API → 返回/推送回复
→ 保存AI回复
```

**流式对话关键代码**（`chatStream` 方法）：

```java
// 1. 先推送会话ID给前端
emitter.send(SseEmitter.event().data(
    objectMapper.writeValueAsString(SseEvent.convId(conversation.getId().toString()))
));

// 2. 流式读取 AI 响应
streamWithOkHttp(model, jsonBody, emitter, fullReply, functions, requestBody);

// 3. 完成后推送 done 事件
emitter.send(SseEmitter.event().data(
    objectMapper.writeValueAsString(SseEvent.done())
));
emitter.complete();
```

**OkHttp 流式读取**（`streamWithOkHttp` 方法）：

```java
private final OkHttpClient httpClient = new OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(120, TimeUnit.SECONDS)
    .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
    .build();

// 逐行读取 SSE 数据流
try (okio.BufferedSource source = response.body().source()) {
    while (!source.exhausted()) {
        String line = source.readUtf8Line();
        if (!line.startsWith("data: ")) continue;
        String data = line.substring(6).trim();
        if ("[DONE]".equals(data)) break;
        // 解析 delta.content → 推送给前端
    }
}
```

### 4.2 SSE 结构化事件协议

**文件**：`SseEvent.java`

前端收到 SSE 数据后 `JSON.parse`，按 `type` 分发处理：

| type | data 类型 | 说明 |
|------|----------|------|
| `conv_id` | String | 会话ID，首次推送 |
| `content` | String | AI 文字片段，逐字累加 |
| `tool_call` | {name, args} | 正在调用工具 |
| `tool_result` | {name, result} | 工具执行结果 |
| `done` | "" | 流结束 |
| `error` | String | 出错信息 |

**SseEvent 静态工厂方法**：

```java
SseEvent.convId("123")        → {"type":"conv_id","data":"123"}
SseEvent.content("你好")       → {"type":"content","data":"你好"}
SseEvent.toolCall("get_weather", "{...}") → {"type":"tool_call","data":{"name":"get_weather","args":"{...}"}}
SseEvent.toolResult("get_weather", "{...}") → {"type":"tool_result","data":{"name":"get_weather","result":"{...}"}}
SseEvent.done()                → {"type":"done","data":""}
SseEvent.error("出错")         → {"type":"error","data":"出错"}
```

### 4.3 Function Calling（工具调用）

**核心文件**：`ChatServiceImpl.java` + `FunctionExecutorImpl.java`

**流程**：

```
用户消息 → AI 判断需要调用工具 → 返回 tool_calls
→ 执行工具 → 结果作为 role="tool" 消息
→ 第二次请求 AI → AI 基于工具结果生成最终回答
```

**同步模式**（`handleToolCalls`）：

1. AI 返回 `tool_calls` → 解析工具名和参数
2. 调用 `functionExecutor.execute(toolName, args)` 执行工具
3. 构建 `role="tool"` 消息，加入对话历史
4. **第二次请求 AI**（不带 tools，避免死循环）
5. 返回 AI 最终回答

**流式模式**（`handleStreamToolCalls`）：

1. 流式读取中检测到 `tool_calls` 碎片 → 用 `StreamToolCall` 累加器拼接
2. 拼接完成后 → 推送 `SseEvent.toolCall` 事件给前端
3. 执行工具 → 推送 `SseEvent.toolResult` 事件给前端
4. 构建第二次请求 → `streamSecondResponse()` 逐字推送最终答案

**内置 3 个工具**：

| 工具名 | 功能 | 参数 |
|--------|------|------|
| `get_weather` | 查天气（模拟数据） | city: 城市名 |
| `get_current_time` | 获取当前时间 | 无 |
| `calculate` | 数学计算器 | expression: 算术表达式 |

**工具定义格式**（`buildTools` 方法将 `AiFunction` 实体转为 DeepSeek API 要求的格式）：

```json
{
  "type": "function",
  "function": {
    "name": "get_weather",
    "description": "查询指定城市的天气",
    "parameters": {
      "type": "object",
      "properties": {
        "city": { "type": "string", "description": "城市名称" }
      },
      "required": ["city"]
    }
  }
}
```

### 4.4 RAG 知识库

**核心文件**：`EmbeddingServiceImpl.java` + `VectorSearchServiceImpl.java` + `ChunkSplitUtil.java`

**完整流程**：

```
上传文档 → 解析文本 → 切片(500字/片) → 向量化(SiliconFlow BAAI/bge-m3)
→ 存储(MySQL 向量字段) → 检索时：问题向量化 → 余弦相似度 → TopK 匹配
```

#### 4.4.1 文本切片（ChunkSplitUtil）

```java
// 默认 500 字/片，优先在句子边界切分
public static List<String> split(String text, int chunkSize) {
    // 在 chunkSize 附近找句子边界（。！？\n；）
    // 最多往前找 150 个字符
    // 找不到就硬切
}
```

#### 4.4.2 向量化（EmbeddingServiceImpl）

- 调用硅基流动 SiliconFlow API，使用 BAAI/bge-m3 模型
- OpenAI 兼容格式，分批调用（每批最多 50 条）
- 输入文本 → 输出 float[] 向量（1024 维）

#### 4.4.3 向量搜索（VectorSearchServiceImpl）

- 从 MySQL 查出知识库下所有有向量的切片
- 逐条计算余弦相似度：`cos(A,B) = (A·B) / (|A| × |B|)`
- 阈值过滤（≥ 0.5）→ 按 TopK 排序返回
- 向量存储格式：逗号分隔字符串 `"0.02,-0.15,0.89,..."`

#### 4.4.4 RAG 注入对话

```java
// ChatServiceImpl.java — buildMessages() 中
if (ragContext != null && !ragContext.isEmpty()) {
    finalSystemPrompt = systemPrompt
        + "\n\n【参考知识库内容】\n"
        + ragContext
        + "\n\n请基于以上知识库内容回答用户问题。如果知识库中没有相关信息，请如实告知。";
}
```

### 4.5 JWT 认证

**核心文件**：`JwtUtil.java` + `JwtInterceptor.java` + `WebMvcConfig.java`

| 组件 | 职责 |
|------|------|
| `JwtUtil` | 生成/解析/验证 Token（HS256 签名，24h 过期） |
| `JwtInterceptor` | 拦截请求 → 从 Header 取 Token → 验证 → 放行/拒绝 |
| `WebMvcConfig` | 注册拦截器，白名单放行 `/api/auth/login` |

**认证流程**：

```
1. 用户登录 → 校验密码 → 生成 JWT Token → 返回 {token, userId, username}
2. 后续请求 Header 携带 Authorization: Bearer <token>
3. 拦截器验证 Token → 提取 userId/username → 存入 request attribute
4. 业务代码可通过 request.getAttribute("userId") 获取当前用户
```

**白名单**：`/api/auth/login`、`/api/user/register`、静态资源

### 4.6 管理员权限

**文件**：`AiAgentController.java`

```java
@GetMapping("/list/{userId}")
public R<List<AiAgent>> listByUserId(@PathVariable Long userId) {
    User user = userMapper.selectById(userId);
    if (user != null && user.getRole() != null && user.getRole() == 2) {
        // 管理员：返回所有启用的智能体
        list = aiAgentService.listAllEnabled();
    } else {
        // 普通用户：只返回自己的
        list = aiAgentService.listByUserId(userId);
    }
}
```

| 角色 | role 值 | 智能体列表范围 |
|------|---------|---------------|
| 普通用户 | 1 | 仅自己创建的 |
| 管理员 | 2 | 所有启用的 |

### 4.7 SSE 线程池

**文件**：`AsyncConfig.java`

```java
@Bean("sseExecutor")
public Executor sseExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);       // 常驻 5 个线程
    executor.setMaxPoolSize(20);       // 最多 20 个线程
    executor.setQueueCapacity(100);    // 排队 100 个任务
    executor.setThreadNamePrefix("chatclow-sse-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    // 排队也满了？由调用者线程自己跑，不丢弃任务
    return executor;
}
```

**使用方式**：

```java
@PostMapping("/send-stream")
public SseEmitter sendStream(@RequestBody ChatRequest request) {
    SseEmitter emitter = new SseEmitter(120000L); // 120秒超时
    sseExecutor.execute(() -> {
        chatService.chatStream(..., emitter);
    });
    return emitter; // 立即返回，数据后续推送
}
```

## 五、数据库表设计

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `chatclow_ai_user` | 用户 | id, username, email, password, role(1普通/2管理员), totals |
| `chatclow_ai_agent` | 智能体 | id, name, description, avatar, system_prompt, model_id, user_id, status, kb_enabled, kb_id |
| `chatclow_ai_model` | AI 模型 | id, model_name, model_code, api_url, api_key |
| `chatclow_ai_function` | 工具函数 | id, agent_id, name, description, parameters(JSON Schema), status |
| `chatclow_rag_kb` | 知识库 | id, name, description, icon, embedding_model_id, rag_enhancement, status |
| `chatclow_rag_document` | 文档 | id, kb_id, file_name, file_path, file_type, chunk_count, status |
| `chatclow_rag_chunk` | 文档切片 | id, kb_id, document_id, chunk_index, content, token_count, vector_data, content_hash |
| `chatclow_agent_conversation` | 会话 | id, user_id, title |
| `chatclow_agent_conversation_record` | 对话记录 | id, conversation_id, role(user/assistant), content |

**关键关系**：

```
User(1) → Agent(N)              一个用户可创建多个智能体
Agent(1) → Function(N)          一个智能体可绑定多个工具
Agent(N) → Model(1)             多个智能体可共享同一模型
Agent(N) → KnowledgeBase(1)     智能体可选绑定知识库
KnowledgeBase(1) → Document(N)  知识库下有多个文档
Document(1) → Chunk(N)          一个文档被切成多个切片
Chunk 向量化后存 vector_data     向量用于余弦相似度搜索
User(1) → Conversation(N)       用户有多个会话
Conversation(1) → Record(N)     会话包含多条对话记录
```

## 六、API 接口文档

### 6.1 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录（username + password）→ 返回 token/userId/username |

### 6.2 对话

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat/send` | 同步对话，返回完整回复 |
| POST | `/api/chat/send-stream` | SSE 流式对话，逐字推送 |

**请求体**（ChatRequest）：

```json
{
  "agentId": 1,
  "userId": 1,
  "message": "你好",
  "conversationId": null  // 首次为null，后续传回的会话ID
}
```

### 6.3 智能体

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/agent/list/{userId}` | 查询智能体列表（管理员看全部） |
| GET | `/api/agent/{id}` | 查询单个智能体 |
| POST | `/api/agent/add` | 新增智能体 |
| PUT | `/api/agent/update` | 更新智能体 |
| DELETE | `/api/agent/delete/{id}` | 删除智能体 |
| PUT | `/api/agent/status/{id}` | 切换启用/禁用 |

### 6.4 知识库

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/kb/list` | 知识库列表 |
| POST | `/api/kb/add` | 新增知识库 |
| PUT | `/api/kb/update` | 更新知识库 |
| DELETE | `/api/kb/delete/{id}` | 删除知识库 |

### 6.5 文档

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/document/upload` | 上传文档（自动切片+向量化） |
| GET | `/api/document/list/{kbId}` | 知识库下的文档列表 |
| DELETE | `/api/document/delete/{id}` | 删除文档 |

### 6.6 切片

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chunk/list/{documentId}` | 文档下的切片列表 |

## 七、前端页面说明

### 7.1 登录页（Login.vue）

- 用户名 + 密码登录
- 登录成功后 token/userId 存入 Pinia + localStorage
- 跳转到聊天主页

### 7.2 聊天主页（Chat.vue）

**布局**：左侧边栏 + 右侧聊天区

**左侧边栏**：
- 智能体下拉选择 + ➕ 新建按钮
- 会话列表（点击切换）
- 底部：知识库入口 + 退出登录

**右侧聊天区**：
- 消息气泡（用户蓝色/AI灰色）
- AI 消息支持 MarkdownIt 渲染
- 工具调用状态展示（正在调用... → 结果展示）
- 流式打字动画（typing indicator）
- 底部输入框（Enter 发送）

**新建智能体弹窗**：
- 名称、描述、模型选择、系统提示词
- 开关：是否启用 RAG → 选择知识库

### 7.3 SSE 流式聊天 API（chat.js）

```javascript
// 用 fetch + ReadableStream 逐行读取 SSE
export async function chatStream({ agentId, userId, message, conversationId }, onEvent) {
  const response = await fetch('/api/chat/send-stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({ agentId, userId, message, conversationId })
  })

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  // 逐行读取 data: 前缀 → JSON.parse → onEvent({type, data})
}
```

前端按 `type` 分发事件：

```javascript
switch (event.type) {
  case 'conv_id':    currentConvId = event.data; break
  case 'content':    aiMsg.content += event.data; break
  case 'tool_call':  aiMsg.toolStatus = `正在调用工具: ${event.data.name}...`; break
  case 'tool_result': aiMsg.toolResult = `${event.data.name}: ${event.data.result}`; break
  case 'done':       streaming = false; break
  case 'error':      aiMsg.content += `❌ 错误: ${event.data}`; break
}
```

## 八、配置说明

### application.yml 关键配置

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatclow?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
  servlet:
    multipart:
      max-file-size: 50MB     # 单文件上传上限
      max-request-size: 100MB # 请求体上限

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true  # 下划线转驼峰

rag:
  embedding:
    api-url: https://api.siliconflow.cn/v1/embeddings
    api-key: sk-xxx          # 硅基流动 API Key
    model: BAAI/bge-m3       # Embedding 模型
```

### vite.config.js 代理

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 九、启动方式

### 后端

```bash
cd chatclow_api
mvn spring-boot:run
# 或在 IDEA 中直接运行 ChatclowApplication
# 启动后访问 http://localhost:8080
```

### 前端

```bash
cd chatclow-web
npm install
npm run dev
# 启动后访问 http://localhost:5173
```

## 十、核心数据流图

### 同步对话

```
前端 → POST /api/chat/send → ChatController
  → ChatServiceImpl.chat()
    → 查智能体 → 查模型 → 获取会话 → 保存用户消息
    → 构建历史 + RAG检索(可选) + 查工具(可选)
    → OkHttp 调用 AI API
      → 有tool_calls? → 执行工具 → 第二次请求 → 最终回答
      → 无tool_calls? → 直接返回回答
    → 保存AI回复 → 返回 ChatResponse
```

### 流式对话

```
前端 → POST /api/chat/send-stream → ChatController
  → sseExecutor 异步执行 → ChatServiceImpl.chatStream()
    → 推送 conv_id → 保存用户消息 → 构建历史 + RAG + 工具
    → streamWithOkHttp() 逐行读取
      → 普通文字 → 推送 SseEvent.content
      → tool_calls碎片 → StreamToolCall 累加
    → 有工具调用?
      → handleStreamToolCalls()
        → 推送 SseEvent.toolCall → 执行工具
        → 推送 SseEvent.toolResult → 第二次流式请求
        → 逐字推送最终答案
    → 推送 SseEvent.done → complete
```

### RAG 知识库

```
上传文档 → RagDocumentController
  → 解析文本(PDF/Word) → ChunkSplitUtil.split(500字/片)
  → EmbeddingServiceImpl.batchEmbed() → 向量化
  → 存入 rag_chunk 表(vector_data 字段)

检索时:
  用户问题 → embed() 向量化 → VectorSearchServiceImpl.search()
    → 查出知识库所有有向量的切片
    → 逐条计算余弦相似度(阈值0.5)
    → TopK 排序返回
  → 注入 system prompt → AI 基于知识回答
```

## 十一、踩坑记录

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| Apipost 测试 SSE 后端报序列化错误 | `completeWithError` 触发 Spring 尝试序列化异常对象 | 改用 `emitter.complete()` + 日志记录 |
| 端口 8080 被占用 | 之前的 `mvn spring-boot:run` 后台进程未释放 | 杀掉占 8080 的 Java 进程 |
| 管理员看不到其他用户的智能体 | 查询逻辑仅按 userId 过滤 | 增加 role=2 判断，管理员返回所有 |
| 知识库创建接口 name 字段丢失 | SQL 语句仅插入 description | 修复插入逻辑补全 name 字段映射 |
| 前端 Pinia 未安装 | package.json 缺依赖 | `npm install pinia` + 重启 Vite |
