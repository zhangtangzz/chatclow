<template>
  <div class="login-page">
    <!-- 背景装饰粒子 -->
    <div class="particles">
      <span v-for="n in 30" :key="n" class="dot" :style="randomDotStyle(n)"></span>
    </div>

    <div class="login-wrapper">
      <!-- 左侧：品牌 & 特性 -->
      <div class="login-left">
        <div class="brand">
          <div class="logo">
            <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="64" height="64" rx="16" fill="url(#logoGrad)" />
              <path d="M20 32C20 25.373 25.373 20 32 20C38.627 20 44 25.373 44 32" stroke="white" stroke-width="3.5" stroke-linecap="round" />
              <circle cx="32" cy="40" r="3" fill="white" />
              <path d="M28 48H36" stroke="white" stroke-width="3" stroke-linecap="round" />
              <defs>
                <linearGradient id="logoGrad" x1="0" y1="0" x2="64" y2="64">
                  <stop stop-color="#06b6d4" />
                  <stop offset="1" stop-color="#3b82f6" />
                </linearGradient>
              </defs>
            </svg>
          </div>
          <div class="brand-text">
            <h1>ChatClow</h1>
            <p class="subtitle">AI CHAT PLATFORM</p>
          </div>
        </div>

        <p class="slogan">让对话更智能，让知识更触手可及</p>

        <div class="feature-list">
          <div class="feature-card">
            <div class="feature-icon"><i class="icon-robot"></i></div>
            <div class="feature-info">
              <h4>AI 智能体对话</h4>
              <p>多模型灵活切换，流式响应，智能体编排</p>
            </div>
          </div>
          <div class="feature-card">
            <div class="feature-icon"><i class="icon-kb"></i></div>
            <div class="feature-info">
              <h4>RAG 知识库</h4>
              <p>文档上传解析，语义向量检索，混合排序</p>
            </div>
          </div>
          <div class="feature-card">
            <div class="feature-icon"><i class="icon-lightning"></i></div>
            <div class="feature-info">
              <h4>全链路可观测</h4>
              <p>Token 消耗追踪，嵌入向量化，重排序优化</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：登录表单 -->
      <div class="login-right">
        <div class="glass-card">
          <h2 class="form-title">密码登录</h2>
          <p class="form-subtitle">请登录您的账号以继续</p>

          <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin" class="login-form">
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                size="large"
                :prefix-icon="User"
                class="dark-input"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="handleLogin"
                class="dark-input"
              />
            </el-form-item>
            <el-form-item>
              <button type="submit" class="login-btn" :disabled="loading">
                <span v-if="!loading">登 录</span>
                <span v-else class="loader"></span>
              </button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { login } from '../api/auth'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function randomDotStyle(n) {
  const size = Math.random() * 3 + 1
  const left = Math.random() * 100
  const top = Math.random() * 100
  const duration = Math.random() * 20 + 10
  const delay = Math.random() * 10
  return {
    width: size + 'px',
    height: size + 'px',
    left: left + '%',
    top: top + '%',
    animationDuration: duration + 's',
    animationDelay: delay + 's'
  }
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form.username, form.password)
    userStore.setUser(res.data)
    ElMessage.success('登录成功')
    router.push(userStore.role === 2 ? '/admin' : '/')
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0b1120 0%, #0f172a 50%, #0a1628 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 40px;
}

/* 粒子背景 */
.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.dot {
  position: absolute;
  background: rgba(6, 182, 212, 0.4);
  border-radius: 50%;
  animation: float linear infinite;
}
@keyframes float {
  0% { transform: translateY(0) scale(1); opacity: 0; }
  20% { opacity: 1; }
  80% { opacity: 1; }
  100% { transform: translateY(-100vh) scale(0.3); opacity: 0; }
}

/* 布局 */
.login-wrapper {
  display: flex;
  width: 1100px;
  max-width: 100%;
  gap: 60px;
  align-items: center;
  z-index: 1;
}

