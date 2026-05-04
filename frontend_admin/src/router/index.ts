import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/auth'

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
    // 根路径按登录态定向：未登录 → 登录页；已登录 → 仪表盘。
    // 不能用固定 redirect '/login'：登录后 push('/') 会被 vue-router
    // 先重定向到 /login，与当前路由相同而判定为「重复导航」，守卫被
    // 跳过，页面卡在登录页（登录成功但无跳转）。
    redirect: () => (getToken() ? '/dashboard' : '/login'),
    children: [] // 业务路由全部由后端动态加载
  },
  {
    // catch-all 不再提前 redirect '/404'：
    // 若重定向先于守卫生效，已登录访问尚未挂载的 /dashboard 时，
    // 守卫来不及加载动态路由，会被直接打入 404。
    // 直接渲染 404 组件，让守卫有机会先挂载动态路由再重放导航。
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory('/'),
  routes
})

export default router
