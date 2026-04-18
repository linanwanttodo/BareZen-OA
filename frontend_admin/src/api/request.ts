import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { getToken, removeToken } from '@/utils/auth'
import { Toast } from '@/utils/toast'
import type { ApiResult } from '@/types/api'

const BASE_URL = import.meta.env.VITE_APP_BASE_API || '/api'

const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
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
    const res = response.data

    // 文件下载（blob 响应直接返回）
    if (response.config.responseType === 'blob') {
      return response as any
    }

    // 未登录或 Token 过期 — 只清除 Token，路由守卫负责跳转并保留 redirect 参数
    if (res.code === 401) {
      removeToken()
      return Promise.reject(new Error(res.msg || '未登录'))
    }

    // 无权限
    if (res.code === 403) {
      Toast.error('没有访问权限')
      return Promise.reject(new Error(res.msg || '无权限'))
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
    const msg = error.response?.data?.msg || error.message || '网络异常'
    Toast.error(msg)
    return Promise.reject(error)
  }
)

export default service
