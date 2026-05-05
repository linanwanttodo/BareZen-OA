import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { getToken, removeToken, setToken } from '@/utils/auth'
import { Toast } from '@/utils/toast'
import type { ApiResult } from '@/types/api'

const BASE_URL = import.meta.env.VITE_APP_BASE_API || '/api'

// 401 整页跳转防重入：并发请求同时 401 时只执行一次跳转，避免重复刷新/重复弹窗
let redirectingToLogin = false

const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  withCredentials: true, // 携带 httpOnly Cookie（跨域时由 CORS allowCredentials 放行）
  headers: {
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
  },
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if (!config.headers['X-Requested-With']) {
      config.headers['X-Requested-With'] = 'XMLHttpRequest'
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    // 滑动 Token 刷新：仅在业务成功（code=200）的响应中信任新 Token
    const res = response.data
    const newToken = response.headers['x-access-token']
    if (newToken && res.code === 200) {
      setToken(newToken)
    }

    // 文件下载（blob 响应直接返回）
    if (response.config.responseType === 'blob') {
      return response as any
    }

    // 参数校验失败
    if (res.code === 400) {
      Toast.warning(res.msg || '参数校验失败')
      return Promise.reject(new Error(res.msg))
    }

    // 服务端异常
    if (res.code !== 200) {
      Toast.error(res.msg || '系统异常')
      return Promise.reject(new Error(res.msg))
    }

    return res as any
  },
  (error) => {
    const status = error.response?.status
    const res = error.response?.data

    // 未登录或 Token 过期（HTTP 401）— 清除 Token 并同步重置 Pinia 状态，
    // 否则路由守卫读到的 userStore.token 仍为旧值，不会跳登录页
    if (status === 401) {
      removeToken()
      // 动态导入避免 request.ts ↔ stores/user 循环依赖
      import('@/stores/user').then(({ useUserStore }) => {
        useUserStore().resetState()
      }).finally(() => {
        // 防重入：已有 401 在跳转中则跳过（页面即将整页刷新）
        if (redirectingToLogin) return
        // 已在登录页时不重复跳转
        if (!window.location.pathname.startsWith('/login')) {
          redirectingToLogin = true
          const redirect = encodeURIComponent(window.location.pathname + window.location.search)
          window.location.href = `/login?redirect=${redirect}`
        }
      })
      return Promise.reject(new Error(res?.msg || '未登录'))
    }

    // 无权限（HTTP 403）
    if (status === 403) {
      Toast.error(res?.msg || '没有访问权限')
      return Promise.reject(new Error(res?.msg || '无权限'))
    }

    const msg = res?.msg || error.message || '网络异常'
    Toast.error(msg)
    return Promise.reject(error)
  }
)

export default service
