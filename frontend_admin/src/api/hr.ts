import request from './request'
import type { PageData } from '@/types/api'

export interface HrEmployee {
  id: string
  empNo: string
  name: string
  gender: string
  deptId: string
  deptName: string
  position: string
  phone: string
  email: string
  idCard: string
  hireDate: string
  leaveDate: string
  employmentType: string
  education: string
  status: string
  remark: string
  createTime: string
}

export interface EmployeeListParams {
  pageNum?: number
  pageSize?: number
  name?: string
  empNo?: string
  deptId?: string
  status?: string
}

/** 员工分页列表 */
export function getEmployeePageApi(params: EmployeeListParams) {
  return request.get<PageData<HrEmployee>>('/hr/v1/employees/page', { params })
}

/** 员工详情 */
export function getEmployeeApi(id: string) {
  return request.get<HrEmployee>(`/hr/v1/employees/${id}`)
}

/** 新增员工 */
export function addEmployeeApi(data: Partial<HrEmployee>) {
  return request.post<void>('/hr/v1/employees', data)
}

/** 更新员工 */
export function updateEmployeeApi(data: Partial<HrEmployee>) {
  return request.put<void>('/hr/v1/employees', data)
}

/** 删除员工 */
export function deleteEmployeeApi(id: string) {
  return request.delete<void>(`/hr/v1/employees/${id}`)
}
