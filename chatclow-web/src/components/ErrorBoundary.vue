<template>
  <div v-if="hasError" class="error-boundary">
    <div class="error-card">
      <el-icon :size="48" color="#f56c6c"><WarningFilled /></el-icon>
      <h2>页面出错了</h2>
      <p>{{ errorMsg }}</p>
      <el-button type="primary" @click="reload">重试</el-button>
      <el-button @click="goHome">返回首页</el-button>
    </div>
  </div>
  <slot v-else />
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'
import { WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()
const hasError = ref(false)
const errorMsg = ref('')

onErrorCaptured((err) => {
  hasError.value = true
  errorMsg.value = err.message || '未知错误'
  console.error('[ErrorBoundary]', err)
  return false // 阻止向上传播
})

function reload() {
  hasError.value = false
  errorMsg.value = ''
  window.location.reload()
}

function goHome() {
  hasError.value = false
  errorMsg.value = ''
  router.push('/')
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
.error-card {
  text-align: center;
  padding: 48px;
}
.error-card h2 {
  margin: 16px 0 8px;
  color: #303133;
}
.error-card p {
  color: #909399;
  margin-bottom: 24px;
}
</style>
