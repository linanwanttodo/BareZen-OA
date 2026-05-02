import request from './request'
import type { PageData } from '@/types/api'

export interface OnlineUser {
  tokenDigest?: string
  userId: string
  userName: string
  nickName: string
  loginIp: string
  loginLocation: string
  browser: string
  os: string
  loginTime: string
}

/** 在线用户列表 */
export function getOnlineUserListApi(params: { pageNum?: number; pageSize?: number }) {
  return request.get<PageData<OnlineUser>>('/system/v1/monitor/online/page', { params })
}

/** 在线用户数 */
export function getOnlineUserCountApi() {
  return request.get<number>('/system/v1/monitor/online/count')
}

/** 强制下线（使用 tokenDigest） */
export function forceLogoutApi(tokenDigest: string) {
  return request.delete<void>(`/system/v1/monitor/online/${encodeURIComponent(tokenDigest)}`)
}
