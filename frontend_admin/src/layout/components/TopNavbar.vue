<template>
  <div class="top-navbar">
    <!-- 左侧：Logo + 品牌名 -->
    <div class="top-navbar__left">
      <div class="top-navbar__logo">B</div>
      <span class="top-navbar__brand">BareZen</span>
    </div>

    <!-- 中间：导航菜单 -->
    <nav class="top-navbar__nav">
      <a class="top-navbar__nav-item" :class="{ 'top-navbar__nav-item--active': route.path === '/dashboard' }" @click="router.push('/dashboard')">仪表盘</a>

      <!-- 系统管理 下拉 -->
      <div class="top-navbar__dropdown" @mouseenter="systemOpen = true" @mouseleave="systemOpen = false">
        <a class="top-navbar__nav-item" :class="{ 'top-navbar__nav-item--active': route.path.startsWith('/system') }">
          系统管理
          <span class="top-navbar__arrow" :class="{ 'top-navbar__arrow--open': systemOpen }">›</span>
        </a>
        <div v-show="systemOpen" class="top-navbar__dropdown-menu">
          <a v-for="item in systemMenus" :key="item.path" class="top-navbar__dropdown-item" :class="{ 'top-navbar__dropdown-item--active': route.path === item.path }" @click="router.push(item.path)">{{ item.title }}</a>
        </div>
      </div>

      <a class="top-navbar__nav-item" :class="{ 'top-navbar__nav-item--active': route.path === '/file' }" @click="router.push('/file')">文件管理</a>
      <a class="top-navbar__nav-item" :class="{ 'top-navbar__nav-item--active': route.path === '/notice' }" @click="router.push('/notice')">通知公告</a>

      <!-- 系统监控 下拉 -->
      <div class="top-navbar__dropdown" @mouseenter="monitorOpen = true" @mouseleave="monitorOpen = false">
        <a class="top-navbar__nav-item" :class="{ 'top-navbar__nav-item--active': route.path.startsWith('/monitor') }">
          系统监控
          <span class="top-navbar__arrow" :class="{ 'top-navbar__arrow--open': monitorOpen }">›</span>
        </a>
        <div v-show="monitorOpen" class="top-navbar__dropdown-menu">
          <a v-for="item in monitorMenus" :key="item.path" class="top-navbar__dropdown-item" :class="{ 'top-navbar__dropdown-item--active': route.path === item.path }" @click="router.push(item.path)">{{ item.title }}</a>
        </div>
      </div>
    </nav>

    <!-- 右侧：用户信息 -->
    <div class="top-navbar__right">
      <a-dropdown>
        <div class="top-navbar__user">
          <div class="top-navbar__avatar">{{ userInitial }}</div>
          <span class="top-navbar__username">{{ nickName }}</span>
        </div>
        <template #content>
          <a-doption @click="router.push('/profile')">个人中心</a-doption>
          <a-doption @click="pwdDialogVisible = true">修改密码</a-doption>
          <a-doption @click="onLogout" status="danger">退出登录</a-doption>
        </template>
      </a-dropdown>
    </div>

    <!-- 修改密码弹窗 -->
    <a-modal
      v-model:visible="pwdDialogVisible"
      title="修改密码"
      :width="400"
      :mask-closable="false"
      @ok="onPwdSubmit"
      @cancel="pwdDialogVisible = false"
    >
      <a-form :model="pwdForm" layout="vertical">
        <a-form-item field="oldPassword" label="旧密码">
          <a-input-password v-model="pwdForm.oldPassword" placeholder="请输入旧密码" />
        </a-form-item>
        <a-form-item field="newPassword" label="新密码">
          <a-input-password v-model="pwdForm.newPassword" placeholder="请输入新密码（至少6位）" />
        </a-form-item>
        <a-form-item field="confirmPassword" label="确认新密码">
          <a-input-password v-model="pwdForm.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useUserStore } from '@/stores/user'
import { changePasswordApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const nickName = computed(() => userStore.userInfo?.nickName || '管理员')
const userInitial = computed(() => nickName.value.charAt(0).toUpperCase())

// 下拉菜单状态
const systemOpen = ref(false)
const monitorOpen = ref(false)

const systemMenus = [
  { title: '用户管理', path: '/system/user' },
  { title: '角色管理', path: '/system/role' },
  { title: '部门管理', path: '/system/dept' },
  { title: '菜单管理', path: '/system/menu' },
  { title: '字典管理', path: '/system/dict' },
  { title: '权限分配', path: '/system/role-dept' },
]

const monitorMenus = [
  { title: '操作日志', path: '/monitor/oper-log' },
  { title: '在线用户', path: '/monitor/online' },
]

async function onLogout() {
  await userStore.logout()
  await router.push('/login')
  Message.success('已退出登录')
}

// ---- 修改密码 ----
const pwdDialogVisible = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

async function onPwdSubmit() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
    Message.warning('请填写完整信息')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    Message.warning('两次输入的新密码不一致')
    return
  }
  try {
    await changePasswordApi({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    Message.success('密码修改成功，请重新登录')
    pwdDialogVisible.value = false
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    await userStore.logout()
    await router.push('/login')
  } catch {
    // 错误已在拦截器中处理
  }
}
</script>

<style scoped>
.top-navbar {
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 32px;
  background: #FFFFFF;
  border-bottom: 1px solid #E5E6EB;
}

/* 左侧 */
.top-navbar__left {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-right: 48px;
  flex-shrink: 0;
}
.top-navbar__logo {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background: #3370FF;
  color: #FFFFFF;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.top-navbar__brand {
  font-size: 16px;
  font-weight: 600;
  color: #1D2129;
}

/* 中间导航 */
.top-navbar__nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}
.top-navbar__nav-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 16px;
  font-size: 14px;
  color: #4E5969;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
}
.top-navbar__nav-item:hover {
  color: #1D2129;
  background: #F7F8FA;
}
.top-navbar__nav-item--active {
  color: #3370FF;
  background: #E8F3FF;
  font-weight: 500;
}

/* 下拉箭头 */
.top-navbar__arrow {
  font-size: 12px;
  transition: transform 0.2s;
}
.top-navbar__arrow--open {
  transform: rotate(90deg);
}

/* 下拉菜单 */
.top-navbar__dropdown {
  position: relative;
}
.top-navbar__dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  min-width: 140px;
  padding: 4px;
  background: #FFFFFF;
  border: 1px solid #E5E6EB;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  z-index: 1000;
}
.top-navbar__dropdown-item {
  display: block;
  padding: 8px 12px;
  font-size: 14px;
  color: #4E5969;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
}
.top-navbar__dropdown-item:hover {
  color: #1D2129;
  background: #F7F8FA;
}
.top-navbar__dropdown-item--active {
  color: #3370FF;
  background: #E8F3FF;
}

/* 右侧 */
.top-navbar__right {
  flex-shrink: 0;
  margin-left: 24px;
}
.top-navbar__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.top-navbar__user:hover {
  background: #F7F8FA;
}
.top-navbar__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #3370FF;
  color: #FFFFFF;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.top-navbar__username {
  font-size: 14px;
  color: #1D2129;
}
</style>
