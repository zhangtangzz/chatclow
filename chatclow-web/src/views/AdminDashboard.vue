<template>
  <div class="dash-layout">
    <!-- 顶部操作栏 -->
    <div class="dash-toolbar">
      <div class="date-badge">
        <span class="date-label">筛选周期</span>
        <span class="date-value">近 7 天</span>
      </div>
      <button class="dash-btn" :class="{ refreshing: loading }" @click="refreshData" :disabled="loading">
        <span class="refresh-icon" :class="{ spinning: loading }">⟳</span>
        {{ loading ? '加载中...' : '刷新' }}
      </button>
    </div>

    <!-- 核心指标 -->
    <div class="metric-grid">
      <div class="metric-card" style="--card-accent: #7c3aed">
        <div class="metric-icon" style="background: rgba(124,58,237,0.12); color: #7c3aed">Σ</div>
        <div class="metric-body">
          <div class="metric-label">Token 总消耗</div>
          <div class="metric-value">{{ formatNum(stats.totalTokens) }}</div>
          <div class="metric-change neutral">环比昨日 {{ stats.tokenChange }}%</div>
        </div>
      </div>
      <div class="metric-card" style="--card-accent: #6366f1">
        <div class="metric-icon" style="background: rgba(99,102,241,0.12); color: #6366f1">💬</div>
        <div class="metric-body">
          <div class="metric-label">总对话</div>
          <div class="metric-value">{{ stats.totalConversations }}</div>
          <div class="metric-change" :class="stats.convChange >= 0 ? 'up' : 'down'">
            <span v-if="stats.convChange !== 0" class="arrow">{{ stats.convChange >= 0 ? '↑' : '↓' }}</span>
            {{ Math.abs(stats.convChange) }}% vs 昨日
          </div>
        </div>
      </div>
      <div class="metric-card" style="--card-accent: #06b6d4">
        <div class="metric-icon" style="background: rgba(6,182,212,0.12); color: #06b6d4">👤</div>
        <div class="metric-body">
          <div class="metric-label">活跃用户</div>
          <div class="metric-value">{{ stats.activeUsers }}</div>
          <div class="metric-change" :class="stats.userChange >= 0 ? 'up' : 'down'">
            <span v-if="stats.userChange !== 0" class="arrow">{{ stats.userChange >= 0 ? '↑' : '↓' }}</span>
            {{ Math.abs(stats.userChange) }}% vs 昨日
          </div>
        </div>
      </div>
      <div class="metric-card" style="--card-accent: #16a34a">
        <div class="metric-icon" style="background: rgba(22,163,74,0.12); color: #16a34a">⏱</div>
        <div class="metric-body">
          <div class="metric-label">平均响应</div>
          <div class="metric-value">{{ stats.avgResponse }}<span class="unit">s</span></div>
          <div class="metric-change neutral">环比昨日 {{ stats.responseChange }}</div>
        </div>
      </div>
    </div>

    <!-- 三大面板 -->
    <div class="panel-grid">
      <!-- 记忆概览 -->
      <div class="dash-panel memory-panel">
        <div class="panel-tape" />
        <h3 class="panel-title">🧠 记忆概览</h3>
        <div class="panel-summary">
          <div class="summary-row">
            <span class="summary-label">总记忆数</span>
            <span class="summary-value">{{ memoryStats.total }}</span>
          </div>
          <div class="summary-row">
            <span class="summary-label">检索效果</span>
            <span class="summary-value">{{ memoryStats.retrievalRate }}%</span>
          </div>
        </div>
        <div class="donut-wrap">
          <svg class="donut" viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="48" fill="none" stroke="rgba(45,45,45,0.1)" stroke-width="12" />
            <circle cx="60" cy="60" r="48" fill="none" stroke="var(--primary)" stroke-width="12"
              :stroke-dasharray="donutDash" stroke-dashoffset="0"
              transform="rotate(-90 60 60)" stroke-linecap="round" />
          </svg>
          <div class="donut-center">
            <span class="donut-pct">{{ memoryStats.total > 0 ? '100' : '0' }}%</span>
            <span class="donut-label">已用</span>
          </div>
        </div>
        <div class="panel-footer">记忆数据按会话自动管理</div>
      </div>

      <!-- RAG 摘要 -->
      <div class="dash-panel rag-panel">
        <div class="panel-tape" />
        <h3 class="panel-title">📚 RAG 摘要</h3>
        <div class="rag-stats">
          <div class="rag-stat-item">
            <span class="stat-num">{{ ragStats.totalKb }}</span>
            <span class="stat-name">知识库</span>
          </div>
          <div class="rag-stat-item">
            <span class="stat-num">{{ ragStats.totalDocs }}</span>
            <span class="stat-name">文档</span>
          </div>
          <div class="rag-stat-item">
            <span class="stat-num">{{ ragStats.totalChunks }}</span>
            <span class="stat-name">分片</span>
          </div>
        </div>
        <div class="top-kb" v-if="ragStats.topKb">
          <div class="top-kb-header">
            <span class="top-kb-label">🏆 热门知识库</span>
            <span class="top-kb-name">{{ ragStats.topKb.name }}</span>
          </div>
          <div class="progress-bar">
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: ragStats.topKb.pct + '%' }" />
            </div>
            <span class="progress-label">{{ ragStats.topKb.docCount }} 文档</span>
          </div>
        </div>
        <div class="panel-footer">知识库文档自动向量化检索</div>
      </div>

      <!-- 技能仪表板 -->
      <div class="dash-panel skill-panel">
        <div class="panel-tape" />
        <h3 class="panel-title">🔧 技能仪表板</h3>
        <div class="skill-count">
          <span class="skill-count-num">{{ skillStats.total }}</span>
          <span class="skill-count-label">个技能</span>
        </div>
        <div class="skill-list">
          <div v-for="sk in skillStats.topSkills" :key="sk.name" class="skill-item">
            <div class="skill-top">
              <span class="skill-name">{{ sk.name }}</span>
              <span class="skill-usage">{{ sk.callCount }} 次调用</span>
            </div>
            <div class="skill-bar">
              <div class="skill-track">
                <div class="skill-fill" :style="{ width: sk.pct + '%' }" />
              </div>
              <span class="skill-size">{{ sk.memory }}</span>
            </div>
          </div>
        </div>
        <div class="panel-footer">技能由 Function Calling 驱动</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import { getAdminTokenSummary, getAdminAvgResponseTime } from '../api/token'
