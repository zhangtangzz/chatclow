<template>
  <div class="login-page">
    <div class="login-wrapper">
      <!-- 左侧：品牌 & 特性 -->
      <div class="login-left">
        <div class="brand">
          <div class="logo wobbly-1">
            <span class="logo-letter">C</span>
          </div>
          <div class="brand-text">
            <h1 class="brand-title">ChatClow</h1>
            <p class="brand-sub">AI 智能问答平台</p>
          </div>
        </div>

        <p class="slogan">让对话更智能，让知识更触手可及</p>

        <div class="feature-list">
          <div class="feat-card wobbly-2">
            <div class="feat-dot" style="background: var(--primary)" />
            <div class="feat-info">
              <h4>AI 智能体对话</h4>
              <p>多模型灵活切换，流式响应，智能体编排</p>
            </div>
          </div>
          <div class="feat-card wobbly-3">
            <div class="feat-dot" style="background: var(--accent)" />
            <div class="feat-info">
              <h4>RAG 知识库</h4>
              <p>文档上传解析，语义向量检索，混合排序</p>
            </div>
          </div>
          <div class="feat-card wobbly-4">
            <div class="feat-dot" style="background: var(--warning)" />
            <div class="feat-info">
              <h4>全链路可观测</h4>
              <p>Token 消耗追踪，嵌入向量化，重排序优化</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：表单 -->
      <div class="login-right">
        <div class="form-card wobbly-1">
          <div class="tape" />
          <h2 class="form-title">{{ isRegisterMode ? '创建账号' : '密码登录' }}</h2>
          <p class="form-subtitle">{{ isRegisterMode ? '注册一个新账号开始使用' : '请登录您的账号以继续' }}</p>

          <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password :key="isRegisterMode ? 'reg-pw' : 'login-pw'" @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item v-if="isRegisterMode" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" size="large" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item v-if="isRegisterMode" prop="email">
              <el-input v-model="form.email" placeholder="邮箱" size="large" :prefix-icon="Message" @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item>
              <button type="submit" class="submit-btn wobbly-2" :disabled="loading">
                {{ isRegisterMode ? '注 册' : '登 录' }}
                <span v-if="loading" class="loader" />
              </button>
            </el-form-item>
            <div class="form-footer">
              <span>{{ isRegisterMode ? '已有账号？' : '还没有账号？' }}</span>
              <a href="#" @click.prevent="toggleMode">{{ isRegisterMode ? '去登录' : '去注册' }}</a>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { login, register } from '../api/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const isRegisterMode = ref(false)

const form = reactive({ username: '', password: '', confirmPassword: '', email: '' })
const rules = computed(() => {
  const base = {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 3, max: 20, message: '用户名 3-20 个字符', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 32, message: '密码 6-32 位', trigger: 'blur' }
    ]
  }
  if (isRegisterMode.value) {
    base.confirmPassword = [
      { required: true, message: '请确认密码', trigger: 'blur' },
      { validator: (_, val, cb) => val === form.password ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' }
    ]
    base.email = [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
    ]
  }
  return base
})

function toggleMode() {
  isRegisterMode.value = !isRegisterMode.value
  form.username = ''; form.password = ''; form.confirmPassword = ''; form.email = ''
  formRef.value?.clearValidate()
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (isRegisterMode.value) {
      await register(form.username, form.password, form.email)
      ElMessage.success('注册成功，请登录')
      isRegisterMode.value = false
      form.password = ''; form.confirmPassword = ''
    } else {
      const res = await login(form.username, form.password)
      userStore.setUser(res.data)
      ElMessage.success('登录成功')
      router.push(userStore.role === 2 ? '/admin' : '/')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: var(--bg-page);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
}

/* 全屏背景图 */
.login-page::before {
  content: '';
  position: fixed;
  inset: 0;
  background: url('../assets/rainbow-bg.png') center/cover no-repeat;
  opacity: 0.5;
  pointer-events: none;
  z-index: 0;
}

/* 布局 */
.login-wrapper {
  display: flex;
  width: 1100px;
  max-width: 100%;
  gap: 60px;
  align-items: center;
  position: relative;
  z-index: 1;
}

/* ===== 左侧品牌 ===== */
.login-left { flex: 1; }

.brand {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.logo {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard);
}
.logo-letter {
  font-family: var(--font-marker);
  font-size: 32px;
  color: #fff;
  line-height: 1;
}

.brand-title {
  font-family: var(--font-marker);
  font-size: 36px;
  color: var(--fg-default);
  margin: 0;
  line-height: 1.1;
}
.brand-sub {
  font-size: 14px;
  color: var(--fg-muted);
  margin: 2px 0 0;
}

.slogan {
  font-size: 18px;
  color: var(--fg-muted);
  margin-bottom: 36px;
}

/* 特性卡片 */
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.feat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  transition: all 0.2s ease-out;
}
.feat-card:hover {
  transform: translateX(4px);
  box-shadow: var(--shadow-hard);
}
.feat-dot {
  width: 14px;
  height: 14px;
  border: 2px solid var(--border-color);
  flex-shrink: 0;
}
.feat-info h4 {
  font-family: var(--font-marker);
  font-size: 16px;
  color: var(--fg-default);
  margin: 0 0 2px;
}
.feat-info p {
  font-size: 14px;
  color: var(--fg-muted);
  margin: 0;
}

/* ===== 右侧表单 ===== */
.login-right {
  width: 420px;
  flex-shrink: 0;
}

.form-card {
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-lg);
  padding: 40px 36px;
  position: relative;
  overflow: hidden;
}

/* 透明胶带装饰 */
.tape {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%) rotate(-1deg);
  width: 80px;
  height: 20px;
  background: rgba(45, 45, 45, 0.08);
  backdrop-filter: blur(4px);
}

.form-title {
  font-family: var(--font-marker);
  font-size: 26px;
  color: var(--fg-default);
  margin: 0 0 6px;
}
.form-subtitle {
  font-size: 16px;
  color: var(--fg-muted);
  margin: 0 0 28px;
}

/* 自定义提交按钮（不使用 Element Plus 的按钮样式） */
.submit-btn {
  width: 100%;
  padding: 12px 24px;
  font-family: var(--font-marker);
  font-size: 20px;
  color: #fff;
  background: var(--primary);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard);
  cursor: pointer;
  transition: all 0.2s ease-out;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hard-lg);
  background: var(--primary-hover);
}
.submit-btn:active {
  transform: translateY(0);
  box-shadow: var(--shadow-hard-sm);
}
.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: var(--shadow-hard-sm);
}

.loader {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 3px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.form-footer {
  text-align: center;
  color: var(--fg-muted);
  font-size: 15px;
  margin-top: 4px;
}
.form-footer a {
  color: var(--accent);
  text-decoration: underline wavy;
  text-underline-offset: 3px;
  transition: color 0.2s;
}
.form-footer a:hover { color: var(--accent-hover); }

/* 响应式 */
@media (max-width: 960px) {
  .login-wrapper { flex-direction: column; gap: 40px; }
  .login-left { text-align: center; }
  .brand { justify-content: center; }
  .feature-list { display: none; }
  .login-right { width: 100%; max-width: 420px; }
}
</style>
