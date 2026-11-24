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

/** 定时任务详情 */
export function getJobApi(id: string) {
  return request.get<SysJob>(`/system/v1/jobs/${encodeURIComponent(id)}`)
}

/** 校验 Cron 表达式（合法返回 true） */
export function checkCronApi(cronExpression: string) {
  return request.get<boolean>('/system/v1/jobs/check-cron', {
    params: { cronExpression },
  })
}

/* ==================== 任务日志（sys_job_log） ==================== */

export interface SysJobLog {
  id: string
  jobId: string
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  /** 执行状态：'0'=成功 '1'=失败（RuoYi 约定） */
  status: string
  errorMsg: string
  startTime: string
  stopTime: string
  /** 耗时（毫秒） */
  duration: number
}

export interface JobLogListParams {
  pageNum?: number
  pageSize?: number
  jobId?: string
  status?: string
}

/** 分页查询任务执行日志 */
export function getJobLogPageApi(params: JobLogListParams) {
  return request.get<PageData<SysJobLog>>('/system/v1/job-logs/page', { params })
}
