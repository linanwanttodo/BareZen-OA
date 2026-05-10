import { ref } from 'vue'
import type { LoginParams, CurrentUser } from '../types/api'

/**
 * 基础用户状态管理逻辑
 * 
 * 提供跨项目共享的用户状态管理功能，包括登录、登出、用户信息获取等
 * 
 * @param getToken 获取token的函数
 * @param setToken 设置token的函数
 * @param removeToken 移除token的函数
 * @param loginApi 登录API
 * @param getCurrentUserApi 获取当前用户信息API
 * @param logoutApi 登出API
 * @param connectNotification 连接WebSocket通知的函数
 * @param disconnectNotification 断开WebSocket通知的函数
 * @param onLogout 登出后的回调函数（可选）
 * @returns 用户状态管理对象
 */
export function createBaseUserStore(
  getToken: () => string | null,
  setToken: (token: string) => void,
  removeToken: () => void,
  loginApi: (data: LoginParams) => Promise<{ data: { token: string } }>,
  getCurrentUserApi: () => Promise<{ data: CurrentUser }>,
  logoutApi: () => Promise<void>,
  connectNotification: () => void,
  disconnectNotification: () => void,
  onLogout?: () => void
) {
  const token = ref<string>(getToken() || '')
  const userInfo = ref<CurrentUser | null>(null)

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
    userInfo.value = res.data
    return res.data
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
    removeToken()
    // 调用自定义登出回调
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