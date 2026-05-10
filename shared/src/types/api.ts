/**
 * API相关类型定义
 */

/** 登录参数 */
export interface LoginParams {
  username: string
  password: string
  captcha?: string
  captchaKey?: string
}

/** 登录结果 */
export interface LoginResult {
  token: string
  expireIn?: number
}

/** 当前用户信息 */
export interface CurrentUser {
  id: number
  username: string
  nickName: string
  avatar?: string
  email?: string
  phone?: string
  roles?: string[]
  permissions?: string[]
  deptId?: number
  deptName?: string
}

/** 验证码结果 */
export interface CaptchaResult {
  captchaKey: string
  captchaImage: string
}

/** API统一响应结构 */
export interface ApiResult<T = any> {
  code: number
  msg: string
  data: T
}