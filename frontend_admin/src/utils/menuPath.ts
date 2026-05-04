import type { SysMenu } from '@/types/menu'

/**
 * 解析菜单点击跳转的完整路由路径。
 *
 * 后端 sys_menu.path 存在两种约定（历史遗留混用）：
 * 1. 已含父前缀的完整路径：如 系统管理(system) > 用户管理(system/user)
 * 2. 相对父级的短路径：如 知识库(kb) > 知识文档(document)
 *
 * 本函数统一将两者解析为与前端动态路由（transformMenusToRoutes 注册的
 * 绝对路径）一致的结果，避免拼接出 /system/system/user 这类无效路径
 * 而落入 catch-all 404。
 */
export function resolveMenuPath(item: SysMenu, parent?: SysMenu): string {
  const path = item.path
  if (!path) return ''
  // 外链不处理
  if (/^https?:\/\//i.test(path)) return path

  if (!parent || !parent.path) {
    return path.startsWith('/') ? path : `/${path}`
  }

  const child = path.startsWith('/') ? path : `/${path}`
  const parentPath = parent.path.startsWith('/') ? parent.path : `/${parent.path}`

  // 子路径已含父前缀（含恰好等于父路径的情况）→ 直接用子路径
  if (child === parentPath || child.startsWith(`${parentPath}/`)) {
    return child
  }

  // 相对路径 → 拼接父前缀
  return `${parentPath}/${path.replace(/^\//, '')}`
}
