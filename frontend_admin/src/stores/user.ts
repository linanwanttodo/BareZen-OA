import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { loginApi, getCurrentUserApi, logoutApi } from '@/api/auth'
import type { LoginParams, CurrentUser } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userInfo = ref<CurrentUser | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  /** 登录 */
  async function login(params: LoginParams) {
    const res = await loginApi(params)
    const data = res.data
    token.value = data.token
    setToken(data.token)
    // 登录成功后同步获取用户信息，避免路由守卫再单独请求
    await fetchUserInfo()
    return data
  }

  /** 获取当前用户信息 */
  async function fetchUserInfo() {
    const res = await getCurrentUserApi()
    const data = res.data
    userInfo.value = data
    roles.value = data.roles || []
    permissions.value = data.permissions || []
    return data
  }

  /** 退出登录 */
  async function logout() {
    try {
      await logoutApi()
    } finally {
      resetState()
    }
  }

  /** 重置状态 */
  function resetState() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    removeToken()
  }

  /** 检查权限 */
  function hasPermission(perm: string): boolean {
    return permissions.value.includes(perm) || permissions.value.includes('*:*:*')
  }

  return {
    token,
    userInfo,
    roles,
    permissions,
    login,
    fetchUserInfo,
    logout,
    resetState,
    hasPermission,
  }
})
