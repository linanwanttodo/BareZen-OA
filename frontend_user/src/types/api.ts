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

/** 站内消息（3.1 WebSocket 离线兜底，消息中心） */
export interface SysMessage {
  id: string
  userId: string
  title: string
  content: string
  msgType: string
  bizType: string | null
  bizId: string | null
  isRead: number
  readTime: string | null
  createTime: string
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

/** 请假申请 */
export interface LeaveRequest {
  id: string
  userId: string
  userName: string
  deptName: string
  leaveType: string // annual=年假 sick=病假 personal=事假 overtime=调休 marriage=婚假 maternity=产假 other=其他
  startTime: string
  endTime: string
  leaveDays: number // 后端 BigDecimal
  reason: string
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

/** 用车申请 */
export interface VehicleRequest {
  id: string
  applicantId: string
  applicantName: string
  deptName: string
  vehicleType: string // car=轿车 van=商务车 bus=客车 other=其他
  departure: string
  destination: string
  purpose: string
  startTime: string
  endTime: string
  passengerCount: number
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

/** 审批节点任务（V36 多级审批引擎） */
export interface FlowTask {
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

/** 会议室 */
export interface MeetingRoom {
  id: string
  roomName: string
  capacity: number
  location: string
  equipment: string // 投影/白板/视频会议，逗号分隔
  status: string // 0=可用 1=停用
  remark: string
  createTime: string
}

/** 会议预订（不走审批流，取消即释放房间占用） */
export interface MeetingBooking {
  id: string
  roomId: string
  roomName: string
  title: string
  bookerId: string
  bookerName: string
  startTime: string // yyyy-MM-dd HH:mm
  endTime: string // yyyy-MM-dd HH:mm
  attendees: string // 参会人员（逗号分隔）
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

/** 工资发放记录（工资条，各金额字段为发放时快照） */
export interface SalaryPaymentRecord {
  id: string
  userId: string
  userName: string
  paymentMonth: string // YYYY-MM
  baseSalary: number | null
  performanceSalary: number | null
  positionAllowance: number | null
  transportAllowance: number | null
  mealAllowance: number | null
  otherAllowance: number | null
  totalIncome: number | null
  socialInsurancePersonal: number | null
  housingFundPersonal: number | null
  incomeTax: number | null
  totalDeduction: number | null
  actualSalary: number | null
  paymentStatus: string // 0=未发放 1=已发放
  paymentTime: string | null
  remark: string | null
  createTime: string
}

/** 工资结构（当前生效版本） */
export interface SalaryStructure {
  id: string
  userId: string
  userName: string
  baseSalary: number | null
  performanceSalary: number | null
  positionAllowance: number | null
  transportAllowance: number | null
  mealAllowance: number | null
  otherAllowance: number | null
  socialInsurancePersonal: number | null
  housingFundPersonal: number | null
  incomeTax: number | null
  remark: string | null
  createTime: string
}

/** 知识库分类（用户端仅返回启用状态） */
export interface KbCategory {
  id: string
  name: string
  parentId: string
  sort: number
  status: number
  createTime: string
}

/** 知识文档（用户端仅返回已发布文档，正文为 HTML 富文本） */
export interface KbDocument {
  id: string
  title: string
  category: string
  categoryId: string | null
  content: string
  deptName: string | null
  authorName: string | null
  status: string // 0=草稿 1=已发布
  viewCount: number | null
  remark: string | null
  createTime: string
  updateTime: string
}

/** 资产分类（用户端仅返回启用状态） */
export interface AssetCategory {
  id: string
  name: string
  parentId: string
  sort: number
  status: number
  createTime: string
}

/** 可借用资产（用户端公开视图，仅在库资产，屏蔽价格/使用人等管理字段） */
export interface AssetItem {
  id: string
  assetNo: string
  assetName: string
  categoryId: string | null
  category: string
  brand: string | null
  model: string | null
  location: string | null
  status: string // 0=在库（用户端仅返回该状态）
}

/** 资产借用记录（用户端仅返回本人记录） */
export interface AssetBorrowRecord {
  id: string
  assetId: string
  borrowerId: string | null
  borrowerName: string
  borrowTime: string | null
  expectReturnTime: string | null
  returnTime: string | null
  remark: string | null
  status: string // 0=借用中 1=已归还
  assetNo: string | null
  assetName: string | null
  createTime: string
}

/** 我的文件（用户端仅返回本人上传的文件，屏蔽存储路径等内部字段） */
export interface UserFile {
  id: string
  fileName: string
  fileSize: number
  fileType: string // image/document/video/audio/other
  storageType: string // local/s3
  createTime: string
}
