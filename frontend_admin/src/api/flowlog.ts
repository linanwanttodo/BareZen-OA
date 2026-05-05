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

/** 审批节点任务（V36 多级审批引擎） */
export interface ApprovalFlowTask {
  id: string
  bizType: string
  bizId: string
  nodeSeq: number
  nodeName: string
  status: string
  approverId: string
  approverName: string
  approveRemark: string
  approveTime: string
  createTime: string
}

/** 查询申请单流程节点（时间正序） */
export function getFlowLogsApi(bizType: string, bizId: string) {
  return request.get<ApprovalFlowNode[]>('/system/v1/flow-logs', {
    params: { bizType, bizId },
  })
}

/** 查询申请单审批节点链（node_seq 升序；未接入引擎的业务返回空链） */
export function getFlowTasksApi(bizType: string, bizId: string) {
  return request.get<ApprovalFlowTask[]>('/system/v1/flow-logs/flow-tasks', {
    params: { bizType, bizId },
  })
}