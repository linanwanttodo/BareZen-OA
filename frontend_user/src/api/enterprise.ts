import request from './request'
import type { ApiResult, DashboardData, Notice, Contact, TodoItem, CreateTodoDTO, UpdateTodoDTO, PageData } from '@/types/api'

// ==================== 工作台 ====================

/** 工作台概览 */
export function getDashboardApi() {
  return request.get<any, ApiResult<DashboardData>>('/app/v1/dashboard')
}

// ==================== 通知公告 ====================

/** 公告列表 */
export function getNoticeListApi(params: { page?: number; pageSize?: number }) {
  return request.get<any, ApiResult<PageData<Notice>>>('/app/v1/notices', { params })
}

/** 公告详情 */
export function getNoticeDetailApi(id: string) {
  return request.get<any, ApiResult<Notice>>(`/app/v1/notices/${encodeURIComponent(id)}`)
}

// ==================== 通讯录 ====================

/** 通讯录列表 */
export function getContactListApi(params: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<any, ApiResult<PageData<Contact>>>('/app/v1/contacts', { params })
}

// ==================== 待办事项 ====================

/** 待办列表 */
export function getTodoListApi(params: { page?: number; pageSize?: number; status?: number }) {
  return request.get<any, ApiResult<PageData<TodoItem>>>('/app/v1/todos', { params })
}

/** 新增待办 */
export function addTodoApi(data: CreateTodoDTO) {
  return request.post<any, ApiResult<void>>('/app/v1/todos', data)
}

/** 更新待办 */
export function updateTodoApi(data: UpdateTodoDTO) {
  return request.put<any, ApiResult<void>>('/app/v1/todos', data)
}

/** 删除待办 */
export function deleteTodoApi(id: string) {
  return request.delete<any, ApiResult<void>>(`/app/v1/todos/${encodeURIComponent(id)}`)
}

// ==================== 个人资料 ====================

/** 更新个人资料 */
export function updateProfileApi(data: { nickName?: string; email?: string; phone?: string; sex?: string }) {
  return request.put<any, ApiResult<void>>('/app/v1/user/profile', data)
}
