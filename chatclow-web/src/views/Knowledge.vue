<template>
  <div class="kb-layout">
    <!-- 顶部导航栏 -->
    <div class="kb-header">
      <div class="header-left">
        <h2 class="page-title">知识库管理</h2>
      </div>
      <div class="header-right">
        <el-button
          type="primary"
          :icon="Plus"
          @click="showAddDialog = true"
          class="create-btn"
        >
          新建知识库
        </el-button>
      </div>
    </div>

    <!-- 知识库列表 -->
    <div class="kb-content">
      <!-- 空状态 -->
      <div v-if="kbList.length === 0" class="empty-state">
        <el-icon :size="64" color="#c0c4cc"><FolderOpened /></el-icon>
        <p class="empty-text">暂无知识库</p>
        <p class="empty-hint" v-if="isAdmin">点击右上角"新建知识库"开始创建</p>
      </div>

      <!-- 卡片网格 -->
      <div class="kb-grid" v-else>
        <div
          v-for="kb in kbList"
          :key="kb.id"
          class="kb-card"
          :class="{ 'is-admin': isAdmin }"
        >
          <!-- 卡片头部 -->
          <div class="card-header">
            <div class="kb-icon">
              <el-icon :size="24"><Collection /></el-icon>
            </div>
            <div class="kb-info">
              <h3 class="kb-name">{{ kb.name }}</h3>
              <el-tag
                size="small"
                :type="kb.status === 1 ? 'success' : 'info'"
                effect="plain"
                round
              >
                {{ kb.status === 1 ? '已启用' : '已禁用' }}
              </el-tag>
              <el-tag
                v-if="kb.userId === userStore.userId"
                size="small"
                type="warning"
                effect="plain"
                round
              >
                我的
              </el-tag>
            </div>
            <!-- 操作下拉菜单（管理员或创建者可见） -->
            <el-dropdown v-if="canManage(kb)" trigger="click" class="card-actions">
              <el-button :icon="MoreFilled" size="small" text type="info" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="openUploadDialog(kb)">
                    <el-icon><Upload /></el-icon>
                    上传文档
                  </el-dropdown-item>
                  <el-dropdown-item @click="editKb(kb)">
                    <el-icon><Edit /></el-icon>
                    编辑信息
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="deleteKb(kb.id)">
                    <el-icon><Delete /></el-icon>
                    <span style="color: #f56c6c">删除知识库</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <!-- 卡片描述 -->
          <div class="card-body">
            <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>

            <!-- 存储后端标签 -->
            <div class="storage-badge" v-if="kb.storeId">
              <el-tag size="small" effect="plain" type="warning">
                <el-icon><Coin /></el-icon>
                {{ getStoreName(kb.storeId) }}
              </el-tag>
            </div>
          </div>

          <!-- 文档列表 -->
          <div class="card-footer">
            <div
              class="doc-list"
              v-if="kb._docs && kb._docs.length > 0"
            >
              <div
                v-for="doc in kb._docs.slice(0, 3)"
                :key="doc.id"
                class="doc-item"
              >
                <el-icon><Document /></el-icon>
                <span class="doc-name">{{ doc.fileName || doc.title || '文档' }}</span>
                <el-button
                  v-if="canManage(kb)"
                  :icon="Delete"
                  size="small"
                  text
                  type="danger"
                  @click.stop="deleteDocument(doc.id)"
                />
              </div>
              <div v-if="kb._docs.length > 3" class="doc-more">
                还有 {{ kb._docs.length - 3 }} 个文档...
              </div>
            </div>

            <!-- 查看文档按钮 -->
            <el-button
              v-if="!kb._docs"
              text
              size="small"
              @click="loadDocs(kb)"
              :icon="FolderOpened"
              class="view-docs-btn"
            >
              查看文档 ({{ kb.docCount || 0 }})
            </el-button>
            <div v-else class="doc-count">
              <el-icon><Document /></el-icon>
              {{ kb._docs.length }} 个文档
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建/编辑知识库对话框 -->
    <el-dialog
      v-model="showAddDialog"
      :title="isEditMode ? '编辑知识库' : '新建知识库'"
      width="520px"
      :close-on-click-modal="false"
      class="kb-dialog"
    >
      <el-form :model="kbForm" label-width="100px" class="kb-form">
        <el-form-item label="知识库名称" required>
          <el-input
            v-model="kbForm.name"
            placeholder="请输入知识库名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="kbForm.description"
            type="textarea"
            :rows="3"
            placeholder="描述知识库的用途（可选）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <!-- 存储后端选择器（仅管理员可见，创建时显示） -->
        <el-form-item label="存储后端" v-if="isAdmin && !isEditMode">
          <el-select
            v-model="kbForm.storeId"
            placeholder="选择向量存储后端"
            style="width: 100%"
          >
            <el-option label="🗄️ MySQL（默认）" :value="1" />
            <el-option label="🐘 PostgreSQL + pgvector" :value="2" />
            <el-option label="🍃 MongoDB" :value="3" />
          </el-select>
          <div class="form-hint">
            <el-icon><InfoFilled /></el-icon>
            选择后不可修改，不同后端适用于不同场景
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddKb" :loading="submitting">
          {{ isEditMode ? '保存修改' : '创建知识库' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 上传文档对话框 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传文档"
      width="520px"
      class="upload-dialog"
    >
      <div class="upload-target">
        <el-icon><FolderOpened /></el-icon>
        <span>上传到：<strong>{{ uploadKb?.name }}</strong></span>
      </div>

      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="10"
        accept=".pdf,.txt,.md,.docx,.doc"
        :on-change="onFileChange"
        :on-exceed="onExceed"
        class="upload-area"
      >
        <el-icon :size="48" color="#67c23a"><UploadFilled /></el-icon>
        <div class="upload-text">
          <em>点击上传</em> 或拖拽文件到此处
        </div>
        <template #tip>
          <div class="upload-tip">
            支持 PDF / TXT / Markdown / Word，单文件 ≤ 50MB
          </div>
        </template>
      </el-upload>

      <!-- 已选文件列表 -->
      <div class="file-list" v-if="fileList.length > 0">
        <div class="file-list-title">已选择 {{ fileList.length }} 个文件：</div>
        <div
          v-for="(file, idx) in fileList"
          :key="idx"
          class="file-item"
        >
          <el-icon><Document /></el-icon>
          <span class="file-name">{{ file.name }}</span>
          <span class="file-size">({{ formatFileSize(file.size) }})</span>
          <el-button
            :icon="Close"
            size="small"
            text
            type="danger"
            @click="removeFile(idx)"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="uploading"
          @click="handleUpload"
          :disabled="fileList.length === 0"
        >
          开始上传并处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import {
  getKbList,
  addKb,
  updateKb,
  deleteKb as deleteKbApi,
  uploadDocument,
  getDocList,
  deleteDoc,
} from '../api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  MoreFilled,
  Document,
  Delete,
  FolderOpened,
  UploadFilled,
  Upload,
  Edit,
  Collection,
  Coin,
  InfoFilled,
  Close,
} from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()

// 判断是否是管理员
const isAdmin = computed(() => {
  return userStore.username === 'admin' || userStore.userId === 1
})

// 判断是否有管理权限（管理员或知识库创建者）
function canManage(kb) {
  return isAdmin.value || (kb.userId && kb.userId === userStore.userId)
}

const kbList = ref([])
const showAddDialog = ref(false)
const showUploadDialog = ref(false)
const uploading = ref(false)
const submitting = ref(false)
const uploadKb = ref(null)
const uploadRef = ref(null)
const fileList = ref([])
const isEditMode = ref(false)
const editingKbId = ref(null)

const kbForm = reactive({
  name: '',
  description: '',
  storeId: 1, // 默认 MySQL
})

// 存储后端名称映射
function getStoreName(storeId) {
  const map = {
    1: 'MySQL',
    2: 'PostgreSQL + pgvector',
    3: 'MongoDB',
  }
  return map[storeId] || '未知'
}

// 格式化文件大小
function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

async function loadKbList() {
  try {
    const res = await getKbList()
    kbList.value = (res.data || []).map((kb) => ({ ...kb, _docs: null }))
  } catch (e) {
    console.error('加载知识库失败:', e)
  }
}

async function loadDocs(kb) {
  try {
    const res = await getDocList(kb.id)
    kb._docs = res.data || []
  } catch (e) {
    console.error('加载文档失败:', e)
  }
}

function openUploadDialog(kb) {
  uploadKb.value = kb
  fileList.value = []
  showUploadDialog.value = true
}

function onFileChange(file, fileListRaw) {
  // 检查文件大小（50MB）
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 50MB')
    return false
  }
  fileList.value = fileListRaw
}

