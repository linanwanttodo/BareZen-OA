import request from './request'
import type { PageData } from '@/types/api'

/** 文件/文件夹实体（对应后端 SysFile） */
export interface SysFileItem {
  id: number
  fileName: string
  originalName: string
  filePath: string
  fileUrl: string
  fileSize: number
  fileType: string
  storageType: string
  /** 是否文件夹（0=文件，1=文件夹） */
  isFolder: number
  /** 父文件夹ID（0=根目录） */
  folderId: number
  /** 可见范围（admin=管理端可见，public=所有用户可见） */
  scope: string
  sortOrder: number
  createTime: string
  updateTime: string
  createBy: string
  updateBy: string
}

/** 上传文件（multipart/form-data） */
export function uploadFileApi(file: File, folderId?: number | null, scope?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (folderId != null && folderId > 0) formData.append('folderId', String(folderId))
  if (scope) formData.append('scope', scope)
  return request.post<SysFileItem>('/system/v1/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 下载文件（blob，需从响应头解析文件名） */
export function downloadFileApi(id: number) {
  return request.get(`/system/v1/files/download/${id}`, { responseType: 'blob' })
}

/** 删除文件/文件夹（文件夹递归删除） */
export function deleteFileApi(id: number) {
  return request.delete<void>(`/system/v1/files/${id}`)
}

/** 创建文件夹 */
export function createFolderApi(data: { name: string; parentId?: number | null; scope?: string }) {
  return request.post<SysFileItem>('/system/v1/files/folder', data)
}

/** 重命名文件/文件夹 */
export function renameFileApi(id: number, name: string) {
  return request.put<SysFileItem>(`/system/v1/files/rename/${id}`, { name })
}

/** 文件夹树（指定父级下的直接子文件夹） */
export function getFileTreeApi(parentId?: number | null) {
  return request.get<SysFileItem[]>('/system/v1/files/tree', {
    params: parentId != null && parentId > 0 ? { parentId } : {},
  })
}

/** 按文件夹分页查询内容（文件夹优先） */
export function pageByFolderApi(params: {
  pageNum: number
  pageSize: number
  folderId?: number | null
  scope?: string
  fileType?: string
  originalName?: string
}) {
  return request.get<PageData<SysFileItem>>('/system/v1/files/page-by-folder', { params })
}

/** 子文件夹列表 */
export function getSubFoldersApi(folderId?: number | null) {
  return request.get<SysFileItem[]>('/system/v1/files/sub-folders', {
    params: folderId != null && folderId > 0 ? { folderId } : {},
  })
}
