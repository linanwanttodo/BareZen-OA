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
          path: 'messages',
          name: 'Messages',
          component: () => import('@/pages/message/index.vue'),
          meta: { title: '消息中心', requiresAuth: true },
        },
        {
          path: 'messages/:id',
          name: 'MessageDetail',
          component: () => import('@/pages/message/detail.vue'),
          meta: { title: '消息详情', requiresAuth: true },
        },
        {
          path: 'notices/:id',
          name: 'NoticeDetail',
          component: () => import('@/pages/notice/detail.vue'),
          meta: { title: '公告详情', requiresAuth: true },
        },
        {
          path: 'contacts',
          name: 'Contacts',
          component: () => import('@/pages/contact/index.vue'),
          meta: { title: '通讯录', requiresAuth: true },
        },
        {
          path: 'todos',
          name: 'Todos',
          component: () => import('@/pages/todo/index.vue'),
          meta: { title: '待办事项', requiresAuth: true },
        },
        {
          path: 'attendance',
          name: 'Attendance',
          component: () => import('@/pages/attendance/index.vue'),
          meta: { title: '我的申请', requiresAuth: true },
        },
        {
          path: 'attendance/clock',
          name: 'Clock',
          component: () => import('@/pages/attendance/clock.vue'),
          meta: { title: '考勤打卡', requiresAuth: true },
        },
        {
          path: 'worklogs',
          name: 'WorkLogs',
          component: () => import('@/pages/worklog/index.vue'),
          meta: { title: '工作日志', requiresAuth: true },
        },
        {
          path: 'schedules',
          name: 'Schedules',
          component: () => import('@/pages/schedule/index.vue'),
          meta: { title: '日程管理', requiresAuth: true },
        },
        {
          path: 'supplies',
          name: 'Supplies',
          component: () => import('@/pages/supply/index.vue'),
          meta: { title: '办公用品', requiresAuth: true },
        },
        {
          path: 'vehicles',
          name: 'Vehicles',
          component: () => import('@/pages/vehicle/index.vue'),
          meta: { title: '用车申请', requiresAuth: true },
        },
        {
          path: 'meetings',
          name: 'Meetings',
          component: () => import('@/pages/meeting/index.vue'),
          meta: { title: '会议预订', requiresAuth: true },
        },
        {
          path: 'salary',
          name: 'Salary',
          component: () => import('@/pages/salary/index.vue'),
          meta: { title: '我的工资条', requiresAuth: true },
        },
        {
          path: 'kb',
          name: 'Kb',
          component: () => import('@/pages/kb/index.vue'),
          meta: { title: '知识库', requiresAuth: true },
        },
        {
          path: 'assets',
          name: 'Assets',
          component: () => import('@/pages/asset/index.vue'),
          meta: { title: '资产借用', requiresAuth: true },
        },
        {
          path: 'files',
          name: 'Files',
          component: () => import('@/pages/files/index.vue'),
          meta: { title: '我的文件', requiresAuth: true },
        },
      ],
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/pages/auth/login.vue'),
      meta: { title: '登录' },
    },
    // 旧地址兼容：统一登录路由为 /login
    { path: '/auth/login', redirect: '/login' },
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
const whiteList = ['/notices', '/login']

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  document.title = `${to.meta.title || 'BareZen'} - 企业门户`

  const token = getToken()

  if (token && to.path === '/login') {
    next({ path: '/' })
  } else if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (token && !whiteList.some(p => to.path === p)) {
    const userStore = useUserStore()
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
      } catch {
        userStore.resetState()
        next({ path: '/login', query: { redirect: to.fullPath } })
        return
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
