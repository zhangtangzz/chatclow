# ChatClow 项目全局约束

## 项目概要

ChatClow 是一个 AI Agent 智能问答平台（Spring Boot + Vue 3），支持 RAG 知识库检索、智能体管理、大模型接入、Function Calling 工具调用。

## 目录结构

```
chatclow/
├── chatclow_api/          # 后端 Spring Boot（见 chatclow_api/CLAUDE.md）
├── chatclow-web/          # 前端 Vue 3（见 chatclow-web/CLAUDE.md）
└── 总结文档/              # 个人学习笔记（不要删除或修改里面的文件）
```

## 全局规则

- 永远用中文回复
- 添加新功能或修改已有功能前，必须先和用户确认需求和方案，不要擅自开始写代码
- 遇到不确定的技术问题或API用法，先去互联网搜索查证，不要自己瞎编
- 不要修改 `总结文档/` 目录下的任何文件
- 后端代码在 `chatclow_api/` 里操作，前端代码在 `chatclow-web/` 里操作
- 不要在没有明确要求的情况下创建新的 md 文件或文档
- 新增功能前先理解现有的责任链和 SPI 扩展机制，不要破坏现有架构
- 优先编辑已有文件，避免创建新文件
- 每次代码改动后，必须在 `更改日志.txt` 末尾追加记录，精确到秒钟（格式：`YYYY-MM-DD HH:MM:SS`）。同一条记录内写清楚功能简述 + 改动文件列表 + 数据库变更（如有）。同一个问题反复修改也要每次都记录，不要合并，不要省略
- 不要提交 `.env`、`application.yml` 中的密钥等敏感信息
- 不要引入不必要的第三方依赖

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 2.6.13 + Java 17 + Maven |
| ORM | MyBatis Plus 3.5.3 |
| 数据库 | MySQL 8.0（主库） + MongoDB + PostgreSQL（向量存储） |
| HTTP客户端 | OkHttp 4.12 |
| 鉴权 | JWT 0.9.1 |
| 前端框架 | Vue 3.5 + Vite 8.0 + Element Plus 2.14 |
| 状态管理 | Pinia |
| AI API | SiliconFlow（Embedding + Rerank） |
| 设计风格 | 手绘白板笔记 Handwritten Sketch（见 chatclow-web/CLAUDE.md） |
