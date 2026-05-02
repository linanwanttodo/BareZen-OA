import request from './request'
import type { PageData } from '@/types/api'

export interface AttOvertimeRequest {
  id: string
  userId: string
  userName: string
  deptName: string
  overtimeDate: string
  startTime: string
  endTime: string
  durationHours: string
  reason: string
  status: string
  approverId: string
  approverName: string
  approveTime: string
  approveRemark: string
  createTime: string
}

export interface OvertimeListParams {
  pageNum?: number
  pageSize?: number
  status?: string
  onlyMine?: boolean
}

/** 加班分页列表 */
export function getOvertimePageApi(params: OvertimeListParams) {
  return request.get<PageData<AttOvertimeRequest>>('/hr/v1/overtimes/page', { params })
}

/** 提交加班申请 */
export function submitOvertimeApi(data: Partial<AttOvertimeRequest>) {
  return request.post<void>('/hr/v1/overtimes', data)
}

/** 审批加班（pass/reject） */
export function approveOvertimeApi(id: string, action: 'pass' | 'reject', remark?: string) {
  return request.put<void>(`/hr/v1/overtimes/${id}/approve`, null, {
    params: { action, remark },
  })
}

/** 撤销加班申请 */
export function revokeOvertimeApi(id: string) {
  return request.put<void>(`/hr/v1/overtimes/${id}/revoke`)
}

/** 删除加班记录 */
export function deleteOvertimeApi(id: string) {
  return request.delete<void>(`/hr/v1/overtimes/${id}`)
}
