import request from './request'
import type { ApiResult, AttClockRecord } from '@/types/api'

// ==================== 考勤打卡 ====================

/** 上班打卡（打卡人身份从 JWT 解析，无需传参） */
export function clockInApi() {
  return request.post<any, ApiResult<AttClockRecord>>('/app/v1/attendance/clock-in')
}

/** 下班打卡 */
export function clockOutApi() {
  return request.post<any, ApiResult<AttClockRecord>>('/app/v1/attendance/clock-out')
}

/** 今日打卡状态（页面初始化） */
export function getTodayClockApi() {
  return request.get<any, ApiResult<AttClockRecord | null>>('/app/v1/attendance/clock/today')
}

/** 当月打卡记录（月度回顾日历数据源） */
export function getMonthClockApi(month: string) {
  return request.get<any, ApiResult<AttClockRecord[]>>('/app/v1/attendance/clock/month', {
    params: { month },
  })
}