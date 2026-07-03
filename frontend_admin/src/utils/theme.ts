/**
 * 暗色模式：TDesign 官方 theme-mode 机制
 * 亮色为默认；暗色通过在 <html> 上设置 theme-mode='dark' 启用
 * （TDesign 自带 :root[theme-mode='dark'] token 覆盖层，
 *   项目 wecom 到 td 的转发层自动跟随）。
 * 偏好持久化 localStorage('bz-theme')；切换后广播 bz-theme-change 事件，
 * 供 ECharts 等画布组件重新解析 token 重绘。
 */
const STORAGE_KEY = 'bz-theme'

export function isDarkMode(): boolean {
  return document.documentElement.getAttribute('theme-mode') === 'dark'
}

export function applyTheme(dark: boolean): void {
  if (dark) {
    document.documentElement.setAttribute('theme-mode', 'dark')
  } else {
    document.documentElement.removeAttribute('theme-mode')
  }
  window.dispatchEvent(new Event('bz-theme-change'))
}

/** 应用启动时调用：仅恢复用户显式选择过的暗色偏好，未设置过一律浅色 */
export function initTheme(): void {
  if (localStorage.getItem(STORAGE_KEY) === 'dark') {
    document.documentElement.setAttribute('theme-mode', 'dark')
  }
}

export function toggleTheme(): boolean {
  const next = !isDarkMode()
  localStorage.setItem(STORAGE_KEY, next ? 'dark' : 'light')
  applyTheme(next)
  return next
}
