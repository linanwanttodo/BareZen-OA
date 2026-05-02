import request from './request'

/** 仪表盘图表数据 */
export interface ChartData {
  visitTrend: { date: string; count: number }[]
  roleDistribution: { name: string; value: number }[]
}

/** 获取仪表盘图表数据 */
export function getDashboardChartsApi() {
  return request.get<ChartData>('/system/v1/dashboard/charts')
}
