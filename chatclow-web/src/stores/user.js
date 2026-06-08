import { defineStore } from 'pinia'
import { ref } from 'vue'
import { verify } from '../api/auth'

export const useUserStore = defineStore('user', () => {
  // 只从 localStorage 恢复 token，用户信息统一走服务器验证
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(0)
  const username = ref('')
  const role = ref(1)
  const isVerified = ref(false)

  function setUser(data) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    role.value = data.role || 1
    localStorage.setItem('token', data.token)
    localStorage.setItem('refreshToken', data.refreshToken || '')
    // 不再存 userId/username/role 到 localStorage
  }

  /**
   * 页面加载时验证 token 是否有效
   * 有效 → 从服务器获取用户信息
   * 无效 → 清除 token，跳到登录页
   */
  async function fetchUserInfo() {
    if (!token.value) {
      isVerified.value = true
      return false
    }
    try {
      const res = await verify()
      if (res.code === 200) {
        userId.value = res.data.userId
        username.value = res.data.username
        role.value = res.data.role || 1
        isVerified.value = true
        return true
      }
    } catch {
      // token 无效，清除
    }
    logout()
    isVerified.value = true
    return false
  }

  function logout() {
    token.value = ''
    userId.value = 0
    username.value = ''
    role.value = 1
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  return { token, userId, username, role, isVerified, setUser, fetchUserInfo, logout }
})
