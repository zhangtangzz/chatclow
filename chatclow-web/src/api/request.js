import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

let isRefreshing = false
let refreshSubscribers = []

function onRefreshed(token) {
  refreshSubscribers.forEach(cb => cb(token))
  refreshSubscribers = []
}

function addRefreshSubscriber(cb) {
  refreshSubscribers.push(cb)
}

// 请求拦截器：自动带 JWT Token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理错误 + 自动刷新 Token
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        router.push('/login')
      }
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  async error => {
    const { config, response } = error
    // 不是 401 或已经是刷新请求 → 直接报错
    if (!response || response.status !== 401 || config.url === '/auth/refresh') {
      if (response && response.status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        router.push('/login')
      } else {
        ElMessage.error(error.message || '网络错误')
      }
      return Promise.reject(error)
    }

    // 尝试刷新 Token
    const refreshToken = localStorage.getItem('refreshToken')
    if (!refreshToken) {
      localStorage.removeItem('token')
      router.push('/login')
      return Promise.reject(error)
    }

    if (!isRefreshing) {
      isRefreshing = true
      try {
        const res = await axios.post('/api/auth/refresh', null, {
          params: { refreshToken }
        })
        if (res.data.code === 200) {
          const { token: newToken, refreshToken: newRefresh } = res.data.data
          localStorage.setItem('token', newToken)
          localStorage.setItem('refreshToken', newRefresh)
          isRefreshing = false
          onRefreshed(newToken)
          // 重试原始请求
          config.headers.Authorization = `Bearer ${newToken}`
          return request(config)
        }
      } catch (e) {
        // 刷新失败
      }
      isRefreshing = false
      refreshSubscribers = []
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      router.push('/login')
    } else {
      // 正在刷新中，排队等待
      return new Promise(resolve => {
        addRefreshSubscriber(newToken => {
          config.headers.Authorization = `Bearer ${newToken}`
          resolve(request(config))
        })
      })
    }
    return Promise.reject(error)
  }
)

export default request
