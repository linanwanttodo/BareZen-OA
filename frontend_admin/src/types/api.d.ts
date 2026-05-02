/** 统一 API 响应体 */
export interface ApiResult<T = any> {
  code: number
  msg: string
  data: T
}

/** 分页请求参数 */
export interface PageParams {
  pageNum: number
  pageSize: number
}

/** 分页响应数据 */
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

/** 当前用户信息 */
export interface CurrentUser {
  userId: string
  userName: string
  nickName: string
  email: string
  phone: string
  avatar: string
  roles: string[]
  permissions: string[]
}

/** 验证码响应 */
export interface CaptchaResult {
  uuid: string
  captchaBase64: string
}