/* 左侧品牌 */
.login-left {
  flex: 1;
  color: #fff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.logo {
  width: 56px;
  height: 56px;
}
.logo svg {
  width: 100%;
  height: 100%;
}
.brand-text h1 {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #22d3ee 0%, #3b82f6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  letter-spacing: 3px;
  color: rgba(255,255,255,0.4);
}

.slogan {
  font-size: 16px;
  color: rgba(255,255,255,0.55);
  margin-bottom: 40px;
}

/* 特性卡片 */
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.feature-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 14px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.06);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}
.feature-card:hover {
  background: rgba(255,255,255,0.07);
  border-color: rgba(6, 182, 212, 0.2);
  transform: translateX(4px);
}
.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(6,182,212,0.15), rgba(59,130,246,0.15));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.feature-icon i {
  width: 20px;
  height: 20px;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  display: inline-block;
}
.icon-robot {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2322d3ee' stroke-width='1.5'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' d='M9.75 3.104v5.714a2.25 2.25 0 01-.659 1.591L5 14.5M9.75 3.104c-.251.023-.501.05-.75.082m.75-.082a24.301 24.301 0 014.5 0m0 0v5.714a2.25 2.25 0 00.659 1.591L19.25 14.5M9.75 3.104c.282.027.563.06.843.094m5.954 2.474l-.842.822m-5.954 0a24.301 24.301 0 01-5.954 0m5.954 0v3.75a2.25 2.25 0 01-1.634 2.163l-1.462.365a2.25 2.25 0 00-1.634 2.163v.75m9.75-9l-2.25 2.25M12 21.75l-2.25-2.25'/%3E%3C/svg%3E");
}
.icon-kb {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2322d3ee' stroke-width='1.5'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' d='M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25'/%3E%3C/svg%3E");
}
.icon-lightning {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2322d3ee' stroke-width='1.5'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' d='M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z'/%3E%3C/svg%3E");
}
.feature-info h4 {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: rgba(255,255,255,0.9);
}
.feature-info p {
  margin: 0;
  font-size: 13px;
  color: rgba(255,255,255,0.45);
}

/* 右侧表单 */
.login-right {
  width: 420px;
  flex-shrink: 0;
}
.glass-card {
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 24px;
  padding: 44px 40px;
  backdrop-filter: blur(20px);
  box-shadow: 0 25px 80px rgba(0,0,0,0.35), inset 0 1px 0 rgba(255,255,255,0.05);
}
.form-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 600;
  color: #fff;
}
.form-subtitle {
  margin: 0 0 32px;
  font-size: 14px;
  color: rgba(255,255,255,0.4);
}

/* 深色输入框 */
:deep(.dark-input .el-input__wrapper) {
  background: rgba(255,255,255,0.05) !important;
  border: 1px solid rgba(255,255,255,0.1) !important;
  box-shadow: none !important;
  border-radius: 12px !important;
  padding: 4px 16px !important;
}
:deep(.dark-input .el-input__inner) {
  color: #fff !important;
  height: 44px;
  font-size: 15px;
}
:deep(.dark-input .el-input__inner::placeholder) {
  color: rgba(255,255,255,0.3) !important;
}
:deep(.dark-input .el-input__icon) {
  color: rgba(255,255,255,0.4) !important;
}
:deep(.dark-input.is-focus .el-input__wrapper) {
  border-color: rgba(6,182,212,0.5) !important;
  box-shadow: 0 0 0 3px rgba(6,182,212,0.1) !important;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.login-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #22d3ee 0%, #60a5fa 100%);
  opacity: 0;
  transition: opacity 0.3s;
}
.login-btn:hover::before {
  opacity: 1;
}
.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 30px rgba(6,182,212,0.3);
}
.login-btn:active {
  transform: translateY(0);
}
.login-btn span {
  position: relative;
  z-index: 1;
}
.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.loader {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 960px) {
  .login-wrapper {
    flex-direction: column;
    gap: 40px;
  }
  .login-left {
    text-align: center;
  }
  .brand {
    justify-content: center;
  }
  .feature-list {
    display: none;
  }
  .login-right {
    width: 100%;
    max-width: 420px;
  }
}
</style>
