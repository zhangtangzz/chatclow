<template>
  <div class="chat-layout">
    <!-- 左侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <el-icon :size="24"><ChatDotRound /></el-icon>
          <h2>ChatClow</h2>
          <span class="logo-watermark">张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮</span>
        </div>
        <el-button type="primary" size="small" @click="newConversation" :icon="Plus" circle />
      </div>

      <!-- 智能体卡片（点击弹出管理面板） -->
      <div class="agent-card" ref="agentCardRef" @click="showAgentPanel = true">
        <div class="agent-card-avatar">
          <img v-if="currentAgent?.avatar" :src="currentAgent.avatar" class="agent-avatar-img" />
          <span v-else>{{ getAgentEmoji(currentAgent?.name || '') }}</span>
        </div>
        <div class="agent-card-info">
          <div class="agent-card-name">{{ currentAgent?.name || '选择智能体' }}</div>
          <div class="agent-card-model" v-if="currentAgent">{{ getModelName(currentAgent.modelId) }}</div>
          <div class="agent-card-hint">{{ currentAgent ? '点击切换' : '点击选择' }}</div>
        </div>
        <el-icon class="agent-card-arrow" :size="18"><ArrowRight /></el-icon>
      </div>

      <!-- 会话列表 -->
      <div class="conversation-list">
        <div
          v-if="conversations.length === 0"
          class="conv-item active placeholder"
          @click="newConversation"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span class="conv-title">新对话</span>
          <el-tag size="small" effect="plain" type="warning" class="new-badge">新</el-tag>
        </div>
        <div
          v-for="conv in conversations"
          :key="conv.id"
          :class="['conv-item', { active: currentConvId === conv.id }]"
          @click="switchConversation(conv)"
        >
          <el-icon><ChatDotRound /></el-icon>
          <div class="conv-title-wrap">
            <span class="conv-title">{{ conv.title || '新对话' }}</span>
            <span class="conv-model" v-if="conv.modelName">{{ conv.modelName }}</span>
          </div>
          <div v-if="conv.memoryEnabled" class="memory-dot" title="已启用记忆" />
          <el-button
            class="conv-delete-btn"
            :icon="Close"
            size="small"
            text
            @click.stop="handleDeleteConversation(conv)"
            title="删除会话"
          />
        </div>
      </div>

      <!-- 底部导航卡片 -->
      <div class="sidebar-footer">
        <div class="nav-card nav-card-pink" @click="activePanel = 'user-docs'">
          <div class="nav-card-icon"><el-icon :size="22"><FolderOpened /></el-icon></div>
          <div class="nav-card-text">
            <div class="nav-card-title">个人文档</div>
            <div class="nav-card-desc">上传文件构建个人知识库</div>
          </div>
          <el-icon class="nav-card-arrow" :size="16"><ArrowRight /></el-icon>
        </div>
        <div class="nav-card nav-card-blue" @click="activePanel = 'token-stats'">
          <div class="nav-card-icon"><el-icon :size="22"><DataAnalysis /></el-icon></div>
          <div class="nav-card-text">
            <div class="nav-card-title">用量统计</div>
            <div class="nav-card-desc">Token 消耗概览</div>
          </div>
          <el-icon class="nav-card-arrow" :size="16"><ArrowRight /></el-icon>
        </div>
        <div class="nav-card nav-card-danger" @click="handleLogout">
          <div class="nav-card-icon"><el-icon :size="22"><SwitchButton /></el-icon></div>
          <div class="nav-card-text">
            <div class="nav-card-title">退出登录</div>
            <div class="nav-card-desc">切换账号</div>
          </div>
          <el-icon class="nav-card-arrow" :size="16"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-main" v-if="activePanel === 'chat'">
      <!-- 聊天顶部工具栏 -->
      <div class="chat-toolbar" v-if="currentAgentId">
        <div class="toolbar-left">
          <el-tag size="small" effect="plain" type="primary">
            {{ getCurrentAgentName() }}
          </el-tag>
          <el-tag v-if="memoryEnabled" size="small" effect="plain" type="success">
            <el-icon><Memo /></el-icon>
            记忆已启用
          </el-tag>
        </div>
        <!-- 公告按钮（居中） -->
        <div class="toolbar-center" v-if="announcementData">
          <div class="announcement-btn" @click="toggleAnnouncement">
            <el-icon :size="16"><Notification /></el-icon>
            <span>公告</span>
            <span v-if="!announcementVisible" class="announcement-dot" />
          </div>
          <transition name="announcement-fade">
            <div v-if="announcementVisible" class="announcement-popup" @click.stop>
              <div class="announcement-popup-header">
                <el-icon :size="18"><Notification /></el-icon>
                <span class="announcement-popup-title">{{ announcementData.title || '系统公告' }}</span>
                <el-button :icon="Close" size="small" text @click="announcementVisible = false" />
              </div>
              <div class="announcement-popup-content">
                <div class="announcement-watermark" data-text="张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮"></div>
                {{ announcementData.content || '暂无公告内容' }}
              </div>
            </div>
          </transition>
        </div>
        <div class="toolbar-right">
          <!-- 使用帮助卡片（炫酷闪烁） -->
          <div class="help-card" @click="showHelp">
            <el-icon :size="16"><QuestionFilled /></el-icon>
            <span>使用帮助</span>
          </div>
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
          <div class="welcome-icon">
            <span class="welcome-emoji">👋</span>
          </div>
          <h3>开始新对话</h3>
          <p class="welcome-desc">在下方输入你的问题，AI 助手会立即回复你</p>
          <div class="welcome-suggestions" v-if="currentAgentId">
            <span class="suggestion-label">试试问：</span>
            <div class="suggestion-chips">
              <span class="suggestion-chip" @click="quickQuestion('解释一下什么是 RAG 技术')">什么是 RAG？</span>
              <span class="suggestion-chip" @click="quickQuestion('帮我写一封商务邮件')">写一封邮件</span>
              <span class="suggestion-chip" @click="quickQuestion('总结一下 AI Agent 的核心概念')">AI Agent 概念</span>
            </div>
          </div>
          <p v-if="memoryEnabled" class="memory-hint">
            <el-icon><Memo /></el-icon>
            已启用记忆，AI 会记住之前的对话内容
          </p>
        </div>

        <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
          <div class="message-avatar">
            <el-avatar
              v-if="msg.role === 'user'"
              :size="36"
              src="/avatars/对话头像本人.jpg"
            />
            <el-avatar v-else :size="36" src="/avatars/对话头像.jpg" />
          </div>
          <div class="message-content">
            <!-- 上传的文件 -->
            <div v-if="msg.files && msg.files.length > 0" class="msg-files">
              <div v-for="(f, fi) in msg.files" :key="fi" class="msg-file">
                <img v-if="['png','jpg','jpeg','gif','webp','bmp'].includes(f.fileType)" :src="f.url" class="msg-file-img" />
                <el-icon v-else :size="20"><Document /></el-icon>
                <span class="msg-file-name">{{ f.fileName }}</span>
              </div>
            </div>
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
            <div v-if="displayContent(msg, idx)" class="markdown-body" v-html="renderMarkdown(displayContent(msg, idx))" />
          </div>
        </div>

        <!-- 正在输入提示 -->
        <div v-if="streaming && !streamText" class="message assistant">
          <div class="message-avatar">
            <el-avatar :size="36" src="/avatars/对话头像.jpg" />
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
        <!-- 上传文件预览 -->
        <div v-if="uploadedFiles.length > 0" class="chat-file-preview">
          <div v-for="(f, idx) in uploadedFiles" :key="idx" class="chat-file-item">
            <img v-if="['png','jpg','jpeg','gif','webp','bmp'].includes(f.fileType)" :src="f.url" class="chat-file-thumb" />
            <el-icon v-else :size="20"><Document /></el-icon>
            <span class="chat-file-name">{{ f.fileName }}</span>
            <el-button :icon="Close" size="small" text @click="removeChatFile(idx)" />
          </div>
        </div>
        <div v-if="retryText" class="retry-banner">
          <span>连接断开，回复可能不完整</span>
          <el-button size="small" type="warning" @click="retryLastMessage">重新发送</el-button>
        </div>
        <div class="input-row">
          <input ref="chatFileInputRef" type="file" hidden accept="image/*,.pdf,.docx,.txt,.md" @change="handleChatFileSelect" />
          <el-button class="upload-btn" :icon="Paperclip" @click="chatFileInputRef?.click()" :disabled="streaming" />
          <el-input
            v-model="inputMessage"
            placeholder="输入消息，Enter 发送"
            :disabled="streaming"
            @keyup.enter="sendMessage"
            size="large"
            clearable
          >
            <template #append>
              <el-button v-if="!streaming" type="primary" :icon="Promotion" @click="sendMessage" />
              <el-button v-else type="danger" :icon="Close" @click="stopStream" class="stop-btn" />
            </template>
          </el-input>
        </div>
      </div>
    </div>

    <!-- 个人文档面板 -->
    <div v-else-if="activePanel === 'user-docs'" class="embedded-panel">
      <div class="embedded-header">
        <h2>个人文档</h2>
      </div>
      <div class="user-docs-content">
        <!-- 上传区域 -->
        <div class="upload-area" @click="triggerUpload" @dragover.prevent @drop.prevent="handleDrop">
          <input ref="fileInputRef" type="file" hidden accept=".pdf,.docx,.txt,.md" @change="handleFileSelected" />
          <el-icon :size="32"><UploadFilled /></el-icon>
          <p class="upload-text">点击或拖拽上传文档</p>
          <p class="upload-hint">支持 PDF、DOCX、TXT、MD</p>
        </div>

        <!-- 文档列表 -->
        <div class="doc-list" v-loading="userDocsLoading">
          <div v-if="userDocs.length === 0" class="doc-empty">
            还没有上传过文档
          </div>
          <div v-for="doc in userDocs" :key="doc.id" class="doc-item" @click="goToChat" style="cursor:pointer">
            <div class="doc-icon">
              <el-icon :size="20"><Document /></el-icon>
            </div>
            <div class="doc-info">
              <div class="doc-name">{{ doc.fileName }}</div>
              <div class="doc-meta">
                <el-tag size="small" :type="statusType(doc.status)" class="doc-status">
                  {{ statusText(doc.status) }}
                </el-tag>
                <span class="doc-size">{{ formatSize(doc.fileSize) }}</span>
              </div>
            </div>
            <el-button size="small" text type="danger" :icon="Delete"
              @click.stop="handleDeleteDoc(doc)" class="doc-delete-btn" />
          </div>
        </div>
      </div>
    </div>

    <!-- 嵌入的用量统计 -->
    <div v-else-if="activePanel === 'token-stats'" class="embedded-panel">
      <div class="embedded-header">
        <h2>用量统计</h2>
      </div>
      <TokenStats />
    </div>

    <!-- 新建/编辑智能体弹窗 -->
    <el-dialog v-model="showAgentDialog" :title="isEditingAgent ? '编辑智能体' : '新建智能体'" width="500px" :close-on-click-modal="false">
      <el-form :model="agentForm" :rules="agentRules" ref="agentFormRef" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="agentForm.name" placeholder="给智能体起个名字" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="agentForm.description" type="textarea" :rows="2" placeholder="简短描述智能体的用途" />
        </el-form-item>
        <el-form-item label="模型" prop="modelId">
          <el-select v-model="agentForm.modelId" placeholder="选择模型" style="width: 100%">
            <el-option v-for="m in modelList" :key="m.id" :label="m.name || m.modelName" :value="m.id" />
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
          <span style="margin-left:8px;font-size:12px;color:var(--fg-muted)">{{ agentForm.kbEnabled === 1 ? '开启后自动检索所有知识库' : '' }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAgentDialog = false">取消</el-button>
        <el-button type="primary" :loading="agentCreating" @click="handleSaveAgent">{{ isEditingAgent ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- 智能体管理面板（从右侧滑出） -->
    <div v-show="showAgentPanel" class="agent-panel-overlay" @click="showAgentPanel = false" />
    <transition name="panel-slide">
      <div v-if="showAgentPanel" class="agent-panel">
          <!-- 头部 -->
          <div class="panel-header">
            <h3 class="panel-title">智能体管理</h3>
            <el-button :icon="Close" text @click="showAgentPanel = false" />
          </div>

          <!-- 当前选中的智能体详情 -->
          <div v-if="currentAgent" class="panel-current">
            <div class="panel-current-avatar">
              <img v-if="currentAgent.avatar" :src="currentAgent.avatar" class="agent-avatar-img" />
              <span v-else>{{ getAgentEmoji(currentAgent.name) }}</span>
            </div>
            <div class="panel-current-info">
              <div class="panel-current-name">{{ currentAgent.name }}</div>
              <div class="panel-current-model">{{ getModelName(currentAgent.modelId) }}</div>
              <div class="panel-current-desc">{{ currentAgent.description || '暂无描述' }}</div>
            </div>
            <div class="panel-current-actions">
              <el-button size="small" :icon="Edit" @click="openEditAgent" title="编辑">编辑</el-button>
              <el-button size="small" :icon="Delete" @click="handleDeleteAgent" title="删除" class="btn-delete">删除</el-button>
            </div>
          </div>

          <!-- 管理员发布的智慧助手 -->
          <div class="panel-section" v-if="adminAgents.length > 0">
            <div class="panel-section-title">🛡️ 智慧助手</div>
            <div
              v-for="agent in adminAgents"
              :key="agent.id"
              :class="['panel-agent-item', { active: agent.id === currentAgentId }]"
              @click="selectAgentFromPanel(agent)"
            >
              <span class="panel-agent-emoji">
                <img v-if="agent.avatar" :src="agent.avatar" class="agent-avatar-img" />
                <span v-else>{{ getAgentEmoji(agent.name) }}</span>
              </span>
              <span class="panel-agent-name">{{ agent.name }}</span>
              <el-tag v-if="agent.id === currentAgentId" size="small" class="panel-agent-tag" type="danger">使用中</el-tag>
            </div>
          </div>

          <!-- 用户自己的个性化助手 -->
          <div class="panel-section" v-if="myAgents.length > 0">
            <div class="panel-section-title">🎨 个性化助手</div>
            <div
              v-for="agent in myAgents"
              :key="agent.id"
              :class="['panel-agent-item', { active: agent.id === currentAgentId }]"
              @click="selectAgentFromPanel(agent)"
            >
              <span class="panel-agent-emoji">
                <img v-if="agent.avatar" :src="agent.avatar" class="agent-avatar-img" />
                <span v-else>{{ getAgentEmoji(agent.name) }}</span>
              </span>
              <span class="panel-agent-name">{{ agent.name }}</span>
              <el-tag v-if="agent.id === currentAgentId" size="small" class="panel-agent-tag" type="danger">使用中</el-tag>
            </div>
          </div>

          <!-- 新建智能体 -->
          <el-button type="primary" class="panel-create-btn" :icon="Plus" @click="openCreateAgent">
            新建智能体
          </el-button>
        </div>
    </transition>

    <!-- 新用户引导：选择智能体 -->
    <div v-if="showOnboarding" class="onboarding-overlay">
      <div class="onboarding-bg" @click="skipOnboarding" />
      <div class="onboarding-container">
        <div class="onboarding-header">
          <h2 class="onboarding-title">选择一个助手开始对话 👋</h2>
          <p class="onboarding-desc">第一次使用？选一个你需要的 AI 助手，立即开始</p>
        </div>
        <div class="onboarding-grid">
          <div
            v-for="agent in agents"
            :key="agent.id"
            class="onboarding-card"
            :ref="(el) => { if (el) cardRefs[agent.id] = el }"
            @click="selectAgentWithAnimation(agent)"
          >
            <div class="onboarding-avatar">{{ getAgentEmoji(agent.name) }}</div>
            <div class="onboarding-name">{{ agent.name }}</div>
            <div class="onboarding-desc-text">{{ agent.description || '智能对话助手' }}</div>
          </div>
        </div>
        <button class="onboarding-skip" @click="skipOnboarding">暂时跳过</button>
      </div>

      <!-- 飞行动画元素 -->
      <div v-if="flyingCard.visible" class="flying-card" :style="flyingCard.style">
        <div class="flying-avatar">{{ flyingCard.emoji }}</div>
        <div class="flying-label">{{ flyingCard.text }}</div>
      </div>
    </div>

    <!-- ===== 使用帮助引导 ===== -->
    <div v-if="showHelpTutorial" class="help-overlay">
      <div class="help-bg" @click="handleHelpBgClick" />

      <!-- 过渡卡片：所有 Step 从对应位置放大/缩小 -->
      <div v-if="helpCard.visible" class="help-trans-card" :style="helpCard.style" ref="helpCardRef">
        <!-- Step 1: 选择智能体 -->
        <div v-if="helpStep === 1" class="trans-content">
          <h2 class="help-title" style="color:var(--fg-default);text-shadow:none">👋 第一步：选择智能体</h2>
          <p class="help-desc" style="color:var(--fg-muted)">选一个你需要的 AI 助手，点击即可开始</p>
          <div class="help-agent-grid">
            <div v-for="agent in agents" :key="agent.id" class="help-agent-card" @click="helpSelectAgent(agent)">
              <div class="help-agent-emoji">{{ getAgentEmoji(agent.name) }}</div>
              <div class="help-agent-name">{{ agent.name }}</div>
              <div class="help-agent-desc">{{ agent.description || '智能对话助手' }}</div>
            </div>
          </div>
          <button class="help-skip" style="color:var(--fg-muted)" @click="advanceHelp">直接下一步</button>
        </div>
        <!-- Step 2: RAG 知识库内容 -->
        <div v-if="helpStep === 2" class="trans-content">
          <div class="rag-icon">📚</div>
          <h3 class="rag-title">RAG 知识库</h3>
          <p class="rag-text">
            每个智能体可以关联知识库，AI 会自动检索相关知识来回答问题。
            <br>你可以在左侧「知识库」卡片中上传文档。
          </p>
          <div class="rag-arrow-hint">点击空白处下一步 →</div>
        </div>
        <!-- Step 3: Token 用量查询 -->
        <div v-if="helpStep === 3" class="trans-content">
          <div class="rag-icon">📊</div>
          <h3 class="rag-title">Token 用量查询</h3>
          <p class="rag-text">
            在左侧「用量统计」卡片中可以查看你的 Token 消耗概览。
            <br>包括每次对话的 Token 使用量和总消耗统计。
          </p>
          <div class="rag-arrow-hint">点击空白处下一步 →</div>
        </div>
        <!-- Step 4: 对话选择内容 -->
        <div v-if="helpStep === 4" class="trans-content">
          <h2 class="conv-step-title">💬 开始对话</h2>
          <p class="conv-step-desc">创建一个新对话，或继续之前的对话</p>
          <div class="help-conv-list">
            <div class="help-conv-card" @click="helpNewConversation">
              <div class="help-conv-icon">＋</div>
              <div class="help-conv-label">新对话</div>
              <div class="help-conv-sub">从头开始</div>
            </div>
            <div
              v-for="conv in recentConvs"
              :key="conv.id"
              class="help-conv-card"
              @click="helpOpenConversation(conv)"
            >
              <div class="help-conv-icon">💬</div>
              <div class="help-conv-label">{{ conv.title || '新对话' }}</div>
              <div class="help-conv-sub">{{ formatConvTime(conv.createdDt) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { chatStream, getConversations, createConversation, getConversationRecords, deleteConversation, clearConversationMemory, uploadChatFile } from '../api/chat'
import { getLatestAnnouncement } from '../api/announcement'
import { getAgentList, addAgent, updateAgent, deleteAgent } from '../api/agent'
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
  Paperclip,
  Delete,
  DataAnalysis,
  Close,
  Edit,
  ArrowRight,
  QuestionFilled,
  ArrowLeft,
  Document,
  UploadFilled,
  Notification,
} from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import TokenStats from './TokenStats.vue'
import { uploadUserDoc, getUserDocList, deleteUserDoc } from '../api/userDoc'

const md = new MarkdownIt({ html: false, breaks: true })

const router = useRouter()
const userStore = useUserStore()

// ===== 数据 =====
const agents = ref([])
const modelList = ref([])
const modelMap = computed(() => {
  const map = {}
  modelList.value.forEach(m => { map[m.id] = m })
  return map
})
const currentAgentId = ref(null)
const activePanel = ref('chat')  // 'chat' | 'user-docs' | 'token-stats'
const conversations = ref([])
const currentConvId = ref(null)
const messages = ref([])
const inputMessage = ref('')
const streaming = ref(false)
const abortController = ref(null)
const retryText = ref('')
const messagesRef = ref(null)
const streamText = ref('')    // 流式文字（响应式 ref，驱动 DOM 更新）
let rawBuffer = ''             // 原始缓冲（非响应式，快速收集 SSE 数据）
let streamTimer = null         // RAF 句柄
const memoryEnabled = ref(true)

// ===== 公告 =====
const announcementData = ref(null)
const announcementVisible = ref(false)

async function loadAnnouncement() {
  try {
    const res = await getLatestAnnouncement()
    if (res.code === 200 && res.data) {
      announcementData.value = res.data
    }
  } catch (_) {}
}

function toggleAnnouncement() {
  announcementVisible.value = !announcementVisible.value
}

// ===== 对话文件上传 =====
const uploadedFiles = ref([])
const chatFileInputRef = ref(null)

// ===== 智能体管理面板 =====
const showAgentPanel = ref(false)
const showAgentDialog = ref(false)
const agentCreating = ref(false)
const isEditingAgent = ref(false)
const editingAgentId = ref(null)
const knowledgeBases = ref([])
const agentFormRef = ref(null)
const agentForm = ref({ name: '', description: '', modelId: null, systemPrompt: '', kbEnabled: 0, kbId: null })
const agentRules = {
  name: [{ required: true, message: '请输入智能体名称', trigger: 'blur' }],
  modelId: [{ required: true, message: '请选择模型', trigger: 'change' }],
  systemPrompt: [{ required: true, message: '请输入系统提示词', trigger: 'blur' }],
}

// ===== 个人文档 =====
const userDocs = ref([])
const userDocsLoading = ref(false)
const fileInputRef = ref(null)
let docPollTimer = null

const currentAgent = computed(() => agents.value.find(a => a.id === currentAgentId.value) || null)
const adminAgents = computed(() => agents.value.filter(a => a.userId !== userStore.userId))
const myAgents = computed(() => agents.value.filter(a => a.userId === userStore.userId))

// ===== 个人文档函数 =====
function triggerUpload() { fileInputRef.value?.click() }

function handleDrop(e) {
  const file = e.dataTransfer.files[0]
  if (file) doUpload(file)
}

function handleFileSelected(e) {
  const file = e.target.files[0]
  if (file) doUpload(file)
  e.target.value = ''
}

async function doUpload(file) {
  try {
    await uploadUserDoc(file)
    ElMessage.success('上传成功，后台处理中...')
    loadUserDocs()
    startDocPolling()
  } catch (e) {
    ElMessage.error('上传失败：' + (e.response?.data?.message || e.message))
  }
}

function startDocPolling() {
  stopDocPolling()
  docPollTimer = setInterval(async () => {
    try {
      const res = await getUserDocList()
      userDocs.value = res.data || []
      const hasProcessing = userDocs.value.some(d => d.status === 1 || d.status === 2)
      if (!hasProcessing) stopDocPolling()
    } catch (_) {}
  }, 2000)
}

function stopDocPolling() {
  if (docPollTimer) { clearInterval(docPollTimer); docPollTimer = null }
}

function goToChat() { activePanel.value = 'chat' }

// ===== 对话文件上传函数 =====
async function handleChatFileSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  e.target.value = ''
  try {
    const res = await uploadChatFile(file)
    uploadedFiles.value.push(res.data)
    ElMessage.success('文件已上传')
  } catch (e) {
    ElMessage.error('文件上传失败：' + (e.response?.data?.message || e.message))
  }
}

function removeChatFile(idx) {
  uploadedFiles.value.splice(idx, 1)
}

async function loadUserDocs() {
  userDocsLoading.value = true
  try {
    const res = await getUserDocList()
    userDocs.value = res.data || []
  } catch (e) {
    console.error('加载文档列表失败', e)
  } finally { userDocsLoading.value = false }
}

async function handleDeleteDoc(doc) {
  try {
    await ElMessageBox.confirm(`确定删除「${doc.fileName}」？`, '删除确认', { type: 'warning' })
    await deleteUserDoc(doc.id)
    ElMessage.success('已删除')
    loadUserDocs()
  } catch (e) { /* 取消 */ }
}

function statusText(status) { return ({ 1: '解析中', 2: '处理中', 3: '已完成', 4: '失败' })[status] || '未知' }
function statusType(status) { return ({ 1: 'warning', 2: 'warning', 3: 'success', 4: 'danger' })[status] || 'info' }
function formatSize(bytes) {
  if (!bytes) return ''
  const kb = bytes / 1024
  return kb < 1024 ? kb.toFixed(1) + ' KB' : (kb / 1024).toFixed(1) + ' MB'
}

watch(activePanel, (val) => {
  if (val === 'user-docs') { loadUserDocs() } else { stopDocPolling() }
})

// 打开新建智能体弹窗
function openCreateAgent() {
  isEditingAgent.value = false
  editingAgentId.value = null
  resetAgentForm()
  loadModelList()
  loadKnowledgeBases()
  showAgentDialog.value = true
}

// 打开编辑智能体弹窗
function openEditAgent() {
  const agent = agents.value.find(a => a.id === currentAgentId.value)
  if (!agent) return
  isEditingAgent.value = true
  editingAgentId.value = agent.id
  agentForm.value = {
    name: agent.name,
    description: agent.description || '',
    modelId: agent.modelId,
    systemPrompt: agent.systemPrompt,
    kbEnabled: agent.kbEnabled || 0,
    kbId: agent.kbId || null,
  }
  loadModelList()
  loadKnowledgeBases()
  showAgentDialog.value = true
}

function resetAgentForm() {
  agentForm.value = {
    name: '',
    description: '',
    modelId: null,
    systemPrompt: '',
    kbEnabled: 0,
    kbId: null,
  }
}

// 删除智能体
async function handleDeleteAgent() {
  const agent = agents.value.find(a => a.id === currentAgentId.value)
  if (!agent) return
  try {
    await ElMessageBox.confirm(
      `确定删除智能体「${agent.name}」？此操作不可恢复。`,
      '删除智能体',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
    )
    await deleteAgent(agent.id)
    ElMessage.success('智能体已删除')
    agents.value = agents.value.filter(a => a.id !== agent.id)
    if (currentAgentId.value === agent.id) {
      currentAgentId.value = agents.value.length > 0 ? agents.value[0].id : null
      messages.value = []
      currentConvId.value = null
    }
  } catch (e) { /* 取消 */ }
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
    await clearConversationMemory(currentConvId.value)
    ElMessage.success('记忆已清除')
    messages.value = []
  } catch (e) {
    // 用户取消或失败
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
    const res = await getAgentList(userStore.userId)
    agents.value = res.data || []
    if (agents.value.length > 0 && !currentAgentId.value) {
      if (!localStorage.getItem('onboardingDone_' + userStore.userId) && conversations.value.length === 0) {
        showOnboarding.value = true
      } else {
        currentAgentId.value = agents.value[0].id
      }
    }
  } catch (e) {
    console.error('加载智能体失败:', e)
  }
}

async function loadModelList() {
  try {
    const res = await getModelList()
    modelList.value = res.data || []
  } catch (e) {
    console.error('加载模型列表失败:', e)
  }
}

// 加载会话列表
async function loadConversations() {
  if (!userStore.userId || userStore.userId === 0) return
  try {
    const res = await getConversations(userStore.userId)
    conversations.value = (res.data || []).reverse()
    if (conversations.value.length === 0) {
      newConversation()
    }
  } catch (e) {
    console.error('加载会话失败:', e)
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

// 保存智能体（新建或更新）
async function handleSaveAgent() {
  const valid = await agentFormRef.value.validate().catch(() => false)
  if (!valid) return

  agentCreating.value = true
  try {
    // RAG 开启时检索全部知识库，不绑定特定 KB
    const formData = { ...agentForm.value, kbId: null }
    if (isEditingAgent.value) {
      await updateAgent({
        id: editingAgentId.value,
        ...formData,
      })
      ElMessage.success('智能体更新成功')
    } else {
      await addAgent({
        ...formData,
        status: 1,
      })
      ElMessage.success('智能体创建成功')
    }
    showAgentDialog.value = false
    await loadAgents()
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
  uploadedFiles.value = []
}

// 新建会话
function newConversation() {
  activePanel.value = 'chat'
  messages.value = []
  currentConvId.value = null
  uploadedFiles.value = []
}

// 删除会话
async function handleDeleteConversation(conv) {
  try {
    await ElMessageBox.confirm(`确定删除会话「${conv.title || '新对话'}」？`, '删除确认', { type: 'warning' })
    await deleteConversation(conv.id)
    ElMessage.success('已删除')
    if (currentConvId.value === conv.id) {
      currentConvId.value = null
      messages.value = []
    }
    await loadConversations()
  } catch (e) { /* 取消 */ }
}

// 切换会话
async function switchConversation(conv) {
  activePanel.value = 'chat'
  currentConvId.value = conv.id
  // 切换会话时自动切换到对应的智能体
  if (conv.agentId) {
    const agent = agents.value.find(a => a.id === conv.agentId)
    if (agent) currentAgentId.value = agent.id
  }
  messages.value = []
  try {
    const res = await getConversationRecords(conv.id)
    const records = res.data || []
    messages.value = records.map(r => ({ role: r.role, content: r.content }))
    scrollToBottom()
  } catch (e) {
    console.error('加载历史消息失败:', e)
  }
}


// 显示消息内容：流式消息尾部追加 streamText，非流式消息原样返回
function displayContent(msg, idx) {
  const base = msg.content || ''
  if (streaming.value && idx === messages.value.length - 1 && msg.role === 'assistant') {
    return base + streamText.value
  }
  return base
}

// 启动逐字渲染：RAF 驱动，每帧从 rawBuffer 取字符写入 streamText
function startStreamRender() {
  stopStreamRender()
  rawBuffer = ''
  streamText.value = ''
  let prevContent = ''
  function tick() {
    if (rawBuffer.length > 0) {
      const take = Math.min(3, rawBuffer.length)
      streamText.value += rawBuffer.slice(0, take)
      rawBuffer = rawBuffer.slice(take)
      // 仅在内容真正变化时滚动，避免无意义布局刷
      if (streamText.value !== prevContent) {
        prevContent = streamText.value
        scrollToBottom()
      }
    }
    streamTimer = requestAnimationFrame(tick)
  }
  streamTimer = requestAnimationFrame(tick)
}

// 停止渲染，剩余字符刷入 streamText
function stopStreamRender() {
  if (streamTimer) { cancelAnimationFrame(streamTimer); streamTimer = null }
  if (rawBuffer.length > 0) {
    streamText.value += rawBuffer
    rawBuffer = ''
  }
}

// 最终提交：将 streamText 写入消息 content，重置所有状态
function commitStreamText(msgIdx) {
  stopStreamRender()
  const msg = messages.value[msgIdx]
  if (msg) {
    msg.content += streamText.value
  }
  streamText.value = ''
  rawBuffer = ''
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
  const userMsg = { role: 'user', content: text }
  if (uploadedFiles.value.length > 0) {
    userMsg.files = uploadedFiles.value.map(f => ({
      fileName: f.fileName, fileType: f.fileType, url: f.url, id: f.id
    }))
  }
  messages.value.push(userMsg)
  streaming.value = true
  scrollToBottom()

  messages.value.push({ role: 'assistant', content: '', toolStatus: '', toolResult: '' })
  const msgIndex = messages.value.length - 1
  scrollToBottom()

  const fileIds = uploadedFiles.value.map(f => f.id)

  // 启动逐字渲染
  startStreamRender()

  // 创建 AbortController 用于终止请求
  abortController.value = new AbortController()

  try {
    await chatStream(
      {
        agentId: currentAgentId.value,
        userId: userStore.userId,
        message: text,
        conversationId: currentConvId.value,
        memoryEnabled: memoryEnabled.value,
        fileIds,
      },
      (event) => {
        switch (event.type) {
          case 'conv_id':
            currentConvId.value = Number(event.data)
            loadConversations()
            break
          case 'content':
            rawBuffer += event.data
            break
          case 'tool_call':
            messages.value[msgIndex].toolStatus = `正在调用工具: ${event.data.name}...`
            scrollToBottom()
            break
          case 'tool_result':
            messages.value[msgIndex].toolStatus = ''
            messages.value[msgIndex].toolResult = `${event.data.name}: ${event.data.result}`
            scrollToBottom()
            break
          case 'done':
            commitStreamText(msgIndex)
            streaming.value = false
            retryText.value = ''
            break
          case 'error':
            commitStreamText(msgIndex)
            messages.value[msgIndex].content += `\n\n❌ 错误: ${event.data}`
            streaming.value = false
            break
        }
      },
      abortController.value.signal
    )
    // 正常结束，清除重试标记
    retryText.value = ''
  } catch (e) {
    commitStreamText(msgIndex)
    if (e.name === 'AbortError') {
      messages.value[msgIndex].content += '\n\n⏹️ 已终止'
      retryText.value = ''
    } else {
      messages.value[msgIndex].content += '\n\n❌ 连接断开，回复不完整'
      retryText.value = text
    }
    streaming.value = false
  }
  // 发送后清除已上传的文件
  uploadedFiles.value = []
}

// 重新发送断线的消息
function retryLastMessage() {
  const text = retryText.value
  if (!text) return
  retryText.value = ''
  inputMessage.value = text
  // 移除最后一条空白 AI 回复（如果有）
  if (messages.value.length > 0 && messages.value[messages.value.length - 1].role === 'assistant') {
    messages.value.pop()
  }
  nextTick(() => sendMessage())
}

// 手动终止流式回复
function stopStream() {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  stopStreamRender()
  streaming.value = false
}

// 退出登录
function handleLogout() {
  userStore.logout()
  router.push('/login')
}

// 使用帮助引导
const showHelpTutorial = ref(false)
const helpStep = ref(1)
const agentCardRef = ref(null)
const helpCardRef = ref(null)
const helpCard = reactive({ visible: false, style: {} })
let helpAnimating = false

const recentConvs = computed(() => {
  return conversations.value.slice(-5).reverse()
})

function formatConvTime(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

function showHelp() {
  helpStep.value = 1
  showHelpTutorial.value = true
  animateCardIn(agentCardRef.value, 560)
}

function advanceHelp() {
  if (helpStep.value === 1) {
    animateCardOut()
  }
}

function handleHelpBgClick() {
  if (helpAnimating) return
  // 点击背景就缩回去
  animateCardOut()
}

// 从指定元素位置飞到屏幕中间（类似新手引导的飞行动画）
function animateCardIn(source, cardW) {
  const el = typeof source === 'string' ? document.querySelector(source) : source
  if (!el) return
  const src = el.getBoundingClientRect()
  const cw = cardW || 380
  const cx = (window.innerWidth - cw) / 2
  const cy = 120

  // 初始：在 source 位置，极小
  helpCard.style = {
    position: 'fixed',
    left: src.left + 'px',
    top: src.top + 'px',
    width: src.width + 'px',
    transition: 'none',
    opacity: '0',
    transform: 'scale(0.3)',
  }
  helpCard.visible = true
  helpAnimating = true

  // 下一帧：飞到屏幕中间展开
  requestAnimationFrame(() => {
    helpCard.style = {
      position: 'fixed',
      left: cx + 'px',
      top: cy + 'px',
      width: cw + 'px',
      opacity: '1',
      transform: 'scale(1)',
      transition: 'all 1s cubic-bezier(0.34, 1.56, 0.64, 1)',
    }
    setTimeout(() => { helpAnimating = false }, 1050)
  })
}

// 从屏幕中间飞回 source 位置
function animateCardOut() {
  if (helpAnimating) return
  helpAnimating = true

  const source = helpStep.value === 1
    ? (agentCardRef.value || document.querySelector('.agent-card'))
    : document.querySelector(
        helpStep.value === 2 ? '.nav-card-pink'
        : helpStep.value === 3 ? '.nav-card-blue'
        : '.conv-item')
  if (!source) { finishStep(); return }
  const src = source.getBoundingClientRect()

  // 两帧法：先锁定当前状态（取消过渡），下一帧再飞到目标位置
  helpCard.style = { ...helpCard.style, transition: 'none' }

  requestAnimationFrame(() => {
    helpCard.style = {
      position: 'fixed',
      left: src.left + 'px',
      top: src.top + 'px',
      width: src.width + 'px',
      height: src.height + 'px',
      padding: '0',
      opacity: '0',
      transform: 'scale(0.3)',
      transition: 'all 1.2s cubic-bezier(0.4, 0, 0.2, 1)',
    }

    setTimeout(() => {
      highlightSource(source)
      // 等高亮闪烁结束后再进入下一步
      setTimeout(() => {
        if (helpStep.value === 4) {
          helpCard.visible = false
          helpAnimating = false
          closeHelpTutorial()
        } else {
          helpAnimating = false
          finishStep()
        }
      }, 1200)
    }, 1300)
  })
}

function finishStep() {
  if (helpStep.value === 1) {
    helpStep.value = 2
    nextTick(() => animateCardIn('.nav-card-pink', 380))
  } else if (helpStep.value === 2) {
    helpStep.value = 3
    nextTick(() => animateCardIn('.nav-card-blue', 380))
  } else if (helpStep.value === 3) {
    helpStep.value = 4
    nextTick(() => animateCardIn('.conv-item', 380))
  } else {
    closeHelpTutorial()
  }
}

function closeHelpTutorial() {
  showHelpTutorial.value = false
  helpStep.value = 1
  helpCard.visible = false
  helpAnimating = false
}

// 目标卡片高亮闪烁
function highlightSource(el) {
  if (!el) return
  el.classList.add('highlight-flash')
  setTimeout(() => el.classList.remove('highlight-flash'), 1200)
}

function helpSelectAgent(agent) {
  if (agent.id !== currentAgentId.value) {
    currentAgentId.value = agent.id
    onAgentChange()
  }
  animateCardOut()
}

function helpNewConversation() {
  closeHelpTutorial()
  newConversation()
}

function helpOpenConversation(conv) {
  closeHelpTutorial()
  switchConversation(conv)
}

// 快捷提问
function quickQuestion(text) {
  inputMessage.value = text
  sendMessage()
}

// 从面板选择智能体
function selectAgentFromPanel(agent) {
  currentAgentId.value = agent.id
  showAgentPanel.value = false
  onAgentChange()
}

// ===== 新用户引导：选择智能体 =====
const showOnboarding = ref(false)
const cardRefs = reactive({})
const agentSelectRef = ref(null)
const flyingCard = reactive({ visible: false, emoji: '', text: '', style: {} })

const AGENT_EMOJI_MAP = {
  '小说': '📖', 'java': '☕', 'python': '🐍', '前端': '🎨',
  '翻译': '🌍', '英语': '🇬🇧', '文案': '✍️', '设计': '🎯',
  '数据': '📊', '客服': '💁', '写作': '✏️', '代码': '💻',
  '法律': '⚖️', '财务': '💰', '医疗': '🏥', '教育': '📚',
}

function getAgentEmoji(name) {
  for (const [key, emoji] of Object.entries(AGENT_EMOJI_MAP)) {
    if (name.toLowerCase().includes(key.toLowerCase())) return emoji
  }
  return '🤖'
}

function getModelName(modelId) {
  const m = modelMap.value[modelId]
  return m ? m.name : '未知模型'
}

function selectAgentWithAnimation(agent) {
  const cardEl = cardRefs[agent.id]
  if (!cardEl || !agentSelectRef.value) {
    currentAgentId.value = agent.id
    showOnboarding.value = false
    return
  }
  const start = cardEl.getBoundingClientRect()
  const end = agentSelectRef.value.getBoundingClientRect()

  flyingCard.emoji = getAgentEmoji(agent.name)
  flyingCard.text = agent.name
  flyingCard.style = {
    left: start.left + 'px',
    top: start.top + 'px',
    width: start.width + 'px',
    height: start.height + 'px',
  }
  flyingCard.visible = true

  cardEl.style.opacity = '0'

  requestAnimationFrame(() => {
    flyingCard.style = {
      left: (end.left + end.width / 2 - 30) + 'px',
      top: end.top + 'px',
      width: '60px',
      height: '40px',
      transform: 'scale(0.25)',
      opacity: '0.6',
      transition: 'all 0.5s cubic-bezier(0.4, 0, 0.2, 1)',
    }
  })

  setTimeout(() => {
    flyingCard.visible = false
    showOnboarding.value = false
    currentAgentId.value = agent.id
    localStorage.setItem('onboardingDone_' + userStore.userId, 'true')
    // 恢复卡片透明度
    if (cardEl) cardEl.style.opacity = ''
  }, 550)
}

function skipOnboarding() {
  showOnboarding.value = false
  localStorage.setItem('onboardingDone_' + userStore.userId, 'true')
}

onMounted(async () => {
  // 先加载会话，再加载智能体（引导弹窗依赖 conversations 判断）
  await loadConversations()
  await loadAgents()
  await loadModelList()
  loadAnnouncement()
})
</script>

<style scoped>
/* ================================
   Chat 页面 — Handwritten Sketch
   ================================ */

.chat-layout {
  display: flex;
  height: 100vh;
  background: var(--bg-page);
}

/* ===== 左侧边栏 ===== */
.sidebar {
  width: 280px;
  background: #f5f0e8;
  border-right: 3px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 3px solid var(--border-color);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
}
.logo-icon span {
  font-family: var(--font-marker);
  font-size: 18px;
  color: #fff;
  line-height: 1;
}
.logo {
  overflow: hidden;
}
.logo h2 {
  font-family: var(--font-marker);
  font-size: 20px;
  color: var(--fg-default);
  margin: 0;
  flex-shrink: 0;
}
.logo-watermark {
  font-family: var(--font-marker);
  font-size: 14px;
  color: rgba(45, 45, 45, 0.25);
  white-space: nowrap;
  flex: 1;
  overflow: hidden;
  text-overflow: clip;
  pointer-events: none;
  user-select: none;
  letter-spacing: 4px;
  line-height: 1;
  transform: rotate(-2deg);
}

/* ===== 智能体卡片 ===== */
.agent-card {
  margin: 14px 16px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(135deg, #fed7aa 0%, #fdba74 100%);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.agent-card:hover {
  box-shadow: var(--shadow-hard);
  transform: translateY(-2px);
}
.agent-card:active {
  transform: translateY(0);
}
.agent-card-avatar {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  background: rgba(251,146,60,0.15);
  border: 2px solid var(--border-color);
  flex-shrink: 0;
  overflow: hidden;
}
.agent-avatar-img {
  width: 100%; height: 100%;
  object-fit: cover;
  display: block;
}
.agent-card-info {
  flex: 1;
  min-width: 0;
}
.agent-card-name {
  font-family: var(--font-marker);
  font-size: 16px;
  color: var(--fg-default);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.3;
}
.agent-card-model {
  font-family: var(--font-hand);
  font-size: 11px;
  color: var(--primary);
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.agent-card-hint {
  font-family: var(--font-hand);
  font-size: 12px;
  color: var(--fg-muted);
  line-height: 1;
}
.agent-card-arrow {
  color: var(--fg-muted);
  flex-shrink: 0;
  transition: transform 0.2s;
}
.agent-card:hover .agent-card-arrow {
  color: var(--primary);
  transform: translateX(3px);
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: rgba(0,0,0,0.02);
}

.empty-sidebar-tip {
  text-align: center;
  color: var(--fg-muted);
  padding: 20px 0;
  font-family: var(--font-hand);
  font-size: 15px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 3px solid #e8dba0;
  cursor: pointer;
  font-family: var(--font-hand);
  font-size: 15px;
  color: var(--fg-default);
  transition: all 0.2s;
  margin-bottom: 6px;
  background: #fef9e7;
  box-shadow: var(--shadow-hard-sm);
}
.conv-item:hover {
  background: #fef5d4;
  border-color: #dcc880;
  box-shadow: var(--shadow-hard);
  transform: translateX(2px);
}
.conv-item.active {
  background: #fce88d;
  border-color: #d4a830;
  box-shadow: var(--shadow-hard);
  color: var(--fg-default);
  transform: translateX(4px);
}
.conv-item.active .conv-title {
  font-family: var(--font-marker);
}

.conv-item.placeholder {
  cursor: default;
  opacity: 0.9;
}
.conv-item.placeholder .conv-title {
  font-family: var(--font-marker);
  color: var(--fg-muted);
}
.new-badge {
  flex-shrink: 0;
  font-family: var(--font-marker);
  font-size: 11px;
  padding: 0 6px;
  line-height: 18px;
}

.conv-title-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.conv-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-model {
  font-family: var(--font-hand);
  font-size: 10px;
  color: var(--primary);
  opacity: 0.7;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.memory-dot {
  width: 8px;
  height: 8px;
  border: 2px solid var(--border-color);
  background: var(--success);
  flex-shrink: 0;
}

.conv-delete-btn {
  opacity: 0;
  transition: all 0.2s;
  flex-shrink: 0;
  margin-left: auto;
  color: var(--fg-muted) !important;
}
.conv-item:hover .conv-delete-btn {
  opacity: 1;
}
.conv-item.active .conv-delete-btn {
  color: #8a6d00 !important;
}

/* 底部导航 */
.sidebar-footer {
  padding: 12px 16px;
  border-top: 3px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: transparent;
}
.nav-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  cursor: pointer;
  transition: all 0.2s;
}
.nav-card:hover {
  box-shadow: var(--shadow-hard);
  transform: translateY(-2px);
}
.nav-card:hover .nav-card-arrow {
  color: var(--primary);
  transform: translateX(3px);
}
.nav-card:active {
  transform: translateY(0);
}
.nav-card-danger:hover {
  background: var(--primary);
  border-color: var(--border-color);
}
.nav-card-danger:hover .nav-card-title,
.nav-card-danger:hover .nav-card-desc,
.nav-card-danger:hover .nav-card-icon {
  color: #fff;
}
.nav-card-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(45,45,45,0.04);
  border: 2px solid var(--border-color);
  flex-shrink: 0;
  color: var(--fg-default);
}
.nav-card-text {
  flex: 1;
  min-width: 0;
}
.nav-card-title {
  font-family: var(--font-marker);
  font-size: 15px;
  color: var(--fg-default);
  line-height: 1.2;
}
.nav-card-desc {
  font-family: var(--font-hand);
  font-size: 11px;
  color: var(--fg-muted);
  line-height: 1;
}
.nav-card-arrow {
  color: var(--fg-muted);
  flex-shrink: 0;
  transition: all 0.2s;
}
.nav-card-pink {
  background: linear-gradient(135deg, #bfdbfe 0%, #93c5fd 100%);
}
.nav-card-pink:hover {
  background: linear-gradient(135deg, #93c5fd 0%, #60a5fa 100%);
}
.nav-card-blue {
  background: linear-gradient(135deg, #a7f3d0 0%, #6ee7b7 100%);
}
.nav-card-blue:hover {
  background: linear-gradient(135deg, #6ee7b7 0%, #34d399 100%);
}
.nav-card-danger {
  background: linear-gradient(135deg, #fecaca 0%, #fca5a5 100%);
  border-color: var(--border-color);
}
.nav-card-danger:hover {
  background: var(--primary);
  border-color: var(--border-color);
}

/* ===== 右侧聊天区 ===== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

/* 聊天工具栏 */
.chat-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: var(--bg-card);
  border-bottom: 3px solid var(--border-color);
  position: relative;
}
.chat-toolbar::after {
  content: '';
  position: absolute;
  top: -8px;
  left: 40%;
  width: 80px;
  height: 18px;
  background: rgba(45, 45, 45, 0.06);
  transform: rotate(1deg);
  pointer-events: none;
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ===== 公告按钮（居中） ===== */
.toolbar-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  z-index: 10;
}
.announcement-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  background: var(--bg-card);
  border: 2px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  cursor: pointer;
  font-family: var(--font-marker);
  font-size: 14px;
  color: var(--fg-default);
  transition: all 0.2s;
  position: relative;
}
.announcement-btn:hover {
  box-shadow: var(--shadow-hard);
  transform: translateY(-1px);
}
.announcement-dot {
  width: 8px;
  height: 8px;
  background: var(--primary);
  border: 1px solid var(--border-color);
  position: absolute;
  top: 4px;
  right: 4px;
  animation: announcement-pulse 1.5s ease-in-out infinite;
}
@keyframes announcement-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
.announcement-popup {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  width: 420px;
  max-height: 320px;
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-lg);
  z-index: 999;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.announcement-fade-enter-active,
.announcement-fade-leave-active {
  transition: all 0.25s ease-out;
}
.announcement-fade-enter-from,
.announcement-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-8px);
}
.announcement-popup-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 3px solid var(--border-color);
  background: var(--bg-page);
}
.announcement-popup-title {
  flex: 1;
  font-family: var(--font-marker);
  font-size: 16px;
  color: var(--fg-default);
}
.announcement-popup-content {
  padding: 16px;
  font-family: var(--font-hand);
  font-size: 14px;
  color: var(--fg-default);
  line-height: 1.7;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
  position: relative;
}
.announcement-watermark {
  position: absolute;
  inset: 0;
  pointer-events: none;
  user-select: none;
  overflow: hidden;
  font-size: 14px;
  color: rgba(45, 45, 45, 0.15);
  font-family: var(--font-marker);
  line-height: 1.6;
  letter-spacing: 4px;
  transform: rotate(-5deg);
}
.announcement-watermark::before {
  content: attr(data-text);
  display: block;
  word-break: break-all;
  padding: 4px;
}

/* ===== 使用帮助卡片（五彩闪烁） ===== */
.help-card {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border: 2px solid var(--border-color);
  cursor: pointer;
  position: relative;
  font-family: var(--font-marker);
  font-size: 14px;
  color: #fff;
  animation: help-rainbow 1.8s linear infinite;
  z-index: 1;
  overflow: visible;
}
.help-card::after {
  content: '👈 点我';
  position: absolute;
  top: -28px;
  right: -10px;
  font-family: var(--font-marker);
  font-size: 13px;
  color: var(--primary);
  background: var(--bg-card);
  border: 2px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  padding: 3px 10px;
  white-space: nowrap;
  animation: help-tag-float 1.6s ease-in-out infinite;
  transform: rotate(4deg);
  pointer-events: none;
  line-height: 1.4;
}
.help-card:hover {
  animation: help-rainbow 0.6s linear infinite;
  transform: scale(1.05);
}
.help-card:hover::after {
  display: none;
}
@keyframes help-tag-float {
  0%, 100% { transform: translateY(0) rotate(4deg); }
  50% { transform: translateY(-6px) rotate(4deg); }
}
@keyframes help-rainbow {
  0%   { background: #ff4d4d; box-shadow: 0 0 12px #ff4d4d; }
  16%  { background: #ff8c00; box-shadow: 0 0 12px #ff8c00; }
  33%  { background: #ffd700; box-shadow: 0 0 12px #ffd700; }
  50%  { background: #2ecc71; box-shadow: 0 0 12px #2ecc71; }
  66%  { background: #3498db; box-shadow: 0 0 12px #3498db; }
  83%  { background: #9b59b6; box-shadow: 0 0 12px #9b59b6; }
  100% { background: #ff4d4d; box-shadow: 0 0 12px #ff4d4d; }
}

.memory-switch {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 14px;
  background: var(--sidebar-bg);
  border: 2px solid var(--border-color);
}
.switch-label {
  font-family: var(--font-hand);
  font-size: 14px;
  color: var(--fg-default);
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
  color: var(--fg-muted);
}
.empty-tip h3 {
  font-family: var(--font-marker);
  font-size: 22px;
  color: var(--fg-default);
  margin: 16px 0 8px;
}
.empty-tip p {
  font-size: 16px;
  color: var(--fg-muted);
}

.welcome-icon {
  margin-bottom: 8px;
}
.welcome-emoji {
  font-size: 48px;
  line-height: 1;
}
.welcome-desc {
  font-size: 15px !important;
  margin-bottom: 24px !important;
}
.welcome-suggestions {
  margin-bottom: 12px;
}
.suggestion-label {
  font-family: var(--font-hand);
  font-size: 13px;
  color: var(--fg-muted);
  display: block;
  margin-bottom: 8px;
}
.suggestion-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}
.suggestion-chip {
  display: inline-block;
  padding: 8px 16px;
  background: var(--bg-card);
  border: 2px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  font-family: var(--font-hand);
  font-size: 14px;
  color: var(--fg-default);
  cursor: pointer;
  transition: all 0.2s;
}
.suggestion-chip:hover {
  background: var(--primary);
  color: #fff;
  transform: translateY(-2px);
  box-shadow: var(--shadow-hard);
}

.memory-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--fg-muted);
  font-size: 14px;
  margin-top: 12px;
}

/* ===== 消息气泡 ===== */
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
  padding: 14px 18px;
  font-family: var(--font-hand);
  font-size: 15px;
  line-height: 1.6;
  word-break: break-word;
  border: 3px solid var(--border-color);
}

.message.user .message-content {
  background: var(--primary);
  color: #fff;
  border-radius: 15px 225px 15px 255px / 255px 15px 225px 15px;
  box-shadow: var(--shadow-hard-sm);
}

.message.assistant .message-content {
  background: #fef5d4;
  color: var(--fg-default);
  border-radius: 20px 225px 15px 255px / 255px 15px 225px 15px;
  box-shadow: var(--shadow-hard);
  border-color: #e8dba0;
}

.tool-status {
  color: var(--primary);
  font-family: var(--font-hand);
  font-size: 14px;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.tool-result {
  margin-bottom: 8px;
}

/* Markdown 内容 */
.markdown-body :deep(p) { margin: 0 0 8px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(code) {
  background: var(--sidebar-bg);
  padding: 2px 6px;
  border: 2px solid var(--border-color);
  font-family: var(--font-hand);
  font-size: 14px;
}
.markdown-body :deep(pre) {
  background: var(--sidebar-bg);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  padding: 12px;
  overflow-x: auto;
  margin: 8px 0;
}
.markdown-body :deep(pre code) {
  background: none;
  border: none;
  padding: 0;
}

/* ===== 打字指示器 ===== */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}
.typing-indicator span {
  width: 10px;
  height: 10px;
  background: var(--border-color);
  border: 2px solid var(--border-color);
  animation: bounce 1.4s infinite ease-in-out both;
}
.typing-indicator span:nth-child(1) { animation-delay: 0s; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); background: var(--border-color); }
  40% { transform: scale(1); background: var(--primary); }
}

/* ===== 输入区域 ===== */
.chat-input {
  padding: 16px 24px;
  border-top: 3px solid var(--border-color);
  background: var(--bg-card);
}

/* ===== 智能体管理面板 ===== */
.agent-panel-overlay {
  position: fixed;
  inset: 0;
  z-index: 5000;
  background: transparent;
}
.agent-panel {
  width: 380px;
  height: 100%;
  position: fixed;
  top: 0;
  right: 0;
  z-index: 5001;
  background: var(--bg-page);
  border-left: 3px solid var(--border-color);
  box-shadow: -6px 0 0 rgba(45,45,45,0.1);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  animation: panel-in 0.3s ease-out;
}
@keyframes panel-in {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

/* 面板头部 */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 3px solid var(--border-color);
  background: var(--bg-card);
}
.panel-title {
  font-family: var(--font-marker);
  font-size: 20px;
  color: var(--fg-default);
  margin: 0;
}

/* 当前智能体详情 */
.panel-current {
  margin: 16px;
  padding: 20px;
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}
.panel-current-avatar {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: rgba(255,77,77,0.08);
  border: 2px solid var(--border-color);
  overflow: hidden;
}
.panel-current-info {
  flex: 1;
  min-width: 140px;
}
.panel-current-name {
  font-family: var(--font-marker);
  font-size: 18px;
  color: var(--fg-default);
}
.panel-current-model {
  font-family: var(--font-hand);
  font-size: 12px;
  color: var(--primary);
  margin: 2px 0;
}
.panel-current-desc {
  font-family: var(--font-hand);
  font-size: 13px;
  color: var(--fg-muted);
  margin-top: 2px;
  line-height: 1.4;
}
.panel-current-actions {
  display: flex;
  gap: 8px;
  width: 100%;
}
.panel-current-actions .el-button {
  flex: 1;
  font-family: var(--font-hand);
  border: 2px solid var(--border-color);
}
.panel-current-actions .btn-delete {
  color: var(--primary);
  border-color: var(--primary);
}

/* 智能体列表 */
.panel-section {
  flex: 1;
  padding: 16px;
}
.panel-section-title {
  font-family: var(--font-marker);
  font-size: 15px;
  color: var(--fg-muted);
  margin-bottom: 10px;
  padding: 0 4px;
}
.panel-agent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}
.panel-agent-item:hover {
  background: rgba(45,45,45,0.04);
  border-color: var(--border-color);
  box-shadow: var(--shadow-hard-sm);
}
.panel-agent-item.active {
  background: #fef5d4;
  border-color: var(--border-color);
  box-shadow: var(--shadow-hard-sm);
}
.panel-agent-emoji {
  font-size: 20px;
  width: 32px;
  text-align: center;
  flex-shrink: 0;
}
.panel-agent-name {
  flex: 1;
  font-family: var(--font-hand);
  font-size: 15px;
  color: var(--fg-default);
}
.panel-agent-tag {
  font-family: var(--font-marker);
  font-size: 11px;
  border: 2px solid var(--border-color) !important;
}

/* 新建按钮 */
.panel-create-btn {
  margin: 8px 16px 24px;
  font-family: var(--font-marker);
  font-size: 15px;
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
}

/* 面板滑入动画 */
.panel-slide-enter-active {
  animation: panel-in 0.3s ease-out;
}
.panel-slide-leave-active {
  animation: panel-in 0.25s ease-in reverse;
}

/* 飞行动画 */
.flying-card {
  position: fixed;
  z-index: 10000;
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard);
  padding: 6px 10px;
  pointer-events: none;
  overflow: hidden;
}

/* ===== 新用户引导 ===== */
.onboarding-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}
.onboarding-bg {
  position: absolute;
  inset: 0;
  background: rgba(45, 45, 45, 0.7);
  backdrop-filter: blur(4px);
}
.onboarding-container {
  position: relative;
  max-width: 640px;
  width: 90%;
  background: var(--bg-page);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-lg);
  padding: 48px 40px 36px;
  text-align: center;
  animation: onboarding-in 0.4s ease-out;
}
@keyframes onboarding-in {
  from { transform: translateY(30px) scale(0.95); opacity: 0; }
  to { transform: translateY(0) scale(1); opacity: 1; }
}
.onboarding-title {
  font-family: var(--font-marker);
  font-size: 26px;
  color: var(--fg-default);
  margin: 0 0 8px;
}
.onboarding-desc {
  font-family: var(--font-hand);
  font-size: 15px;
  color: var(--fg-muted);
  margin: 0 0 32px;
}
.onboarding-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}
.onboarding-card {
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  padding: 20px 14px;
  cursor: pointer;
  transition: all 0.2s ease-out;
  position: relative;
  border-radius: 20px 225px 15px 255px / 255px 15px 225px 15px;
}
.onboarding-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-hard-lg);
  border-color: var(--primary);
}
.onboarding-card:active {
  transform: translateY(-2px) scale(0.97);
}
.onboarding-avatar {
  font-size: 36px;
  line-height: 1;
  margin-bottom: 10px;
}
.onboarding-name {
  font-family: var(--font-marker);
  font-size: 15px;
  color: var(--fg-default);
  margin-bottom: 6px;
}
.onboarding-desc-text {
  font-family: var(--font-hand);
  font-size: 12px;
  color: var(--fg-muted);
  line-height: 1.4;
}
.onboarding-skip {
  font-family: var(--font-hand);
  font-size: 14px;
  color: var(--fg-muted);
  background: none;
  border: none;
  text-decoration: underline wavy;
  text-underline-offset: 3px;
  cursor: pointer;
  transition: color 0.2s;
}
.onboarding-skip:hover {
  color: var(--fg-default);
}

.flying-avatar {
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
}
.flying-label {
  font-family: var(--font-marker);
  font-size: 12px;
  color: var(--fg-default);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===== 使用帮助引导 ===== */
.help-overlay {
  position: fixed;
  inset: 0;
  z-index: 9998;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: helpOverlayIn 0.3s ease both;
}

@keyframes helpOverlayIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.help-bg {
  position: absolute;
  inset: 0;
  background: rgba(45,45,45,0.6);
  backdrop-filter: blur(3px);
}
.help-title {
  font-family: var(--font-marker);
  font-size: 26px;
  color: #fff;
  text-align: center;
  margin-bottom: 8px;
  text-shadow: 2px 2px 0 rgba(0,0,0,0.3);
}
.help-desc {
  font-family: var(--font-hand);
  font-size: 16px;
  color: rgba(255,255,255,0.85);
  text-align: center;
  margin-bottom: 28px;
}
.help-skip {
  display: block;
  margin: 20px auto 0;
  font-family: var(--font-hand);
  font-size: 14px;
  color: rgba(255,255,255,0.6);
  background: none;
  border: none;
  text-decoration: underline wavy;
  text-underline-offset: 3px;
  cursor: pointer;
  transition: color 0.2s;
}
.help-skip:hover { color: #fff; }

.help-agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
  max-width: 560px;
}
.help-agent-card {
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard);
  padding: 22px 16px;
  cursor: pointer;
  transition: all 0.25s ease-out;
  text-align: center;
}
.help-agent-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-hard-lg);
  border-color: var(--primary);
}
.help-agent-card:active {
  transform: translateY(-2px) scale(0.97);
}
.help-agent-emoji { font-size: 38px; line-height: 1; margin-bottom: 10px; }
.help-agent-name {
  font-family: var(--font-marker);
  font-size: 16px;
  color: var(--fg-default);
  margin-bottom: 4px;
}
.help-agent-desc {
  font-family: var(--font-hand);
  font-size: 12px;
  color: var(--fg-muted);
  line-height: 1.4;
}

