/** 菜单项（后端返回格式） */
export interface SysMenu {
  id: string
  parentId: string
  menuName: string
  menuType: 'M' | 'C' | 'F'  // M=目录 C=菜单 F=按钮
  perms: string
  path: string
  component: string
  icon: string
  orderNum: number
  visible: string
  status: string
  remark: string
  children?: SysMenu[]
  createTime?: string
  updateTime?: string
}

/** 路由菜单项（前端处理后的格式） */
export interface RouteMenu {
  path: string
  name: string
  component?: string
  redirect?: string
  meta: RouteMeta
  children?: RouteMenu[]
}

/** 路由元信息 */
export interface RouteMeta {
  title: string
  icon?: string
  hidden?: boolean
  permissions?: string[]
  keepAlive?: boolean
  affix?: boolean
  activeMenu?: string
}
