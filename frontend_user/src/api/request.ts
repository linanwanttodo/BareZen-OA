import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { getToken, removeToken, setToken } from '@/utils/auth'
import { Toast } from '@/utils/toast'
import router from '@/router'
import type { ApiResult } from '@/types/api'
import { useUserStore } from '@/stores/user'

const BASE_URL = import.meta.env.VITE_APP_BASE_API || '/api'

const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  withCredentials: true, // 携带 httpOnly Cookie（跨域时由 CORS allowCredentials 放行）
  headers: {
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
  },
})

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
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response: AxiosResponse<ApiResult | Blob>) => {
    // 滑动 Token 刷新：仅在业务成功（code=200）的响应中信任新 Token
    const res = response.data as ApiResult
    const newToken = response.headers['x-access-token']
    if (newToken && res.code === 200) {
      setToken(newToken)
    }

    // 文件下载（blob 响应）直接返回原始响应
    if (response.config.responseType === 'blob') {
      return response as unknown as AxiosResponse<Blob>
    }

    // 参数校验失败
    if (res.code === 400) {
      Toast.warning(res.msg || '参数校验失败')
      return Promise.reject(new Error(res.msg))
    }

    if (res.code !== 200) {
      Toast.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }

    // 拦截器已解包，调用方拿到的是 ApiResult（类型上仍声明为 AxiosResponse 以兼容 axios）
    return res as unknown as AxiosResponse
  },
  (error) => {
    const status = error.response?.status
    const res = error.response?.data

    // 未登录或 Token 过期（HTTP 401）— 清除 Token 并跳转登录页。
    // 必须先于 blob 提前返回分支处理：blob 请求 401 同样需要清理会话
    if (status === 401) {
      // 完整清理本地状态：token、用户信息、Pinia 状态、WebSocket 连接
      useUserStore().resetState()
      // 跳转登录页并带上当前地址，登出后可以跳转回来
      const redirect = router.currentRoute.value.fullPath
      router.push({ path: '/auth/login', query: redirect !== '/auth/login' ? { redirect } : {} })
      Toast.warning('登录已过期，请重新登录')
      return Promise.reject(new Error(res?.msg || '未登录'))
    }

    // 无权限（HTTP 403）
    if (status === 403) {
      Toast.error(res?.msg || '没有访问权限')
      return Promise.reject(new Error(res?.msg || '无权限'))
    }

    // 非鉴权类失败：blob 请求返回原始 error 让调用者处理
    if (error.config?.responseType === 'blob') {
      return Promise.reject(error)
    }

    Toast.error(res?.msg || error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