/* ===== 过渡卡片（Step 2 & 3 共用） ===== */
/* 嵌入面板 */
.embedded-panel {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-page);
  display: flex;
  flex-direction: column;
}
.embedded-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 3px solid var(--border-color);
  background: var(--bg-card);
}
.embedded-header h2 {
  font-family: var(--font-marker);
  font-size: 18px;
  color: var(--fg-default);
  margin: 0;
}
.embedded-panel .stats-header .el-button.text {
  display: none;
}

/* 个人文档面板 */
.user-docs-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
.upload-area {
  border: 3px dashed var(--border-color);
  border-radius: 0;
  padding: 30px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--bg-card);
  margin-bottom: 20px;
}
.upload-area:hover {
  border-color: var(--primary);
  background: rgba(255,77,77,0.03);
}
.upload-text {
  font-family: var(--font-hand);
  font-size: 15px;
  color: var(--fg-default);
  margin: 8px 0 4px;
}
.upload-hint {
  font-family: var(--font-hand);
  font-size: 12px;
  color: var(--fg-muted);
  margin: 0;
}
.doc-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.doc-empty {
  text-align: center;
  padding: 40px 0;
  font-family: var(--font-hand);
  color: var(--fg-muted);
  font-size: 14px;
}
.doc-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  transition: all 0.2s;
}
.doc-item:hover {
  box-shadow: var(--shadow-hard);
}
.doc-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(45,45,45,0.05);
  border: 2px solid var(--border-color);
  flex-shrink: 0;
}
.doc-info {
  flex: 1;
  min-width: 0;
}
.doc-name {
  font-family: var(--font-marker);
  font-size: 14px;
  color: var(--fg-default);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
.doc-size {
  font-family: var(--font-hand);
  font-size: 11px;
  color: var(--fg-muted);
}
.doc-status {
  font-family: var(--font-hand);
}
.doc-delete-btn {
  flex-shrink: 0;
}

.help-trans-card {
  position: fixed;
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-lg);
  z-index: 9999;
  overflow: hidden;
  padding: 32px 36px;
}
.trans-content {
  text-align: center;
}
.rag-icon { font-size: 48px; margin-bottom: 10px; }
.rag-title {
  font-family: var(--font-marker);
  font-size: 22px;
  color: var(--fg-default);
  margin-bottom: 12px;
}
.rag-text {
  font-family: var(--font-hand);
  font-size: 15px;
  color: var(--fg-muted);
  line-height: 1.7;
  margin-bottom: 14px;
}
.rag-arrow-hint {
  font-family: var(--font-marker);
  font-size: 15px;
  color: var(--primary);
  animation: rag-bounce-left 1s ease-in-out infinite alternate;
}
@keyframes rag-bounce-left {
  0%   { transform: translateX(0); }
  100% { transform: translateX(-8px); }
}

