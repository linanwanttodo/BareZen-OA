import request from './request'
import type { ApiResult, PageData, AssetCategory, AssetItem, AssetBorrowRecord } from '@/types/api'

export type { AssetCategory, AssetItem, AssetBorrowRecord } from '@/types/api'

// ==================== 资产借用 ====================

/** 启用中的资产分类列表（按排序号升序） */
export function getAssetCategoriesApi() {
  return request.get<any, ApiResult<AssetCategory[]>>('/app/v1/assets/categories')
}

/** 可借用资产分页列表（服务端强制只返回在库资产，支持分类与名称关键字筛选） */
export function getAvailableAssetsApi(params: {
  page?: number
  pageSize?: number
  categoryId?: string
  assetName?: string
}) {
  return request.get<any, ApiResult<PageData<AssetItem>>>('/app/v1/assets/items', { params })
}

/** 可借用资产详情（非在库资产不可见） */
export function getAssetDetailApi(id: string) {
  return request.get<any, ApiResult<AssetItem>>(`/app/v1/assets/items/${encodeURIComponent(id)}`)
}

/** 申请借用资产（借用人由服务端强制取当前登录用户） */
export function borrowAssetApi(id: string, params: { expectReturnTime?: string; remark?: string }) {
  return request.post<any, ApiResult<void>>(`/app/v1/assets/items/${encodeURIComponent(id)}/borrow`, null, { params })
}

/** 我的借用记录分页（服务端强制按当前登录用户过滤） */
export function getMyBorrowsApi(params: { page?: number; pageSize?: number }) {
  return request.get<any, ApiResult<PageData<AssetBorrowRecord>>>('/app/v1/assets/my-borrows', { params })
}

/** 归还借用资产（仅本人可归还） */
export function returnAssetApi(id: string) {
  return request.put<any, ApiResult<void>>(`/app/v1/assets/my-borrows/${encodeURIComponent(id)}/return`)
}
