import request from './request'
import type { PageData } from '@/types/api'

export interface AssetItem {
  id: string
  assetNo: string
  assetName: string
  category: string
  categoryId: string
  brand: string
  model: string
  price: string
  purchaseDate: string
  deptName: string
  holderName: string
  location: string
  status: string
  scrapDate: string
  remark: string
  createTime: string
}

export interface AssetListParams {
  pageNum?: number
  pageSize?: number
  assetName?: string
  assetNo?: string
  category?: string
  categoryId?: string
  status?: string
  holderName?: string
}

/** 资产分类 */
export interface AssetCategory {
  id: string
  name: string
  parentId: string
  sort: number
  status: number
}

/** 资产借用记录 */
export interface AssetBorrowRecord {
  id: string
  assetId: string
  borrowerName: string
  borrowTime: string
  expectReturnTime: string
  returnTime: string
  remark: string
  status: string // 0=借用中 1=已归还
}

/** 资产分页列表 */
export function getAssetPageApi(params: AssetListParams) {
  return request.get<PageData<AssetItem>>('/asset/v1/items/page', { params })
}

/** 新增资产 */
export function addAssetApi(data: Partial<AssetItem>) {
  return request.post<void>('/asset/v1/items', data)
}

/** 更新资产 */
export function updateAssetApi(data: Partial<AssetItem>) {
  return request.put<void>('/asset/v1/items', data)
}

/** 删除资产 */
export function deleteAssetApi(id: string) {
  return request.delete<void>(`/asset/v1/items/${id}`)
}

/** 资产报废 */
export function scrapAssetApi(id: string) {
  return request.put<void>(`/asset/v1/items/${id}/scrap`)
}

/** 借用资产 */
export function borrowAssetApi(id: string, params: { borrowerName?: string; expectReturnTime?: string; remark?: string }) {
  return request.put<void>(`/asset/v1/items/${id}/borrow`, null, { params })
}

/** 归还资产 */
export function returnAssetApi(id: string) {
  return request.put<void>(`/asset/v1/items/${id}/return`)
}

/** 资产借用记录 */
export function getBorrowRecordsApi(id: string) {
  return request.get<AssetBorrowRecord[]>(`/asset/v1/items/${id}/borrow-records`)
}

/** 分类列表 */
export function getCategoryListApi(params?: { status?: number }) {
  return request.get<AssetCategory[]>('/asset/v1/categories', { params })
}

/** 新增分类 */
export function addCategoryApi(data: Partial<AssetCategory>) {
  return request.post<void>('/asset/v1/categories', data)
}

/** 更新分类 */
export function updateCategoryApi(data: Partial<AssetCategory>) {
  return request.put<void>(`/asset/v1/categories/${data.id}`, data)
}

/** 删除分类 */
export function deleteCategoryApi(id: string) {
  return request.delete<void>(`/asset/v1/categories/${id}`)
}
