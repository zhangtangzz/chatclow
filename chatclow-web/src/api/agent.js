import request from './request'

// 获取智能体列表
export function getAgentList(userId) {
  return request.get(`/agent/list/${userId}`)
}

// 获取智能体详情
export function getAgentDetail(id) {
  return request.get(`/agent/${id}`)
}

// 新增智能体
export function addAgent(data) {
  return request.post('/agent/add', data)
}

// 更新智能体
export function updateAgent(data) {
  return request.put('/agent/update', data)
}

// 删除智能体
export function deleteAgent(id) {
  return request.delete(`/agent/delete/${id}`)
}

// 切换启用/禁用
export function toggleAgentStatus(id) {
  return request.put(`/agent/status/${id}`)
}
