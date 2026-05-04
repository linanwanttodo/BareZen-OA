import { describe, it, expect } from 'vitest'
import { resolveMenuPath } from '@/utils/menuPath'
import type { SysMenu } from '@/types/menu'

function menu(overrides: Partial<SysMenu>): SysMenu {
  return {
    id: '1',
    parentId: '0',
    menuName: '菜单',
    menuType: 'C',
    perms: '',
    path: '',
    component: '',
    icon: '',
    orderNum: 1,
    visible: '1',
    status: '1',
    remark: '',
    ...overrides,
  }
}

describe('resolveMenuPath', () => {
  it('顶层菜单（无父级）返回自身路径', () => {
    expect(resolveMenuPath(menu({ path: 'dashboard' }))).toBe('/dashboard')
    expect(resolveMenuPath(menu({ path: '/dashboard' }))).toBe('/dashboard')
  })

  it('子菜单路径已含父前缀时不重复拼接（system/user 属于 system）', () => {
    const parent = menu({ menuType: 'M', path: 'system' })
    const child = menu({ path: 'system/user' })
    expect(resolveMenuPath(child, parent)).toBe('/system/user')
  })

  it('子菜单路径为相对路径时拼接父前缀（document 属于 kb）', () => {
    const parent = menu({ menuType: 'M', path: 'kb' })
    const child = menu({ path: 'document' })
    expect(resolveMenuPath(child, parent)).toBe('/kb/document')
  })

  it('子菜单路径带斜杠前缀且含父前缀时原样返回', () => {
    const parent = menu({ menuType: 'M', path: 'hr' })
    const child = menu({ path: '/hr/employee' })
    expect(resolveMenuPath(child, parent)).toBe('/hr/employee')
  })

  it('子菜单路径与父路径相同时使用自身路径，避免拼接成 parent/parent', () => {
    const parent = menu({ menuType: 'M', path: 'office' })
    const child = menu({ path: 'office' })
    expect(resolveMenuPath(child, parent)).toBe('/office')
  })

  it('http(s) 外链原样返回', () => {
    const parent = menu({ menuType: 'M', path: 'tool' })
    expect(resolveMenuPath(menu({ path: 'https://example.com' }), parent)).toBe('https://example.com')
    expect(resolveMenuPath(menu({ path: 'http://example.com' }))).toBe('http://example.com')
  })

  it('path 为空时返回空串（调用方自行忽略）', () => {
    expect(resolveMenuPath(menu({ path: '' }))).toBe('')
  })
})
