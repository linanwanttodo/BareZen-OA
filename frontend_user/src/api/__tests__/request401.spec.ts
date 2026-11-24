import { describe, it, expect, vi, beforeEach } from 'vitest'

const warning = vi.fn()
const error = vi.fn()
vi.mock('@/utils/toast', () => ({ Toast: { warning, error, success: vi.fn(), info: vi.fn() } }))

const push = vi.fn()
let currentFullPath = '/dashboard'
const afterEachCbs: Array<(to: { path: string }) => void> = []
vi.mock('@/router', () => ({
  default: {
    push: (to: unknown) => push(to),
    get currentRoute() {
      return { value: { fullPath: currentFullPath } }
    },
    afterEach: (cb: (to: { path: string }) => void) => afterEachCbs.push(cb),
  },
}))

const resetState = vi.fn()
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ resetState }) }))

/** 每次重新取模块实例，避免模块级防重入标志跨用例污染 */
async function freshService(status = 401) {
  vi.resetModules()
  const { default: service } = await import('@/api/request')
  service.defaults.adapter = () =>
    Promise.reject(
      Object.assign(new Error(`HTTP ${status}`), {
        config: { url: '/x' },
        response: { status, data: { msg: '未登录' } },
      })
    )
  return service
}

describe('用户端 401 拦截', () => {
  beforeEach(() => {
    warning.mockClear()
    error.mockClear()
    push.mockClear()
    resetState.mockClear()
    afterEachCbs.length = 0
    currentFullPath = '/dashboard'
  })

  it('并发多个 401 只跳转一次、只提示一次', async () => {
    const service = await freshService()
    const calls = [1, 2, 3].map(() => service.get('/x').catch(() => null))
    await Promise.all(calls)

    expect(resetState).toHaveBeenCalledTimes(3)
    expect(push).toHaveBeenCalledTimes(1)
    expect(warning).toHaveBeenCalledTimes(1)
    expect(push.mock.calls[0][0]).toEqual({ path: '/login', query: { redirect: '/dashboard' } })
  })

  it('离开登录页后再次 401 仍能跳转（防重入标志不得变成一次性）', async () => {
    const service = await freshService()
    await service.get('/x').catch(() => null)
    expect(push).toHaveBeenCalledTimes(1)

    // 本端是 SPA 内跳转，不像管理端整页刷新会重置模块作用域：
    // 没有 afterEach 复位时，第二次会话过期将永远不再跳登录页
    currentFullPath = '/login'
    afterEachCbs.forEach((cb) => cb({ path: '/login' }))
    await service.get('/x').catch(() => null)
    expect(push).toHaveBeenCalledTimes(1)

    afterEachCbs.forEach((cb) => cb({ path: '/home' }))
    currentFullPath = '/home'
    await service.get('/x').catch(() => null)
    expect(push).toHaveBeenCalledTimes(2)
  })

  it('已在登录页时不再重复跳转', async () => {
    currentFullPath = '/login'
    const service = await freshService()
    await service.get('/x').catch(() => null)
    expect(push).not.toHaveBeenCalled()
    expect(warning).not.toHaveBeenCalled()
  })
})
