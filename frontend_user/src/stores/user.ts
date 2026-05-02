import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { connectNotification, disconnectNotification } from '@/utils/notification'
import { loginApi, getCurrentUserApi, logoutApi } from '@/api/auth'
import type { LoginParams, CurrentUser } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userInfo = ref<CurrentUser | null>(null)

  // 页面刷新时若已有令牌（未过期），恢复 WebSocket 连接
  if (token.value) {
    connectNotification()
  }

  async function login(params: LoginParams) {
    const res = await loginApi(params)
    const data = res.data
    token.value = data.token
    setToken(data.token)
    // 登录成功后建立 WebSocket 实时通知连接
    connectNotification()
    return data
  }

  async function fetchUserInfo() {
    const res = await getCurrentUserApi()
    userInfo.value = res.data
    return res.data
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      resetState()
    }
  }

  function resetState() {
    // 断开 WebSocket，避免登出后仍收到广播
    disconnectNotification()
    token.value = ''
    userInfo.value = null
    removeToken()
  }

  return {
    token,
    userInfo,
    login,
    fetchUserInfo,
    logout,
    resetState,
  }
})
