import request from './request'
import type { ApiResult, PageData, SalaryPaymentRecord, SalaryStructure } from '@/types/api'

export type { SalaryPaymentRecord, SalaryStructure } from '@/types/api'

// ==================== 我的工资条 ====================

/** 我的工资条分页列表（仅返回当前登录用户的记录） */
export function getMyPayrollsApi(params: { page?: number; pageSize?: number }) {
  return request.get<any, ApiResult<PageData<SalaryPaymentRecord>>>('/app/v1/salary/payrolls', { params })
}

/** 工资条详情（服务端校验归属，非本人返回错误） */
export function getPayrollDetailApi(id: string) {
  return request.get<any, ApiResult<SalaryPaymentRecord>>(`/app/v1/salary/payrolls/${encodeURIComponent(id)}`)
}

/** 我的当前工资结构 */
export function getMySalaryStructureApi() {
  return request.get<any, ApiResult<SalaryStructure>>('/app/v1/salary/structure')
}
