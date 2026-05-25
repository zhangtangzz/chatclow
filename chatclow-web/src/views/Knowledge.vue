<template>
  <div class="kb-layout">
    <!-- 顶部导航 -->
    <div class="kb-header">
      <el-button :icon="ArrowLeft" @click="$router.push('/')">返回聊天</el-button>
      <h3>知识库管理</h3>
      <el-button type="primary" :icon="Plus" @click="showAddDialog = true">新建知识库</el-button>
    </div>

    <!-- 知识库列表 -->
    <div class="kb-content">
      <el-row :gutter="20">
        <el-col :span="8" v-for="kb in kbList" :key="kb.id">
          <el-card shadow="hover" class="kb-card">
            <template #header>
              <div class="kb-card-header">
                <span>{{ kb.name }}</span>
                <el-dropdown>
                  <el-button :icon="MoreFilled" size="small" text />
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="openUploadDialog(kb)">上传文档</el-dropdown-item>
                      <el-dropdown-item @click="deleteKb(kb.id)">删除知识库</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
            <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>
            <div class="kb-meta">
              <el-tag size="small" :type="kb.status === 1 ? 'success' : 'info'">
                {{ kb.status === 1 ? '已启用' : '已禁用' }}
              </el-tag>
            </div>

            <!-- 文档列表 -->
            <div class="doc-list" v-if="kb._docs && kb._docs.length > 0">
              <div v-for="doc in kb._docs" :key="doc.id" class="doc-item">
                <el-icon><Document /></el-icon>
                <span>{{ doc.fileName || doc.title || '文档' }}</span>
                <el-button :icon="Delete" size="small" text type="danger"
                  @click.stop="deleteDocument(doc.id)" />
              </div>
            </div>
            <el-button text size="small" @click="loadDocs(kb)"
              v-if="!kb._docs" :icon="FolderOpened">
              查看文档
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 新建知识库对话框 -->
    <el-dialog v-model="showAddDialog" title="新建知识库" width="450px">
      <el-form :model="kbForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="kbForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="kbForm.description" type="textarea" :rows="3" placeholder="知识库描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddKb">确定</el-button>
      </template>
    </el-dialog>

    <!-- 上传文档对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="450px">
      <p style="color:#909399;margin-bottom:12px">上传到知识库: {{ uploadKb?.name }}</p>
      <el-upload ref="uploadRef" drag :auto-upload="false" :limit="5"
        accept=".pdf,.txt,.md,.docx,.doc"
        :on-change="onFileChange">
        <el-icon :size="48"><UploadFilled /></el-icon>
        <div>拖拽文件到此处，或 <em>点击上传</em></div>
        <template #tip>
          <p class="el-upload__tip">支持 PDF / TXT / Markdown / Word，单文件最大 50MB</p>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传并处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getKbList, addKb, deleteKb as deleteKbApi, uploadDocument, getDocList, deleteDoc } from '../api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, MoreFilled, Document, Delete, FolderOpened, UploadFilled } from '@element-plus/icons-vue'

const kbList = ref([])
const showAddDialog = ref(false)
const showUploadDialog = ref(false)
const uploading = ref(false)
const uploadKb = ref(null)
const uploadRef = ref(null)
const fileList = ref([])

const kbForm = reactive({ name: '', description: '' })

async function loadKbList() {
  try {
    const res = await getKbList()
    kbList.value = (res.data || []).map(kb => ({ ...kb, _docs: null }))
  } catch (e) { /* ignore */ }
}

async function loadDocs(kb) {
  try {
    const res = await getDocList(kb.id)
    kb._docs = res.data || []
  } catch (e) { /* ignore */ }
}

async function handleAddKb() {
  if (!kbForm.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  try {
    await addKb(kbForm)
    ElMessage.success('创建成功')
    showAddDialog.value = false
    kbForm.name = ''
    kbForm.description = ''
    loadKbList()
  } catch (e) { /* ignore */ }
}

async function deleteKb(id) {
  try {
    await ElMessageBox.confirm('确定删除该知识库？', '提示', { type: 'warning' })
    await deleteKbApi(id)
    ElMessage.success('删除成功')
    loadKbList()
  } catch (e) { /* ignore */ }
}

async function deleteDocument(id) {
  try {
    await deleteDoc(id)
    ElMessage.success('删除成功')
    loadKbList()
  } catch (e) { /* ignore */ }
}

function openUploadDialog(kb) {
  uploadKb.value = kb
  fileList.value = []
  showUploadDialog.value = true
}

function onFileChange(file) {
  fileList.value.push(file)
}

async function handleUpload() {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    for (const f of fileList.value) {
      await uploadDocument(f.raw, uploadKb.value.id)
    }
    ElMessage.success('上传成功，正在处理中...')
    showUploadDialog.value = false
    loadKbList()
  } catch (e) { /* ignore */ } finally {
    uploading.value = false
  }
}

onMounted(() => {
  loadKbList()
})
</script>

<style scoped>
.kb-layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.kb-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.kb-header h3 {
  margin: 0;
}

.kb-content {
  padding: 24px;
}

.kb-card {
  margin-bottom: 20px;
}

.kb-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.kb-desc {
  color: #909399;
  font-size: 13px;
  margin: 0 0 12px;
}

.kb-meta {
  margin-bottom: 12px;
}

.doc-list {
  border-top: 1px solid #ebeef5;
  padding-top: 8px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 0;
  font-size: 13px;
  color: #606266;
}

.doc-item span {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
