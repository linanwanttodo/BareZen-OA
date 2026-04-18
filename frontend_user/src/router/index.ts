import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'

NProgress.configure({ showSpinner: false })

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        {
          path: '',
          name: 'Home',
          component: () => import('@/pages/dashboard/index.vue'),
          meta: { title: '工作台', requiresAuth: true },
        },
        {
          path: 'notices',
          name: 'NoticeList',
          component: () => import('@/pages/notice/list.vue'),
          meta: { title: '通知公告' },
        },
        {
          path: 'notices/:id',
          name: 'NoticeDetail',
          component: () => import('@/pages/notice/detail.vue'),
          meta: { title: '公告详情' },
        },
        {
          path: 'contacts',
          name: 'Contacts',
          component: () => import('@/pages/contact/index.vue'),
          meta: { title: '通讯录', requiresAuth: true },
        },
        {
          path: 'files',
          name: 'Files',
          component: () => import('@/pages/file/index.vue'),
          meta: { title: '文件中心', requiresAuth: true },
        },
        {
          path: 'todos',
          name: 'Todos',
          component: () => import('@/pages/todo/index.vue'),
          meta: { title: '待办事项', requiresAuth: true },
        },
      ],
    },
    {
      path: '/auth',
      component: () => import('@/layouts/AuthLayout.vue'),
      children: [
        {
          path: 'login',
          name: 'Login',
          component: () => import('@/pages/auth/login.vue'),
          meta: { title: '登录' },
        },
      ],
    },
    {
      path: '/user',
      component: () => import('@/layouts/UserLayout.vue'),
      children: [
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/pages/user/profile.vue'),
          meta: { title: '个人信息', requiresAuth: true },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/pages/error/404.vue'),
      meta: { title: '页面不存在' },
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

// 白名单路由：不需要登录即可访问的页面
// 注意：首页（/）不在白名单中，因为其 meta.requiresAuth = true
const whiteList = ['/notices', '/auth/login', '/notices/:id(\\d+)']

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  document.title = `${to.meta.title || 'BareZen'} - 企业门户`

  const token = getToken()

  if (to.meta.requiresAuth && !token) {
    next({ path: '/auth/login', query: { redirect: to.fullPath } })
  } else if (token && !whiteList.includes(to.path)) {
    const userStore = useUserStore()
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
      } catch {
        userStore.resetState()
      }
    }
    next()
  } else {
    next()
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
