import request from './request'

export interface ApprovalFlowNode {
  id: string
  bizType: string
  bizId: string
  nodeName: string
  nodeStatus: string
  operatorId: string
  operatorName: string
  remark: string
  createTime: string
}

/** 查询申请单流程节点（时间正序） */
export function getFlowLogsApi(bizType: string, bizId: string) {
  return request.get<ApprovalFlowNode[]>('/system/v1/flow-logs', {
    params: { bizType, bizId },
  })
}
