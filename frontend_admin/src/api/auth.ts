import request from './request'
import type { LoginParams, LoginResult, CurrentUser, CaptchaResult } from '@/types/api'

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
  return request.post<LoginResult>('/auth/v1/login', data)
}

/** 用户注册 */
export function registerApi(data: RegisterParams) {
  return request.post<void>('/auth/v1/register', data)
}

/** 获取验证码 */
export function getCaptchaApi() {
  return request.get<CaptchaResult>('/auth/v1/captcha')
}

/** 获取当前用户信息 */
export function getCurrentUserApi() {
  return request.get<CurrentUser>('/auth/v1/current/user')
}

/** 修改密码 */
export function changePasswordApi(data: { oldPassword: string; newPassword: string }) {
  return request.put<void>('/auth/v1/password', data)
}

/** 退出登录 */
export function logoutApi() {
  return request.post<void>('/auth/v1/logout')
}

/** 刷新 Token */
export function refreshTokenApi() {
  return request.post<{ token: string }>('/auth/v1/refresh')
}