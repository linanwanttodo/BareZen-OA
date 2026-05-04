import request from './request'
import type { PageData } from '@/types/api'

export interface SysUser {
  id: string
  userName: string
  nickName: string
  email: string
  phone: string
  sex: string
  avatar: string
  status: string
  loginIp: string
  loginDate: string
  remark: string
  createTime: string
  updateTime: string
}

export interface UserListParams {
  pageNum?: number
  pageSize?: number
  userName?: string
  status?: string
  phone?: string
}

/** 用户分页列表 */
export function getUserListApi(params: UserListParams) {
  return request.get<PageData<SysUser>>('/system/v1/users/page', { params })
}

/** 新增用户 */
export function addUserApi(data: Partial<SysUser> & { password?: string }) {
  return request.post<void>('/system/v1/users', data)
}

/** 批量创建账号（含强密码策略校验） */
export function batchAddUsersApi(data: Array<Partial<SysUser> & { password: string }>) {
  return request.post<number>('/system/v1/users/batch', data)
}

/** 修改用户 */
export function updateUserApi(data: Partial<SysUser>) {
  return request.put<void>('/system/v1/users', data)
}

/** 删除用户 */
export function deleteUserApi(id: string) {
  return request.delete<void>(`/system/v1/users/${encodeURIComponent(id)}`)
}

/** 批量删除用户 */
export function batchDeleteUserApi(ids: string[]) {
  return request.delete<void>('/system/v1/users/batch', { data: ids })
}

/** 修改用户状态 */
export function changeUserStatusApi(id: string, status: string) {
  return request.put<void>(`/system/v1/users/${encodeURIComponent(id)}/status/${encodeURIComponent(status)}`)
}

/** 获取用户角色ID列表 */
export function getUserRoleIdsApi(userId: string) {
  return request.get<string[]>(`/system/v1/users/${encodeURIComponent(userId)}/roles`)
}

/** 分配用户角色 */
export function assignUserRolesApi(userId: string, roleIds: string[]) {
  return request.put<void>(`/system/v1/users/${encodeURIComponent(userId)}/roles`, roleIds)
}

/** 重置密码 */
export function resetUserPasswordApi(userId: string, password: string) {
  return request.put<void>(`/system/v1/users/${encodeURIComponent(userId)}/password`, { password })
}

/** === 角色/部门联合筛选 === */

export interface FilterUsersParams {
  roleIds?: string
  deptIds?: string
  userName?: string
  status?: string
  pageNum?: number
  pageSize?: number
}

/** 按角色/部门筛选用户（分页） */
export function filterUsersApi(params: FilterUsersParams) {
  return request.get<PageData<SysUser>>('/system/v1/users/filter', { params })
}
