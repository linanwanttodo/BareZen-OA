import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { connectNotification, disconnectNotification } from '@/utils/notification'
import { loginApi, getCurrentUserApi, logoutApi } from '@/api/auth'
import { usePermissionStore } from '@/stores/permission'
import type { LoginParams, CurrentUser } from '@/types/api'

// 基础用户状态管理逻辑（内联版本，后续可提取到共享包）
function createBaseUserStore(
  getToken: () => string | null,
  setToken: (token: string) => void,
  removeToken: () => void,
  loginApi: (data: LoginParams) => Promise<{ data: { token: string } }>,
  getCurrentUserApi: () => Promise<{ data: CurrentUser }>,
  logoutApi: () => Promise<unknown>,
  connectNotification: () => void,
  disconnectNotification: () => void,
  onLogout?: () => void
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
    } catch {
      // 吞掉异常，本地清理照常执行
    } finally {
      resetState()
    }
  }

  function resetState() {
    disconnectNotification()
    token.value = ''
    userInfo.value = null
    removeToken()
    if (onLogout) {
      onLogout()
    }
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
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  // 使用基础用户状态管理逻辑
  const baseUser = createBaseUserStore(
    getToken,
    setToken,
    removeToken,
    loginApi,
    getCurrentUserApi,
    logoutApi,
    connectNotification,
    disconnectNotification,
    () => {
      // 登出后清空权限状态与菜单/路由缓存，避免旧账号权限残留
      roles.value = []
      permissions.value = []
      usePermissionStore().resetPermission()
    }
  )

  /** 获取当前用户信息（重写以包含角色和权限） */
  async function fetchUserInfo() {
    const data = await baseUser.fetchUserInfo()
    roles.value = data.roles || []
    permissions.value = data.permissions || []
    return data
  }

  /** 检查权限 */
  function hasPermission(perm: string): boolean {
    return permissions.value.includes(perm) || permissions.value.includes('*:*:*')
  }

  return {
    // 导出基础用户状态
    ...baseUser,
    // 导出扩展的权限相关状态和方法
    roles,
    permissions,
    fetchUserInfo,
    hasPermission,
  }
})
