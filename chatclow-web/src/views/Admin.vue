<template>
  <div class="admin-layout">
    <!-- 左侧菜单 -->
    <div class="admin-sidebar">
      <div class="sidebar-title">
        ChatClow 管理后台
        <span class="sidebar-watermark">张文亮张文亮张文亮张文亮张文亮张文亮张文亮张文亮</span>
      </div>
      <div class="sidebar-menu-cards">
        <div
            v-for="item in menu"
            :key="item.key"
            class="sidebar-card"
            :class="{ active: activeMenu === item.key }"
            :style="{ background: cardBg[item.key] }"
            @click="activeMenu = item.key"
        >
          <div class="sidebar-card-inner">
            <el-icon><component :is="item.icon" /></el-icon>
            <span class="sidebar-card-label">{{ item.label }}</span>
            <el-icon class="sidebar-card-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
      <div class="sidebar-bottom">
        <div class="sidebar-card logout-card" :style="{ background: cardBg.logout }" @click="handleLogout">
          <div class="sidebar-card-inner">
            <el-icon><SwitchButton /></el-icon>
            <span class="sidebar-card-label">退出登录</span>
            <el-icon class="sidebar-card-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
        <div class="sidebar-user">{{ userStore.username }}（管理员）</div>
      </div>
    </div>

    <!-- 右侧内容 -->
    <div class="admin-content">
      <!-- 运营概览 -->
      <div v-if="activeMenu === 'dashboard'" class="dashboard-wrap">
        <AdminDashboard />
      </div>

      <!-- 用户管理 -->
      <div v-if="activeMenu === 'users'" class="content-panel">
        <div class="panel-header">
          <h2>用户管理</h2>
          <el-button type="primary" @click="showAddUserDialog = true">
            <el-icon><Plus /></el-icon> 新建用户
          </el-button>
        </div>
        <el-table :data="userList" border stripe style="width: 100%" :resizable="false">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="用户名" width="200" />
          <el-table-column prop="email" label="邮箱" width="520" />
          <el-table-column label="角色" width="240">
            <template #default="{ row }">
              <el-tag :type="row.role === 2 ? 'danger' : 'info'">
                {{ row.role === 2 ? '管理员' : '普通用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Token消耗" width="150">
            <template #default="{ row }">
              {{ (tokenMap[row.id] || 0).toLocaleString() }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="520">
            <template #header>
              <div style="text-align:center">操作</div>
            </template>
            <template #default="{ row }">
              <div class="action-row">
                <el-button size="small" @click="openEditUser(row)">编辑</el-button>
                <el-button size="small" @click="viewUserConversations(row)">查看对话</el-button>
                <el-button size="small" type="danger" @click="handleDeleteUser(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 知识库管理（管理员视图） -->
      <div v-if="activeMenu === 'kb'" class="content-panel kb-panel">

        <!-- ====== 知识库列表视图 ====== -->
        <template v-if="!viewingKb">
          <div class="panel-header">
            <div>
              <h2>知识库管理</h2>
              <p class="panel-subtitle">RAG 知识库</p>
            </div>
            <el-button type="primary" @click="showAddKbDialog = true">
              <el-icon><Plus /></el-icon> 新建知识库
            </el-button>
          </div>

          <div class="kb-grid">
            <div
              v-for="kb in kbList"
              :key="kb.id"
              class="kb-card"
              @click="enterKbDetail(kb)"
            >
              <div class="kb-card-header">
                <div class="kb-avatar">
                  <el-icon :size="24"><Collection /></el-icon>
                </div>
                <el-dropdown @command="cmd => handleKbCardMenu(cmd, kb)" @click.stop>
                  <el-icon class="kb-menu-btn"><More /></el-icon>
                  <template #dropdown>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </template>
                </el-dropdown>
              </div>
              <h4 class="kb-card-name">{{ kb.name }}</h4>
              <p class="kb-card-desc">{{ kb.description || '暂无描述' }}</p>
              <div class="kb-card-footer">
                <span>{{ getStoreName(kb.vectorStoreType) }}</span>
                <span>
                  <el-icon><Timer /></el-icon> {{ formatTime(kb.updatedDt) }}
                </span>
              </div>
            </div>
          </div>
        </template>

        <!-- ====== 知识库详情视图 ====== -->
        <template v-else>
          <div class="kb-detail-header">
            <div class="kb-detail-breadcrumb">
              <el-icon @click="closeKbDetail" class="back-btn"><ArrowLeft /></el-icon>
              <div class="kb-detail-title">
                <el-icon :size="28" style="color:var(--fg-default)"><Collection /></el-icon>
                <span>{{ viewingKb.name }}</span>
                <el-tag size="small" :type="viewingKb.status === 1 ? 'success' : 'info'">
                  {{ viewingKb.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </div>
            </div>
            <div class="kb-detail-actions">
              <el-button size="small" type="danger" @click="handleDeleteKb(viewingKb)">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </div>
          </div>

          <div class="kb-detail-body">
            <!-- 左侧：基本配置 -->
            <div class="kb-detail-left">
              <div class="detail-section">
                <h3>基本信息</h3>
                <div class="detail-form">
                  <div class="form-row">
                    <label>名称</label>
                    <el-input v-model="viewingKb.name" size="small" />
                  </div>
                  <div class="form-row">
                    <label>描述</label>
                    <el-input v-model="viewingKb.description" type="textarea" :rows="3" size="small" />
                  </div>
                  <div class="form-row" style="border-bottom:none">
                    <label>存储后端</label>
                    <el-tag>{{ getStoreName(viewingKb.vectorStoreType) }}</el-tag>
                  </div>
                </div>
              </div>

              <div class="detail-section">
                <el-button type="primary" @click="handleSaveKbConfig" style="width:100%">
                  <el-icon><Check /></el-icon> 保存配置
                </el-button>
              </div>
            </div>

            <!-- 右侧：文档管理 -->
            <div class="kb-detail-right">
              <h3>文档管理</h3>
              <div class="kb-doc-upload">
                <input ref="kbFileInputRef" type="file" hidden accept=".pdf,.docx,.txt,.md" @change="handleKbFileSelected" />
                <el-button type="primary" :loading="kbUploading" @click="triggerKbUpload" :icon="UploadFilled">
                  上传文档
                </el-button>
                <span class="kb-doc-hint">支持 PDF、DOCX、TXT、MD</span>
              </div>
              <div class="kb-doc-list" v-loading="kbDocsLoading">
                <div v-if="kbDocs.length === 0" class="kb-doc-empty">
                  暂无文档
                </div>
                <div v-for="doc in kbDocs" :key="doc.id" class="kb-doc-item">
                  <el-icon :size="18"><Document /></el-icon>
                  <span class="kb-doc-name">{{ doc.fileName || doc.name || '未知文档' }}</span>
                  <span class="kb-doc-time">{{ formatTime(doc.createdDt) }}</span>
                  <el-button size="small" text type="danger" :icon="Delete" @click="handleKbDeleteDoc(doc)" />
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 用户对话查看 -->
      <div v-if="activeMenu === 'conversations'" class="content-panel">
        <div class="panel-header">
          <h2>用户对话记录 — {{ currentViewUser?.username }}</h2>
          <el-button @click="activeMenu = 'users'">返回用户列表</el-button>
        </div>
        <el-table :data="conversationList" border stripe style="width: 100%" :resizable="false">
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
          <el-table :data="modelList" border stripe style="width: 100%" :resizable="false">
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
            <el-table-column label="操作" width="260">
              <template #default="{ row }">
                <div class="action-row">
                  <el-button size="small" text @click="openEditModel(row)">编辑</el-button>
                  <el-button size="small" text @click="setDefaultModel(row)">设为默认</el-button>
                  <el-button size="small" text type="danger" @click="handleDeleteModel(row)">删除</el-button>
                </div>
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
                <div class="agent-avatar" :style="avatarStyle(agent)">
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
                <div class="agent-avatar-sm" :style="avatarStyle(viewingAgent)">{{ viewingAgent.avatar ? '' : viewingAgent.name.charAt(0) }}</div>
                <span>{{ viewingAgent.name }}</span>
                <el-tag size="small" :type="viewingAgent.status === 1 ? 'success' : 'info'" class="status-tag">
                  {{ viewingAgent.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </div>
            </div>
            <div class="agent-detail-actions">
              <el-button size="small" type="danger" @click="handleDeleteAgent(viewingAgent)">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
              <el-button size="small" :type="viewingAgent.status === 1 ? 'default' : 'primary'" @click="handlePublishAgent">
                <el-icon><Promotion /></el-icon> {{ viewingAgent.status === 1 ? '已发布' : '发布' }}
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
                <h3>配置信息</h3>
                <div class="detail-form">
                  <div class="form-row">
                    <label>绑定模型</label>
                    <el-select v-model="viewingAgent.modelId" size="small" style="width: 100%">
                      <el-option v-for="m in modelList" :key="m.id" :label="m.name + ' (' + m.modelCode + ')'" :value="m.id" />
                    </el-select>
                  </div>
                  <div class="form-row">
                    <label>知识库RAG</label>
                    <el-switch v-model="viewingAgent.kbEnabled" :active-value="1" :inactive-value="0" />
                    <span class="switch-label">{{ viewingAgent.kbEnabled === 1 ? '已开启' : '已关闭' }}</span>
                  </div>
                  <div class="form-row" v-if="viewingAgent.kbEnabled === 1" style="border-bottom:none">
                    <span style="font-size:12px;color:var(--fg-muted);font-family:var(--font-hand)">开启后自动检索所有知识库</span>
                  </div>
                </div>
              </div>

              <div class="detail-section">
                <el-button type="primary" @click="handleSaveAgentConfig" style="width: 100%">
                  <el-icon><Check /></el-icon> 保存配置
                </el-button>
              </div>
            </div>

            <!-- 右侧：系统提示词 -->
            <div class="agent-detail-right">
              <h3>系统提示词</h3>
              <div class="prompt-editor-box">
                <el-input
                  v-model="viewingAgent.systemPrompt"
                  type="textarea"
                  placeholder="在此编写系统提示词（System Prompt）..."
                  class="prompt-textarea"
                />
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 公告管理 -->
    <div v-if="activeMenu === 'announcement'" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>公告管理</h2>
          <p class="panel-subtitle">管理系统公告</p>
        </div>
        <el-button type="primary" @click="openAddAnnouncement">
          <el-icon><Plus /></el-icon> 新增公告
        </el-button>
      </div>

      <div class="announcement-list" v-loading="announcementList.length === 0 && announcementList._loading">
        <div v-if="announcementList.length === 0" class="announcement-empty">
          暂无公告
        </div>
        <div v-for="item in announcementList" :key="item.id" class="announcement-item">
          <div class="announcement-item-left">
            <div class="announcement-item-title">
              <el-icon :size="16"><Notification /></el-icon>
              <span>{{ item.title }}</span>
              <el-tag v-if="item.status === 1" size="small" type="success" effect="plain">启用</el-tag>
              <el-tag v-else size="small" type="info" effect="plain">禁用</el-tag>
            </div>
            <div class="announcement-item-preview">{{ item.content }}</div>
            <div class="announcement-item-time">{{ formatTime(item.createdDt) }}</div>
          </div>
          <div class="announcement-item-actions">
            <el-button size="small" @click="openEditAnnouncement(item)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDeleteAnnouncement(item)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建/编辑公告对话框 -->
    <el-dialog v-model="showAnnouncementDialog" :title="editingAnnouncement ? '编辑公告' : '新增公告'" width="600px">
      <el-form :model="announcementForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="announcementForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            :rows="8"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="announcementForm.status" :active-value="1" :inactive-value="0" />
          <span style="margin-left:8px;color:#999">{{ announcementForm.status === 1 ? '启用（用户可见）' : '禁用' }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAnnouncementDialog = false">取消</el-button>
        <el-button type="primary" :loading="announcementSaving" @click="handleSaveAnnouncement">确定</el-button>
      </template>
    </el-dialog>

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
          <el-input v-model="kbForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="kbForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="存储后端">
          <el-select v-model="kbForm.storeInstanceId" style="width: 100%" placeholder="请选择存储后端">
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
        <el-button :loading="testingModel" @click="handleTestModel">测试连接</el-button>
        <el-button type="primary" @click="handleSaveModel">确定</el-button>
      </template>
    </el-dialog>

    <!-- 对话详情弹窗 -->
    <el-dialog v-model="showConvDetailDialog" title="对话详情" width="700px">
      <div v-loading="convDetailLoading">
        <div v-if="convDetailMessages.length === 0" style="text-align:center;color:#909399;padding:40px 0">
          暂无对话记录
        </div>
        <div v-else class="conv-detail-list">
          <div
            v-for="(msg, idx) in convDetailMessages"
            :key="idx"
            :class="['conv-detail-msg', msg.role]"
          >
            <el-tag :type="msg.role === 'user' ? 'primary' : 'success'" size="small" class="conv-detail-role">
              {{ msg.role === 'user' ? '用户' : 'AI' }}
            </el-tag>
            <div class="conv-detail-content">{{ msg.content }}</div>
            <div class="conv-detail-time">{{ formatTime(msg.createdDt) }}</div>
          </div>
        </div>
      </div>
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
        <el-form-item label="绑定模型">
          <el-select v-model="agentForm.modelId" style="width:100%">
            <el-option v-for="m in modelList" :key="m.id" :label="m.name + ' (' + m.modelCode + ')'" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识库RAG">
          <el-switch v-model="agentForm.kbEnabled" :active-value="1" :inactive-value="0" />
          <span style="margin-left:8px;color:#999">{{ agentForm.kbEnabled === 1 ? '已开启，自动检索所有知识库' : '已关闭' }}</span>
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
  Search, Sort, More, View, ArrowLeft, ArrowRight, ArrowDown, Promotion, Check, Monitor, RefreshRight,
  DataAnalysis, Timer, Document, UploadFilled, FolderOpened, Notification
} from '@element-plus/icons-vue'
import request from '../api/request'
import { getAdminTokenSummary } from '../api/token'
import { testModelConnection } from '../api/model'
import { getAnnouncementList, addAnnouncement, updateAnnouncement, deleteAnnouncement } from '../api/announcement'
import AdminDashboard from './AdminDashboard.vue'

const router = useRouter()
const userStore = useUserStore()

const activeMenu = ref('users')
const userList = ref([])
const tokenMap = ref({})
const kbList = ref([])
const viewingKb = ref(null)
const kbDocs = ref([])
const kbDocsLoading = ref(false)
const kbFileInputRef = ref(null)
const kbUploading = ref(false)
const agentList = ref([])
const conversationList = ref([])
const currentViewUser = ref(null)
const showAddUserDialog = ref(false)
const showAddKbDialog = ref(false)
const showAddAgentDialog = ref(false)
const editingUser = ref(null)
const editingKb = ref(null)
const editingAgent = ref(null)
const userForm = ref({ username: '', password: '', email: '', role: 1 })
const kbForm = ref({ name: '', description: '', storeInstanceId: 1 })
const agentForm = ref({ name: '', description: '', systemPrompt: '', modelId: 1, status: 1, kbEnabled: 0, kbId: null })

// 非管理员强制跳转
if (userStore.role !== 2) {
  router.replace('/')
}

// 大模型管理
const modelList = ref([])
const modelSearch = ref('')
const showAddModelDialog = ref(false)
const editingModel = ref(null)
const testingModel = ref(false)
const modelForm = ref({ name: '', provider: '', modelCode: '', apiUrl: '', apiKey: '', status: 1, temperature: 0.7, maxTokens: 4096, topP: 1.0 })

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
  modelForm.value = { name: '', provider: '', modelCode: '', apiUrl: '', apiKey: '', status: 1, temperature: 0.7, maxTokens: 4096, topP: 1.0 }
  showAddModelDialog.value = true
}

function openEditModel(row) {
  editingModel.value = row
  modelForm.value = { ...row }
  showAddModelDialog.value = true
}

async function handleTestModel() {
  testingModel.value = true
  try {
    await testModelConnection(modelForm.value)
    ElMessage.success('连接成功！')
  } catch (e) {
    ElMessage.error('连接失败，请检查 API 地址和密钥')
  } finally {
    testingModel.value = false
  }
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
const showConvDetailDialog = ref(false)
const convDetailMessages = ref([])
const convDetailLoading = ref(false)

// 头像颜色生成
function avatarStyle(agent) {
  if (agent.avatar) {
    return { backgroundImage: 'url(' + agent.avatar + ')', backgroundSize: 'cover', backgroundPosition: 'center' }
  }
  const colors = ['#3b82f6', '#8b5cf6', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#6366f1']
  let hash = 0
  for (let i = 0; i < agent.name.length; i++) hash = agent.name.charCodeAt(i) + ((hash << 5) - hash)
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
}

function closeAgentDetail() {
  viewingAgent.value = null
}

function handleAgentCardMenu(cmd, agent) {
  if (cmd === 'edit') openEditAgent(agent)
  else if (cmd === 'toggle') toggleAgentStatus(agent)
  else if (cmd === 'delete') handleDeleteAgent(agent)
}

async function handleSaveAgentConfig() {
  try {
    viewingAgent.value.kbId = null  // RAG 开启时检索全部知识库
    await request.put('/agent/update', viewingAgent.value)
    ElMessage.success('配置已保存')
    // 刷新列表
    const idx = agentList.value.findIndex(a => a.id === viewingAgent.value.id)
    if (idx >= 0) agentList.value[idx] = { ...viewingAgent.value }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function handlePublishAgent() {
  try {
    const res = await request.put(`/agent/status/${viewingAgent.value.id}`)
    if (res.code === 200) {
      const newStatus = res.data.newStatus
      viewingAgent.value.status = newStatus
      // 同步刷新列表中的状态
      const idx = agentList.value.findIndex(a => a.id === viewingAgent.value.id)
      if (idx >= 0) agentList.value[idx].status = newStatus
      ElMessage.success(newStatus === 1 ? '已发布，所有用户可见' : '已下架，用户不可见')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const menu = [
  { key: 'dashboard', label: '运营概览', icon: DataAnalysis },
  { key: 'users', label: '用户管理', icon: User },
  { key: 'kb', label: '知识库管理', icon: Collection },
  { key: 'agents', label: '智能体管理', icon: Cpu },
  { key: 'models', label: '大模型管理', icon: Monitor },
  { key: 'announcement', label: '公告管理', icon: Notification },
]

// 侧边栏卡片颜色（橙黄绿青蓝红）
const cardBg = {
  dashboard: '#fdba74',
  users: '#fde68a',
  kb: '#a7f3d0',
  agents: '#99f6e4',
  models: '#bfdbfe',
  announcement: '#f0abfc',
  logout: '#fecaca',
}

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
  kbForm.value = { name: row.name, description: row.description, storeInstanceId: row.storeInstanceId }
  showAddKbDialog.value = true
}

async function handleSaveKb() {
  try {
    if (editingKb.value) {
      await request.put(`/kb/update/${editingKb.value.id}`, kbForm.value)
      ElMessage.success('修改成功')
    } else {
      await request.post('/kb/add', kbForm.value)
      ElMessage.success('创建成功')
    }
    showAddKbDialog.value = false
    editingKb.value = null
    kbForm.value = { name: '', description: '', storeInstanceId: 1 }
    await loadKbList()
  } catch (e) {
    ElMessage.error('操作失败：' + (e.response?.data?.message || e.message))
  }
}

function enterKbDetail(kb) {
  viewingKb.value = kb
  loadKbDocs(kb.id)
}

function closeKbDetail() {
  viewingKb.value = null
  kbDocs.value = []
}

async function loadKbDocs(kbId) {
  kbDocsLoading.value = true
  try {
    const res = await request.get('/document/list', { params: { kbId } })
    if (res.code === 200) kbDocs.value = res.data || []
  } catch (_) {} finally {
    kbDocsLoading.value = false
  }
}

function triggerKbUpload() {
  kbFileInputRef.value?.click()
}

async function handleKbFileSelected(e) {
  const file = e.target.files[0]
  if (!file || !viewingKb.value) return
  kbUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    await request.post('/document/upload', formData, {
      params: { kbId: viewingKb.value.id },
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('上传成功')
    e.target.value = ''
    loadKbDocs(viewingKb.value.id)
  } catch (e) {
    ElMessage.error('上传失败：' + (e.response?.data?.message || e.message))
  } finally {
    kbUploading.value = false
  }
}

async function handleKbDeleteDoc(doc) {
  try {
    await ElMessageBox.confirm(`确定删除「${doc.fileName || doc.name}」？`, '删除确认', { type: 'warning' })
    await request.delete(`/document/delete/${doc.id}`)
    ElMessage.success('已删除')
    loadKbDocs(viewingKb.value.id)
  } catch (_) {}
}

function handleKbCardMenu(cmd, kb) {
  if (cmd === 'edit') openEditKb(kb)
  else if (cmd === 'delete') handleDeleteKb(kb)
}

async function handleSaveKbConfig() {
  try {
    await request.put(`/kb/update/${viewingKb.value.id}`, {
      name: viewingKb.value.name,
      description: viewingKb.value.description
    })
    ElMessage.success('保存成功')
    await loadKbList()
    // 更新 viewingKb 引用
    const updated = kbList.value.find(k => k.id === viewingKb.value.id)
    if (updated) viewingKb.value = updated
  } catch (e) {
    ElMessage.error('保存失败：' + (e.response?.data?.message || e.message))
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
  agentForm.value = { name: '', description: '', systemPrompt: '', modelId: modelList.value[0]?.id || 1, status: 1, kbEnabled: 0, kbId: null }
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
    // RAG 开启时检索全部知识库，不绑定特定 KB
    const formData = { ...agentForm.value, kbId: null }
    if (editingAgent.value) {
      const data = { ...formData, id: editingAgent.value.id }
      await request.put('/agent/update', data)
      ElMessage.success('修改成功')
    } else {
      await request.post('/agent/add', formData)
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

async function viewConversationDetail(row) {
  showConvDetailDialog.value = true
  convDetailLoading.value = true
  try {
    const res = await request.get('/record/list', { params: { conversationId: row.id } })
    convDetailMessages.value = res.data || []
  } catch (e) {
    ElMessage.error('加载对话详情失败')
    convDetailMessages.value = []
  } finally {
    convDetailLoading.value = false
  }
}

// ========== 公告管理 ==========
const announcementList = ref([])
const showAnnouncementDialog = ref(false)
const editingAnnouncement = ref(null)
const announcementForm = ref({ title: '', content: '', status: 1 })
const announcementSaving = ref(false)

async function loadAnnouncementList() {
  try {
    const res = await getAnnouncementList()
    if (res.code === 200) {
      announcementList.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('加载公告列表失败')
  }
}

function openAddAnnouncement() {
  editingAnnouncement.value = null
  announcementForm.value = { title: '', content: '', status: 1 }
  showAnnouncementDialog.value = true
}

function openEditAnnouncement(row) {
  editingAnnouncement.value = row
  announcementForm.value = { title: row.title, content: row.content, status: row.status }
  showAnnouncementDialog.value = true
}

async function handleSaveAnnouncement() {
  if (!announcementForm.value.title.trim()) {
    ElMessage.warning('请输入公告标题')
    return
  }
  announcementSaving.value = true
  try {
    if (editingAnnouncement.value) {
      await updateAnnouncement({ id: editingAnnouncement.value.id, ...announcementForm.value })
      ElMessage.success('公告更新成功')
    } else {
      await addAnnouncement(announcementForm.value)
      ElMessage.success('公告创建成功')
    }
    showAnnouncementDialog.value = false
    editingAnnouncement.value = null
    await loadAnnouncementList()
  } catch (e) {
    ElMessage.error('操作失败：' + (e.response?.data?.message || e.message))
  } finally {
    announcementSaving.value = false
  }
}

async function handleDeleteAnnouncement(row) {
  try {
    await ElMessageBox.confirm(`确定删除公告「${row.title}」？`, '删除确认', { type: 'warning' })
    await deleteAnnouncement(row.id)
    ElMessage.success('删除成功')
    await loadAnnouncementList()
  } catch (_) {}
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

async function loadTokenSummary() {
  try {
    const res = await getAdminTokenSummary()
    if (res.code === 200) {
      const map = {}
      ;(res.data || []).forEach(item => { map[item.userId] = item.totalTokens })
      tokenMap.value = map
    }
  } catch (e) { /* 忽略 */ }
}

onMounted(() => {
  loadUsers()
  loadKbList()
  loadAgentList()
  loadModelList()
  loadTokenSummary()
  loadAnnouncementList()
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background: var(--bg-page);
}
.admin-sidebar {
  width: 280px;
  background: var(--bg-page);
  border-right: 3px solid var(--border-color);
  display: flex;
  flex-direction: column;
  padding: 20px 16px;
  flex-shrink: 0;
}
.sidebar-title {
  font-family: var(--font-marker);
  font-size: 18px;
  color: var(--fg-default);
  padding: 0 4px 16px;
  border-bottom: 3px solid var(--border-color);
  margin-bottom: 12px;
  overflow: hidden;
  white-space: nowrap;
}
.sidebar-watermark {
  font-family: var(--font-marker);
  font-size: 12px;
  color: rgba(45, 45, 45, 0.22);
  margin-left: 8px;
  letter-spacing: 3px;
  pointer-events: none;
  user-select: none;
  display: inline-block;
  transform: rotate(-2deg);
}
.sidebar-menu-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.dashboard-wrap {
  background: transparent;
  padding: 0;
  border: none;
  box-shadow: none;
}
.sidebar-card {
  padding: 4px;
  cursor: pointer;
  transition: all 0.2s;
  border: 3px solid transparent;
  box-shadow: var(--shadow-hard-sm);
}
.sidebar-card:hover {
  border-color: rgba(45,45,45,0.3);
  box-shadow: var(--shadow-hard);
  transform: translateX(2px);
}
.sidebar-card.active {
  border-color: rgba(45,45,45,0.7);
  box-shadow: var(--shadow-hard);
  transform: translateX(4px);
}
.sidebar-card-inner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(255,255,255,0.88);
  border: 2px solid rgba(255,255,255,0.95);
}
.sidebar-card-label {
  font-weight: 700;
  font-size: 14px;
  font-family: var(--font-hand);
  color: var(--fg-default);
}
.sidebar-card.active .sidebar-card-label {
  font-family: var(--font-marker);
}
.sidebar-card-arrow {
  margin-left: auto;
  font-size: 14px;
  color: var(--fg-muted);
  transition: all 0.2s;
}
.sidebar-card:hover .sidebar-card-arrow {
  color: var(--primary);
  transform: translateX(3px);
}
.sidebar-card.active .sidebar-card-arrow {
  color: var(--primary);
}
.sidebar-bottom {
  margin-top: auto;
  padding: 16px 4px 0;
  border-top: 3px solid var(--border-color);
}
.sidebar-bottom .logout-card:hover {
  border-color: var(--primary);
}
.sidebar-bottom .logout-card:hover .sidebar-card-label,
.sidebar-bottom .logout-card:hover .sidebar-card-arrow {
  color: var(--primary);
}
.sidebar-user {
  font-size: 12px;
  color: var(--fg-muted);
  margin-top: 10px;
  text-align: center;
  font-family: var(--font-hand);
}
.action-row { display: flex; gap: 6px; align-items: center; justify-content: center; flex-wrap: nowrap; }
.action-row .el-button { padding: 5px 10px; white-space: nowrap; flex-shrink: 0; }
.admin-content .el-table .el-table__cell { padding: 6px 6px !important; }
.admin-content .el-table th.el-table__cell { background: #e8e4da !important; font-weight: 700 !important; color: var(--fg-default) !important; }
.admin-content .el-button--small { padding: 4px 10px !important; font-size: 12px !important; }
.admin-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: var(--bg-page);
}
.content-panel {
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard);
  padding: 24px;
  border-radius: 0;
  position: relative;
}
.content-panel::before {
  content: '';
  position: absolute;
  top: -12px;
  left: 50%;
  width: 96px;
  height: 24px;
  background: rgba(0,0,0,0.08);
  transform: translateX(-50%) rotate(-1deg);
  pointer-events: none;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 3px solid var(--border-color);
}
.panel-header h2 {
  margin: 0;
  font-family: var(--font-marker);
  font-size: 20px;
  color: var(--fg-default);
  text-decoration: underline wavy;
  text-underline-offset: 4px;
}

/* ===== 智能体管理 ===== */
.agent-panel { padding: 0; background: transparent; box-shadow: none; }
.agent-panel .panel-header { padding: 24px 24px 16px; background: var(--bg-card); border: 3px solid var(--border-color); box-shadow: var(--shadow-hard); margin-bottom: 16px; border-bottom: 3px solid var(--border-color); }
.panel-subtitle { margin: 4px 0 0; font-size: 13px; color: var(--fg-muted); font-family: var(--font-hand); }
.agent-toolbar { display: flex; gap: 10px; align-items: center; }
.agent-search { width: 260px; }

/* 卡片网格 */
.agent-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.agent-card {
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease-out;
  position: relative;
  border-radius: 0;
}
.agent-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hard-lg);
}
.agent-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.agent-avatar {
  width: 48px; height: 48px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 20px; font-weight: 600;
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  overflow: hidden;
}
.agent-avatar .agent-avatar-img,
.agent-avatar-sm .agent-avatar-img {
  width: 100%; height: 100%;
  object-fit: cover;
  display: block;
}
.agent-avatar-sm {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 14px; font-weight: 600;
  border: 2px solid var(--border-color);
  flex-shrink: 0;
  overflow: hidden;
}
.agent-menu-btn {
  padding: 4px;
  color: var(--fg-muted); cursor: pointer;
}
.agent-menu-btn:hover { color: var(--fg-default); }
.agent-card-name {
  margin: 0 0 8px; font-family: var(--font-marker); font-size: 16px; color: var(--fg-default);
}
.agent-card-desc {
  margin: 0 0 16px; font-size: 13px; color: var(--fg-muted);
  line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden;
}
.agent-card-footer {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: var(--fg-muted); font-family: var(--font-hand);
}
.agent-card-footer .el-icon { vertical-align: middle; margin-right: 2px; }

/* 详情页 */
.agent-detail-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 24px; background: var(--bg-card); border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard); margin-bottom: 16px;
  border-radius: 0;
}
.back-btn { cursor: pointer; color: var(--fg-default); font-size: 18px; margin-right: 12px; }
.back-btn:hover { color: var(--primary); }
.agent-detail-breadcrumb { display: flex; align-items: center; }
.agent-detail-title { display: flex; align-items: center; gap: 10px; font-family: var(--font-marker); font-size: 18px; color: var(--fg-default); }
.status-tag { margin-left: 4px; }
.agent-detail-actions { display: flex; gap: 8px; }

.agent-detail-body {
  display: flex; gap: 16px; height: calc(100vh - 220px);
}
.agent-detail-left {
  width: 420px; flex-shrink: 0;
  background: var(--bg-card); border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard); padding: 20px;
  overflow-y: auto;
  border-radius: 0;
}
.agent-detail-right {
  flex: 1;
  background: var(--bg-card); border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard); padding: 20px;
  display: flex; flex-direction: column;
  border-radius: 0;
}
.agent-detail-right h3 { margin: 0 0 16px; font-family: var(--font-marker); font-size: 16px; color: var(--fg-default); text-decoration: underline wavy; text-underline-offset: 3px; }

.detail-section {
  margin-bottom: 24px;
  background: var(--bg-page);
  border: 2px dashed rgba(45,45,45,0.2);
  padding: 16px;
}
.detail-section h3 {
  margin: 0 0 12px; font-family: var(--font-marker); font-size: 14px; color: var(--fg-default);
  text-decoration: underline wavy; text-underline-offset: 3px;
}
.detail-form .form-row {
  margin-bottom: 14px;
  padding: 8px 10px;
  border-bottom: 1px dashed rgba(45,45,45,0.1);
}
.detail-form .form-row:last-child { border-bottom: none; }
.detail-form .form-row label {
  display: block; font-size: 13px; color: var(--fg-default); margin-bottom: 6px;
  font-family: var(--font-marker);
}
.switch-label { margin-left: 8px; font-size: 13px; color: var(--fg-muted); }
.system-prompt-input :deep(.el-textarea__inner) { font-family: 'Menlo', 'Monaco', monospace; font-size: 13px; }

/* 系统提示词编辑器 */
.prompt-editor-box {
  flex: 1; display: flex; flex-direction: column;
  border: 3px solid var(--border-color); box-shadow: var(--shadow-hard-sm);
  overflow: hidden;
}
.prompt-editor-box .prompt-textarea {
  flex: 1; display: flex;
}
.prompt-editor-box .prompt-textarea :deep(.el-textarea__inner) {
  min-height: 400px; height: 100% !important;
  font-family: 'Menlo', 'Monaco', monospace; font-size: 13px; line-height: 1.6;
  border: none; border-radius: 0; resize: vertical;
}

/* ===== 知识库管理卡片 ===== */
.kb-panel { padding: 0; background: transparent; box-shadow: none; }
.kb-panel .panel-header { padding: 24px 24px 16px; background: var(--bg-card); border: 3px solid var(--border-color); box-shadow: var(--shadow-hard); margin-bottom: 16px; border-bottom: 3px solid var(--border-color); }
.kb-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.kb-card {
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease-out;
  position: relative;
}
.kb-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hard-lg);
}
.kb-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.kb-avatar {
  width: 48px; height: 48px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(45,45,45,0.05);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
}
.kb-menu-btn { padding: 4px; color: var(--fg-muted); cursor: pointer; }
.kb-menu-btn:hover { color: var(--fg-default); }
.kb-card-name {
  margin: 0 0 8px; font-family: var(--font-marker); font-size: 16px; color: var(--fg-default);
}
.kb-card-desc {
  margin: 0 0 16px; font-size: 13px; color: var(--fg-muted);
  line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden;
}
.kb-card-footer {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: var(--fg-muted); font-family: var(--font-hand);
}
.kb-card-footer .el-icon { vertical-align: middle; margin-right: 2px; }

/* 知识库详情 */
.kb-detail-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 24px; background: var(--bg-card); border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard); margin-bottom: 16px;
}
.kb-detail-breadcrumb { display: flex; align-items: center; }
.kb-detail-title { display: flex; align-items: center; gap: 10px; font-family: var(--font-marker); font-size: 18px; color: var(--fg-default); }
.kb-detail-actions { display: flex; gap: 8px; }
.kb-detail-body {
  display: flex; gap: 16px; height: calc(100vh - 220px);
}
.kb-detail-left {
  width: 420px; flex-shrink: 0;
  overflow-y: auto;
}
.kb-detail-left .detail-section {
  background: var(--bg-card); border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm); padding: 20px;
  margin-bottom: 16px;
}
.kb-detail-left .detail-section h3 {
  margin: 0 0 16px; font-family: var(--font-marker); font-size: 16px;
  border-bottom: 2px dotted var(--border-color); padding-bottom: 8px;
}
.kb-detail-right {
  flex: 1; display: flex; flex-direction: column;
  background: var(--bg-card); border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm); padding: 20px;
}
.kb-detail-right h3 {
  margin: 0 0 16px; font-family: var(--font-marker); font-size: 16px;
  border-bottom: 2px dotted var(--border-color); padding-bottom: 8px;
}
.kb-doc-upload {
  display: flex; align-items: center; gap: 12px; margin-bottom: 16px;
}
.kb-doc-hint {
  font-size: 12px; color: var(--fg-muted); font-family: var(--font-hand);
}
.kb-doc-list {
  flex: 1; overflow-y: auto;
}
.kb-doc-empty {
  padding: 40px 0; text-align: center; color: var(--fg-muted); font-family: var(--font-hand); font-size: 14px;
}
.kb-doc-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border: 2px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm); margin-bottom: 8px;
  transition: all 0.2s;
}
.kb-doc-item:hover { box-shadow: var(--shadow-hard); }
.kb-doc-name { flex: 1; font-size: 14px; font-family: var(--font-hand); min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.kb-doc-time { font-size: 12px; color: var(--fg-muted); white-space: nowrap; }

/* ===== 大模型管理 ===== */
.model-panel { padding: 0; background: transparent; box-shadow: none; border: none; }

.model-providers {
  background: var(--bg-card); border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard); padding: 24px;
  margin-bottom: 16px;
  border-radius: 0;
  position: relative;
}
.model-providers::before {
  content: '';
  position: absolute;
  top: -12px;
  left: 50%;
  width: 96px;
  height: 24px;
  background: rgba(0,0,0,0.08);
  transform: translateX(-50%) rotate(2deg);
  pointer-events: none;
}
.provider-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  padding-bottom: 14px; border-bottom: 3px solid var(--border-color);
}
.provider-header h3 {
  margin: 0; font-family: var(--font-marker); font-size: 16px; color: var(--fg-default);
  text-decoration: underline wavy;
  text-underline-offset: 3px;
}
.provider-search { width: 260px; }
.provider-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px;
}
.provider-card {
  border: 3px solid var(--border-color); box-shadow: var(--shadow-hard-sm);
  padding: 18px; background: var(--bg-card);
  transition: all 0.2s ease-out;
  border-radius: 0;
}
.provider-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hard);
}
.provider-top {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;
}
.provider-name { font-family: var(--font-marker); font-size: 15px; color: var(--fg-default); }
.provider-desc { font-size: 12px; color: var(--fg-muted); margin-bottom: 14px; min-height: 18px; }
.provider-actions {
  display: flex; gap: 4px;
}
.provider-actions .el-button { padding: 4px 8px; font-size: 12px; }

