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

// 刷新 Token
export function refreshToken(refreshToken) {
  return request.post('/auth/refresh', null, { params: { refreshToken } })
}
