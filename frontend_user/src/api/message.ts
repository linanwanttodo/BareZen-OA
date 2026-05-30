import request from './request'
import type { ApiResult, PageData, SysMessage } from '@/types/api'

// ==================== 站内消息（3.1 消息中心） ====================

/** 分页查询我的站内信（isRead 空=全部 0=未读 1=已读） */
export function getMyMessagesApi(params: { page?: number; pageSize?: number; isRead?: number }) {
  return request.get<any, ApiResult<PageData<SysMessage>>>('/app/v1/messages', { params })
}

/** 未读消息数量 */
export function getUnreadCountApi() {
  return request.get<any, ApiResult<number>>('/app/v1/messages/unread-count')
}

/** 标记单条已读 */
export function markMessageReadApi(id: string) {
  return request.put<any, ApiResult<boolean>>(`/app/v1/messages/${encodeURIComponent(id)}/read`)
}

/** 全部标记已读 */
export function markAllMessagesReadApi() {
  return request.put<any, ApiResult<number>>('/app/v1/messages/read-all')
}

/** 查询单条消息详情（校验属主 + 自动标记已读） */
export function getMessageDetailApi(id: number) {
  return request.get<any, ApiResult<SysMessage>>(`/app/v1/messages/${id}`)
}