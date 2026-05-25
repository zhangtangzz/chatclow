import request from './request'

// 获取知识库列表
export function getKbList() {
  return request.get('/kb/list')
}

// 新建知识库
export function addKb(data) {
  return request.post('/kb/add', data)
}

// 删除知识库
export function deleteKb(id) {
  return request.delete(`/kb/delete/${id}`)
}

// 上传文档到知识库
export function uploadDocument(file, kbId) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/document/upload', formData, {
    params: { kbId },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 获取知识库下的文档列表
export function getDocList(kbId) {
  return request.get('/document/list', { params: { kbId } })
}

// 删除文档
export function deleteDoc(id) {
  return request.delete(`/document/delete/${id}`)
}
