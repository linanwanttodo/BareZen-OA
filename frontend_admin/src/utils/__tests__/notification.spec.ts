/**
 * WebSocket 通知客户端重连策略测试
 *
 * 通过 mock SockJS 验证：指数退避、单连接守卫、旧连接 onclose 迟到回调不破坏新连接。
 * 模块内部持有连接状态，故每个用例 resetModules + 动态导入以保证隔离。
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

const instances = vi.hoisted(() => [] as any[])

vi.mock('sockjs-client', () => ({
  default: class {
    onopen: (() => void) | null = null
    onmessage: ((e: { data: string }) => void) | null = null
    onclose: (() => void) | null = null
    onerror: (() => void) | null = null
    constructor(public url: string) {
      instances.push(this)
    }
    close() {
      // 测试中由用例手动触发 onclose
    }
  },
}))

vi.mock('@/utils/auth', () => ({ getToken: () => 'tok' }))
vi.mock('@/utils/toast', () => ({ toast: vi.fn() }))

type Notification = typeof import('@/utils/notification')

let notification: Notification

describe('notification 重连策略', () => {
  beforeEach(async () => {
    instances.length = 0
    vi.useFakeTimers()
    vi.resetModules()
    notification = await import('@/utils/notification')
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('reconnectDelay 按 5s/10s/20s/40s 指数退避并封顶 60s', () => {
    expect(notification.reconnectDelay(0)).toBe(5000)
    expect(notification.reconnectDelay(1)).toBe(10000)
    expect(notification.reconnectDelay(2)).toBe(20000)
    expect(notification.reconnectDelay(3)).toBe(40000)
    expect(notification.reconnectDelay(4)).toBe(60000)
    expect(notification.reconnectDelay(10)).toBe(60000)
  })

  it('连续调用 connectNotification 只创建一个连接', () => {
    notification.connectNotification()
    notification.connectNotification()
    notification.connectNotification()
    expect(instances.length).toBe(1)
  })

  it('重连定时器挂起时再次调用不创建第二连接', () => {
    notification.connectNotification()
    instances[0].onclose()
    notification.connectNotification()
    vi.advanceTimersByTime(5000)
    expect(instances.length).toBe(2)
  })

  it('断开后按退避间隔重连：首次 5s，再次断开 10s', () => {
    notification.connectNotification()
    instances[0].onclose()
    vi.advanceTimersByTime(4999)
    expect(instances.length).toBe(1)
    vi.advanceTimersByTime(1)
    expect(instances.length).toBe(2)
    instances[1].onclose()
    vi.advanceTimersByTime(5000)
    expect(instances.length).toBe(2)
    vi.advanceTimersByTime(5000)
    expect(instances.length).toBe(3)
  })

  it('旧连接的 onclose 迟到回调不破坏新连接（不产生重复连接）', () => {
    notification.connectNotification() // A
    notification.disconnectNotification() // 登出清理 A
    notification.connectNotification() // B
    instances[0].onclose() // A 的迟到回调
    vi.advanceTimersByTime(60000)
    expect(instances.length).toBe(2)
  })

  it('登出后 onclose 不再重连', () => {
    notification.connectNotification()
    notification.disconnectNotification()
    instances[0].onclose()
    vi.advanceTimersByTime(60000)
    expect(instances.length).toBe(1)
  })
})
