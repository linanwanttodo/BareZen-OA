import request from './request'
import type { ApiResult, LoginParams, LoginResult, CurrentUser, CaptchaResult } from '@/types/api'

/** 用户注册参数（注册功能经后端 registerEnabled 开关控制，默认关闭） */
export interface RegisterParams {
  userName: string
  nickName?: string
  password: string
  email?: string
  phone?: string
}

/** 用户登录 */
export function loginApi(data: LoginParams) {
  return request.post<any, ApiResult<LoginResult>>('/auth/v1/login', data)
}

/** 用户注册 */
export function registerApi(data: RegisterParams) {
  return request.post<any, ApiResult<void>>('/auth/v1/register', data)
}

/** 获取验证码 */
export function getCaptchaApi() {
  return request.get<any, ApiResult<CaptchaResult>>('/auth/v1/captcha')
}

/** 获取当前用户信息 */
export function getCurrentUserApi() {
  return request.get<any, ApiResult<CurrentUser>>('/auth/v1/current/user')
}

/** 修改密码 */
export function changePasswordApi(data: { oldPassword: string; newPassword: string }) {
  return request.put<any, ApiResult<void>>('/auth/v1/password', data)
}

/** 退出登录 */
export function logoutApi() {
  return request.post<any, ApiResult<void>>('/auth/v1/logout')
}

/** 刷新 Token */
export function refreshTokenApi() {
  return request.post<any, ApiResult<{ token: string }>>('/auth/v1/refresh')
}
