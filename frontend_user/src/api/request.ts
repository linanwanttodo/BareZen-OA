import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { getToken, removeToken } from '@/utils/auth'
import { Toast } from '@/utils/toast'
import router from '@/router'
import type { ApiResult } from '@/types/api'

const BASE_URL = import.meta.env.VITE_APP_BASE_API || '/api'

const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const res = response.data

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

    return res as any
  },
  (error) => {
    Toast.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
