import type { RouteMeta } from './menu'

/** 扩展 Vue Router 的 RouteMeta */
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    hidden?: boolean
    permissions?: string[]
    keepAlive?: boolean
    affix?: boolean
    activeMenu?: string
  }
}
