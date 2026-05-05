import request from './request'
import type { PageData } from '@/types/api'

/** 月度考勤统计（后端 AttClockMonthlyStat，SQL GROUP BY 聚合） */
export interface AttClockMonthlyStat {
  userId: string
  userName: string
  deptName: string
  workDays: number
  lateCount: number
  earlyCount: number
  absentCount: number
}

export interface ClockStatParams {
  month: string // yyyy-MM
  pageNum?: number
  pageSize?: number
  userName?: string
  deptId?: number
}

/** 月度考勤统计分页列表 */
export function getClockStatPageApi(params: ClockStatParams) {
  return request.get<PageData<AttClockMonthlyStat>>('/hr/v1/clock/page', { params })
}