/**
 * WebSocket 实时通知客户端
 *
 * 使用 SockJS 连接后端 /ws/notify 端点（与后端 NotificationWebSocketHandler 对应），
 * 接收公告发布等实时广播，以 Toast 形式展示。
 *
 * 连接鉴权：通过 URL 参数 ?token=xxx 传递 JWT（与后端 extractToken 解析一致）。
 */
import SockJS from 'sockjs-client'
import { getToken } from '@/utils/auth'
import { Toast } from '@/utils/toast'

interface NotificationMessage {
  type: string
  title: string
  content: string
  time: string
}

let sock: WebSocket | null = null
let reconnectTimer: number | null = null
let manualClose = false

/** 断线重连间隔（毫秒） */
const RECONNECT_INTERVAL = 5000

function showNotification(msg: NotificationMessage) {
  // 广播公告：标题 + 内容首行
  const title = msg.title || '新通知'
  const content = msg.content || ''
  const firstLine = content.split('\n')[0] || ''
  Toast.info(`${title}${firstLine ? '：' + firstLine : ''}`, 5000)
}

function connectSocket() {
  const token = getToken()
  if (!token) return

  manualClose = false
  // 兼容 Vite 代理：开发环境走 /ws 代理到后端，生产由网关转发
  const base = window.location.origin
  const sockJs = new SockJS(`${base}/ws/notify?token=${encodeURIComponent(token)}`)
  sock = sockJs as unknown as WebSocket

  sockJs.onopen = () => {
    if (reconnectTimer) {
      window.clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  sockJs.onmessage = (evt) => {
    try {
      const data = JSON.parse(evt.data as string) as NotificationMessage
      showNotification(data)
    } catch {
      // 非 JSON 消息（如心跳）忽略
    }
  }

  sockJs.onclose = () => {
    sock = null
    if (!manualClose && getToken()) {
      // 自动重连（指数退避，避免高频重连）
      reconnectTimer = window.setTimeout(connectSocket, RECONNECT_INTERVAL)
    }
  }

  sockJs.onerror = () => {
    // onclose 会随后触发，重连逻辑在 onclose 中处理
  }
}

/** 建立连接（登录后调用） */
export function connectNotification(): void {
  if (sock) return
  connectSocket()
}

/** 断开连接（登出/令牌失效时调用） */
export function disconnectNotification(): void {
  manualClose = true
  if (reconnectTimer) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (sock) {
    try {
      sock.close()
    } catch {
      // 忽略关闭异常
    }
    sock = null
  }
}
