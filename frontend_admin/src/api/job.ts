import request from './request'
import type { PageData } from '@/types/api'

export interface SysJob {
  id: string
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  misfirePolicy: string
  concurrent: string
  status: string
  remark: string
  createTime: string
}

/** 分页查询定时任务 */
export function getJobPageApi(params: { pageNum: number; pageSize: number; jobName?: string; jobGroup?: string; status?: string }) {
  return request.get<PageData<SysJob>>('/system/v1/jobs/page', { params })
}

/** 新增定时任务 */
export function addJobApi(data: Partial<SysJob>) {
  return request.post<void>('/system/v1/jobs', data)
}

/** 更新定时任务 */
export function updateJobApi(data: Partial<SysJob> & { id: string }) {
  return request.put<void>('/system/v1/jobs', data)
}

/** 删除定时任务 */
export function deleteJobApi(id: string) {
  return request.delete<void>(`/system/v1/jobs/${encodeURIComponent(id)}`)
}

/** 启动定时任务 */
export function resumeJobApi(id: string) {
  return request.put<void>(`/system/v1/jobs/${encodeURIComponent(id)}/resume`)
}

/** 暂停定时任务 */
export function pauseJobApi(id: string) {
  return request.put<void>(`/system/v1/jobs/${encodeURIComponent(id)}/pause`)
}

/** 立即执行定时任务 */
export function runJobApi(id: string) {
  return request.put<void>(`/system/v1/jobs/${encodeURIComponent(id)}/run`)
}
