# ChatClow 🤖

> 基于 RAG + AI Agent 的智能问答平台，支持知识库管理、智能体配置与大模型接入。

[![Java](https://img.shields.io/badge/Java-17-blue?logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.6.13-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42b883?logo=vue.js)](https://vuejs.org/)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

---

## ✨ 功能特性

| 模块 | 功能 |
|------|------|
| 🔐 用户系统 | 注册 / 登录 / JWT 鉴权 / 角色区分（用户 / 管理员） |
| 💬 智能对话 | 基于 AI Agent 的多轮对话，支持流式 SSE 输出 |
| 📚 RAG 知识库 | PDF / Word / TXT 文档解析、智能切片、向量检索、Re-rank 重排序 |
| 🤖 智能体管理 | 可视化卡片管理、配置系统提示词、独立对话窗口 |
| 🧠 大模型管理 | 多模型接入（OpenAI / Claude / 本地模型）、提供商管理 |
| 📊 可观测 | 对话历史记录、Token 用量统计、全链路日志 |
| 🛠 管理员后台 | 用户管理、知识库管理、智能体管理、模型管理 |

---

## 🛠 技术栈

### 后端

| 技术 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 2.6.13 |
| MyBatis Plus | 3.5.3 |
| MySQL | 8.0.33 |
| JWT | 0.11.5 |
| SiliconFlow API | embedding / rerank |
| 向量存储 | MySQL / pgvector / MongoDB（可切换） |

### 前端

| 技术 | 版本 |
|------|------|
| Vue | 3.5 |
| Vite | 8.0 |
| Element Plus | 2.14 |
| Pinia | 3.0 |
| Vue Router | 4.6 |
| markdown-it | 14.2 |

---

## 📁 项目结构

```
ChatClow2.0/
├── chatclow_api/          # 后端（Spring Boot）
│   ├── src/main/java/com/chatclow/
│   │   ├── controller/    # 接口层
│   │   ├── service/       # 业务层
│   │   ├── mapper/        # 数据访问层
│   │   ├── entity/        # 实体类
│   │   ├── config/        # 配置类（JWT / CORS / MyBatis）
│   │   └── util/          # 工具类
│   └── pom.xml
│
├── chatclow-web/          # 前端（Vue 3）
│   ├── src/
│   │   ├── views/         # 页面组件
│   │   ├── components/    # 通用组件
│   │   ├── api/           # 接口封装
│   │   ├── router/        # 路由配置
│   │   └── stores/        # Pinia 状态管理
│   └── package.json
│
└── sql/                   # 数据库初始化脚本（如有）
```

---

## 🚀 快速启动

### 后端

```bash
# 1. 克隆项目
git clone https://github.com/zhangtangzz/ChatClow2.0.git
cd ChatClow2.0/chatclow_api

# 2. 配置数据库（修改 src/main/resources/application.yml）
#   - 数据库地址、用户名、密码

# 3. 启动项目
mvn spring-boot:run
# 后端默认运行在 http://localhost:8080
```

### 前端

```bash
# 1. 进入前端目录
cd ../chatclow-web

# 2. 安装依赖
npm install

# 3. 启动开发服务器
npm run dev
# 前端默认运行在 http://localhost:5173
```

---

## ⚙️ 环境配置

在 `chatclow_api/src/main/resources/application.yml` 中配置：

```yaml
# 数据库
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatclow?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

# JWT 密钥
jwt:
  secret: your_secret_key
  expiration: 86400

# AI 模型 API Key（SiliconFlow）
siliconflow:
  api-key: your_api_key
  embedding-model: BAAI/bge-m3
  rerank-model: BAAI/bge-reranker-v2-m3
```

---

## 📸 截图

| 页面 | 说明 |
|------|------|
| 登录页 | 深色毛玻璃设计，支持账号密码 / 注册 |
| 对话页 | 左侧历史记录，右侧对话区，支持流式输出 |
| 知识库管理 | 文档上传、切片预览、检索测试 |
| 智能体管理 | 卡片式展示，点击进入配置 + 对话 |
| 管理员后台 | 用户 / 知识库 / 智能体 / 模型统一管理 |

---

## 📌 TODO

- [ ] 支持更多文档格式（Excel / PPT / Markdown）
- [ ] 智能体 Function Calling 支持
- [ ] 多轮对话上下文压缩
- [ ] 模型管理支持在线测试
- [ ] Docker 一键部署
- [ ] 接入更多向量数据库（Milvus / Qdrant）

---

## 📄 License

[MIT License](LICENSE)

---

## 🙏 致谢

- [SiliconFlow](https://siliconflow.cn) — 向量化 & Rerank API
- [Element Plus](https://element-plus.org) — Vue 3 组件库
- [MyBatis Plus](https://baomidou.com) — 增强 ORM 框架
