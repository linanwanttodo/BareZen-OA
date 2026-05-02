import request from './request'

export interface BackupInfo {
  filename: string
  fileSize: number
  createTime: string
}

/** 创建备份 */
export function createBackupApi() {
  return request.post<{ success: boolean; filename?: string; fileSize?: number; message?: string }>('/system/v1/backup/create')
}

/** 备份列表 */
export function getBackupListApi() {
  return request.get<BackupInfo[]>('/system/v1/backup/list')
}

/** 下载备份 */
export function downloadBackupApi(filename: string) {
  return request.get(`/system/v1/backup/download/${encodeURIComponent(filename)}`, { responseType: 'blob' })
}

/** 删除备份 */
export function deleteBackupApi(filename: string) {
  return request.delete<void>(`/system/v1/backup/${encodeURIComponent(filename)}`)
}
