import request from './request'
import type { ApiResult, LoginParams, LoginResult, CurrentUser, CaptchaResult } from '@/types/api'

export function loginApi(data: LoginParams) {
  return request.post<any, ApiResult<LoginResult>>('/auth/v1/login', data)
}

export function getCaptchaApi() {
  return request.get<any, ApiResult<CaptchaResult>>('/auth/v1/captcha')
}

export function getCurrentUserApi() {
  return request.get<any, ApiResult<CurrentUser>>('/auth/v1/current/user')
}

export function logoutApi() {
  return request.post<any, ApiResult<void>>('/auth/v1/logout')
}

export function changePasswordApi(data: { oldPassword: string; newPassword: string }) {
  return request.put<any, ApiResult<void>>('/auth/v1/password', data)
}
