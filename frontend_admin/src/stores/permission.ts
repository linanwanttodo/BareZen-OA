import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import type { SysMenu } from '@/types/menu'

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([])
  const menuList = ref<SysMenu[]>([])
  /** 已挂载动态路由的移除函数（router.addRoute 返回值），登出/换账号时逐个移除 */
  const routeRemovers = ref<Array<() => void>>([])

  /** 根据后端菜单生成路由 */
  function generateRoutes(menus: SysMenu[]) {
    menuList.value = menus
    const dynamicRoutes = transformMenusToRoutes(menus)
    routes.value = dynamicRoutes
    return dynamicRoutes
  }

  /** 登记动态路由移除函数（路由守卫挂载路由时调用） */
  function collectRouteRemover(removeFn: () => void) {
    routeRemovers.value.push(removeFn)
  }

  /** 重置权限（登出/换账号）：先移除已挂载的动态路由，避免旧账号路由残留导致越权 */
  function resetPermission() {
    routeRemovers.value.forEach((removeFn) => removeFn())
    routeRemovers.value = []
    routes.value = []
    menuList.value = []
  }

  return {
    routes,
    menuList,
    routeRemovers,
    generateRoutes,
    collectRouteRemover,
    resetPermission,
  }
})

/** 将后端菜单数据转换为前端路由格式 */
function transformMenusToRoutes(menus: SysMenu[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []

  for (const menu of menus) {
    if (menu.menuType === 'F') continue // 按钮级别不生成路由

    // 动态路由需按条件逐步补充 component/redirect/children，
    // 用 Partial 先构建骨架，最终在 push 时断言为完整 RouteRecordRaw
    const route: Partial<RouteRecordRaw> = {
      path: menu.path.startsWith('/') ? menu.path : `/${menu.path}`,
      name: menu.path.replace(/\//g, '-').replace(/^-/, '') || `menu-${menu.id}`,
      meta: {
        title: menu.menuName,
        icon: menu.icon || undefined,
        hidden: menu.visible === '1',
        permissions: menu.perms ? [menu.perms] : undefined,
      },
    }

    if (menu.component) {
      route.component = loadView(menu.component)
    }

    if (menu.children && menu.children.length > 0) {
      const childRoutes = transformMenusToRoutes(menu.children)
      if (childRoutes.length > 0) {
        // 如果有子路由，设置 redirect 到第一个子路由
        route.redirect = childRoutes[0].path
        route.children = childRoutes
      }
    }

    // 骨架构建完毕（path/name/meta + 可选 component/redirect/children），
    // 断言为完整 RouteRecordRaw 后入栈
    routes.push(route as RouteRecordRaw)
  }

  return routes
}

/** 预扫描所有视图组件（懒加载）；
 * 动态 import(`@/views/${x}.vue`) 的别名写法在生产构建中不被 Rollup 支持，
 * 必须用 import.meta.glob 让 Vite 在构建期确定所有可能的模块 */
const viewModules = import.meta.glob('@/views/**/*.vue')

/** 动态加载视图组件 */
function loadView(component: string) {
  // component 格式如 "system/user/index"
  const path = `/src/views/${component}.vue`
  const loader = viewModules[path]
  if (!loader) {
    console.warn(`[router] 视图组件不存在: ${path}`)
    return () => import('@/views/error/404.vue')
  }
  return loader
}
