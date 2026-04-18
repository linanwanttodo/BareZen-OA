import request from './request'
import type { PageData } from '@/types/api'

export interface SysNotice {
  id: string
  noticeTitle: string
  noticeContent: string
  noticeType: string
  status: string
  scope: string
  createTime: string
  updateTime: string
}

export interface NoticeListParams {
  pageNum?: number
  pageSize?: number
  noticeTitle?: string
  noticeType?: string
  status?: string
}

/** 公告分页 */
export function getNoticePageApi(params: NoticeListParams) {
  return request.get<PageData<SysNotice>>('/system/v1/notices/page', { params })
}

/** 公告详情 */
export function getNoticeApi(id: string) {
  return request.get<SysNotice>(`/system/v1/notices/${id}`)
}

/** 新增公告 */
export function addNoticeApi(data: Partial<SysNotice>) {
  return request.post<void>('/system/v1/notices', data)
}

/** 修改公告 */
export function updateNoticeApi(data: Partial<SysNotice>) {
  return request.put<void>('/system/v1/notices', data)
}

/** 删除公告 */
export function deleteNoticeApi(id: string) {
  return request.delete<void>(`/system/v1/notices/${id}`)
}

/** 最新公告 */
export function getLatestNoticeApi() {
  return request.get<SysNotice>('/system/v1/notices/latest')
}

/** 已发布公告 */
export function getPublishedNoticesApi() {
  return request.get<SysNotice[]>('/system/v1/notices/published')
}

/** 发布公告 */
export function publishNoticeApi(id: string) {
  return request.put<void>(`/system/v1/notices/publish/${id}`)
}

/** 下架公告 */
export function revokeNoticeApi(id: string) {
  return request.put<void>(`/system/v1/notices/revoke/${id}`)
}
