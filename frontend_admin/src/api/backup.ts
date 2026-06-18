import request from './request'

export interface BackupInfo {
  filename: string
  fileSize: number
  createTime: string
}

/** 服务端 mysqldump 耗时可远超全局 15s 超时：中断只发生在前端，备份仍会继续完成并造成误报失败 */
const DUMP_TIMEOUT = 300000

/** 创建备份 */
export function createBackupApi() {
  return request.post<{ success: boolean; filename?: string; fileSize?: number; message?: string }>(
    '/system/v1/backup/create', undefined, { timeout: DUMP_TIMEOUT })
}

/** 备份列表 */
export function getBackupListApi() {
  return request.get<BackupInfo[]>('/system/v1/backup/list')
}

/** 下载备份 */
export function downloadBackupApi(filename: string) {
  return request.get(`/system/v1/backup/download/${encodeURIComponent(filename)}`,
    { responseType: 'blob', timeout: DUMP_TIMEOUT })
}

/** 删除备份 */
export function deleteBackupApi(filename: string) {
  return request.delete<void>(`/system/v1/backup/${encodeURIComponent(filename)}`)
}
