import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'

describe('userStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
  })

  it('初始状态 token 为空字符串', () => {
    const store = useUserStore()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.roles).toEqual([])
    expect(store.permissions).toEqual([])
  })

  it('从 sessionStorage 恢复 token', () => {
    sessionStorage.setItem('bz_admin_token', 'saved-token')
    const store = useUserStore()
    expect(store.token).toBe('saved-token')
  })

  it('resetState 清除状态和 sessionStorage', () => {
    const store = useUserStore()
    store.token = 'test-token'
    store.userInfo = { userName: 'admin' } as any
    store.roles = ['admin']

    store.resetState()

    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.roles).toEqual([])
    expect(store.permissions).toEqual([])
    expect(sessionStorage.getItem('bz_admin_token')).toBeNull()
  })

  it('hasPermission 检查权限', () => {
    const store = useUserStore()
    store.permissions = ['system:user:list', 'system:role:list']

    expect(store.hasPermission('system:user:list')).toBe(true)
    expect(store.hasPermission('system:user:delete')).toBe(false)
  })

  it('hasPermission 通配符 *:*:* 通过所有检查', () => {
    const store = useUserStore()
    store.permissions = ['*:*:*']

    expect(store.hasPermission('any:perm')).toBe(true)
    expect(store.hasPermission('system:user:delete')).toBe(true)
  })
})
