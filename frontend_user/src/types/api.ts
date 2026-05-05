/** 统一 API 响应体 */
export interface ApiResult<T = any> {
  code: number
  msg: string
  data: T
}

/** 分页响应数据（对应后端 PageResult） */
export interface PageData<T = any> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** 登录请求 */
export interface LoginParams {
  username: string
  password: string
  captchaCode: string
  uuid: string
}

/** 登录响应 */
export interface LoginResult {
  token: string
  userId: string
  userName: string
  nickName: string
}

/** 验证码响应 */
export interface CaptchaResult {
  uuid: string
  captchaBase64: string
}

/** 当前用户信息 */
export interface CurrentUser {
  userId: string
  userName: string
  nickName: string
  email: string
  phone: string
  avatar: string
  sex: string
  roles: string[]
  permissions: string[]
}

/** 通知公告 */
export interface Notice {
  id: string
  noticeTitle: string
  noticeContent: string
  noticeType: string
  status: string
  scope: string
  requireRead: string
  isRead?: boolean
  createTime: string
  updateTime: string
}

/** 通讯录联系人 */
export interface Contact {
  userId: string
  userName: string
  nickName: string
  email: string
  phone: string
  sex: string
  avatar: string
  roleNames: string[]
}

/** 创建待办 DTO — 仅包含前端可提交字段 */
export interface CreateTodoDTO {
  title: string
  description?: string
  priority?: number
  dueDate?: string
}

/** 更新待办 DTO — 仅包含前端可修改字段 */
export interface UpdateTodoDTO {
  id: string
  title?: string
  description?: string
  priority?: number
  status?: number
  dueDate?: string
}

/** 待办事项 */
export interface TodoItem {
  id: string
  title: string
  description: string
  userId: string
  priority: number  // 0=低 1=中 2=高 3=紧急
  status: number    // 0=待办 1=进行中 2=已完成 3=已取消
  dueDate: string
  completedTime: string
  createTime: string
  updateTime: string
  done?: boolean    // 前端视图态：是否已完成（status === 2）
}

/** 工作台概览数据 */
export interface DashboardData {
  todoCount: number
  doneCount: number
  urgentCount: number
  latestNotices: Notice[]
  upcomingTodos: TodoItem[]
}

/** 优先级标签 */
export const PRIORITY_MAP: Record<number, { label: string; theme: string }> = {
  0: { label: '低', theme: 'default' },
  1: { label: '中', theme: 'primary' },
  2: { label: '高', theme: 'warning' },
  3: { label: '紧急', theme: 'danger' },
}

/** 状态标签 */
export const STATUS_MAP: Record<number, { label: string; theme: string }> = {
  0: { label: '待办', theme: 'default' },
  1: { label: '进行中', theme: 'primary' },
  2: { label: '已完成', theme: 'success' },
  3: { label: '已取消', theme: 'default' },
}

/** 加班申请 */
export interface OvertimeRequest {
  id: string
  userId: string
  userName: string
  deptName: string
  overtimeDate: string
  startTime: string
  endTime: string
  durationHours: number // 后端 BigDecimal
  reason: string
  status: string
  approverName: string
  approveTime: string
  approveRemark: string
  createTime: string
}

/** 出差申请 */
export interface TravelRequest {
  id: string
  userId: string
  userName: string
  deptName: string
  destination: string
  purpose: string
  startTime: string
  endTime: string
  days: number // 后端 BigDecimal
  status: string
  approverName: string
  approveTime: string
  approveRemark: string
  createTime: string
}

/** 工作日志 */
export interface WorkLog {
  id: string
  userId: string
  userName: string
  deptName: string
  logType: string // daily / weekly
  logDate: string
  content: string
  reviewerId: string
  reviewerName: string
  reviewTime: string
  reviewRemark: string
  createTime: string
}

/** 个人日程 */
export interface Schedule {
  id: string
  userId: string
  userName: string
  title: string
  description: string
  startTime: string
  endTime: string
  remindTime: string
  status: string // 0=未开始 1=进行中 2=已完成 3=已取消
  location: string
  createTime: string
}

/** 办公用品 */
export interface Supply {
  id: string
  supplyName: string
  category: string
  spec: string
  stock: number
  unit: string
  status: string
  remark: string
  createTime: string
}

/** 用品申领记录 */
export interface SupplyRequest {
  id: string
  supplyId: string
  supplyName: string
  userId: string
  userName: string
  deptName: string
  quantity: number
  reason: string
  status: string
  approverName: string
  approveTime: string
  approveRemark: string
  createTime: string
}

/** 审批流程节点 */
export interface FlowLog {
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

/** 考勤打卡记录（对应后端 AttClockRecord，同一用户同日仅一行） */
export interface AttClockRecord {
  id: string
  userId: string
  userName: string
  deptName: string
  clockDate: string // yyyy-MM-dd
  clockInTime: string // 可空，未上班打卡时为 null
  clockOutTime: string // 可空，未下班打卡时为 null
  lateMinutes: number // 迟到分钟数，服务端计算
  earlyMinutes: number // 早退分钟数，服务端计算
  status: string // normal=正常 late=迟到 early=早退 absent=缺卡 abnormal=异常
  createTime: string
  updateTime: string
}
