import request from './request'
import type { PageData } from '@/types/api'

export interface SysDictType {
  id: string
  dictName: string
  dictType: string
  status: string
  createTime: string
  updateTime: string
}

export interface SysDictData {
  id: string
  dictCode: string
  dictLabel: string
  dictValue: string
  dictType: string
  dictSort: number
  cssClass: string
  listClass: string
  isDefault: string
  status: string
  createTime: string
  updateTime: string
}

export interface DictTypeListParams {
  pageNum?: number
  pageSize?: number
  dictName?: string
  dictType?: string
  status?: string
}

/** 字典类型分页 */
export function getDictTypePageApi(params: DictTypeListParams) {
  return request.get<PageData<SysDictType>>('/system/v1/dict/types/page', { params })
}

/** 字典类型详情 */
export function getDictTypeApi(id: string) {
  return request.get<SysDictType>(`/system/v1/dict/types/${encodeURIComponent(id)}`)
}

/** 新增字典类型 */
export function addDictTypeApi(data: Partial<SysDictType>) {
  return request.post<void>('/system/v1/dict/types', data)
}

/** 修改字典类型 */
export function updateDictTypeApi(data: Partial<SysDictType>) {
  return request.put<void>('/system/v1/dict/types', data)
}

/** 删除字典类型 */
export function deleteDictTypeApi(id: string) {
  return request.delete<void>(`/system/v1/dict/types/${encodeURIComponent(id)}`)
}

/** 根据类型获取字典数据 */
export function getDictDataByTypeApi(dictType: string) {
  return request.get<SysDictData[]>(`/system/v1/dict/data/type/${encodeURIComponent(dictType)}`)
}

/** 字典数据分页 */
export function getDictDataPageApi(params: { pageNum?: number; pageSize?: number; dictType?: string; dictLabel?: string; status?: string }) {
  return request.get<PageData<SysDictData>>('/system/v1/dict/data/page', { params })
}

/** 新增字典数据 */
export function addDictDataApi(data: Partial<SysDictData>) {
  return request.post<void>('/system/v1/dict/data', data)
}

/** 修改字典数据 */
export function updateDictDataApi(data: Partial<SysDictData>) {
  return request.put<void>('/system/v1/dict/data', data)
}

/** 删除字典数据 */
export function deleteDictDataApi(id: string) {
  return request.delete<void>(`/system/v1/dict/data/${encodeURIComponent(id)}`)
}
