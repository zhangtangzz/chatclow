<template>
  <div class="chat-layout">
    <!-- 左侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <el-icon :size="24"><ChatDotRound /></el-icon>
          <h2>ChatClow</h2>
        </div>
        <el-button type="primary" size="small" @click="newConversation" :icon="Plus" circle />
      </div>

      <!-- 智能体选择 -->
      <div class="agent-select">
        <div style="display: flex; gap: 8px; align-items: center;">
          <el-select
            v-model="currentAgentId"
            placeholder="选择智能体"
            size="small"
            style="flex: 1"
            @change="onAgentChange"
          >
            <el-option v-for="agent in agents" :key="agent.id" :label="agent.name" :value="agent.id" />
          </el-select>
          <el-button size="small" :icon="Plus" @click="showAgentDialog = true" title="新建智能体" />
        </div>
      </div>

      <!-- 会话列表 -->
      <div class="conversation-list">
        <div v-if="conversations.length === 0" class="empty-sidebar-tip">
          暂无会话
        </div>
        <div
          v-for="conv in conversations"
          :key="conv.id"
          :class="['conv-item', { active: currentConvId === conv.id }]"
          @click="switchConversation(conv)"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span class="conv-title">{{ conv.title || '新对话' }}</span>
          <!-- 记忆指示点 -->
          <div
            v-if="conv.memoryEnabled"
            class="memory-dot"
            title="已启用记忆"
          />
        </div>
      </div>

      <!-- 底部导航 -->
      <div class="sidebar-footer">
        <el-button text @click="$router.push('/knowledge')">
          <el-icon><FolderOpened /></el-icon>
          知识库
        </el-button>
        <el-button text @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出
        </el-button>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-main">
      <!-- 聊天顶部工具栏 -->
      <div class="chat-toolbar" v-if="currentAgentId">
        <div class="toolbar-left">
          <el-tag size="small" effect="plain" type="info">
            {{ getCurrentAgentName() }}
          </el-tag>
          <el-tag v-if="memoryEnabled" size="small" effect="plain" type="success">
            <el-icon><Memo /></el-icon>
            记忆已启用
          </el-tag>
        </div>
        <div class="toolbar-right">
          <!-- 记忆开关 -->
          <div class="memory-switch">
            <span class="switch-label">记忆</span>
            <el-switch
              v-model="memoryEnabled"
              :active-value="true"
              :inactive-value="false"
              size="small"
              @change="onMemoryChange"
            />
          </div>
          <el-button
            v-if="currentConvId"
            size="small"
            text
            type="info"
            @click="clearMemory"
            title="清除当前对话的记忆"
          >
            <el-icon><Delete /></el-icon>
            清除记忆
          </el-button>
        </div>
      </div>

      <div class="chat-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="empty-tip">
          <el-icon :size="64" color="#c0c4cc"><ChatLineSquare /></el-icon>
          <h3>选择一个智能体，开始对话</h3>
          <p v-if="memoryEnabled" class="memory-hint">
            <el-icon><Memo /></el-icon>
            已启用记忆，AI 会记住之前的对话内容
          </p>
        </div>

        <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
          <div class="message-avatar">
            <el-avatar
              :size="36"
              :style="{ background: msg.role === 'user' ? '#409eff' : '#67c23a' }"
            >
              {{ msg.role === 'user' ? '我' : 'AI' }}
            </el-avatar>
          </div>
          <div class="message-content">
            <!-- 工具调用状态 -->
            <div v-if="msg.toolStatus" class="tool-status">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>{{ msg.toolStatus }}</span>
            </div>
            <!-- 工具结果 -->
            <div v-if="msg.toolResult" class="tool-result">
              <el-tag size="small" type="info">{{ msg.toolResult }}</el-tag>
            </div>
            <!-- 文字内容（支持 Markdown） -->
            <div v-if="msg.content" class="markdown-body" v-html="renderMarkdown(msg.content)" />
          </div>
        </div>

        <!-- 正在输入提示 -->
        <div v-if="streaming" class="message assistant">
          <div class="message-avatar">
            <el-avatar :size="36" :style="{ background: '#67c23a' }">AI</el-avatar>
          </div>
          <div class="message-content">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input">
        <el-input
          v-model="inputMessage"
          placeholder="输入消息，Enter 发送"
          :disabled="streaming"
          @keyup.enter="sendMessage"
          size="large"
          clearable
        >
          <template #append>
            <el-button type="primary" :icon="Promotion" :loading="streaming" @click="sendMessage" />
          </template>
        </el-input>
      </div>
    </div>

    <!-- 新建智能体弹窗 -->
    <el-dialog v-model="showAgentDialog" title="新建智能体" width="500px" :close-on-click-modal="false">
      <el-form :model="agentForm" :rules="agentRules" ref="agentFormRef" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="agentForm.name" placeholder="给智能体起个名字" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="agentForm.description" type="textarea" :rows="2" placeholder="简短描述智能体的用途" />
        </el-form-item>
        <el-form-item label="模型" prop="modelId">
          <el-select v-model="agentForm.modelId" placeholder="选择模型" style="width: 100%">
            <el-option v-for="m in models" :key="m.id" :label="m.modelName" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input
            v-model="agentForm.systemPrompt"
            type="textarea"
            :rows="4"
            placeholder="定义智能体的角色和行为，如：你是一个专业的Java开发助手"
          />
        </el-form-item>
        <el-form-item label="启用RAG">
          <el-switch v-model="agentForm.kbEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="agentForm.kbEnabled === 1" label="知识库">
          <el-select v-model="agentForm.kbId" placeholder="选择知识库（可选）" clearable style="width: 100%">
            <el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAgentDialog = false">取消</el-button>
        <el-button type="primary" :loading="agentCreating" @click="handleCreateAgent">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { chatStream, getConversations, createConversation } from '../api/chat'