/* Step 3 对话选择 */
.conv-step-title {
  font-family: var(--font-marker);
  font-size: 24px;
  color: var(--fg-default);
  margin-bottom: 4px;
}
.conv-step-desc {
  font-family: var(--font-hand);
  font-size: 15px;
  color: var(--fg-muted);
  margin-bottom: 20px;
}
.help-conv-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.help-conv-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  cursor: pointer;
  transition: all 0.25s ease-out;
  text-align: left;
}
.help-conv-card:hover {
  box-shadow: var(--shadow-hard);
  transform: translateY(-3px);
  border-color: var(--primary);
}
.help-conv-icon {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: rgba(255,77,77,0.08);
  border: 2px solid var(--border-color);
  flex-shrink: 0;
}
.help-conv-label {
  flex: 1;
  font-family: var(--font-marker);
  font-size: 15px;
  color: var(--fg-default);
}
.help-conv-sub {
  font-family: var(--font-hand);
  font-size: 12px;
  color: var(--fg-muted);
}

/* ===== 高亮闪烁动画（引导飞回时触发） ===== */
@keyframes highlight-pulse {
  0% { outline: 0 solid rgba(255,77,77,0); outline-offset: 0; }
 15% { outline: 5px solid rgba(255,77,77,0.85); outline-offset: -2px; }
 50% { outline: 5px solid rgba(255,77,77,0.85); outline-offset: -2px; }
100% { outline: 0 solid rgba(255,77,77,0); outline-offset: 0; }
}
.highlight-flash {
  animation: highlight-pulse 1.1s ease-out;
  position: relative;
  z-index: 1;
}

