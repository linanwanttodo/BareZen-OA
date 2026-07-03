import { describe, it, expect } from 'vitest'
import { kebabToCamel, routeKeyCandidates, labelFromPath, resolveMenuLabel } from '@/utils/menuLabel'
import type { SysMenu } from '@/types/menu'

/** 已登记的 route.* 键（取自 i18n 语言包实际内容，覆盖约定推导会用到的部分） */
const KNOWN = new Set([
  'dashboard', 'system', 'user', 'role', 'menu', 'dept', 'dict', 'notice', 'config',
  'monitor', 'online', 'operLog', 'logininfor', 'jobs', 'tool', 'gen', 'profile',
  'backup', 'roleDept', 'file', 'kb', 'document', 'hr', 'employee', 'salary',
  'structure', 'assetCategory', 'kbCategory', 'statistics', 'todoCenter',
])

const t = (key: string) => `「${key}」`
const te = (key: string) => KNOWN.has(key.replace(/^route\./, ''))

describe('kebabToCamel', () => {
  it('转换短横线分段，已驼峰的保持不变', () => {
    expect(kebabToCamel('oper-log')).toBe('operLog')
    expect(kebabToCamel('supply-request')).toBe('supplyRequest')
    expect(kebabToCamel('logininfor')).toBe('logininfor')
    expect(kebabToCamel('roleDept')).toBe('roleDept')
  })
})

describe('routeKeyCandidates', () => {
  it('多级 path 先给全路径键再给末段键', () => {
    expect(routeKeyCandidates('/system/user')).toEqual(['systemUser', 'user'])
    expect(routeKeyCandidates('/kb/document')).toEqual(['kbDocument', 'document'])
  })

  it('单段 path 只给一个键，不重复', () => {
    expect(routeKeyCandidates('/dashboard')).toEqual(['dashboard'])
  })

  it('外链与空 path 不产生候选键', () => {
    expect(routeKeyCandidates('https://example.com/foo')).toEqual([])
    expect(routeKeyCandidates('')).toEqual([])
  })
})

describe('labelFromPath', () => {
  it('全路径键存在时优先于末段键（区分同名末段的兄弟菜单）', () => {
    expect(labelFromPath('/asset/category', '资产分类', t, te)).toBe('「route.assetCategory」')
    expect(labelFromPath('/kb/category', '知识分类', t, te)).toBe('「route.kbCategory」')
  })

  it('全路径键未登记时退回末段键，兼容既有 route.* 命名', () => {
    expect(labelFromPath('/system/user', '用户管理', t, te)).toBe('「route.user」')
    expect(labelFromPath('/monitor/oper-log', '操作日志', t, te)).toBe('「route.operLog」')
  })

  it('两个候选键都未登记时退回原始标题', () => {
    expect(labelFromPath('/system/not-registered', '未登记菜单', t, te)).toBe('未登记菜单')
  })
})

describe('resolveMenuLabel', () => {
  const menu = (path: string, menuName = '名字'): SysMenu =>
    ({ id: '1', parentId: '0', menuName, menuType: 'C', perms: '', path, component: '', icon: '', orderNum: 1, visible: '0', status: '0', remark: '' }) as SysMenu

  it('按菜单 path 解析翻译', () => {
    expect(resolveMenuLabel(menu('/system/dept'), t, te)).toBe('「route.dept」')
  })

  it('path 为空时退回数据库菜单名', () => {
    expect(resolveMenuLabel(menu('', '资产分类'), t, te)).toBe('资产分类')
  })
})
