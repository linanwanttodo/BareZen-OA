/**
 * WebSocket 实时通知客户端
 *
 * 使用 SockJS 连接后端 /ws/notify 端点（与后端 NotificationWebSocketHandler 对应），
 * 接收公告发布等实时广播，以 Toast 形式展示。
 *
 * 连接鉴权：登录后后端已下发 httpOnly Cookie（bz_token），同源握手自动携带，
 * 不再通过 URL 参数传递 JWT（避免 Token 进入代理访问日志）。
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
/** 连续断线次数（成功连接后清零），用于指数退避计算 */
let reconnectAttempts = 0

/** 断线重连基础间隔（毫秒），按 5s、10s、20s、40s… 指数退避，封顶 60s */
const RECONNECT_BASE_MS = 5000
const RECONNECT_MAX_MS = 60000

/** 计算第 attempt 次重连（从 0 开始）的等待毫秒数（导出仅供测试） */
export function reconnectDelay(attempt: number): number {
  return Math.min(RECONNECT_BASE_MS * 2 ** Math.max(0, attempt), RECONNECT_MAX_MS)
}

function showNotification(msg: NotificationMessage) {
  // 广播公告：标题 + 内容首行（Toast.info 仅接收消息文本，时长走内部默认值）
  const title = msg.title || '新通知'
  const content = msg.content || ''
  const firstLine = content.split('\n')[0] || ''
  Toast.info(`${title}${firstLine ? '：' + firstLine : ''}`)
}

function connectSocket() {
  const token = getToken()
  if (!token) return

  manualClose = false
  // 兼容 Vite 代理：开发环境走 /ws 代理到后端，生产由网关转发
  // 鉴权依赖同源 httpOnly Cookie（bz_token），无需 URL 传参
  const base = window.location.origin
  const sockJs = new SockJS(`${base}/ws/notify`)
  sock = sockJs as unknown as WebSocket

  sockJs.onopen = () => {
    reconnectAttempts = 0
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
    // 仅当关闭的是"当前连接"时才清理与调度重连：
    // 旧连接的迟到 onclose 不得清空新连接引用，否则会产生重复连接/重复 Toast
    if (sock !== sockJs) return
    sock = null
    if (!manualClose && getToken()) {
      // 指数退避重连，避免服务端故障时高频重连
      reconnectTimer = window.setTimeout(() => {
        reconnectTimer = null
        connectSocket()
      }, reconnectDelay(reconnectAttempts++))
    }
  }

  sockJs.onerror = () => {
    // onclose 会随后触发，重连逻辑在 onclose 中处理
  }
}

/** 建立连接（登录后调用） */
export function connectNotification(): void {
  // 已有活动连接或重连定时器挂起时直接返回，避免创建第二连接
  if (sock || reconnectTimer) return
  connectSocket()
}

/** 断开连接（登出/令牌失效时调用） */
export function disconnectNotification(): void {
  manualClose = true
  reconnectAttempts = 0
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
