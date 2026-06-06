import request from './request'

/** 用户查看自己的 token 消耗明细（按会话维度） */
export function getUserTokenStats(userId) {
  return request.get('/user/token-stats', { params: { userId } })
}

/** 管理员查看所有用户的 token 汇总 */
export function getAdminTokenSummary() {
  return request.get('/admin/token-summary')
}

/** 管理员查看所有对话的平均响应时长 */
export function getAdminAvgResponseTime() {
  return request.get('/admin/avg-response-time')
}
