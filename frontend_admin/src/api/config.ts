import request from './request'
import type { PageData } from '@/types/api'

export interface SysConfig {
  id: string
  configName: string
  configKey: string
  configValue: string
  configType: string
  status: string
  remark: string
  createTime: string
}

export interface ConfigListParams {
  pageNum?: number
  pageSize?: number
  configName?: string
  configKey?: string
}

/** 配置分页列表 */
export function getConfigPageApi(params: ConfigListParams) {
  return request.get<PageData<SysConfig>>('/system/v1/config/page', { params })
}

/** 获取配置详情 */
export function getConfigApi(configKey: string) {
  return request.get<string>(`/system/v1/config/${encodeURIComponent(configKey)}`)
}

/** 修改配置 */
export function updateConfigApi(data: Partial<SysConfig>) {
  return request.put<void>('/system/v1/config', data)
}

/** 清除配置缓存 */
export function clearConfigCacheApi(configKey: string) {
  return request.delete<void>(`/system/v1/config/cache/${encodeURIComponent(configKey)}`)
}

/** 清除全部配置缓存 */
export function clearConfigCacheAllApi() {
  return request.delete<void>('/system/v1/config/cache/all')
}
