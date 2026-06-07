import request from './request'

/**
 * 获取最新一条启用的公告（用户端）
 */
export function getLatestAnnouncement() {
  return request.get('/announcement/latest')
}

/**
 * 查询所有公告（管理员用）
 */
export function getAnnouncementList() {
  return request.get('/announcement/admin/list')
}

/**
 * 新增公告
 */
export function addAnnouncement(data) {
  return request.post('/announcement/add', data)
}

/**
 * 更新公告
 */
export function updateAnnouncement(data) {
  return request.put('/announcement/update', data)
}

/**
 * 删除公告
 */
export function deleteAnnouncement(id) {
  return request.delete(`/announcement/delete/${id}`)
}
