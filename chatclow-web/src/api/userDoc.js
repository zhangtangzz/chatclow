import request from './request'

/** 上传个人文档 */
export function uploadUserDoc(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user-doc/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 获取个人文档列表 */
export function getUserDocList() {
  return request.get('/user-doc/list')
}

/** 删除个人文档 */
export function deleteUserDoc(id) {
  return request.delete(`/user-doc/delete/${id}`)
}

/** 获取文档数量 */
export function getUserDocCount() {
  return request.get('/user-doc/count')
}
