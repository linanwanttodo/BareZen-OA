import request from './request'
import type { PageData } from '@/types/api'

export interface SysOperLog {
  id: string
  title: string
  businessType: number
  method: string
  requestMethod: string
  operUser: string
  operIp: string
  operUrl: string
  className: string
  methodName: string
  operParam: string
  jsonResult: string
  status: number
  errorMsg: string
  costTime: number
  operTime: string
  createTime: string
}

export interface OperLogListParams {
  pageNum?: number
  pageSize?: number
  title?: string
  operUser?: string
  businessType?: number
  status?: number
}

/** 操作日志分页 */
export function getOperLogPageApi(params: OperLogListParams) {
  return request.get<PageData<SysOperLog>>('/system/v1/operlogs/page', { params })
}

/** 操作日志详情 */
export function getOperLogApi(id: string) {
  return request.get<SysOperLog>(`/system/v1/operlogs/${encodeURIComponent(id)}`)
}

/** 删除操作日志 */
export function deleteOperLogApi(id: string) {
  return request.delete<void>(`/system/v1/operlogs/${encodeURIComponent(id)}`)
}

/** 批量删除操作日志 */
export function batchDeleteOperLogApi(ids: string[]) {
  return request.delete<void>('/system/v1/operlogs/batch', { data: ids })
}

/** 清空指定天数前的日志 */
export function cleanOperLogApi(beforeDays = 30) {
  return request.delete<void>(`/system/v1/operlogs/clean?beforeDays=${beforeDays}`)
}
