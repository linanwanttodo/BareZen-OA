import request from './request'
import type { PageData } from '@/types/api'

export interface AttTravelRequest {
  id: string
  userId: string
  userName: string
  deptName: string
  destination: string
  purpose: string
  startTime: string
  endTime: string
  days: string
  status: string
  approverId: string
  approverName: string
  approveTime: string
  approveRemark: string
  createTime: string
}

export interface TravelListParams {
  pageNum?: number
  pageSize?: number
  status?: string
  onlyMine?: boolean
}

/** 出差分页列表 */
export function getTravelPageApi(params: TravelListParams) {
  return request.get<PageData<AttTravelRequest>>('/hr/v1/travels/page', { params })
}

/** 提交出差申请 */
export function submitTravelApi(data: Partial<AttTravelRequest>) {
  return request.post<void>('/hr/v1/travels', data)
}

/** 审批出差（pass/reject） */
export function approveTravelApi(id: string, action: 'pass' | 'reject', remark?: string) {
  return request.put<void>(`/hr/v1/travels/${id}/approve`, null, {
    params: { action, remark },
  })
}

/** 撤销出差申请 */
export function revokeTravelApi(id: string) {
  return request.put<void>(`/hr/v1/travels/${id}/revoke`)
}

/** 删除出差记录 */
export function deleteTravelApi(id: string) {
  return request.delete<void>(`/hr/v1/travels/${id}`)
}
