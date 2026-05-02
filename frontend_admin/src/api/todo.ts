import request from './request'
import type { PageData } from '@/types/api'

export interface TodoItem {
  type: string // leave / vehicle
  typeName: string
  id: string
  title: string
  applicantName: string
  startTime: string
  endTime: string
  createTime: string
}

export interface TodoSummary {
  leave: number
  vehicle: number
  total: number
}

/** 待办分页列表 */
export function getTodoPageApi(pageNum: number, pageSize: number) {
  return request.get<PageData<TodoItem>>('/system/v1/todo-center/page', {
    params: { pageNum, pageSize },
  })
}

/** 待办分类统计 */
export function getTodoSummaryApi() {
  return request.get<TodoSummary>('/system/v1/todo-center/summary')
}
