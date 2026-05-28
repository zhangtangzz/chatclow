<template>
  <div class="admin-layout">
    <!-- 左侧菜单 -->
    <div class="admin-sidebar">
      <div class="sidebar-title">ChatClow 管理后台</div>
      <div
          v-for="item in menu"
          :key="item.key"
          class="sidebar-item"
          :class="{ active: activeMenu === item.key }"
          @click="activeMenu = item.key"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </div>
      <div class="sidebar-bottom">
        <div class="sidebar-item" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出登录</span>
        </div>
        <div class="sidebar-user">
          {{ userStore.username }}（管理员）
        </div>
      </div>
    </div>

    <!-- 右侧内容 -->
    <div class="admin-content">
      <!-- 用户管理 -->
      <div v-if="activeMenu === 'users'" class="content-panel">
        <div class="panel-header">
          <h2>用户管理</h2>
          <el-button type="primary" @click="showAddUserDialog = true">
            <el-icon><Plus /></el-icon> 新建用户
          </el-button>
        </div>
        <el-table :data="userList" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 2 ? 'danger' : 'info'">
                {{ row.role === 2 ? '管理员' : '普通用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totals" label="剩余次数" width="100" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button size="small" @click="openEditUser(row)">编辑</el-button>
              <el-button size="small" @click="viewUserConversations(row)">查看对话</el-button>
              <el-button size="small" type="danger" @click="handleDeleteUser(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 知识库管理（管理员视图） -->
      <div v-if="activeMenu === 'kb'" class="content-panel">
        <div class="panel-header">
          <h2>知识库管理</h2>
          <el-button type="primary" @click="showAddKbDialog = true">
            <el-icon><Plus /></el-icon> 新建知识库
          </el-button>
        </div>
        <el-table :data="kbList" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="kbName" label="知识库名称" />
          <el-table-column prop="description" label="描述" />
          <el-table-column label="存储后端" width="130">
            <template #default="{ row }">
              <el-tag>{{ getStoreName(row.vectorStoreType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="上次修改时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.updatedDt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button size="small" @click="openEditKb(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteKb(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 用户对话查看 -->
      <div v-if="activeMenu === 'conversations'" class="content-panel">
        <div class="panel-header">
          <h2>用户对话记录 — {{ currentViewUser?.username }}</h2>
          <el-button @click="activeMenu = 'users'">返回用户列表</el-button>
        </div>
        <el-table :data="conversationList" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="对话标题" />
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.createdDt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" @click="viewConversationDetail(row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 大模型管理 -->
      <div v-if="activeMenu === 'models'" class="content-panel model-panel">
        <!-- 模型提供商卡片 -->
        <div class="model-providers">
          <div class="provider-header">
            <h3>模型提供商</h3>
            <el-input v-model="modelSearch" placeholder="搜索提供商名称/Key" prefix-icon="Search" class="provider-search" clearable />
            <el-button type="primary" @click="openAddModelDialog">
              <el-icon><Plus /></el-icon> 新增提供商
            </el-button>
          </div>
          <div class="provider-grid">
            <div v-for="provider in providerList" :key="provider.name" class="provider-card">
              <div class="provider-top">
                <div class="provider-name">{{ provider.name }}</div>
                <el-switch
                  v-model="provider.status"
                  :active-value="1"
                  :inactive-value="0"
                  @change="v => toggleModelStatusByProvider(provider, v)"
                />
              </div>
              <div class="provider-desc">{{ provider.desc }}</div>
              <div class="provider-actions">
                <el-button text size="small" @click="viewProviderDetail(provider)">详情</el-button>
                <el-button text size="small" @click="editProvider(provider)">编辑</el-button>
                <el-button text size="small" type="danger" @click="deleteProvider(provider)">删除</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 大模型配置表格 -->
        <div class="model-table-wrap">
          <div class="model-table-header">
            <h3>模型配置</h3>
            <div class="model-table-actions">
              <el-button size="small" @click="loadModelList">
                <el-icon><RefreshRight /></el-icon> 刷新
              </el-button>
              <el-button size="small" type="primary" @click="openAddModelDialog">
                <el-icon><Plus /></el-icon> 新增
              </el-button>
            </div>
          </div>
          <el-table :data="modelList" border stripe style="width: 100%">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="name" label="模型名称" min-width="140" />
            <el-table-column prop="provider" label="模型提供商" width="130" />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.modelType === 'embedding' ? 'warning' : ''">
                  {{ row.modelType === 'embedding' ? '向量模型' : '对话模型' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="modelCode" label="模型Key" width="180" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-switch
                  v-model="row.status"
                  :active-value="1"
                  :inactive-value="0"
                  @change="() => toggleModelStatus(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="默认模型" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault === 1" size="small" type="warning">默认</el-tag>
                <span v-else style="color:#c0c4cc">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" text @click="openEditModel(row)">编辑</el-button>
                <el-button size="small" text @click="setDefaultModel(row)">设为默认</el-button>
                <el-button size="small" text type="danger" @click="handleDeleteModel(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 智能体管理 -->
      <div v-if="activeMenu === 'agents'" class="content-panel agent-panel">

        <!-- ====== 智能体列表视图 ====== -->
        <template v-if="!viewingAgent">
          <div class="panel-header">
            <div>
              <h2>智能体</h2>
              <p class="panel-subtitle">企业智能体</p>
            </div>
            <div class="agent-toolbar">
              <el-input
                v-model="agentSearch"
                placeholder="搜索智能体名称或描述"
                prefix-icon="Search"
                class="agent-search"
                clearable
              />
              <el-dropdown @command="handleAgentSort">
                <el-button>
                  <el-icon><Sort /></el-icon> {{ agentSortLabel }}
                </el-button>
                <template #dropdown>
                  <el-dropdown-item command="newest">最新</el-dropdown-item>
                  <el-dropdown-item command="oldest">最早</el-dropdown-item>
                  <el-dropdown-item command="name">名称</el-dropdown-item>
                </template>
              </el-dropdown>
              <el-button type="primary" @click="openAddAgentDialog">
                <el-icon><Plus /></el-icon> 新增智能体
              </el-button>
            </div>
          </div>

          <div class="agent-grid">
            <div
              v-for="agent in filteredAgentList"
              :key="agent.id"
              class="agent-card"
              @click="enterAgentDetail(agent)"
            >
              <div class="agent-card-header">
                <div class="agent-avatar" :style="avatarStyle(agent.name)">
                  {{ agent.name.charAt(0) }}
                </div>
                <el-dropdown @command="cmd => handleAgentCardMenu(cmd, agent)" @click.stop>
                  <el-icon class="agent-menu-btn"><More /></el-icon>
                  <template #dropdown>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="toggle">{{ agent.status === 1 ? '禁用' : '启用' }}</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </template>
                </el-dropdown>
              </div>
              <h4 class="agent-card-name">{{ agent.name }}</h4>
              <p class="agent-card-desc">{{ agent.description || '暂无描述' }}</p>
              <div class="agent-card-footer">
                <span>ID: {{ agent.id }}</span>
                <span>
                  <el-icon><View /></el-icon> {{ agent.modelId }}
                </span>
              </div>
            </div>
          </div>
        </template>

        <!-- ====== 智能体详情视图 ====== -->
        <template v-else>
          <!-- 顶部导航栏 -->
          <div class="agent-detail-header">
            <div class="agent-detail-breadcrumb">
              <el-icon @click="closeAgentDetail" class="back-btn"><ArrowLeft /></el-icon>
              <div class="agent-detail-title">
                <div class="agent-avatar-sm" :style="avatarStyle(viewingAgent.name)">{{ viewingAgent.name.charAt(0) }}</div>
                <span>{{ viewingAgent.name }}</span>
                <el-tag size="small" :type="viewingAgent.status === 1 ? 'success' : 'info'" class="status-tag">
                  {{ viewingAgent.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </div>
            </div>
            <div class="agent-detail-actions">
              <el-button size="small" @click="openEditAgent(viewingAgent)">
                <el-icon><Edit /></el-icon> 编辑
              </el-button>
              <el-button size="small" type="danger" @click="handleDeleteAgent(viewingAgent)">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
              <el-button size="small" type="primary" @click="handlePublishAgent">
                <el-icon><Promotion /></el-icon> 发布
              </el-button>
            </div>
          </div>

          <!-- 左右分栏 -->
          <div class="agent-detail-body">
            <!-- 左侧：配置编辑 -->
            <div class="agent-detail-left">
              <div class="detail-section">
                <h3>基本信息</h3>
                <div class="detail-form">
                  <div class="form-row">
                    <label>名称</label>
                    <el-input v-model="viewingAgent.name" size="small" />
                  </div>
                  <div class="form-row">
                    <label>描述</label>
                    <el-input v-model="viewingAgent.description" type="textarea" :rows="3" size="small" />
                  </div>
                </div>
              </div>

              <div class="detail-section">
                <h3>系统提示词</h3>
                <el-input
                  v-model="viewingAgent.systemPrompt"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入系统提示词（System Prompt）"
                  class="system-prompt-input"
                />
              </div>

              <div class="detail-section">
                <h3>配置信息</h3>
                <div class="detail-form">
                  <div class="form-row">
                    <label>绑定模型</label>
                    <el-select v-model="viewingAgent.modelId" size="small" style="width: 100%">
                      <el-option label="DeepSeek-V3" :value="1" />
                      <el-option label="Qwen-Max" :value="2" />
                      <el-option label="GPT-4o" :value="3" />
                    </el-select>
                  </div>
                  <div class="form-row">
                    <label>知识库RAG</label>
                    <el-switch v-model="viewingAgent.kbEnabled" :active-value="1" :inactive-value="0" />
                    <span class="switch-label">{{ viewingAgent.kbEnabled === 1 ? '已开启' : '已关闭' }}</span>
                  </div>
                  <div class="form-row" v-if="viewingAgent.kbEnabled === 1">
                    <label>绑定知识库</label>
                    <el-select v-model="viewingAgent.kbId" size="small" style="width: 100%" placeholder="选择知识库">
                      <el-option v-for="kb in kbList" :key="kb.id" :label="kb.kbName" :value="kb.id" />
                    </el-select>
                  </div>
                </div>
              </div>

              <div class="detail-section">
                <el-button type="primary" @click="handleSaveAgentConfig" style="width: 100%">
                  <el-icon><Check /></el-icon> 保存配置
                </el-button>
              </div>
            </div>

            <!-- 右侧：预览与测试 -->
            <div class="agent-detail-right">
              <h3>预览与调试</h3>
              <div class="chat-preview-box">
                <div class="chat-messages" ref="chatMessagesRef">
                  <div v-for="(msg, i) in testMessages" :key="i" :class="['chat-msg', msg.role]">
                    <div class="msg-avatar" :style="avatarStyle(msg.role === 'user' ? '我' : viewingAgent.name)">
                      {{ msg.role === 'user' ? '我' : viewingAgent.name.charAt(0) }}
                    </div>
                    <div class="msg-content">
                      <div class="msg-text" v-html="msg.content"></div>
                    </div>
                  </div>
                  <div v-if="testLoading" class="chat-msg assistant">
                    <div class="msg-avatar" :style="avatarStyle(viewingAgent.name)">{{ viewingAgent.name.charAt(0) }}</div>
                    <div class="msg-content">
                      <div class="msg-typing"><span></span><span></span><span></span></div>
                    </div>
                  </div>
                </div>
                <div class="chat-input-bar">
                  <el-input
                    v-model="testInput"
                    placeholder="输入任何问题测试..."
                    @keyup.enter="sendTestMessage"
                  />
                  <el-button type="primary" circle @click="sendTestMessage" :loading="testLoading">
                    <el-icon><Promotion /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 新建/编辑用户对话框 -->
    <el-dialog v-model="showAddUserDialog" :title="editingUser ? '编辑用户' : '新建用户'" width="450px">
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="userForm.username" :disabled="!!editingUser" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="userForm.password" type="password" placeholder="不修改请留空（编辑时）" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option label="普通用户（role=1）" :value="1" />
            <el-option label="管理员（role=2）" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="剩余次数">
          <el-input-number v-model="userForm.totals" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddUserDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveUser">确定</el-button>
      </template>
    </el-dialog>
    <!-- 新建/编辑知识库对话框 -->
    <el-dialog v-model="showAddKbDialog" :title="editingKb ? '编辑知识库' : '新建知识库'" width="500px">
      <el-form :model="kbForm" label-width="100px">
        <el-form-item label="知识库名称">
          <el-input v-model="kbForm.kbName" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="kbForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="存储后端">
          <el-select v-model="kbForm.storeId" style="width: 100%" placeholder="请选择存储后端">
            <el-option label="MySQL" :value="1" />
            <el-option label="PostgreSQL+pgvector" :value="2" />
            <el-option label="MongoDB" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddKbDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveKb">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新建/编辑大模型对话框 -->
    <el-dialog v-model="showAddModelDialog" :title="editingModel ? '编辑模型' : '新增模型'" width="520px">
      <el-form :model="modelForm" label-width="100px">
        <el-form-item label="模型名称">
          <el-input v-model="modelForm.name" placeholder="如：DeepSeek-V3" />
        </el-form-item>
        <el-form-item label="提供商">
          <el-input v-model="modelForm.provider" placeholder="如：SiliconFlow、OpenAI" />
        </el-form-item>
        <el-form-item label="模型Key">
          <el-input v-model="modelForm.modelCode" placeholder="如：deepseek-ai/DeepSeek-V3" />
        </el-form-item>
        <el-form-item label="API地址">
          <el-input v-model="modelForm.apiUrl" placeholder="如：https://api.siliconflow.cn/v1/chat/completions" />
        </el-form-item>
        <el-form-item label="API密钥">
          <el-input v-model="modelForm.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="modelForm.status" :active-value="1" :inactive-value="0" />
          <span style="margin-left:8px;color:#999">{{ modelForm.status === 1 ? '启用' : '禁用' }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddModelDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveModel">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新建/编辑智能体对话框 -->
    <el-dialog v-model="showAddAgentDialog" :title="editingAgent ? '编辑智能体' : '新建智能体'" width="600px">
      <el-form :model="agentForm" label-width="110px">
        <el-form-item label="智能体名称">
          <el-input v-model="agentForm.name" placeholder="请输入智能体名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="agentForm.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="系统提示词">
          <el-input v-model="agentForm.systemPrompt" type="textarea" :rows="4" placeholder="请输入系统提示词（system prompt）" />
        </el-form-item>
        <el-form-item label="绑定模型ID">
          <el-input-number v-model="agentForm.modelId" :min="1" />
        </el-form-item>
        <el-form-item label="知识库RAG">
          <el-switch v-model="agentForm.kbEnabled" :active-value="1" :inactive-value="0" />
          <span style="margin-left:8px;color:#999">{{ agentForm.kbEnabled === 1 ? '已开启' : '已关闭' }}</span>
        </el-form-item>
        <el-form-item v-if="agentForm.kbEnabled === 1" label="绑定知识库ID">
          <el-input-number v-model="agentForm.kbId" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddAgentDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAgent">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, SwitchButton, User, Collection, ChatLineSquare, Edit, Delete, Cpu,
  Search, Sort, More, View, ArrowLeft, Promotion, Check, Monitor, RefreshRight
} from '@element-plus/icons-vue'
import request from '../api/request'

const router = useRouter()
const userStore = useUserStore()

const activeMenu = ref('users')
const userList = ref([])
const kbList = ref([])
const agentList = ref([])
const conversationList = ref([])
const currentViewUser = ref(null)
const showAddUserDialog = ref(false)
const showAddKbDialog = ref(false)
const showAddAgentDialog = ref(false)
const editingUser = ref(null)
const editingKb = ref(null)
const editingAgent = ref(null)
const userForm = ref({ username: '', password: '', email: '', role: 1, totals: 100 })
const kbForm = ref({ kbName: '', description: '', storeId: 1 })
const agentForm = ref({ name: '', description: '', systemPrompt: '', modelId: 1, status: 1, kbEnabled: 0, kbId: null })

// 大模型管理
const modelList = ref([])
const modelSearch = ref('')
const showAddModelDialog = ref(false)
const editingModel = ref(null)
const modelForm = ref({ name: '', provider: '', modelCode: '', apiUrl: '', apiKey: '', status: 1 })

// 按提供商分组
const providerList = computed(() => {
  const map = {}
  modelList.value.forEach(m => {
    if (!map[m.provider]) {
      map[m.provider] = { name: m.provider, models: [], status: m.status, desc: '' }
    }
    map[m.provider].models.push(m)
    // 如果任一模型启用，则提供商显示启用
    if (m.status === 1) map[m.provider].status = 1
  })
  // 生成描述
  Object.values(map).forEach(p => {
    const types = [...new Set(p.models.map(m => m.modelType || 'chat'))]
    p.desc = types.includes('embedding') ? '多模态模型接入' : '对话模型接入'
    if (p.name.toLowerCase().includes('ollama')) p.desc = '本地开源模型（Llama, Mistral等）'
    else if (p.name.toLowerCase().includes('openai')) p.desc = 'OpenAI官方模型（GPT-4, GPT-3.5等）'
  })
  return Object.values(map)
})

async function loadModelList() {
  try {
    const res = await request.get('/model/admin/list')
    if (res.code === 200) {
      modelList.value = res.data.map(m => ({
        ...m,
        modelType: m.modelCode?.includes('embedding') ? 'embedding' : 'chat',
        isDefault: m.isDefault || 0
      }))
    }
  } catch (e) {
    ElMessage.error('加载模型列表失败')
  }
}

function openAddModelDialog() {
  editingModel.value = null
  modelForm.value = { name: '', provider: '', modelCode: '', apiUrl: '', apiKey: '', status: 1 }
  showAddModelDialog.value = true
}

function openEditModel(row) {
  editingModel.value = row
  modelForm.value = { ...row }
  showAddModelDialog.value = true
}

async function handleSaveModel() {
  try {
    if (editingModel.value) {
      await request.put('/model/update', modelForm.value)
      ElMessage.success('修改成功')
    } else {
      await request.post('/model/add', modelForm.value)
      ElMessage.success('创建成功')
    }
    showAddModelDialog.value = false
    editingModel.value = null
    await loadModelList()
  } catch (e) {
    ElMessage.error('操作失败：' + (e.response?.data?.message || e.message))
  }
}

async function toggleModelStatus(row) {
  try {
    const res = await request.put(`/model/status/${row.id}`)
    if (res.code === 200) {
      ElMessage.success(res.data.statusText)
      row.status = res.data.newStatus
    }
  } catch (e) {
    ElMessage.error('状态切换失败')
  }
}

async function toggleModelStatusByProvider(provider, newStatus) {
  // 批量切换该提供商下所有模型状态
  for (const m of provider.models) {
    if (m.status !== newStatus) {
      try {
        await request.put(`/model/status/${m.id}`)
        m.status = newStatus
      } catch (e) { /* ignore */ }
    }
  }
  ElMessage.success(`已${newStatus === 1 ? '启用' : '禁用'}「${provider.name}」全部模型`)
}

async function setDefaultModel(row) {
  // 将当前模型设为默认，其他取消默认
  modelList.value.forEach(m => { m.isDefault = (m.id === row.id) ? 1 : 0 })
  ElMessage.success(`「${row.name}」已设为默认模型`)
  // TODO: 后端需加 isDefault 字段支持
}

async function handleDeleteModel(row) {
  try {
    await ElMessageBox.confirm(`确定删除模型「${row.name}」？`, '删除确认', { type: 'warning' })
    await request.delete(`/model/delete/${row.id}`)
    ElMessage.success('删除成功')
    await loadModelList()
  } catch (e) { /* 取消 */ }
}

function viewProviderDetail(provider) {
  ElMessage.info(`「${provider.name}」下共有 ${provider.models.length} 个模型`)
}

function editProvider(provider) {
  // 编辑该提供商的第一个模型（简化处理）
  if (provider.models.length > 0) {
    openEditModel(provider.models[0])
  }
}

async function deleteProvider(provider) {
  try {
    await ElMessageBox.confirm(`确定删除提供商「${provider.name}」及其下全部 ${provider.models.length} 个模型？`, '删除确认', { type: 'warning' })
    for (const m of provider.models) {
      await request.delete(`/model/delete/${m.id}`)
    }
    ElMessage.success('删除成功')
    await loadModelList()
  } catch (e) { /* 取消 */ }
}

// 智能体详情视图
const viewingAgent = ref(null)
const agentSearch = ref('')
const agentSortLabel = ref('最新')
const testMessages = ref([])
const testInput = ref('')
const testLoading = ref(false)
const chatMessagesRef = ref(null)

// 头像颜色生成
function avatarStyle(name) {
  const colors = ['#3b82f6', '#8b5cf6', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#6366f1']
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  const idx = Math.abs(hash) % colors.length
  return { background: colors[idx] }
}

// 智能体搜索 + 排序
const filteredAgentList = computed(() => {
  let list = [...agentList.value]
  if (agentSearch.value) {
    const kw = agentSearch.value.toLowerCase()
    list = list.filter(a => (a.name + a.description).toLowerCase().includes(kw))
  }
  if (agentSortLabel.value === '最新') list.sort((a, b) => new Date(b.createdDt) - new Date(a.createdDt))
  else if (agentSortLabel.value === '最早') list.sort((a, b) => new Date(a.createdDt) - new Date(b.createdDt))
  else if (agentSortLabel.value === '名称') list.sort((a, b) => a.name.localeCompare(b.name))
  return list
})

function handleAgentSort(cmd) {
  const map = { newest: '最新', oldest: '最早', name: '名称' }
  agentSortLabel.value = map[cmd]
}

function enterAgentDetail(agent) {
  viewingAgent.value = { ...agent }
  testMessages.value = []
}

function closeAgentDetail() {
  viewingAgent.value = null
  testMessages.value = []
  testInput.value = ''
}

function handleAgentCardMenu(cmd, agent) {
  if (cmd === 'edit') openEditAgent(agent)
  else if (cmd === 'toggle') toggleAgentStatus(agent)
  else if (cmd === 'delete') handleDeleteAgent(agent)
}

async function handleSaveAgentConfig() {
  try {
    await request.put('/agent/update', viewingAgent.value)
    ElMessage.success('配置已保存')
    // 刷新列表
    const idx = agentList.value.findIndex(a => a.id === viewingAgent.value.id)
    if (idx >= 0) agentList.value[idx] = { ...viewingAgent.value }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

function handlePublishAgent() {
  ElMessage.success('发布功能待接入')
}

async function sendTestMessage() {
  if (!testInput.value.trim()) return
  const userMsg = testInput.value.trim()
  testMessages.value.push({ role: 'user', content: userMsg })
  testInput.value = ''
  testLoading.value = true
  await nextTick()
  scrollToBottom()

  // 模拟 AI 回复（实际应调用 chatStream 接口）
  setTimeout(() => {
    testLoading.value = false
    testMessages.value.push({
      role: 'assistant',
      content: `<b>我是「${viewingAgent.value.name}」</b><br>您说的是：${userMsg}<br><br>（此处为模拟回复，正式环境将调用对话接口）`
    })
    nextTick(scrollToBottom)
  }, 1200)
}

function scrollToBottom() {
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
  }
}

const menu = [
  { key: 'users', label: '用户管理', icon: User },
  { key: 'kb', label: '知识库管理', icon: Collection },
  { key: 'agents', label: '智能体管理', icon: Cpu },
  { key: 'models', label: '大模型管理', icon: Monitor },
]

// 存储后端名称映射（vectorStoreType 是字符串：MYSQL / PGVECTOR / MONGODB）
const storeNameMap = { 'MYSQL': 'MySQL', 'PGVECTOR': 'PostgreSQL+pgvector', 'MONGODB': 'MongoDB' }
function getStoreName(vectorStoreType) {
  return storeNameMap[vectorStoreType] || '未知(' + vectorStoreType + ')'
}

function formatTime(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleString('zh-CN')
}

// ========== 用户管理 ==========
async function loadUsers() {
  try {
    const res = await request.get('/user/admin/list')
    if (res.code === 200) {
      userList.value = res.data
    }
  } catch (e) {
    ElMessage.error('加载用户列表失败，请确认后端已启用管理员接口')
  }
}

async function handleSaveUser() {
  try {
    if (editingUser.value) {
      await request.put(`/user/admin/update/${editingUser.value.id}`, userForm.value)
      ElMessage.success('修改成功')
    } else {
      await request.post('/user/admin/create', userForm.value)
      ElMessage.success('创建成功')
    }
    showAddUserDialog.value = false
    await loadUsers()
  } catch (e) {
    ElMessage.error('操作失败：' + (e.response?.data?.message || e.message))
  }
}

function openEditUser(row) {
  editingUser.value = row
  userForm.value = { ...row, password: '' }
  showAddUserDialog.value = true
}

async function handleDeleteUser(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」？`, '删除确认', { type: 'warning' })
    await request.delete(`/user/admin/delete/${row.id}`)
    ElMessage.success('删除成功')
    await loadUsers()
  } catch (e) { /* 取消 */ }
}

// ========== 知识库管理 ==========
async function loadKbList() {
  try {
    const res = await request.get('/kb/list')
    if (res.code === 200) {
      kbList.value = res.data
    }
  } catch (e) {
    ElMessage.error('加载知识库列表失败')
  }
}

async function handleDeleteKb(row) {
  try {
    await ElMessageBox.confirm(`确定删除知识库「${row.kbName}」？`, '删除确认', { type: 'warning' })
    await request.delete(`/kb/delete/${row.id}`)
    ElMessage.success('删除成功')
    await loadKbList()
  } catch (e) { /* 取消 */ }
}

function openEditKb(row) {
  editingKb.value = row
  kbForm.value = { kbName: row.kbName, description: row.description, storeId: row.storeId }
  showAddKbDialog.value = true
}

async function handleSaveKb() {
  try {
    if (editingKb.value) {
      await request.put(`/kb/update/${editingKb.value.id}`, kbForm.value)
      ElMessage.success('修改成功')
    } else {
      await request.post('/kb/create', kbForm.value)
      ElMessage.success('创建成功')
    }
    showAddKbDialog.value = false
    editingKb.value = null
    kbForm.value = { kbName: '', description: '', storeId: 1 }
    await loadKbList()
  } catch (e) {
    ElMessage.error('操作失败：' + (e.response?.data?.message || e.message))
  }
}

// ========== 智能体管理 ==========
async function loadAgentList() {
  try {
    const res = await request.get('/agent/admin/list')
    if (res.code === 200) {
      agentList.value = res.data
    }
  } catch (e) {
    ElMessage.error('加载智能体列表失败')
  }
}

function openAddAgentDialog() {
  editingAgent.value = null
  agentForm.value = { name: '', description: '', systemPrompt: '', modelId: 1, status: 1, kbEnabled: 0, kbId: null }
  showAddAgentDialog.value = true
}

function openEditAgent(row) {
  editingAgent.value = row
  agentForm.value = {
    name: row.name,
    description: row.description,
    systemPrompt: row.systemPrompt,
    modelId: row.modelId,
    status: row.status,
    kbEnabled: row.kbEnabled || 0,
    kbId: row.kbId || null
  }
  showAddAgentDialog.value = true
}

async function handleSaveAgent() {
  try {
    if (editingAgent.value) {
      const data = { ...agentForm.value, id: editingAgent.value.id }
      await request.put('/agent/update', data)
      ElMessage.success('修改成功')
    } else {
      await request.post('/agent/add', agentForm.value)
      ElMessage.success('创建成功')
    }
    showAddAgentDialog.value = false
    editingAgent.value = null
    await loadAgentList()
  } catch (e) {
    ElMessage.error('操作失败：' + (e.response?.data?.message || e.message))
  }
}

async function toggleAgentStatus(row) {
  try {
    const res = await request.put(`/agent/status/${row.id}`)
    if (res.code === 200) {
      ElMessage.success(res.data.statusText)
      row.status = res.data.newStatus
    }
  } catch (e) {
    ElMessage.error('状态切换失败')
  }
}

async function handleDeleteAgent(row) {
  try {
    await ElMessageBox.confirm(`确定删除智能体「${row.name}」？`, '删除确认', { type: 'warning' })
    await request.delete(`/agent/delete/${row.id}`)
    ElMessage.success('删除成功')
    await loadAgentList()
  } catch (e) { /* 取消 */ }
}

// ========== 对话查看 ==========
async function viewUserConversations(row) {
  currentViewUser.value = row
  activeMenu.value = 'conversations'
  try {
    const res = await request.get(`/user/admin/conversations/${row.id}`)
    if (res.code === 200) {
      conversationList.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('加载对话记录失败，后端接口待实现')
    conversationList.value = []
  }
}

function viewConversationDetail(row) {
  ElMessage.info('对话详情查看功能待实现')
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  loadUsers()
  loadKbList()
  loadAgentList()
  loadModelList()
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}
.admin-sidebar {
  width: 220px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 20px 0;
}
.sidebar-title {
  font-size: 18px;
  font-weight: bold;
  padding: 0 20px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  margin-bottom: 10px;
}
.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.2s;
  font-size: 14px;
}
.sidebar-item:hover { background: rgba(255,255,255,0.1); }
.sidebar-item.active { background: rgba(103,194,58,0.3); border-right: 3px solid #67c23a; }
.sidebar-bottom {
  margin-top: auto;
  padding: 20px;
  border-top: 1px solid rgba(255,255,255,0.1);
}
.sidebar-user {
  font-size: 12px;
  color: rgba(255,255,255,0.6);
  margin-top: 10px;
}
.admin-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
.content-panel {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.panel-header h2 { margin: 0; font-size: 20px; }

/* ===== 智能体管理 ===== */
.agent-panel { padding: 0; background: transparent; box-shadow: none; }
.agent-panel .panel-header { padding: 24px 24px 16px; background: #fff; border-radius: 12px; margin-bottom: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.04); }
.panel-subtitle { margin: 4px 0 0; font-size: 13px; color: #909399; }
.agent-toolbar { display: flex; gap: 10px; align-items: center; }
.agent-search { width: 260px; }

/* 卡片网格 */
.agent-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.agent-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  border: 1px solid #f0f0f0;
  transition: all 0.25s ease;
  position: relative;
}
.agent-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0,0,0,0.08);
  border-color: #e0e7ff;
}
.agent-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.agent-avatar {
  width: 48px; height: 48px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 20px; font-weight: 600;
}
.agent-avatar-sm {
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 14px; font-weight: 600;
}
.agent-menu-btn {
  padding: 4px; border-radius: 6px;
  color: #c0c4cc; cursor: pointer;
}
.agent-menu-btn:hover { background: #f5f7fa; color: #606266; }
.agent-card-name {
  margin: 0 0 8px; font-size: 16px; font-weight: 600; color: #303133;
}
.agent-card-desc {
  margin: 0 0 16px; font-size: 13px; color: #909399;
  line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden;
}
.agent-card-footer {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: #c0c4cc;
}
.agent-card-footer .el-icon { vertical-align: middle; margin-right: 2px; }

/* 详情页 */
.agent-detail-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 24px; background: #fff; border-radius: 12px;
  margin-bottom: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.back-btn { cursor: pointer; color: #606266; font-size: 18px; margin-right: 12px; }
.back-btn:hover { color: #3b82f6; }
.agent-detail-breadcrumb { display: flex; align-items: center; }
.agent-detail-title { display: flex; align-items: center; gap: 10px; font-size: 18px; font-weight: 600; color: #303133; }
.status-tag { margin-left: 4px; }
.agent-detail-actions { display: flex; gap: 8px; }

.agent-detail-body {
  display: flex; gap: 16px; height: calc(100vh - 220px);
}
.agent-detail-left {
  width: 420px; flex-shrink: 0;
  background: #fff; border-radius: 12px; padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  overflow-y: auto;
}
.agent-detail-right {
  flex: 1;
  background: #fff; border-radius: 12px; padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  display: flex; flex-direction: column;
}
.agent-detail-right h3 { margin: 0 0 16px; font-size: 16px; }

.detail-section { margin-bottom: 24px; }
.detail-section h3 {
  margin: 0 0 12px; font-size: 14px; font-weight: 600; color: #606266;
  padding-left: 8px; border-left: 3px solid #3b82f6;
}
.detail-form .form-row { margin-bottom: 14px; }
.detail-form .form-row label {
  display: block; font-size: 13px; color: #606266; margin-bottom: 6px; font-weight: 500;
}
.switch-label { margin-left: 8px; font-size: 13px; color: #909399; }
.system-prompt-input :deep(.el-textarea__inner) { font-family: 'Menlo', 'Monaco', monospace; font-size: 13px; }

/* 聊天预览 */
.chat-preview-box {
  flex: 1; display: flex; flex-direction: column;
  border: 1px solid #f0f0f0; border-radius: 12px; overflow: hidden;
}
.chat-messages {
  flex: 1; padding: 16px; overflow-y: auto;
  background: #fafbfc;
}
.chat-msg {
  display: flex; gap: 10px; margin-bottom: 16px;
}
.chat-msg.user { flex-direction: row-reverse; }
.chat-msg.user .msg-content { background: #3b82f6; color: #fff; border-radius: 16px 16px 4px 16px; }
.msg-content {
  max-width: 70%; padding: 10px 14px;
  background: #fff; border-radius: 16px 16px 16px 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  font-size: 14px; line-height: 1.6; color: #303133;
}
.msg-typing { display: flex; gap: 4px; padding: 6px 0; }
.msg-typing span {
  width: 6px; height: 6px; background: #c0c4cc; border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}
.msg-typing span:nth-child(1) { animation-delay: -0.32s; }
.msg-typing span:nth-child(2) { animation-delay: -0.16s; }
@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}
.chat-input-bar {
  display: flex; gap: 8px; padding: 12px 16px;
  background: #fff; border-top: 1px solid #f0f0f0;
}
.chat-input-bar .el-input { flex: 1; }

/* ===== 大模型管理 ===== */
.model-panel { padding: 0; background: transparent; box-shadow: none; }

/* 提供商卡片 */
.model-providers {
  background: #fff; border-radius: 12px; padding: 24px;
  margin-bottom: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.provider-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
}
.provider-header h3 { margin: 0; font-size: 16px; font-weight: 600; }
.provider-search { width: 260px; }
.provider-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px;
}
.provider-card {
  border: 1px solid #f0f0f0; border-radius: 12px; padding: 18px;
  transition: all 0.25s ease;
}
.provider-card:hover {
  border-color: #d0d7ff; box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}
.provider-top {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;
}
.provider-name { font-size: 15px; font-weight: 600; color: #303133; }
.provider-desc { font-size: 12px; color: #909399; margin-bottom: 14px; min-height: 18px; }
.provider-actions {
  display: flex; gap: 4px;
}
.provider-actions .el-button { padding: 4px 8px; font-size: 12px; }

/* 模型表格 */
.model-table-wrap {
  background: #fff; border-radius: 12px; padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.model-table-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
}
.model-table-header h3 { margin: 0; font-size: 16px; font-weight: 600; }
.model-table-actions { display: flex; gap: 8px; }
</style>
