import request from './request'
import type { PageData } from '@/types/api'

export interface KbDocument {
  id: string
  title: string
  category: string
  categoryId: string
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
  categoryId?: string
  status?: string
}

/** 知识库分类 */
export interface KbCategory {
  id: string
  name: string
  parentId: string
  sort: number
  status: number
}

/** 知识文档版本 */
export interface KbDocumentVersion {
  id: string
  docId: string
  versionNo: number
  title: string
  categoryId: string
  content: string
  operatorName: string
  createTime: string
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

/** 文档版本历史 */
export function getDocVersionsApi(id: string) {
  return request.get<KbDocumentVersion[]>(`/kb/v1/documents/${id}/versions`)
}

/** 分类列表 */
export function getKbCategoryListApi(params?: { status?: number }) {
  return request.get<KbCategory[]>('/kb/v1/categories', { params })
}

/** 新增分类 */
export function addKbCategoryApi(data: Partial<KbCategory>) {
  return request.post<void>('/kb/v1/categories', data)
}

/** 更新分类 */
export function updateKbCategoryApi(data: Partial<KbCategory>) {
  return request.put<void>(`/kb/v1/categories/${data.id}`, data)
}

/** 删除分类 */
export function deleteKbCategoryApi(id: string) {
  return request.delete<void>(`/kb/v1/categories/${id}`)
}
