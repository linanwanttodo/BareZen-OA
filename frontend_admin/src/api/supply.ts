import request from './request'
import type { PageData } from '@/types/api'

export interface OfficeSupply {
  id: string
  supplyName: string
  category: string
  spec: string
  stock: number
  unit: string
  status: string
  remark: string
  createTime: string
}

export interface OfficeSupplyRequest {
  id: string
  supplyId: string
  supplyName: string
  userId: string
  userName: string
  deptName: string
  quantity: number
  reason: string
  status: string
  approverId: string
  approverName: string
  approveTime: string
  approveRemark: string
  createTime: string
}

export interface SupplyListParams {
  pageNum?: number
  pageSize?: number
  supplyName?: string
  category?: string
}

/** 用品分页列表 */
export function getSupplyPageApi(params: SupplyListParams) {
  return request.get<PageData<OfficeSupply>>('/office/v1/supplies/page', { params })
}

/** 新增用品 */
export function addSupplyApi(data: Partial<OfficeSupply>) {
  return request.post<void>('/office/v1/supplies', data)
}

/** 更新用品 */
export function updateSupplyApi(data: Partial<OfficeSupply>) {
  return request.put<void>('/office/v1/supplies', data)
}

/** 删除用品 */
export function deleteSupplyApi(id: string) {
  return request.delete<void>(`/office/v1/supplies/${id}`)
}

/** 申领记录分页列表 */
export function getSupplyRequestPageApi(pageNum: number, pageSize: number, status?: string) {
  return request.get<PageData<OfficeSupplyRequest>>('/office/v1/supplies/requests/page', {
    params: { pageNum, pageSize, status },
  })
}

/** 审批申领（pass/reject） */
export function approveSupplyRequestApi(id: string, action: 'pass' | 'reject', remark?: string) {
  return request.put<void>(`/office/v1/supplies/requests/${id}/approve`, null, {
    params: { action, remark },
  })
}

/** 删除申领记录 */
export function deleteSupplyRequestApi(id: string) {
  return request.delete<void>(`/office/v1/supplies/requests/${id}`)
}
