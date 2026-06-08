import request from './request'

// 登录
export function login(username, password) {
  return request.post('/auth/login', null, {
    params: { username, password }
  })
}

// 注册
export function register(username, password, email) {
  return request.post('/user/register', { username, password, email })
}

// 验证 Token 是否有效（页面加载时调用）
export function verify() {
  return request.get('/auth/verify')
}
export function refreshToken(refreshToken) {
  return request.post('/auth/refresh', null, { params: { refreshToken } })
}
