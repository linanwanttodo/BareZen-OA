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
