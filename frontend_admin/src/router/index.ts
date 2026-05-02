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
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    // 根路径先指向登录页：/dashboard 是登录后动态挂载的路由，
    // 未登录时直接 redirect 到 /dashboard 会落入 catch-all 404。
    // 已登录用户访问根路径时，路由守卫会自动跳转到 /dashboard。
    redirect: '/login',
    children: [] // 业务路由全部由后端动态加载
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
