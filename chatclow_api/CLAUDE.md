# ChatClow API 编码规范

## 核心架构：责任链模式

对话流程使用责任链，入口在 `ChatChainImpl`，7 个 Step 按 `@Order` 自动排序执行。

### Step 顺序（@Order 值）

```
[10] ContextAssemblyStep  — 加载 Agent/Model/会话/历史消息
[20] RagStep              — 向量化 → 双路检索 → 去重 → Rerank
[25] MessageBuildStep     — 构建 messages + tools JSON
[30] ModelCallStep        — 流式 SSE 调用（← 与 SyncAiCallStep 互斥）
[30] SyncAiCallStep       — 非流式同步调用（← 与 ModelCallStep 互斥）
[40] FunctionCallingStep  — 执行工具 + 二次流式请求（仅流式）
[50] RecordSaveStep       — 保存 AI 回复到数据库
[60] ResponseFinalizeStep — 发送 DONE 事件，关闭 SSE 连接
```

### 添加新 Step 的规则

1. 实现 `ChatChainStep` 接口
2. 加 `@Component` + `@Order(n)` 注解
3. 实现 `process(ChatContext ctx)` 和 `shouldSkip(ChatContext ctx)`
4. **不需要**修改 `ChatChainImpl`，Spring 会自动注入
5. 注释格式参考现有 Step：第几步 → 输入参数来源 → 调用什么方法

### 数据传递

所有 Step 共享同一个 `ChatContext` 对象（引用传递）。前一步 `ctx.setXxx()`，后一步 `ctx.getXxx()` 即可。

## 关键常量和工具类

| 类 | 用途 |
|----|------|
| `ChatRole` | 消息角色常量：`SYSTEM`/`USER`/`ASSISTANT`/`TOOL`，不要在代码里写死字符串 |
| `ToolCallHelper` | FC 公共逻辑：`prepareMessages()`/`executeTool()`，流式和非流式共用 |
| `R` | 统一 API 响应：`R.ok(data)` / `R.error(msg)` |
| `SseEvent` | SSE 事件构造：`content()`/`done()`/`toolCall()`/`toolResult()` |

## 分层规范

```
controller/   → 只做参数校验和路由，调用 service，返回 R<T> 或 SseEmitter
service/      → 业务逻辑，只依赖 mapper 和工具类
mapper/       → MyBatis Plus 接口，继承 BaseMapper<T>
entity/       → POJO，对应数据库表，用 Lombok
dto/          → 请求/响应 DTO
config/       → Spring 配置类
```

### 日志

用 SLF4J，不要用 `e.printStackTrace()` 或 `System.err.println()`：

```java
private static final Logger log = LoggerFactory.getLogger(Xxx.class);
log.info("xxx: {}", value);
log.error("xxx", e);
```

## 添加新工具（Function Calling）

在 `FunctionExecutorImpl` 中：
1. 写一个 `private Map<String, Object> xxx(Map<String, Object> args)` 方法
2. 在 `init()` 中加一行 `registry.put("工具名", this::xxx);`
3. 用 `ObjectMapper` 构建返回值，不要手拼 JSON 字符串

## 向量存储

`ChatClowVectorStore` 接口 + `VectorStoreFactory` 实现 SPI 可插拔。
目前支持 MySQL / MongoDB / pgvector，按知识库级别路由。
添加新后端：实现 `ChatClowVectorStore` 接口 + `@Component`，Factory 自动发现。

## 不要做的事

- 不要在 Controller 里写业务逻辑
- 不要用 `new Thread()` 创建线程，用 `sseExecutor` 线程池
- 不要在 Step 之间用返回值传递数据，用 ChatContext
- 不要手写 JSON（getWeather 那种），用 ObjectMapper
- 不要在 FC 逻辑中写重复代码，用 ToolCallHelper
