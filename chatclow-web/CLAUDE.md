# ChatClow Web 前端编码规范

## 技术栈

Vue 3.5（Composition API）+ Vite 8 + Element Plus 2.14 + Pinia 3 + Vue Router 4

## 目录结构

```
src/
├── views/         # 页面组件：Chat / Knowledge / Admin / Login
├── components/    # 可复用组件
├── api/           # axios 接口封装（auth / chat / agent / knowledge / model）
├── router/        # Vue Router 路由配置
├── stores/        # Pinia 状态管理（user.js）
└── assets/        # 静态资源
```

## 编码规范

### API 调用

- 所有接口调用走 `src/api/` 下的封装，不要在组件里直接用 axios
- `request.js` 是 axios 实例，统一配置 baseURL、拦截器、JWT token

### 路由

- 路由配置在 `router/index.js`，按页面模块分组
- 需要登录的页面加 `meta: { requiresAuth: true }`

### 状态管理

- 用户状态放 `stores/user.js`（token、用户信息）
- 不要在每个组件里单独存重复的用户状态

### 样式

- 优先用 Element Plus 组件，不要自己造轮子
- 样式放 `<style scoped>` 里

### SSE 流式对话

- 前端通过 `EventSource` 或 `fetch` + `ReadableStream` 接收 SSE 事件
- SSE 事件类型：`content`（逐字文本）、`tool_call`（工具调用）、`tool_result`（工具结果）、`done`（结束）、`convId`（会话ID）

## 设计系统：手绘白板笔记风格 (Handwritten Sketch)

所有 UI 改动必须遵循以下设计规范。

### 色彩系统

| Token | HEX | 用途 |
|-------|-----|------|
| `--bg-page` | #fdfbf7 | 页面纸张背景 |
| `--bg-card` | #ffffff | 卡片白色底 |
| `--fg-default` | #2d2d2d | 墨水黑，主要文字与边框 |
| `--fg-muted` | rgba(45,45,45,0.6) | 次要文字 |
| `--primary` | #ff4d4d | 标记笔红，强调数值与CTA |
| `--accent` | #2d5da1 | 钢笔蓝，链接与辅助强调 |
| `--success` | #16a34a | 成功/已连接 |
| `--warning` | #facc15 | 便利贴黄，处理中 |
| `--danger` | #ff4d4d | 错误/失败 |

使用比例：70% 暖白纸张底 / 20% 白色卡片 / 10% 标记笔色点缀

### 字体

- **标题/强调**: `Kalam, cursive` — 粗体手写标记笔风格（Google Fonts）
- **正文/描述**: `Patrick Hand, cursive` — 自然手写正文（Google Fonts）
- 全局使用手写字体，禁止混入 Inter/Roboto 等无衬线字体

### 核心视觉签名

1. **有机不规则圆角**：卡片不要用常规 `border-radius`，用 wobbly 预设按 index 轮换
   - `wobbly-1`: `255px 15px 225px 15px / 15px 225px 15px 255px`
   - `wobbly-2`: `20px 225px 15px 255px / 255px 15px 225px 15px`
   - `wobbly-3`: `15px 225px 15px 255px / 255px 15px 225px 15px`
   - `wobbly-4`: `225px 15px 225px 15px / 15px 225px 15px 255px`

2. **粗黑墨线边框**：卡片 `border: 3px solid #2d2d2d`

3. **硬阴影 (Hard Shadow)**：无模糊纯偏移，不能用 `box-shadow` 带模糊
   - 普通：`4px 4px 0px 0px #2d2d2d`
   - 小：`2px 2px 0px 0px #2d2d2d`
   - 大：`8px 8px 0px 0px #2d2d2d`

4. **微旋转**：卡片 +/-0.5~2deg 模拟随意粘贴效果

5. **波浪下划线**：Section 标题用 `text-decoration: underline wavy`

### 装饰元素（克制使用）

- 透明胶带：`absolute; top: -12px; left: 50%; width: 96px; height: 24px; background: rgba(0,0,0,0.1); rotate: 1deg`
- 图钉：`width: 16px; height: 16px; border-radius: 50%; background: #ff4d4d; border: 2px solid #2d2d2d`

### 动效

- 卡片上浮：`hover: translateY(-4px) + shadow-hard-lg`
- 列表滑移：`hover: translateX(4px)`
- 状态变化：200ms ease-out

### 应避免

- ❌ 深色主题 — 手绘风格锁定暖白纸张底
- ❌ 常规圆角 (rounded-lg/rounded-xl) — 必须用 wobbly
- ❌ 模糊阴影 — 必须用硬阴影
- ❌ 无衬线字体混用
- ❌ 高饱和大面积用色

## 不要做的事

- 不要在组件里直接操作 localStorage 存 token，用 user store
- 不要用 Options API，统一用 `<script setup>` Composition API
- 不要手写 axios 请求，用 `api/` 下的封装
