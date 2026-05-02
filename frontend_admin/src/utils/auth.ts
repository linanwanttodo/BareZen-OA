/**
 * Token 管理
 *
 * TODO: 生产环境建议迁移到 httpOnly Cookie 方案，避免 XSS 窃取 token。
 * 当前使用 sessionStorage (而非 localStorage) 可减少 token 暴露窗口，
 * 关闭标签页后 token 自动清除。
 */
const TOKEN_KEY = 'bz_admin_token'

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
}
