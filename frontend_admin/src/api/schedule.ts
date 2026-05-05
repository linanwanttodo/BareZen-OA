import request from './request'
import type { PageData } from '@/types/api'

export interface HrSchedule {
  id: string
  userId: string
  userName: string
  title: string
  description: string
  startTime: string
  endTime: string
  remindTime: string
  status: string // 0=未开始 1=进行中 2=已完成 3=已取消
  location: string
  createTime: string
}

export interface ScheduleListParams {
  pageNum?: number
  pageSize?: number
  userName?: string
  status?: string
  startDate?: string
  endDate?: string
}

/** 全员日程分页列表（管理端） */
export function getSchedulePageApi(params: ScheduleListParams) {
  return request.get<PageData<HrSchedule>>('/hr/v1/schedules/page', { params })
}

/** 新增日程（管理端代建，可指定 userId/userName） */
export function addScheduleApi(data: Partial<HrSchedule>) {
  return request.post<void>('/hr/v1/schedules', data)
}

/** 修改日程（管理端） */
export function updateScheduleApi(data: Partial<HrSchedule>) {
  return request.put<void>(`/hr/v1/schedules/${data.id}`, data)
}

/** 删除日程（管理端） */
export function deleteScheduleApi(id: string) {
  return request.delete<void>(`/hr/v1/schedules/${id}`)
}