import { getKbList, getDocList } from '../api/knowledge'

const loading = ref(false)

// ─── 仪表盘统计数据 ───

const stats = reactive({
  totalTokens: 0,
  tokenChange: 0,
  totalConversations: 0,
  convChange: 0,
  activeUsers: 0,
  userChange: 0,
  avgResponse: 0,
  responseChange: '0.0',
})

const memoryStats = reactive({ total: 0, retrievalRate: 0.0 })

const ragStats = reactive({
  totalKb: 0, totalDocs: 0, totalChunks: 0,
  topKb: null,
})

const skillStats = reactive({
  total: 2,
  topSkills: [
    { name: 'docx 解析', callCount: 0, memory: '7.0KB', pct: 71 },
    { name: 'pdf 解析', callCount: 0, memory: '2.9KB', pct: 29 },
  ],
})

const donutDash = computed(() => {
  const circumference = 2 * Math.PI * 48
  const ratio = memoryStats.total > 0 ? 0.6 : 0
  return `${circumference * ratio} ${circumference * (1 - ratio)}`
})

function formatNum(n) {
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
  return String(n)
}



async function loadDashboardData() {
  loading.value = true
  try {
    // ① Token 总消耗 — 汇总所有用户的 token
    const tokenRes = await getAdminTokenSummary()
    const tokenData = tokenRes.data || []
    stats.totalTokens = tokenData.reduce((sum, u) => sum + (Number(u.totalTokens) || 0), 0)

    // ② 用户列表 & 对话统计
    const userRes = await request.get('/user/admin/list')
    const users = userRes.data || []
    const normalUsers = users.filter(u => u.role === 1)
    stats.activeUsers = normalUsers.length

    let convCount = 0
    for (const u of normalUsers) {
      try {
        const convRes = await request.get('/conversation/list', { params: { userId: u.id } })
        convCount += (convRes.data || []).length
      } catch (_) { /* 跳过单个用户加载失败 */ }
    }
    stats.totalConversations = convCount

    // ③ 平均响应时长
    try {
      const rtRes = await getAdminAvgResponseTime()
      const rtData = rtRes.data || {}
      stats.avgResponse = rtData.avgSec || 0
      stats.responseChange = '实时'
    } catch (_) {}

    // ④ RAG 统计
    const kbRes = await getKbList()
    const kbs = kbRes.data || []
    ragStats.totalKb = kbs.length

    let docsCount = 0
    let chunksCount = 0
    let topKb = null
    for (const kb of kbs) {
      try {
        const docRes = await getDocList(kb.id)
        const docs = docRes.data || []
        docsCount += docs.length
        docs.forEach(d => { chunksCount += Number(d.chunkCount || d.chunk_count || 0) })
        if (!topKb || docs.length > topKb.docCount) {
          topKb = { name: kb.kbName || kb.name, docCount: docs.length, pct: 100 }
        }
      } catch (_) { /* 跳过单个 KB 加载失败 */ }
    }
    ragStats.totalDocs = docsCount
    ragStats.totalChunks = chunksCount
    if (topKb) ragStats.topKb = topKb

    ElMessage.success('数据已刷新')
  } catch (e) {
    ElMessage.error('加载数据失败：' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function refreshData() {
  loadDashboardData()
}

onMounted(loadDashboardData)
</script>

<style scoped>
/* ===== 布局 ===== */
.dash-layout {
  font-family: var(--font-hand);
}

/* ===== 顶部操作栏 ===== */
.dash-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 24px;
}
.date-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  background: var(--bg-card);
}
.date-label {
  font-size: 12px;
  color: var(--fg-muted);
}
.date-value {
  font-family: var(--font-marker);
  font-size: 14px;
  color: var(--fg-default);
}
.dash-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 18px;
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  background: var(--bg-card);
  font-family: var(--font-hand);
  font-size: 14px;
  color: var(--fg-default);
  cursor: pointer;
  transition: all 0.2s ease-out;
}
.dash-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hard);
}
.refresh-icon {
  font-size: 16px;
  display: inline-block;
}
.dash-btn.refreshing { opacity: 0.7; cursor: not-allowed; }
.refresh-icon.spinning { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* ===== 指标卡片 ===== */
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}
.metric-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard);
  transition: all 0.2s ease-out;
}
.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hard-lg);
}
.metric-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  flex-shrink: 0;
}
.metric-body {
  flex: 1;
  min-width: 0;
}
.metric-label {
  font-size: 13px;
  color: var(--fg-muted);
  margin-bottom: 4px;
}
.metric-value {
  font-family: var(--font-marker);
  font-size: 26px;
  color: var(--fg-default);
  line-height: 1.2;
  margin-bottom: 6px;
}
.metric-value .unit {
  font-size: 14px;
  color: var(--fg-muted);
  font-family: var(--font-hand);
}
.metric-change {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 2px;
}
.metric-change.up { color: var(--success); }
.metric-change.down { color: var(--danger); }
.metric-change.neutral { color: var(--fg-muted); }
.arrow { font-size: 14px; }

