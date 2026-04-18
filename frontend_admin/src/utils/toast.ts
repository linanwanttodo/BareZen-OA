/**
 * 轻量 Toast 提示（扁平、无动画/渐变/发光）
 * 统一管理全局消息提示，避免引入额外的组件库依赖。
 */
type ToastType = 'success' | 'error' | 'warning' | 'info'

let container: HTMLElement | null = null

function ensureContainer(): HTMLElement {
  if (container) return container
  container = document.createElement('div')
  container.style.position = 'fixed'
  container.style.top = '16px'
  container.style.left = '50%'
  container.style.transform = 'translateX(-50%)'
  container.style.zIndex = '9999'
  container.style.display = 'flex'
  container.style.flexDirection = 'column'
  container.style.gap = '8px'
  container.style.pointerEvents = 'none'
  document.body.appendChild(container)
  return container
}

const styles: Record<ToastType, string> = {
  success: 'border-[var(--primary)] text-[var(--foreground)]',
  error: 'border-[var(--destructive)] text-[var(--foreground)]',
  warning: 'border-[var(--border)] text-[var(--foreground)]',
  info: 'border-[var(--border)] text-[var(--foreground)]',
}

export function toast(message: string, type: ToastType = 'info', duration = 2500) {
  const root = ensureContainer()
  const el = document.createElement('div')
  el.className = `rounded-md border bg-[var(--card)] px-4 py-2 text-sm shadow-none ${styles[type]}`
  el.textContent = message
  el.style.pointerEvents = 'auto'
  root.appendChild(el)

  window.setTimeout(() => {
    el.remove()
  }, duration)
}

export const Toast = {
  success: (msg: string) => toast(msg, 'success'),
  error: (msg: string) => toast(msg, 'error'),
  warning: (msg: string) => toast(msg, 'warning'),
  info: (msg: string) => toast(msg, 'info'),
}
