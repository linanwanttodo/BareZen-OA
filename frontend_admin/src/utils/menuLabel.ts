import type { SysMenu } from '@/types/menu'

/**
 * 菜单 / 路由标题的 i18n 解析（导航栏、侧边菜单、标签页、面包屑共用）。
 *
 * 历史上这里有三份互不重叠的硬编码映射表：TopNavbar 一份（path 末段 → route key，
 * 21 条）、TagsView 一份（route name → route key，17 条）、Breadcrumb 直接按
 * route.name 拼键（动态路由 name 形如 system-user，永远命不中，一直在退回中文）。
 * 新增一个菜单要改三处，漏改即退回显示数据库中文 menuName。
 *
 * 现改为按约定推导候选键，依次尝试：
 *   1. 全路径转 camelCase：kb/category → route.kbCategory
 *   2. 末段转 camelCase：  system/user → route.user
 * 全路径优先是为了区分不同模块下的同名末段（资产分类 asset/category 与
 * 知识分类 kb/category 若都按末段推导会落到同一个 route.category）。
 */

/** kebab-case → camelCase；已是 camelCase 的保持不变 */
export function kebabToCamel(input: string): string {
  return input.replace(/-([a-z0-9])/g, (_, ch: string) => ch.toUpperCase())
}

/** 由路由 path 生成 i18n 候选键：全路径优先，末段兜底；外链不参与推导 */
export function routeKeyCandidates(path: string): string[] {
  if (/^https?:\/\//i.test(path)) return []
  const clean = path.split('?')[0].replace(/^\//, '')
  const segments = clean.split('/').filter(Boolean)
  if (!segments.length) return []
  const full = kebabToCamel(segments.join('-'))
  const last = kebabToCamel(segments[segments.length - 1])
  return full === last ? [full] : [full, last]
}

/**
 * 按路由 path 解析显示文案：命中第一个存在的 route.* 键，否则退回 fallback。
 * t / te 直接传 useI18n() 返回的同名函数。
 */
export function labelFromPath(
  path: string,
  fallback: string,
  t: (key: string) => string,
  te: (key: string) => boolean
): string {
  for (const key of routeKeyCandidates(path)) {
    const i18nKey = `route.${key}`
    if (te(i18nKey)) return t(i18nKey)
  }
  return fallback
}

/** 解析菜单显示文案，无 path 时退回数据库菜单名 */
export function resolveMenuLabel(
  menu: SysMenu,
  t: (key: string) => string,
  te: (key: string) => boolean
): string {
  return labelFromPath(menu.path || '', menu.menuName, t, te)
}
