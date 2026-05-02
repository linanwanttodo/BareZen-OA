/**
 * 安全重定向：只允许同站相对路径
 * 防止开放重定向漏洞
 */
export function safeRedirect(path: string): string {
  if (!path || typeof path !== 'string') return '/'
  // 只允许以 / 开头且不以 // 开头的同站路径
  if (path.startsWith('/') && !path.startsWith('//')) {
    return path
  }
  return '/'
}
