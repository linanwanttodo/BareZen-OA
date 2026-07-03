import request from './request'
import type { PageData } from '@/types/api'

export interface AttLeaveRequest {
  id: string
  userId: string
  userName: string
  deptName: string
  leaveType: string
  startTime: string
  endTime: string
  leaveDays: string
  reason: string
  status: string
  approverId: string
  approverName: string
  approveTime: string
  approveRemark: string
  createTime: string
}

export interface LeaveListParams {
  pageNum?: number
  pageSize?: number
  status?: string
  userId?: string
  onlyMine?: boolean
}

/** 请假分页列表 */
export function getLeavePageApi(params: LeaveListParams) {
  return request.get<PageData<AttLeaveRequest>>('/hr/v1/leaves/page', { params })
}

/** 提交请假申请 */
export function submitLeaveApi(data: Partial<AttLeaveRequest>) {
  return request.post<void>('/hr/v1/leaves', data)
}

/** 修改请假申请（仅本人、仅待审批状态） */
export function updateLeaveApi(id: string, data: Partial<AttLeaveRequest>) {
  return request.put<void>(`/hr/v1/leaves/${id}`, data)
}

/** 审批请假（pass/reject） */
export function approveLeaveApi(id: string, action: 'pass' | 'reject', remark?: string) {
  return request.put<void>(`/hr/v1/leaves/${id}/approve`, null, {
    params: { action, remark },
  })
}

/** 撤销请假申请 */
export function revokeLeaveApi(id: string) {
  return request.put<void>(`/hr/v1/leaves/${id}/revoke`)
}

/** 删除请假记录 */
export function deleteLeaveApi(id: string) {
  return request.delete<void>(`/hr/v1/leaves/${id}`)
}
