import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { connectNotification, disconnectNotification } from '@/utils/notification'
import { loginApi, getCurrentUserApi, logoutApi } from '@/api/auth'
import { usePermissionStore } from '@/stores/permission'
import type { LoginParams, CurrentUser } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userInfo = ref<CurrentUser | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  // 页面刷新时若已有令牌（未过期），恢复 WebSocket 连接
  if (token.value) {
    connectNotification()
  }

  /** 登录 */
  async function login(params: LoginParams) {
    const res = await loginApi(params)
    const data = res.data
    token.value = data.token
    setToken(data.token)
    // 登录成功后建立 WebSocket 实时通知连接
    connectNotification()
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
    } catch {
      // 登出接口失败（如 token 已被拉黑/过期返回 401）属预期：
      // 吞掉异常，本地清理照常执行，避免调用方再弹「未登录」造成双弹窗
    } finally {
      resetState()
    }
  }

  /** 重置状态 */
  function resetState() {
    // 断开 WebSocket，避免登出后仍收到广播
    disconnectNotification()
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    removeToken()
    // 同步清空菜单/路由缓存，避免换账号登录后沿用上一个账号的动态路由
    usePermissionStore().resetPermission()
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
