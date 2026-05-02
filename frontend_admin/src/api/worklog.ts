import request from './request'
import type { PageData } from '@/types/api'

export interface HrWorkLog {
  id: string
  userId: string
  userName: string
  deptName: string
  logType: string // daily / weekly
  logDate: string
  content: string
  reviewerId: string
  reviewerName: string
  reviewTime: string
  reviewRemark: string
  createTime: string
}

export interface WorkLogListParams {
  pageNum?: number
  pageSize?: number
  logType?: string
  userName?: string
  onlyMine?: boolean
}

/** 工作日志分页列表 */
export function getWorkLogPageApi(params: WorkLogListParams) {
  return request.get<PageData<HrWorkLog>>('/hr/v1/worklogs/page', { params })
}

/** 新增工作日志 */
export function addWorkLogApi(data: Partial<HrWorkLog>) {
  return request.post<void>('/hr/v1/worklogs', data)
}

/** 更新工作日志 */
export function updateWorkLogApi(data: Partial<HrWorkLog>) {
  return request.put<void>('/hr/v1/worklogs', data)
}

/** 删除工作日志 */
export function deleteWorkLogApi(id: string) {
  return request.delete<void>(`/hr/v1/worklogs/${id}`)
}

/** 批阅工作日志 */
export function reviewWorkLogApi(id: string, remark?: string) {
  return request.put<void>(`/hr/v1/worklogs/${id}/review`, null, {
    params: { remark },
  })
}
