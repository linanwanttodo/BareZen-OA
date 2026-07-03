/**
 * 全局消息提示 —— TDesign MessagePlugin 统一封装
 *
 * 规范依据：TDesign 官方消息组件（含主题色、图标、关闭按钮、动画），
 * 样式由 tdesign-vue-next 官方样式的 --td-* token 驱动（暗色经 theme-mode 自动跟随）；不再手写 DOM。
 * 保持原有 toast(message, type, duration) 与 Toast.xxx 调用签名，调用点零改动。
 */
import { MessagePlugin } from 'tdesign-vue-next'
import type { MessageThemeList } from 'tdesign-vue-next'

type ToastType = 'success' | 'error' | 'warning' | 'info'

export function toast(message: string, type: ToastType = 'info', duration = 2500) {
  MessagePlugin(type as MessageThemeList, message, duration)
}

export const Toast = {
  success: (msg: string) => MessagePlugin.success(msg),
  error: (msg: string) => MessagePlugin.error(msg),
  warning: (msg: string) => MessagePlugin.warning(msg),
  info: (msg: string) => MessagePlugin.info(msg),
}