import { getAgentList, addAgent } from '../api/agent'
import { getModelList } from '../api/model'
import { getKbList } from '../api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  ChatDotRound,
  FolderOpened,
  SwitchButton,
  ChatLineSquare,
  Promotion,
  Loading,
  Delete,
} from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({ html: false, breaks: true })

const router = useRouter()
const userStore = useUserStore()

// 数据
const agents = ref([])
const currentAgentId = ref(null)
const conversations = ref([])
const currentConvId = ref(null)
const messages = ref([])
const inputMessage = ref('')
const streaming = ref(false)
const messagesRef = ref(null)

// 记忆开关
const memoryEnabled = ref(true) // 默认启用

// 新建智能体相关
const showAgentDialog = ref(false)
const agentCreating = ref(false)
const models = ref([])
const knowledgeBases = ref([])
const agentFormRef = ref(null)
const agentForm = ref({
  name: '',
  description: '',
  modelId: null,
  systemPrompt: '',
  kbEnabled: 0,
  kbId: null,
})
const agentRules = {
  name: [{ required: true, message: '请输入智能体名称', trigger: 'blur' }],
  modelId: [{ required: true, message: '请选择模型', trigger: 'change' }],
  systemPrompt: [{ required: true, message: '请输入系统提示词', trigger: 'blur' }],
}

// 获取当前智能体名称
function getCurrentAgentName() {
  const agent = agents.value.find(a => a.id === currentAgentId.value)
  return agent ? agent.name : ''
}

// 记忆开关变化
function onMemoryChange(val) {
  if (val) {
    ElMessage.success('已启用记忆，AI 会记住对话内容')
  } else {
    ElMessage.info('已关闭记忆，AI 不会记住之前的对话')
  }
  // 清空当前消息，重新开始
  if (!val) {
    messages.value = []
  }
}

// 清除当前对话的记忆
async function clearMemory() {
  if (!currentConvId.value) {
    ElMessage.warning('当前没有活跃的对话')
    return
  }
  try {
    await ElMessageBox.confirm(
      '确定清除当前对话的记忆？AI 将不再记住之前的对话内容。',
      '清除记忆',
      { type: 'warning' }
    )
    // TODO: 后端实现 DELETE /api/chat/conversation/memory/{convId} 后替换
    ElMessage.success('记忆已清除（前端）')
    messages.value = []
    currentConvId.value = null
  } catch (e) {
    // 用户取消
  }
}

