import request from './request'
import type { PageData } from '@/types/api'

/** 文件/文件夹实体
 * id/folderId 用 string —— 后端是 Long，JS 的 Number 只能安全表示 2^53 以内的整数，
 * 而 MyBatis-Plus 雪花算法生成的 ID 超出该范围，Jackson 会序列化为字符串防精度丢失。
 */
export interface SysFile {
  id: string
  fileName: string
  originalName: string
  filePath: string
  fileUrl: string
  fileSize: number
  fileType: string
  storageType: string
  isFolder: number   // 0=文件 1=文件夹
  folderId: number | string  // 父文件夹ID（0=根目录时可能是 number 0，其它情况可能是 string）
  scope: string      // admin=管理端可见 public=所有用户可见
  sortOrder: number
  createTime: string
  updateTime: string
}

/** 文件分页参数 */
export interface FilePageParams {
  pageNum?: number
  pageSize?: number
  folderId?: string
  scope?: string
  fileType?: string
  originalName?: string
}

// ============ 文件操作 ============

/** 文件分页（按文件夹） */
export function getFilePageApi(params: FilePageParams) {
  return request.get<PageData<SysFile>>('/system/v1/files/page-by-folder', { params })
}

/** 上传文件（支持指定文件夹和可见范围） */
export function uploadFileApi(file: File, folderId?: string, scope?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (folderId) formData.append('folderId', folderId)
  if (scope) formData.append('scope', scope)
  return request.post<SysFile>('/system/v1/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 下载文件 URL */
export function downloadFileUrl(id: string) {
  return `${import.meta.env.VITE_APP_BASE_API || '/api'}/system/v1/files/download/${id}`
}

/** 下载文件（使用 axios 流式下载，支持认证） */
export function downloadFileAsBlobApi(id: string, fileName?: string) {
  return request.get<Blob>(`/system/v1/files/download/${id}`, {
    responseType: 'blob',
  })
}

/** 删除文件/文件夹 */
export function deleteFileApi(id: string) {
  return request.delete<void>(`/system/v1/files/${id}`)
}

/** 重命名 */
export function renameFileApi(id: string, name: string) {
  return request.put<SysFile>(`/system/v1/files/rename/${id}`, { name })
}

// ============ 文件夹操作 ============

/** 创建文件夹 */
export function createFolderApi(name: string, parentId?: string, scope?: string) {
  return request.post<SysFile>('/system/v1/files/folder', { name, parentId, scope })
}

/** 获取文件夹树 */
export function getFolderTreeApi(parentId?: string) {
  return request.get<SysFile[]>('/system/v1/files/tree', {
    params: parentId != null ? { parentId } : {},
  })
}

/** 获取子文件夹列表 */
export function getSubFoldersApi(folderId?: string) {
  return request.get<SysFile[]>('/system/v1/files/sub-folders', {
    params: folderId != null ? { folderId } : {},
  })
}
