import request from './request'
import type { PageData } from '@/types/api'

/** API Key（明文仅在创建响应返回一次，列表仅 maskedKey） */
export interface ApiKeyItem {
  id: string
  keyName: string
  maskedKey: string
  scopes: string
  status: string
  expireTime: string
  createTime: string
}

/** 创建 API Key 的响应：明文只出现一次 */
export interface ApiKeyCreated {
  id: string
  apiKey: string
  keyName: string
  scopes: string
  status: string
  expireTime: string
}

export interface ApiKeyListParams {
  pageNum?: number
  pageSize?: number
  keyName?: string
}

/** Webhook 订阅 */
export interface WebhookItem {
  id: string
  name: string
  url: string
  secret: string
  events: string
  status: string
  createTime: string
}

export interface WebhookListParams {
  pageNum?: number
  pageSize?: number
  name?: string
}

/** API Key 分页列表（密钥脱敏） */
export function getApiKeyPageApi(params: ApiKeyListParams) {
  return request.get<PageData<ApiKeyItem>>('/system/v1/api-keys/page', { params })
}

/** 创建 API Key（明文仅本次响应返回） */
export function createApiKeyApi(data: { keyName: string; scopes?: string; expireTime?: string }) {
  return request.post<ApiKeyCreated>('/system/v1/api-keys', data)
}

/** 启用/禁用 API Key（0=启用 1=禁用） */
export function changeApiKeyStatusApi(id: string, status: '0' | '1') {
  return request.put<void>(`/system/v1/api-keys/${id}/status/${status}`)
}

/** 删除 API Key */
export function deleteApiKeyApi(id: string) {
  return request.delete<void>(`/system/v1/api-keys/${id}`)
}

/** Webhook 分页列表（secret 脱敏） */
export function getWebhookPageApi(params: WebhookListParams) {
  return request.get<PageData<WebhookItem>>('/system/v1/webhooks/page', { params })
}

/** 新增 Webhook 订阅 */
export function createWebhookApi(data: { name: string; url: string; secret?: string; events: string }) {
  return request.post<void>('/system/v1/webhooks', data)
}

/** 启用/禁用 Webhook（0=启用 1=禁用） */
export function changeWebhookStatusApi(id: string, status: '0' | '1') {
  return request.put<void>(`/system/v1/webhooks/${id}/status/${status}`)
}

/** 删除 Webhook 订阅 */
export function deleteWebhookApi(id: string) {
  return request.delete<void>(`/system/v1/webhooks/${id}`)
}