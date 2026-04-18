<template>
  <div class="user-layout">
    <!-- 顶部导航 -->
    <header class="user-header">
      <!-- 左侧：Logo + 品牌名 -->
      <div class="user-header__left">
        <div class="user-header__logo">B</div>
        <span class="user-header__brand">BareZen</span>
      </div>

      <!-- 中间：导航菜单 -->
      <nav class="user-header__nav">
        <a
          v-for="item in navItems"
          :key="item.path"
          class="user-header__nav-item"
          :class="{ 'user-header__nav-item--active': route.path === item.path || route.path.startsWith(item.path + '/') }"
          @click="router.push(item.path)"
        >
          {{ item.title }}
        </a>
      </nav>

      <!-- 右侧：用户信息 -->
      <div class="user-header__right">
        <template v-if="userStore.token">
          <a-dropdown>
            <div class="user-header__user">
              <div class="user-header__avatar">{{ userInitial }}</div>
              <span class="user-header__username">{{ nickName }}</span>
            </div>
            <template #content>
              <a-doption @click="router.push('/user/profile')">个人中心</a-doption>
              <a-doption @click="pwdDialogVisible = true">修改密码</a-doption>
              <a-doption @click="onLogout" status="danger">退出登录</a-doption>
            </template>
          </a-dropdown>
        </template>
        <template v-else>
          <a-button type="primary" size="small" @click="router.push('/auth/login')">登录</a-button>
        </template>
      </div>
    </header>

    <!-- 主内容 -->
    <main class="user-main">
      <router-view />
    </main>

    <!-- 页脚 -->
    <footer class="user-footer">
      <div class="user-footer__inner">
        <div class="user-footer__left">
          <div class="user-footer__logo">B</div>
          <span>BareZen 企业门户</span>
        </div>
        <p>Powered by Spring Boot 3 + Vue 3</p>
      </div>
    </footer>

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
import { useRouter, useRoute } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useUserStore } from '@/stores/user'
import { changePasswordApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const nickName = computed(() => userStore.userInfo?.nickName || '用户')
const userInitial = computed(() => nickName.value.charAt(0).toUpperCase())

const navItems = [
  { title: '工作台', path: '/' },
  { title: '通知公告', path: '/notices' },
  { title: '通讯录', path: '/contacts' },
  { title: '文件中心', path: '/files' },
  { title: '待办事项', path: '/todos' },
]

async function onLogout() {
  await userStore.logout()
  router.push('/')
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
    router.push('/auth/login')
  } catch {
    // 错误已在拦截器中处理
  }
}
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #F7F8FA;
}

/* 顶部导航 */
.user-header {
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 32px;
  background: #FFFFFF;
  border-bottom: 1px solid #E5E6EB;
  position: sticky;
  top: 0;
  z-index: 100;
}

.user-header__left {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-right: 48px;
  flex-shrink: 0;
}
.user-header__logo {
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
.user-header__brand {
  font-size: 16px;
  font-weight: 600;
  color: #1D2129;
}

.user-header__nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}
.user-header__nav-item {
  padding: 6px 16px;
  font-size: 14px;
  color: #4E5969;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
}
.user-header__nav-item:hover {
  color: #1D2129;
  background: #F7F8FA;
}
.user-header__nav-item--active {
  color: #3370FF;
  background: #E8F3FF;
  font-weight: 500;
}

.user-header__right {
  flex-shrink: 0;
  margin-left: 24px;
}
.user-header__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.user-header__user:hover {
  background: #F7F8FA;
}
.user-header__avatar {
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
.user-header__username {
  font-size: 14px;
  color: #1D2129;
}

/* 主内容 */
.user-main {
  flex: 1;
  padding: 24px 32px;
}

/* 页脚 */
.user-footer {
  background: #FFFFFF;
  border-top: 1px solid #E5E6EB;
  padding: 20px 32px;
}
.user-footer__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
}
.user-footer__left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #4E5969;
}
.user-footer__logo {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background: #3370FF;
  color: #FFFFFF;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.user-footer p {
  font-size: 13px;
  color: #86909C;
}
</style>
