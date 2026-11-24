import request from './request'
import type { ApiResult, PageData, UserFile } from '@/types/api'

export type { UserFile } from '@/types/api'

// ==================== 我的文件 ====================

/** 我的文件分页列表（服务端强制按当前登录用户过滤，按上传时间倒序） */
export function getMyFilesApi(params: { page?: number; pageSize?: number; fileName?: string }) {
  return request.get<any, ApiResult<PageData<UserFile>>>('/app/v1/files', { params })
}

/** 上传文件（multipart/form-data；上传人由服务端强制取当前登录用户） */
export function uploadFileApi(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, ApiResult<UserFile>>('/app/v1/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000, // 大文件上传：默认 15s 超时不够用
  })
}

/** 下载我的文件（blob，仅本人文件可下载；文件名从响应头解析） */
export function downloadFileApi(id: string) {
  return request.get(`/app/v1/files/${encodeURIComponent(id)}/download`, { responseType: 'blob' })
}

/** 删除我的文件（仅本人可删除） */
export function deleteFileApi(id: string) {
  return request.delete<any, ApiResult<void>>(`/app/v1/files/${encodeURIComponent(id)}`)
}
