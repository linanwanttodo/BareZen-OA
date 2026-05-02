import request from './request'
import type { PageData } from '@/types/api'

export interface SysLogininfor {
  id: string
  userName: string
  ipAddr: string
  status: string
  msg: string
  loginTime: string
}

export interface LoginLogParams {
  pageNum?: number
  pageSize?: number
  userName?: string
  status?: string
}

/** 登录日志分页 */
export function getLoginLogPageApi(params: LoginLogParams) {
  return request.get<PageData<SysLogininfor>>('/system/v1/logininfor/page', { params })
}

/** 清空登录日志 */
export function cleanLoginLogApi() {
  return request.delete<void>('/system/v1/logininfor/clean')
}
