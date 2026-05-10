import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { connectNotification, disconnectNotification } from '@/utils/notification'
import { loginApi, getCurrentUserApi, logoutApi } from '@/api/auth'
import type { LoginParams, CurrentUser } from '@/types/api'

// 基础用户状态管理逻辑（内联版本，与管理端保持一致）
function createBaseUserStore(
  getToken: () => string | null,
  setToken: (token: string) => void,
  removeToken: () => void,
  loginApi: (data: LoginParams) => Promise<{ data: { token: string } }>,
  getCurrentUserApi: () => Promise<{ data: CurrentUser }>,
  logoutApi: () => Promise<unknown>,
  connectNotification: () => void,
  disconnectNotification: () => void
) {
  const token = ref<string>(getToken() || '')
  const userInfo = ref<CurrentUser | null>(null)

  if (token.value) {
    connectNotification()
  }

  async function login(params: LoginParams) {
    const res = await loginApi(params)
    const data = res.data
    token.value = data.token
    setToken(data.token)
    connectNotification()
    await fetchUserInfo()
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
}

export const useUserStore = defineStore('user', () => {
  // 使用基础用户状态管理逻辑
  const baseUser = createBaseUserStore(
    getToken,
    setToken,
    removeToken,
    loginApi,
    getCurrentUserApi,
    logoutApi,
    connectNotification,
    disconnectNotification
  )

  return {
    ...baseUser,
  }
})
