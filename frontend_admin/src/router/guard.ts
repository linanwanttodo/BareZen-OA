import router from '@/router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { getRoutersApi } from '@/api/menu'
import type { RouteLocationNormalized, RouteRecordRaw } from 'vue-router'

const whiteList = ['/login', '/404', '/401']

/** 将后端菜单树添加为布局路由的子路由 */
function addDynamicRoutes(menus: any[]) {
  const permissionStore = usePermissionStore()
  const dynamicRoutes = permissionStore.generateRoutes(menus)

  // 动态路由作为布局路由（name: 'Layout'）的子路由挂载
  // 注意：addRoute 的第一个参数是父路由的 name，不是 path
  for (const route of dynamicRoutes) {
    router.addRoute('Layout', route as RouteRecordRaw)
  }
}

router.beforeEach(async (to: RouteLocationNormalized, _from, next) => {
  const title = to.meta.title
  document.title = title ? `${title} - BareZen 管理后台` : 'BareZen 管理后台'

  const userStore = useUserStore()
  const hasToken = userStore.token

  if (hasToken) {
    // 用户信息尚未加载 → 先请求
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
      } catch {
        await userStore.logout()
        next(`/login?redirect=${to.path}`)
        return
      }
    }

    // 从后端加载菜单并生成动态路由（须在跳转 /dashboard 之前完成，
    // 否则 /dashboard 尚未挂载会落入 catch-all 404）
    const permissionStore = usePermissionStore()
    if (permissionStore.menuList.length === 0) {
      try {
        const res = await getRoutersApi()
        const menus = res.data
        if (menus && menus.length > 0) {
          addDynamicRoutes(menus)
          // 动态路由刚挂载，当前导航是按旧路由表匹配的，
          // 必须重放一次才能命中新增路由（否则首次访问会落入 404）
          next({ ...to, replace: true })
          return
        }
      } catch {
        // 菜单加载失败不影响导航
      }
    }

    // 已登录访问登录页 → 跳仪表盘（此时动态路由已挂载）
    if (to.path === '/login') {
      next({ path: '/dashboard' })
      return
    }

    next()
  } else {
    if (whiteList.some(p => to.path === p)) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
    }
  }
})
