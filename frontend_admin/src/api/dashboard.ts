import request from './request'

/** 审批效率统计项（对应后端 getApprovalStats 聚合结果） */
export interface ApprovalStat {
  type: string
  typeName: string
  total: number
  pending: number
  rejected: number
  avgHours: number
  rejectRate: number
}

/** 仪表盘图表数据 */
export interface ChartData {
  visitTrend: { date: string; count: number }[]
  roleDistribution: { name: string; value: number }[]
  approvalStats: ApprovalStat[]
}

/** 获取仪表盘图表数据 */
export function getDashboardChartsApi() {
  return request.get<ChartData>('/system/v1/dashboard/charts')
}
