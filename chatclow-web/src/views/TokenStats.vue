<template>
  <div class="token-stats-layout">
    <div class="token-stats-container">
      <div class="stats-header">
        <h2>用量统计</h2>
      </div>

      <div class="stats-summary">
        <div class="summary-card">
          <div class="summary-label">总会话数</div>
          <div class="summary-value">{{ statsList.length }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-label">Token 总消耗</div>
          <div class="summary-value">{{ totalTokens.toLocaleString() }}</div>
        </div>
      </div>

      <el-table :data="statsList" border stripe style="width: 100%" v-loading="loading" empty-text="暂无对话记录">
        <el-table-column prop="conversationId" label="会话ID" width="80" />
        <el-table-column prop="title" label="会话标题" min-width="200">
          <template #default="{ row }">
            <span>{{ row.title || '新对话' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdDt) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalTokens" label="Token 消耗" width="140" sortable>
          <template #default="{ row }">
            <span class="token-count">{{ (row.totalTokens || 0).toLocaleString() }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import { getUserTokenStats } from '../api/token'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const statsList = ref([])
const loading = ref(false)

const totalTokens = computed(() =>
  statsList.value.reduce((sum, item) => sum + (item.totalTokens || 0), 0)
)

function formatTime(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleString('zh-CN')
}

async function loadStats() {
  loading.value = true
  try {
    const res = await getUserTokenStats(userStore.userId)
    statsList.value = res.data || []
  } catch (e) {
    ElMessage.error('加载用量统计失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<style scoped>
.token-stats-layout {
  min-height: 100vh;
  background: var(--bg-page);
  display: flex;
  justify-content: center;
  padding: 40px 20px;
}

.token-stats-container {
  width: 800px;
  max-width: 100%;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  margin: 0;
  font-family: var(--font-marker);
  font-size: 22px;
  color: var(--fg-default);
}

.stats-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.summary-card {
  flex: 1;
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard);
  padding: 20px 24px;
  border-radius: 20px 225px 15px 255px / 255px 15px 225px 15px;
}

.summary-label {
  font-size: 13px;
  color: var(--fg-muted);
  margin-bottom: 8px;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary);
}

.token-count {
  font-weight: 600;
  color: var(--primary);
}
</style>