// Markdown 渲染
function renderMarkdown(text) {
  return md.render(text || '')
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

// 加载智能体列表
async function loadAgents() {
  try {
    console.log('[DEBUG] userId =', userStore.userId, 'token =', userStore.token ? '有' : '无')
    const res = await getAgentList(userStore.userId)
    console.log('[DEBUG] agentList response =', res)
    agents.value = res.data || []
    if (agents.value.length > 0 && !currentAgentId.value) {
      currentAgentId.value = agents.value[0].id
    }
  } catch (e) {
    console.error('加载智能体失败:', e)
  }
}

// 加载会话列表
async function loadConversations() {
  try {
    const res = await getConversations(userStore.userId)
    conversations.value = (res.data || []).reverse()
  } catch (e) {
    console.error('加载会话失败:', e)
  }
}

// 加载模型列表
async function loadModels() {
  try {
    const res = await getModelList()
    models.value = res.data || []
  } catch (e) {
    console.error('加载模型列表失败:', e)
  }
}

// 加载知识库列表
async function loadKnowledgeBases() {
  try {
    const res = await getKbList()
    knowledgeBases.value = res.data || []
  } catch (e) {
    console.error('加载知识库列表失败:', e)
  }
}

// 打开智能体弹窗时加载模型和知识库
watch(showAgentDialog, (val) => {
  if (val) {
    loadModels()
    loadKnowledgeBases()
    agentForm.value = {
      name: '',
      description: '',
      modelId: null,
      systemPrompt: '',
      kbEnabled: 0,
      kbId: null,
    }
  }
})

// 创建智能体
async function handleCreateAgent() {
  const valid = await agentFormRef.value.validate().catch(() => false)
  if (!valid) return

  agentCreating.value = true
  try {
    await addAgent({
      ...agentForm.value,
      userId: userStore.userId,
      status: 1,
    })
    ElMessage.success('智能体创建成功')
    showAgentDialog.value = false
    await loadAgents()
    // 自动选中新创建的智能体
    if (agents.value.length > 0) {
      currentAgentId.value = agents.value[agents.value.length - 1].id
    }
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    agentCreating.value = false
  }
}

// 切换智能体
function onAgentChange() {
  messages.value = []
  currentConvId.value = null
}

// 新建会话
function newConversation() {
  messages.value = []
  currentConvId.value = null
}

// 切换会话
function switchConversation(conv) {
  currentConvId.value = conv.id
  messages.value = []
  // TODO: 加载该会话的历史消息
}

// 发送消息
async function sendMessage() {
  const text = inputMessage.value.trim()
  if (!text || streaming.value) return
  if (!currentAgentId.value) {
    ElMessage.warning('请先选择或创建一个智能体')
    return
  }

  inputMessage.value = ''
  messages.value.push({ role: 'user', content: text })
  streaming.value = true
  scrollToBottom()

  // 当前消息（流式追加）
  const aiMsg = { role: 'assistant', content: '', toolStatus: '', toolResult: '' }
  messages.value.push(aiMsg)
  scrollToBottom()

  try {
    await chatStream(
      {
        agentId: currentAgentId.value,
        userId: userStore.userId,
        message: text,
        conversationId: currentConvId.value,
        memoryEnabled: memoryEnabled.value, // 传递记忆开关状态
      },
      (event) => {
        switch (event.type) {
          case 'conv_id':
            currentConvId.value = Number(event.data)
            loadConversations()
            break
          case 'content':
            aiMsg.content += event.data
            scrollToBottom()
            break
          case 'tool_call':
            aiMsg.toolStatus = `正在调用工具: ${event.data.name}...`
            scrollToBottom()
            break
          case 'tool_result':
            aiMsg.toolStatus = ''
            aiMsg.toolResult = `${event.data.name}: ${event.data.result}`
            scrollToBottom()
            break
          case 'done':
            streaming.value = false
            break
          case 'error':
            aiMsg.content += `\n\n❌ 错误: ${event.data}`
            streaming.value = false
            break
        }
      }
    )
  } catch (e) {
    aiMsg.content += '\n\n❌ 请求失败，请重试'
    streaming.value = false
  }

  scrollToBottom()
}

// 退出登录
function handleLogout() {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  loadAgents()
  loadConversations()
})
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}

/* 左侧边栏 */
.sidebar {
  width: 280px;
  background: linear-gradient(180deg, #1d1e2c 0%, #2c2d3c 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.1);
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #67c23a 0%, #409eff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.agent-select {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.empty-sidebar-tip {
  text-align: center;
  color: #606266;
  padding: 20px 0;
  font-size: 13px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  color: #c0c4cc;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.conv-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.conv-item.active {
  background: rgba(103, 194, 58, 0.15);
  color: #67c23a;
}

.conv-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.memory-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #67c23a;
  flex-shrink: 0;
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-footer .el-button {
  color: #909399;
  justify-content: flex-start;
  padding: 10px 16px;
  border-radius: 8px;
  transition: all 0.2s;
}

.sidebar-footer .el-button:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

/* 右侧聊天区 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* 聊天工具栏 */
.chat-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fafbfc;
  border-bottom: 1px solid #ebeef5;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.memory-switch {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: #f0f9ff;
  border-radius: 20px;
}

.switch-label {
  font-size: 13px;
  color: #606266;
}

.memory-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
  margin-top: 12px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
}

.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #c0c4cc;
}

.empty-tip h3 {
  margin: 16px 0 8px;
  font-size: 18px;
  color: #909399;
}

.empty-tip p {
  font-size: 14px;
  color: #c0c4cc;
}

/* 消息气泡 */
.message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message.user .message-content {
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-content {
  background: #f4f4f5;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.tool-status {
  color: #e6a23c;
  font-size: 13px;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tool-result {
  margin-bottom: 8px;
}

.markdown-body :deep(p) {
  margin: 0 0 8px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.markdown-body :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #c0c4cc;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) { animation-delay: 0s; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* 输入区域 */
.chat-input {
  padding: 16px 24px;
  border-top: 1px solid #ebeef5;
  background: #fafbfc;
}

.chat-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding-right: 0;
}

.chat-input :deep(.el-input-group__append) {
  padding: 0;
  border-radius: 0 12px 12px 0;
}

.chat-input :deep(.el-input-group__append button) {
  border-radius: 0 12px 12px 0;
  border: none;
  padding: 0 20px;
}
</style>
