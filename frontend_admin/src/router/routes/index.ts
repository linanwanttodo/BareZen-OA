import type { RouteRecordRaw } from 'vue-router'

/** 静态路由（不需要权限） */
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true },
  },
  {
    path: '/error',
    name: 'Error',
    component: () => import('@/views/error/index.vue'),
    meta: { title: '错误', hidden: true },
    children: [
      {
        path: '401',
        name: 'Error401',
        component: () => import('@/views/error/401.vue'),
        meta: { title: '未授权', hidden: true },
      },
      {
        path: '403',
        name: 'Error403',
        component: () => import('@/views/error/403.vue'),
        meta: { title: '禁止访问', hidden: true },
      },
      {
        path: '404',
        name: 'Error404',
        component: () => import('@/views/error/404.vue'),
        meta: { title: '页面不存在', hidden: true },
      },
      {
        path: '500',
        name: 'Error500',
        component: () => import('@/views/error/500.vue'),
        meta: { title: '服务器错误', hidden: true },
      },
    ],
  },
]
