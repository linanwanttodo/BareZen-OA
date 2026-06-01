import request from './request'
import type { ApiResult, DashboardData, Notice, Contact, TodoItem, CreateTodoDTO, UpdateTodoDTO, PageData, OvertimeRequest, TravelRequest, LeaveRequest, WorkLog, Schedule, Supply, SupplyRequest, FlowLog, FlowTask } from '@/types/api'

// 页面组件从 @/api/enterprise 导入业务实体类型，此处 re-export 保持入口统一
export type { OvertimeRequest, TravelRequest, LeaveRequest, WorkLog, Schedule, Supply, SupplyRequest, FlowLog, FlowTask } from '@/types/api'

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

// ==================== 加班申请 ====================

/** 我的加班申请列表 */
export function getMyOvertimesApi(params: { page?: number; pageSize?: number; status?: string }) {
  return request.get<any, ApiResult<PageData<OvertimeRequest>>>('/app/v1/overtimes', { params })
}

/** 提交加班申请 */
export function submitOvertimeApi(data: Partial<OvertimeRequest>) {
  return request.post<any, ApiResult<void>>('/app/v1/overtimes', data)
}

/** 撤销加班申请 */
export function revokeOvertimeApi(id: string) {
  return request.put<any, ApiResult<void>>(`/app/v1/overtimes/${encodeURIComponent(id)}/revoke`)
}

// ==================== 请假申请 ====================

/** 我的请假申请列表 */
export function getMyLeavesApi(params: { page?: number; pageSize?: number; status?: string }) {
  return request.get<any, ApiResult<PageData<LeaveRequest>>>('/app/v1/leaves', { params })
}

/** 提交请假申请 */
export function submitLeaveApi(data: Partial<LeaveRequest>) {
  return request.post<any, ApiResult<void>>('/app/v1/leaves', data)
}

/** 撤销请假申请 */
export function revokeLeaveApi(id: string) {
  return request.put<any, ApiResult<void>>(`/app/v1/leaves/${encodeURIComponent(id)}/revoke`)
}

// ==================== 出差申请 ====================

/** 我的出差申请列表 */
export function getMyTravelsApi(params: { page?: number; pageSize?: number; status?: string }) {
  return request.get<any, ApiResult<PageData<TravelRequest>>>('/app/v1/travels', { params })
}

/** 提交出差申请 */
export function submitTravelApi(data: Partial<TravelRequest>) {
  return request.post<any, ApiResult<void>>('/app/v1/travels', data)
}

/** 撤销出差申请 */
export function revokeTravelApi(id: string) {
  return request.put<any, ApiResult<void>>(`/app/v1/travels/${encodeURIComponent(id)}/revoke`)
}

// ==================== 工作日志 ====================

/** 我的工作日志列表 */
export function getMyWorkLogsApi(params: { page?: number; pageSize?: number; logType?: string }) {
  return request.get<any, ApiResult<PageData<WorkLog>>>('/app/v1/worklogs', { params })
}

/** 提交工作日志 */
export function submitWorkLogApi(data: Partial<WorkLog>) {
  return request.post<any, ApiResult<void>>('/app/v1/worklogs', data)
}

/** 更新工作日志 */
export function updateWorkLogApi(data: Partial<WorkLog>) {
  return request.put<any, ApiResult<void>>('/app/v1/worklogs', data)
}

/** 删除工作日志 */
export function deleteWorkLogApi(id: string | number) {
  return request.delete<any, ApiResult<void>>(`/app/v1/worklogs/${encodeURIComponent(String(id))}`)
}

// ==================== 个人日程 ====================

/** 我的日程列表 */
export function getMySchedulesApi(params: { page?: number; pageSize?: number; status?: string; date?: string }) {
  return request.get<any, ApiResult<PageData<Schedule>>>('/app/v1/schedules', { params })
}

/** 新增日程 */
export function addScheduleApi(data: Partial<Schedule>) {
  return request.post<any, ApiResult<void>>('/app/v1/schedules', data)
}

/** 更新日程 */
export function updateScheduleApi(data: Partial<Schedule>) {
  return request.put<any, ApiResult<void>>('/app/v1/schedules', data)
}

/** 删除日程 */
export function deleteScheduleApi(id: string) {
  return request.delete<any, ApiResult<void>>(`/app/v1/schedules/${encodeURIComponent(id)}`)
}

// ==================== 办公用品申领 ====================

/** 可申领用品列表 */
export function getSuppliesApi(params: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<any, ApiResult<PageData<Supply>>>('/app/v1/supplies', { params })
}

/** 我的申领记录 */
export function getMySupplyRequestsApi(params: { page?: number; pageSize?: number; status?: string }) {
  return request.get<any, ApiResult<PageData<SupplyRequest>>>('/app/v1/supplies/requests', { params })
}

/** 提交申领 */
export function submitSupplyRequestApi(data: Partial<SupplyRequest>) {
  return request.post<any, ApiResult<void>>('/app/v1/supplies/requests', data)
}

/** 撤销申领 */
export function revokeSupplyRequestApi(id: string) {
  return request.put<any, ApiResult<void>>(`/app/v1/supplies/requests/${encodeURIComponent(id)}/revoke`)
}

// ==================== 审批流程进度 ====================

/** 查询我的申请流程节点 */
export function getFlowLogsApi(bizType: string, bizId: string) {
  return request.get<any, ApiResult<FlowLog[]>>('/app/v1/flow-logs', {
    params: { bizType, bizId },
  })
}

/** 查询我的申请审批节点链（V36 多级审批引擎） */
export function getFlowTasksApi(bizType: string, bizId: string) {
  return request.get<any, ApiResult<FlowTask[]>>('/app/v1/flow-logs/flow-tasks', {
    params: { bizType, bizId },
  })
}
