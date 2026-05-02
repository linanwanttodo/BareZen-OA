import request from './request'
import type { PageData } from '@/types/api'

export interface KbDocument {
  id: string
  title: string
  category: string
  content: string
  deptId: string
  deptName: string
  authorId: string
  authorName: string
  status: string
  viewCount: number
  remark: string
  createTime: string
  updateTime: string
}

export interface DocListParams {
  pageNum?: number
  pageSize?: number
  title?: string
  category?: string
  status?: string
}

/** 知识文档分页列表 */
export function getDocPageApi(params: DocListParams) {
  return request.get<PageData<KbDocument>>('/kb/v1/documents/page', { params })
}

/** 文档详情（自动 +1 浏览量） */
export function getDocApi(id: string) {
  return request.get<KbDocument>(`/kb/v1/documents/${id}`)
}

/** 新增文档 */
export function addDocApi(data: Partial<KbDocument>) {
  return request.post<void>('/kb/v1/documents', data)
}

/** 更新文档 */
export function updateDocApi(data: Partial<KbDocument>) {
  return request.put<void>('/kb/v1/documents', data)
}

/** 删除文档 */
export function deleteDocApi(id: string) {
  return request.delete<void>(`/kb/v1/documents/${id}`)
}

/** 发布/下线文档 */
export function changeDocStatusApi(id: string, status: string) {
  return request.put<void>(`/kb/v1/documents/${id}/status/${status}`)
}
