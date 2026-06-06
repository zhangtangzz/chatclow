import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(Number(localStorage.getItem('userId')) || 0)
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(Number(localStorage.getItem('role')) || 1)

  function setUser(data) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    role.value = data.role || 1
    localStorage.setItem('token', data.token)
    localStorage.setItem('refreshToken', data.refreshToken || '')
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role || 1)
  }

  function logout() {
    token.value = ''
    userId.value = 0
    username.value = ''
    role.value = 1
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }

  return { token, userId, username, role, setUser, logout }
})
