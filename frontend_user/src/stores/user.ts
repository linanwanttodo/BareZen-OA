import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { loginApi, getCurrentUserApi, logoutApi } from '@/api/auth'
import type { LoginParams, CurrentUser } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userInfo = ref<CurrentUser | null>(null)

  async function login(params: LoginParams) {
    const res = await loginApi(params)
    const data = res.data
    token.value = data.token
    setToken(data.token)
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
    token.value = ''
    userInfo.value = null
    removeToken()
  }

  const isLoggedIn = ref(!!getToken())

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    fetchUserInfo,
    logout,
    resetState,
  }
})