function onExceed() {
  ElMessage.warning('最多同时上传 10 个文件')
}

function removeFile(idx) {
  fileList.value.splice(idx, 1)
}

async function handleUpload() {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择文件')
    return
  }

  uploading.value = true
  let successCount = 0
  let failCount = 0

  try {
    for (const f of fileList.value) {
      try {
        await uploadDocument(f.raw, uploadKb.value.id)
        successCount++
      } catch (e) {
        failCount++
        console.error('上传失败:', f.name, e)
      }
    }

    if (successCount > 0) {
      ElMessage.success(`成功上传 ${successCount} 个文件，正在处理中...`)
    }
    if (failCount > 0) {
      ElMessage.error(`${failCount} 个文件上传失败`)
    }

    showUploadDialog.value = false
    loadKbList()
  } catch (e) {
    ElMessage.error('上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

function editKb(kb) {
  isEditMode.value = true
  editingKbId.value = kb.id
  kbForm.name = kb.name
  kbForm.description = kb.description || ''
  kbForm.storeId = kb.storeId || 1
  showAddDialog.value = true
}

async function handleAddKb() {
  if (!kbForm.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }

  submitting.value = true
  try {
    if (isEditMode.value) {
      // 编辑模式
      await updateKb(editingKbId.value, {
        name: kbForm.name,
        description: kbForm.description,
      })
      ElMessage.success('修改成功')
    } else {
      // 创建模式
      await addKb(kbForm)
      ElMessage.success('知识库创建成功')
    }

    showAddDialog.value = false
    resetForm()
    loadKbList()
  } catch (e) {
    console.error('操作失败:', e)
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  kbForm.name = ''
  kbForm.description = ''
  kbForm.storeId = 1
  isEditMode.value = false
  editingKbId.value = null
}

async function deleteKb(id) {
  try {
    await ElMessageBox.confirm(
      '确定删除该知识库？此操作将同时删除所有关联的文档和向量数据。',
      '确认删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteKbApi(id)
    ElMessage.success('删除成功')
    loadKbList()
  } catch (e) {
    // 用户取消
  }
}

async function deleteDocument(id) {
  try {
    await ElMessageBox.confirm('确定删除该文档？', '确认删除', {
      type: 'warning',
    })
    await deleteDoc(id)
    ElMessage.success('删除成功')
    loadKbList()
  } catch (e) {
    // 用户取消
  }
}

onMounted(() => {
  loadKbList()
})
</script>

<style scoped>
.kb-layout {
  min-height: 100vh;
  background: var(--bg-page);
}

.kb-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 32px;
  background: var(--bg-card);
  border-bottom: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.page-title {
  margin: 0;
  font-family: var(--font-marker);
  font-size: 22px;
  color: var(--fg-default);
}

.kb-content {
  padding: 32px;
  max-width: 1400px;
  margin: 0 auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120px 0;
}

.empty-text {
  font-size: 18px;
  color: var(--fg-muted);
  margin-top: 20px;
}

.empty-hint {
  font-size: 14px;
  color: var(--fg-muted);
  margin-top: 8px;
  opacity: 0.7;
}

.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.kb-card {
  background: var(--bg-card);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard);
  padding: 24px;
  transition: all 0.2s ease-out;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
}

.kb-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hard-lg);
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.kb-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  color: #fff;
  flex-shrink: 0;
}

.kb-info {
  flex: 1;
  min-width: 0;
}

.kb-name {
  margin: 0 0 8px 0;
  font-family: var(--font-marker);
  font-size: 17px;
  color: var(--fg-default);
}

.card-actions {
  flex-shrink: 0;
}

.card-body {
  margin-bottom: 16px;
}

.kb-desc {
  font-size: 14px;
  color: var(--fg-muted);
  line-height: 1.6;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.storage-badge {
  margin-top: 8px;
}

.card-footer {
  border-top: 3px solid var(--border-color);
  padding-top: 16px;
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg-page);
  border: 2px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  font-size: 13px;
  color: var(--fg-default);
  transition: all 0.2s;
}

.doc-item:hover {
  transform: translateX(2px);
  box-shadow: var(--shadow-hard);
}

.doc-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-more {
  font-size: 12px;
  color: var(--fg-muted);
  padding: 4px 12px;
}

.view-docs-btn {
  font-size: 13px;
}

.doc-count {
  font-size: 13px;
  color: var(--fg-muted);
  display: flex;
  align-items: center;
  gap: 6px;
}

.form-hint {
  font-size: 12px;
  color: var(--fg-muted);
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.upload-target {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--sidebar-bg);
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  margin-bottom: 20px;
  font-size: 14px;
  color: var(--fg-default);
}

.upload-area :deep(.el-upload-dragger) {
  border: 3px solid var(--border-color);
  box-shadow: var(--shadow-hard-sm);
  background: var(--bg-card);
  transition: all 0.2s;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: var(--primary);
  box-shadow: var(--shadow-hard);
  background: var(--bg-page);
}

.upload-text {
  font-size: 14px;
  color: var(--fg-muted);
  margin-top: 12px;
}

.upload-text em {
  color: var(--primary);
  font-style: normal;
}

.upload-tip {
  font-size: 12px;
  color: var(--fg-muted);
  margin-top: 8px;
}

.file-list {
  margin-top: 16px;
}

.file-list-title {
  font-size: 13px;
  color: var(--fg-default);
  margin-bottom: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg-page);
  border: 2px solid var(--border-color);
  margin-bottom: 6px;
  font-size: 13px;
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  color: var(--fg-muted);
  font-size: 12px;
}
</style>
