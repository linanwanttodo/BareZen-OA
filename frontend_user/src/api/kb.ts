import request from './request'
import type { ApiResult, PageData, KbCategory, KbDocument } from '@/types/api'

export type { KbCategory, KbDocument } from '@/types/api'

// ==================== 知识库（只读浏览） ====================

/** 启用中的知识分类列表（按排序号升序） */
export function getKbCategoriesApi() {
  return request.get<any, ApiResult<KbCategory[]>>('/app/v1/kb/categories')
}

/** 已发布文档分页列表（服务端强制过滤未发布状态，支持分类与标题关键字筛选） */
export function getKbDocumentsApi(params: {
  page?: number
  pageSize?: number
  categoryId?: string
  title?: string
}) {
  return request.get<any, ApiResult<PageData<KbDocument>>>('/app/v1/kb/documents', { params })
}

/** 文档详情（未发布文档返回错误；查看时浏览量 +1） */
export function getKbDocumentDetailApi(id: string) {
  return request.get<any, ApiResult<KbDocument>>(`/app/v1/kb/documents/${encodeURIComponent(id)}`)
}
