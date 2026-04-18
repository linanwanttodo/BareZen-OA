import router from '@/router'
import { useUserStore } from '@/stores/user'
import type { RouteLocationNormalized } from 'vue-router'

const whiteList = ['/login', '/404', '/401']

router.beforeEach(async (to: RouteLocationNormalized, _from, next) => {
  const title = to.meta.title
  document.title = title ? `${title} - BareZen 管理后台` : 'BareZen 管理后台'

  const userStore = useUserStore()
  const hasToken = userStore.token

  if (hasToken) {
    if (to.path === '/login') {
      next({ path: '/dashboard' })
    } else {
      if (!userStore.userInfo) {
        try {
          await userStore.fetchUserInfo()
          next({ ...to, replace: true })
        } catch {
          await userStore.logout()
          next(`/login?redirect=${to.path}`)
        }
      } else {
        next()
      }
    }
  } else {
    if (whiteList.some(p => to.path.startsWith(p))) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
    }
  }
})