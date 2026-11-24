import request from './request'

/** 插件信息 */
export interface PluginInfo {
  pluginId: string
  pluginName: string
  version: string
  description: string
  author: string
  status: string // INITIALIZED/STARTED/STOPPED 等
  enabled: boolean
  available: boolean
  dependencies: string[]
}

/** 插件列表 */
export function getPluginListApi() {
  return request.get<PluginInfo[]>('/system/v1/plugins')
}

/** 启动插件 */
export function startPluginApi(pluginId: string) {
  return request.post(`/system/v1/plugins/${pluginId}/start`)
}

/** 停止插件 */
export function stopPluginApi(pluginId: string) {
  return request.post(`/system/v1/plugins/${pluginId}/stop`)
}

/** 查询已启用的插件 */
export function getEnabledPluginsApi() {
  return request.get<PluginInfo[]>('/system/v1/plugins/enabled')
}

/** 插件详情 */
export function getPluginDetailApi(pluginId: string) {
  return request.get<PluginInfo>(`/system/v1/plugins/${encodeURIComponent(pluginId)}`)
}

/** 卸载插件 */
export function unloadPluginApi(pluginId: string) {
  return request.delete<void>(`/system/v1/plugins/${encodeURIComponent(pluginId)}`)
}
