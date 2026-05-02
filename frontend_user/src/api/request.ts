import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { getToken, removeToken, setToken } from '@/utils/auth'
import { Toast } from '@/utils/toast'
import router from '@/router'
import type { ApiResult } from '@/types/api'

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

    if (res.code === 401) {
      removeToken()
      router.push('/auth/login')
      Toast.warning('登录已过期，请重新登录')
      return Promise.reject(new Error(res.msg || '未登录'))
    }

    if (res.code !== 200) {
      Toast.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }

    // 拦截器已解包，调用方拿到的是 ApiResult（类型上仍声明为 AxiosResponse 以兼容 axios）
    return res as unknown as AxiosResponse
  },
  (error) => {
    // 如果是 blob 请求且失败了，返回原始 error 让调用者处理
    if (error.config?.responseType === 'blob') {
      return Promise.reject(error)
    }
    Toast.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
