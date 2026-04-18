import request from './request'
import type { SysMenu } from '@/types/menu'

/** 获取路由菜单（当前用户） */
export function getRoutersApi() {
  return request.get<SysMenu[]>('/system/v1/menus/routers')
}

/** 获取菜单树（用于选择上级） */
export function getMenuTreeSelectApi() {
  return request.get<SysMenu[]>('/system/v1/menus/treeselect')
}

/** 获取菜单列表（扁平） */
export function getMenuListApi() {
  return request.get<SysMenu[]>('/system/v1/menus/list')
}

/** 新增菜单 */
export function addMenuApi(data: Partial<SysMenu>) {
  return request.post<void>('/system/v1/menus', data)
}

/** 修改菜单 */
export function updateMenuApi(data: Partial<SysMenu>) {
  return request.put<void>('/system/v1/menus', data)
}

/** 删除菜单 */
export function deleteMenuApi(id: string) {
  return request.delete<void>(`/system/v1/menus/${id}`)
}
