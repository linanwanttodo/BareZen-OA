import request from './request'
import type { PageData } from '@/types/api'

/** 工资结构 */
export interface SalaryStructure {
  id: string
  userId: string
  userName: string
  baseSalary: number
  performanceSalary: number
  positionAllowance: number
  transportAllowance: number
  mealAllowance: number
  otherAllowance: number
  socialInsurancePersonal: number
  housingFundPersonal: number
  incomeTax: number
  remark: string
  createTime: string
  updateTime: string
}

/** 工资发放记录 */
export interface SalaryPaymentRecord {
  id: string
  userId: string
  userName: string
  paymentMonth: string
  baseSalary: number
  performanceSalary: number
  positionAllowance: number
  transportAllowance: number
  mealAllowance: number
  otherAllowance: number
  totalIncome: number
  socialInsurancePersonal: number
  housingFundPersonal: number
  incomeTax: number
  totalDeduction: number
  actualSalary: number
  paymentStatus: string // 0=未发放 1=已发放
  paymentTime: string
  remark: string
  createTime: string
}

/** 工资统计月度项 */
export interface SalaryMonthlyStat {
  paymentMonth: string
  headcount: number
  paidCount: number
  unpaidCount: number
  totalIncome: number
  totalDeduction: number
  totalActual: number
}

/** 工资统计结果 */
export interface SalaryStatistics {
  summary: Omit<SalaryMonthlyStat, 'paymentMonth'>
  monthly: SalaryMonthlyStat[]
}

/** 工资结构分页 */
export function getSalaryStructurePageApi(params: { pageNum?: number; pageSize?: number; keyword?: string }) {
  return request.get<PageData<SalaryStructure>>('/salary/v1/structures/page', { params })
}

/** 工资结构提交载荷：userId 表单经 input-number 以 number 绑定，接口回显为 string */
export type SalaryStructurePayload = Omit<Partial<SalaryStructure>, 'userId'> & { userId?: string | number }

/** 新增工资结构 */
export function addSalaryStructureApi(data: SalaryStructurePayload) {
  return request.post('/salary/v1/structures', data)
}

/** 更新工资结构 */
export function updateSalaryStructureApi(data: SalaryStructurePayload) {
  return request.put('/salary/v1/structures', data)
}

/** 删除工资结构 */
export function deleteSalaryStructureApi(id: string) {
  return request.delete(`/salary/v1/structures/${id}`)
}

/** 发放记录分页 */
export function getSalaryPaymentPageApi(params: {
  pageNum?: number
  pageSize?: number
  paymentMonth?: string
  paymentStatus?: string
  keyword?: string
}) {
  return request.get<PageData<SalaryPaymentRecord>>('/salary/v1/payments/page', { params })
}

/** 按月份生成发放草稿 */
export function generateSalaryPaymentApi(data: { paymentMonth: string; userIds?: string[] }) {
  return request.post<number>('/salary/v1/payments/generate', data)
}

/** 执行发放 */
export function executeSalaryPaymentApi(ids: string[]) {
  return request.post<number>('/salary/v1/payments/execute', ids)
}

/** 撤销发放 */
export function revokeSalaryPaymentApi(id: string) {
  return request.put(`/salary/v1/payments/${id}/revoke`)
}

/** 删除发放记录 */
export function deleteSalaryPaymentApi(id: string) {
  return request.delete(`/salary/v1/payments/${id}`)
}

/** 工资统计 */
export function getSalaryStatisticsApi(paymentMonth?: string) {
  return request.get<SalaryStatistics>('/salary/v1/statistics', { params: { paymentMonth } })
}

/** 查询员工工资结构 */
export function getSalaryStructureByUserApi(userId: string | number) {
  return request.get<SalaryStructure>(`/salary/v1/structures/user/${encodeURIComponent(String(userId))}`)
}

/** 计算员工实发工资（后端 BigDecimal 以 string 返回） */
export function calculateSalaryApi(userId: string | number) {
  return request.get<string>(`/salary/v1/structures/calculate/${encodeURIComponent(String(userId))}`)
}

/** 批量计算实发工资（返回对应员工的结构列表） */
export function batchCalculateSalaryApi(userIds: Array<string | number>) {
  return request.post<SalaryStructure[]>('/salary/v1/structures/calculate/batch', userIds)
}

/** 发放记录详情 */
export function getSalaryPaymentApi(id: string) {
  return request.get<SalaryPaymentRecord>(`/salary/v1/payments/${encodeURIComponent(id)}`)
}

/** 修改发放记录（已发放记录后端拒绝；未发放记录仅放行备注等修正） */
export function updateSalaryPaymentApi(data: Partial<SalaryPaymentRecord> & { id: string }) {
  return request.put<void>('/salary/v1/payments', data)
}
