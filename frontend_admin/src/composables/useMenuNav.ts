import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { usePermissionStore } from '@/stores/permission'
import { resolveMenuPath } from '@/utils/menuPath'
import { resolveMenuLabel } from '@/utils/menuLabel'
import type { SysMenu } from '@/types/menu'

/**
 * 导航栏共享逻辑：顶部形态与侧边形态必须消费同一份菜单、同一套 active
 * 判定和同一个跳转函数，否则两种形态的高亮与路由行为会分叉。
 */
export function useMenuNav() {
  const route = useRoute()
  const router = useRouter()
  const { t, te } = useI18n()
  const permissionStore = usePermissionStore()

  /**
   * 可见菜单树（仅 M=目录 / C=菜单，排除 F 按钮与 visible='1' 的隐藏菜单）。
   * 隐藏菜单（如「个人中心」）只通过头像下拉访问，两种形态均不展示。
   */
  const visibleMenus = computed<SysMenu[]>(() =>
    permissionStore.menuList
      .filter((m) => m.menuType !== 'F' && m.visible !== '1')
      .map((m) => ({
        ...m,
        children: (m.children || []).filter((c) => c.menuType !== 'F' && c.visible !== '1'),
      }))
  )

  const label = (menu: SysMenu) => resolveMenuLabel(menu, t, te)

  const isActive = (item: SysMenu, parent?: SysMenu): boolean => {
    const normalized = resolveMenuPath(item, parent)
    if (!normalized || normalized.startsWith('http')) return false
    return route.path === normalized || route.path.startsWith(`${normalized}/`)
  }

  /** 当前路径对应的菜单 value（供 t-menu 的 value 与 expanded 反查） */
  const activePath = computed(() => {
    for (const top of visibleMenus.value) {
      if (isActive(top)) {
        const hit = (top.children || []).find((c) => isActive(c, top))
        return hit ? resolveMenuPath(hit, top) : resolveMenuPath(top)
      }
    }
    return route.path
  })

  /** 当前激活项所属的顶层目录 value（侧边形态用于展开对应分组） */
  const activeRootValue = computed(() => {
    for (const top of visibleMenus.value) {
      if ((top.children || []).some((c) => isActive(c, top)) || isActive(top)) {
        return resolveMenuPath(top)
      }
    }
    return ''
  })

  function navigate(item: SysMenu, parent?: SysMenu) {
    const fullPath = resolveMenuPath(item, parent)
    if (!fullPath) return
    if (fullPath.startsWith('http')) {
      window.open(fullPath, '_blank')
      return
    }
    router.push(fullPath)
  }

  return { visibleMenus, label, isActive, navigate, activePath, activeRootValue }
}
