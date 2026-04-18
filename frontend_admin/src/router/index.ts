import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/404',
    name: '404',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  },
  {
    path: '/401',
    name: '401',
    component: () => import('@/views/error/401.vue'),
    meta: { title: '401' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'dashboard' }
      },
      {
        path: 'system/user',
        name: 'system-user',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'user' }
      },
      {
        path: 'system/role',
        name: 'system-role',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'peoples' }
      },
      {
        path: 'system/menu',
        name: 'system-menu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'tree-table' }
      },
      {
        path: 'system/dict',
        name: 'system-dict',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理', icon: 'list' }
      },
      {
        path: 'system/dept',
        name: 'system-dept',
        component: () => import('@/views/system/dept/index.vue'),
        meta: { title: '部门管理', icon: 'tree' }
      },
      {
        path: 'system/role-dept',
        name: 'system-role-dept',
        component: () => import('@/views/system/role-dept/index.vue'),
        meta: { title: '权限分配', icon: 'peoples' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'user' }
      },
      {
        path: 'file',
        name: 'file',
        component: () => import('@/views/file/index.vue'),
        meta: { title: '文件管理', icon: 'folder' }
      },
      {
        path: 'notice',
        name: 'notice',
        component: () => import('@/views/notice/index.vue'),
        meta: { title: '通知公告', icon: 'notification' }
      },
      {
        path: 'monitor/online',
        name: 'monitor-online',
        component: () => import('@/views/monitor/online/index.vue'),
        meta: { title: '在线用户', icon: 'desktop' }
      },
      {
        path: 'monitor/oper-log',
        name: 'monitor-oper-log',
        component: () => import('@/views/monitor/oper-log/index.vue'),
        meta: { title: '操作日志', icon: 'file-text' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory('/'),
  routes
})

export default router
