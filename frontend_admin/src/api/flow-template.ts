import request from './request'
import type { PageData } from '@/types/api'

/** 审批流程模板节点 */
export interface FlowTemplate {
  id: string
  bizType: string
  nodeSeq: number
  nodeName: string
  enabled: number
  createTime: string
  updateTime: string
}

/** 模板节点分页参数 */
export interface FlowTemplatePageParams {
  pageNum?: number
  pageSize?: number
  bizType?: string
}

/** 模板节点分页列表 */
export function getFlowTemplatePageApi(params: FlowTemplatePageParams) {
  return request.get<PageData<FlowTemplate>>('/system/v1/flow-templates/page', { params })
}

/** 新增模板节点 */
export function addFlowTemplateApi(data: Partial<FlowTemplate>) {
  return request.post<void>('/system/v1/flow-templates', data)
}

/** 修改模板节点 */
export function updateFlowTemplateApi(data: Partial<FlowTemplate>) {
  return request.put<void>(`/system/v1/flow-templates/${data.id}`, data)
}

/** 删除模板节点（逻辑删除） */
export function deleteFlowTemplateApi(id: string) {
  return request.delete<void>(`/system/v1/flow-templates/${id}`)
}