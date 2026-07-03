import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createMemoryHistory, type RouteRecordRaw } from 'vue-router'
import { h } from 'vue'
import { usePermissionStore } from '@/stores/permission'
import { resolveMenuPath } from '@/utils/menuPath'
import type { SysMenu } from '@/types/menu'

const Stub = { render: () => h('div') }

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
    visible: '0',
    status: '0',
    remark: '',
    ...overrides,
  }
}

/**
 * 复刻线上 sys_menu 的两种 path 约定混用现状：
 * 多数子菜单带父前缀（system/user），少数由早期迁移按相对约定插入
 * （kb 下的 category、salary 下的 structure 等）。
 */
const FIXTURE: SysMenu[] = [
  menu({
    id: '1',
    menuName: '系统管理',
    menuType: 'M',
    path: 'system',
    children: [
      menu({ id: '2', parentId: '1', menuName: '用户管理', path: 'system/user', component: 'system/user/index' }),
      menu({ id: '10016', parentId: '1', menuName: '文件管理', path: 'file', component: 'file/index' }),
      menu({ id: '10021', parentId: '1', menuName: '审批流程模板', path: 'flow-template', component: 'system/flow-template/index' }),
      menu({ id: '10019', parentId: '1', menuName: '资产分类', path: 'category', component: 'asset/category/index' }),
    ],
  }),
  menu({
    id: '22',
    menuName: '知识库',
    menuType: 'M',
    path: 'kb',
    children: [
      menu({ id: '23', parentId: '22', menuName: '知识文档', path: 'kb/document', component: 'kb/document/index' }),
      // 与 10019 同为相对路径 category：旧规则下两者都被注册成 /category 互相遮蔽
      menu({ id: '10020', parentId: '22', menuName: '知识分类', path: 'category', component: 'kb/category/index' }),
    ],
  }),
  menu({
    id: '10031',
    menuName: '工资管理',
    menuType: 'M',
    path: 'salary',
    children: [
      menu({ id: '10032', parentId: '10031', menuName: '工资结构', path: 'structure', component: 'salary/structure/index' }),
      menu({ id: '10034', parentId: '10031', menuName: '工资统计', path: 'statistics', component: 'salary/statistics/index' }),
    ],
  }),
  menu({ id: '14', menuName: '仪表盘', path: 'dashboard', component: 'dashboard/index' }),
  menu({ id: '10262', parentId: '2', menuName: '资产借用', menuType: 'F', perms: 'asset:item:borrow' }),
]

/** 递归收集路由树中所有 path */
function flattenPaths(routes: RouteRecordRaw[]): string[] {
  return routes.flatMap((r) => [r.path, ...flattenPaths((r.children ?? []) as RouteRecordRaw[])])
}

/** 按 guard.ts 的方式把动态路由挂到 Layout 下 */
function buildRouter(routes: RouteRecordRaw[]) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', name: 'Layout', component: Stub, children: [] }],
  })
  routes.forEach((r) => router.addRoute('Layout', r))
  return router
}

/** 收集树中所有可导航菜单（M/C，排除外链与无 path） */
function collectNavigable(
  menus: SysMenu[],
  parent?: SysMenu,
  acc: Array<{ item: SysMenu; parent?: SysMenu }> = []
) {
  for (const m of menus) {
    if (m.menuType === 'F') continue
    if (m.path) acc.push({ item: m, parent })
    if (m.children?.length) collectNavigable(m.children, m, acc)
  }
  return acc
}

describe('动态路由 path 与导航侧解析一致', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('导航侧解析出的每个菜单路径都能命中已注册路由（不落入 catch-all）', () => {
    const store = usePermissionStore()
    const router = buildRouter(store.generateRoutes(FIXTURE))

    const broken = collectNavigable(FIXTURE)
      .map(({ item, parent }) => resolveMenuPath(item, parent))
      .filter((target) => {
        const resolved = router.resolve(target)
        return !resolved.matched.some((m) => m.path === target)
      })

    expect(broken).toEqual([])
  })

  it('相对路径子菜单注册在父前缀下，不再脱离父级', () => {
    const store = usePermissionStore()
    const router = buildRouter(store.generateRoutes(FIXTURE))

    // 旧规则把 kb 下的 category 注册成 /category，导航跳 /kb/category 得 404
    expect(router.resolve('/kb/category').matched.map((m) => m.path)).toContain('/kb/category')
    expect(router.resolve('/system/category').matched.map((m) => m.path)).toContain('/system/category')
    expect(router.resolve('/salary/structure').matched.map((m) => m.path)).toContain('/salary/structure')
  })

  it('不同父级下的同名相对路径互不遮蔽', () => {
    const store = usePermissionStore()
    const paths = flattenPaths(store.generateRoutes(FIXTURE))
    const dup = paths.filter((p, i) => paths.indexOf(p) !== i)
    expect(dup).toEqual([])
    expect(paths).toContain('/kb/category')
    expect(paths).toContain('/system/category')
  })

  it('F 类型按钮不生成路由', () => {
    const store = usePermissionStore()
    const paths = flattenPaths(store.generateRoutes(FIXTURE))
    // 10262 是挂在用户管理下的按钮；按钮只贡献权限字符串，不该贡献路由
    expect(paths).toContain('/system/user')
    expect(paths.filter((p) => p.includes('borrow'))).toEqual([])
    expect(paths).toHaveLength(12)
  })
})
