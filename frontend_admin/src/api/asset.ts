import request from './request'
import type { PageData } from '@/types/api'

export interface AssetItem {
  id: string
  assetNo: string
  assetName: string
  category: string
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
  status?: string
  holderName?: string
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
