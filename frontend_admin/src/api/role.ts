import request from './request'
import type { PageData } from '@/types/api'

export interface SysRole {
  id: string
  roleName: string
  roleKey: string
  roleSort: number
  status: string
  remark: string
  createTime: string
  updateTime: string
}

export interface RoleListParams {
  pageNum?: number
  pageSize?: number
  roleName?: string
  roleKey?: string
  status?: string
}

/** 角色分页列表 */
export function getRoleListApi(params: RoleListParams) {
  return request.get<PageData<SysRole>>('/system/v1/roles/page', { params })
}

/** 新增角色 */
export function addRoleApi(data: Partial<SysRole>) {
  return request.post<void>('/system/v1/roles', data)
}

/** 修改角色 */
export function updateRoleApi(data: Partial<SysRole>) {
  return request.put<void>('/system/v1/roles', data)
}

/** 删除角色 */
export function deleteRoleApi(id: string) {
  return request.delete<void>(`/system/v1/roles/${encodeURIComponent(id)}`)
}

/** 批量删除角色 */
export function batchDeleteRoleApi(ids: string[]) {
  return request.delete<void>('/system/v1/roles/batch', { data: ids })
}

/** 获取角色已有菜单ID列表 */
export function getRoleMenuIdsApi(roleId: string) {
  return request.get<string[]>(`/system/v1/roles/${encodeURIComponent(roleId)}/menus`)
}

/** 分配菜单权限 */
export function assignRoleMenusApi(roleId: string, menuIds: string[]) {
  return request.put<void>(`/system/v1/roles/${encodeURIComponent(roleId)}/menus`, menuIds)
}

/** 修改角色状态 */
export function changeRoleStatusApi(id: string, status: string) {
  return request.put<void>(`/system/v1/roles/${encodeURIComponent(id)}/status/${encodeURIComponent(status)}`)
}

/** 获取所有角色（无分页，用于筛选器） */
export function getAllRolesListApi() {
  return request.get<SysRole[]>('/system/v1/roles/page', {
    params: { pageNum: 1, pageSize: 9999 },
  })
}