/* ===== 对话文件上传 ===== */
.input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.input-row .el-input {
  flex: 1;
}
.upload-btn {
  border: 2px solid var(--border-color);
  flex-shrink: 0;
}
.retry-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 16px;
  margin-bottom: 8px;
  background: #fffbeb;
  border: 2px solid #f59e0b;
  border-radius: 8px;
  font-size: 14px;
}
.stop-btn {
  font-weight: bold;
  border: 2px solid #ff4d4d;
}
.chat-file-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  padding: 8px;
  background: rgba(45,45,45,0.03);
  border: 2px dashed var(--border-color);
}
.chat-file-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: var(--bg-card);
  border: 2px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  font-family: var(--font-hand);
  font-size: 12px;
  max-width: 200px;
}
.chat-file-thumb {
  width: 32px;
  height: 32px;
  object-fit: cover;
  border: 1px solid var(--border-color);
  flex-shrink: 0;
}
.chat-file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

/* ===== 消息气泡中的文件显示 ===== */
.msg-files {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.msg-file {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: rgba(45,45,45,0.05);
  border: 2px solid var(--border-color);
  font-family: var(--font-hand);
  font-size: 12px;
}
.message.user .msg-file {
  background: rgba(255,255,255,0.2);
  border-color: rgba(255,255,255,0.4);
}
.msg-file-img {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border: 1px solid var(--border-color);
}
.msg-file-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