/* ===== 三大面板 ===== */
.panel-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
}
.dash-panel {
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard);
  padding: 24px;
  position: relative;
}
.panel-tape {
  position: absolute;
  top: -10px;
  left: 50%;
  width: 60px;
  height: 20px;
  background: rgba(0,0,0,0.07);
  transform: translateX(-50%) rotate(-1deg);
  pointer-events: none;
}
.panel-title {
  font-family: var(--font-marker);
  font-size: 17px;
  color: var(--fg-default);
  margin: 0 0 18px;
  text-decoration: underline wavy;
  text-underline-offset: 3px;
  padding-bottom: 10px;
  border-bottom: 2px dashed rgba(45,45,45,0.15);
}

/* ---------- 记忆概览 ---------- */
.panel-summary {
  margin-bottom: 20px;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px dashed rgba(45,45,45,0.1);
}
.summary-row:last-child { border-bottom: none; }
.summary-label { font-size: 13px; color: var(--fg-muted); }
.summary-value { font-family: var(--font-marker); font-size: 18px; color: var(--fg-default); }

.donut-wrap {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 16px;
}
.donut { width: 100%; height: 100%; }
.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.donut-pct {
  font-family: var(--font-marker);
  font-size: 20px;
  color: var(--fg-default);
  line-height: 1;
}
.donut-label {
  font-size: 11px;
  color: var(--fg-muted);
  margin-top: 2px;
}

/* ---------- RAG 摘要 ---------- */
.rag-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
}
.rag-stat-item {
  text-align: center;
}
.stat-num {
  display: block;
  font-family: var(--font-marker);
  font-size: 24px;
  color: var(--fg-default);
  line-height: 1.2;
}
.stat-name {
  font-size: 12px;
  color: var(--fg-muted);
}
.top-kb {
  padding: 14px;
  background: var(--bg-page);
  border: 2px dashed rgba(45,45,45,0.2);
  margin-bottom: 12px;
}
.top-kb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.top-kb-label { font-size: 12px; color: var(--fg-muted); }
.top-kb-name {
  font-family: var(--font-marker);
  font-size: 14px;
  color: var(--fg-default);
}
.progress-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}
.progress-track {
  flex: 1;
  height: 14px;
  background: rgba(45,45,45,0.08);
  border: 2px solid var(--border-color);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: var(--primary);
  transition: width 0.6s ease-out;
}
.progress-label {
  font-size: 12px;
  color: var(--fg-muted);
  white-space: nowrap;
}

/* ---------- 技能仪表板 ---------- */
.skill-count {
  text-align: center;
  margin-bottom: 18px;
}
.skill-count-num {
  font-family: var(--font-marker);
  font-size: 28px;
  color: var(--fg-default);
}
.skill-count-label {
  font-size: 13px;
  color: var(--fg-muted);
  margin-left: 6px;
}
.skill-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 12px;
}
.skill-item {
  padding: 12px;
  background: var(--bg-page);
  border: 2px dashed rgba(45,45,45,0.2);
}
.skill-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.skill-name {
  font-family: var(--font-marker);
  font-size: 14px;
  color: var(--fg-default);
}
.skill-usage {
  font-size: 12px;
  color: var(--fg-muted);
}
.skill-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}
.skill-track {
  flex: 1;
  height: 10px;
  background: rgba(45,45,45,0.08);
  border: 2px solid var(--border-color);
  overflow: hidden;
}
.skill-fill {
  height: 100%;
  background: var(--accent);
  transition: width 0.6s ease-out;
}
.skill-size {
  font-size: 11px;
  color: var(--fg-muted);
  white-space: nowrap;
}

/* ---------- 面板底部 ---------- */
.panel-footer {
  font-size: 11px;
  color: rgba(45,45,45,0.35);
  text-align: center;
  border-top: 2px dashed rgba(45,45,45,0.1);
  padding-top: 10px;
  margin-top: 4px;
}
</style>
