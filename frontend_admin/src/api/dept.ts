import request from './request'

export interface SysDept {
  id: string
  parentId: string | number
  deptName: string
  sort: number
  leader: string
  phone: string
  email: string
  status: string
  createTime: string
  updateTime: string
  /** 前端树形展示用 */
  children?: SysDept[]
}

export interface SysUserDept {
  userId: string
  deptId: string
  roleType: string // 0=普通用户 1=管理员
  deptName?: string // 前端展示用
}

/** 获取部门树形列表 */
export function getDeptTreeApi() {
  return request.get<SysDept[]>('/system/v1/depts/tree')
}

/** 获取部门详情 */
export function getDeptApi(id: string) {
  return request.get<SysDept>(`/system/v1/depts/${id}`)
}

/** 新增部门 */
export function addDeptApi(data: Partial<SysDept>) {
  return request.post<void>('/system/v1/depts', data)
}

/** 更新部门 */
export function updateDeptApi(data: Partial<SysDept>) {
  return request.put<void>('/system/v1/depts', data)
}

/** 删除部门 */
export function deleteDeptApi(id: string) {
  return request.delete<void>(`/system/v1/depts/${id}`)
}

/** 修改部门状态 */
export function changeDeptStatusApi(id: string, status: string) {
  return request.put<void>(`/system/v1/depts/${id}/status/${status}`)
}

/** 获取用户所属部门 */
export function getUserDeptsApi(userId: string) {
  return request.get<SysUserDept[]>(`/system/v1/users/${userId}/depts`)
}

/** 分配用户部门 */
export function assignUserDeptsApi(userId: string, deptList: SysUserDept[]) {
  return request.put<void>(`/system/v1/users/${userId}/depts`, deptList)
}
