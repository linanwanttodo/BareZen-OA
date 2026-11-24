import request from './request'
import type { PageData } from '@/types/api'

/** 主数据基础字段（供应商/客户/物料同构：编码/名称/联系人/电话/状态） */
export interface SupplierInfo {
  id: string
  supplierCode: string
  supplierName: string
  contactPerson: string
  contactPhone: string
  status: string // 0=正常 1=停用
  remark: string
  createTime: string
  updateTime: string
}

export interface CustomerInfo {
  id: string
  customerCode: string
  customerName: string
  contactPerson: string
  contactPhone: string
  status: string // 0=正常 1=停用
  remark: string
  createTime: string
  updateTime: string
}

export interface MaterialInfo {
  id: string
  materialCode: string
  materialName: string
  contactPerson: string
  contactPhone: string
  status: string // 0=正常 1=停用
  remark: string
  createTime: string
  updateTime: string
}

export interface MasterDataPageParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: string
}

type MasterDataPayload<T> = Partial<T> & { id?: string | number }

// ---- 供应商 ----

export function getSupplierPageApi(params: MasterDataPageParams) {
  return request.get<PageData<SupplierInfo>>('/masterdata/v1/suppliers/page', { params })
}

export function getSupplierApi(id: string) {
  return request.get<SupplierInfo>(`/masterdata/v1/suppliers/${encodeURIComponent(id)}`)
}

export function addSupplierApi(data: MasterDataPayload<SupplierInfo>) {
  return request.post<SupplierInfo>('/masterdata/v1/suppliers', data)
}

export function updateSupplierApi(data: MasterDataPayload<SupplierInfo>) {
  return request.put<SupplierInfo>('/masterdata/v1/suppliers', data)
}

export function deleteSupplierApi(id: string) {
  return request.delete(`/masterdata/v1/suppliers/${encodeURIComponent(id)}`)
}

// ---- 客户 ----

export function getCustomerPageApi(params: MasterDataPageParams) {
  return request.get<PageData<CustomerInfo>>('/masterdata/v1/customers/page', { params })
}

export function getCustomerApi(id: string) {
  return request.get<CustomerInfo>(`/masterdata/v1/customers/${encodeURIComponent(id)}`)
}

export function addCustomerApi(data: MasterDataPayload<CustomerInfo>) {
  return request.post<CustomerInfo>('/masterdata/v1/customers', data)
}

export function updateCustomerApi(data: MasterDataPayload<CustomerInfo>) {
  return request.put<CustomerInfo>('/masterdata/v1/customers', data)
}

export function deleteCustomerApi(id: string) {
  return request.delete(`/masterdata/v1/customers/${encodeURIComponent(id)}`)
}

// ---- 物料 ----

export function getMaterialPageApi(params: MasterDataPageParams) {
  return request.get<PageData<MaterialInfo>>('/masterdata/v1/materials/page', { params })
}

export function getMaterialApi(id: string) {
  return request.get<MaterialInfo>(`/masterdata/v1/materials/${encodeURIComponent(id)}`)
}

export function addMaterialApi(data: MasterDataPayload<MaterialInfo>) {
  return request.post<MaterialInfo>('/masterdata/v1/materials', data)
}

export function updateMaterialApi(data: MasterDataPayload<MaterialInfo>) {
  return request.put<MaterialInfo>('/masterdata/v1/materials', data)
}

export function deleteMaterialApi(id: string) {
  return request.delete(`/masterdata/v1/materials/${encodeURIComponent(id)}`)
}