.model-table-wrap {
  background: var(--bg-card); border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard); padding: 24px;
  border-radius: 0;
}
.model-table-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  padding-bottom: 12px; border-bottom: 3px solid var(--border-color);
}
.model-table-header h3 { margin: 0; font-family: var(--font-marker); font-size: 16px; color: var(--fg-default); text-decoration: underline wavy; text-underline-offset: 3px; }
.model-table-actions { display: flex; gap: 8px; }

/* 对话详情弹窗 */
.conv-detail-list { max-height: 500px; overflow-y: auto; }
.conv-detail-msg {
  padding: 12px 16px; margin-bottom: 12px;
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  border-radius: 0;
}
.conv-detail-msg.user { background: var(--sidebar-bg); }
.conv-detail-msg.assistant { background: var(--bg-card); }
.conv-detail-role { margin-bottom: 8px; font-family: var(--font-marker); }
.conv-detail-content { font-size: 14px; line-height: 1.6; color: var(--fg-default); white-space: pre-wrap; word-break: break-word; font-family: var(--font-hand); }
.conv-detail-time { font-size: 12px; color: var(--fg-muted); margin-top: 6px; }

/* ===== 公告管理 ===== */
.announcement-list { display: flex; flex-direction: column; gap: 10px; }
.announcement-empty { text-align: center; padding: 60px 0; color: var(--fg-muted); font-family: var(--font-hand); font-size: 15px; }
.announcement-item {
  display: flex; justify-content: space-between; align-items: flex-start;
  padding: 16px 20px; background: var(--bg-card);
  border: 3px solid var(--border-color); box-shadow: var(--shadow-hard-sm);
  transition: all 0.2s;
}
.announcement-item:hover { box-shadow: var(--shadow-hard); }
.announcement-item-left { flex: 1; min-width: 0; }
.announcement-item-title {
  display: flex; align-items: center; gap: 8px;
  font-family: var(--font-marker); font-size: 16px; color: var(--fg-default);
  margin-bottom: 8px;
}
.announcement-item-title .el-icon { color: var(--primary); }
.announcement-item-preview {
  font-family: var(--font-hand); font-size: 13px; color: var(--fg-muted);
  line-height: 1.6; white-space: pre-wrap; word-break: break-word;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical;
  overflow: hidden; margin-bottom: 6px;
}
.announcement-item-time { font-size: 12px; color: #c0c4cc; font-family: var(--font-hand); }
.announcement-item-actions { display: flex; gap: 6px; flex-shrink: 0; margin-left: 16px; }
</style>
