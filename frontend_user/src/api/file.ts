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
  folderId: number | string  // 父文件夹ID（0=根目录时可能是 number 0）
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

/** 文件分页（按文件夹，仅公开文件） */
export function getPublicFilePageApi(params: FilePageParams) {
  return request.get<PageData<SysFile>>('/system/v1/files/page-by-folder', {
    params: { ...params, scope: 'public' },
  })
}

/** 下载文件 URL */
export function downloadFileUrl(id: string) {
  return `${import.meta.env.VITE_APP_BASE_API || '/api'}/system/v1/files/download/${id}`
}

/** 下载文件（使用 axios 流式下载，支持认证） */
export function downloadFileAsBlobApi(id: string) {
  return request.get<Blob>(`/system/v1/files/download/${id}`, {
    responseType: 'blob',
  })
}

/** 获取公开文件夹树 */
export function getPublicFolderTreeApi(parentId?: string) {
  return request.get<SysFile[]>('/system/v1/files/tree', {
    params: parentId != null ? { parentId } : {},
  })
}
