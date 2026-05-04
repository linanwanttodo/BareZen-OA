<template>
  <div class="user-layout">
    <!-- 顶部导航 -->
    <header class="user-header">
      <!-- 左侧：Logo + 品牌名 -->
      <div class="user-header__left">
        <img src="/logo-icon.svg" alt="BareZen" class="user-header__logo" />
        <span class="user-header__brand">BareZen</span>
      </div>

      <!-- 中间：导航菜单 -->
      <nav class="user-header__nav">
        <a
          v-for="item in flatNavItems"
          :key="item.path"
          class="user-header__nav-item"
          :class="{ 'user-header__nav-item--active': isNavActive(item.path) }"
          @click="router.push(item.path)"
        >
          {{ item.title }}
        </a>

        <a-dropdown
          v-for="group in dropdownGroups"
          :key="group.title"
          trigger="hover"
          position="bl"
          :hide-on-select="false"
        >
          <div
            class="user-header__nav-item user-header__nav-group"
            :class="{ 'user-header__nav-item--active': isGroupActive(group) }"
          >
            <span>{{ group.title }}</span>
            <ChevronDown :size="14" class="user-header__nav-group-icon" />
          </div>
          <template #content>
            <a-doption v-for="child in group.children" :key="child.path" @click="router.push(child.path)">
              {{ child.title }}
            </a-doption>
          </template>
        </a-dropdown>
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
          <img src="/logo-icon.svg" alt="BareZen" class="user-footer__logo" />
          <span>BareZen 企业门户</span>
        </div>
        <p>© 2026 BareZen</p>
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
          <a-input-password v-model="pwdForm.newPassword" placeholder="请输入新密码" />
        </a-form-item>
        <!-- 密码强度指示条（仿大型网站） -->
        <div v-if="pwdForm.newPassword" class="-mt-2 mb-2">
          <div class="flex gap-1">
            <div
              v-for="i in 4"
              :key="i"
              class="h-1.5 flex-1 rounded"
              :class="strengthBarClass(i)"
            ></div>
          </div>
          <p class="mt-1 text-xs" :class="strengthTextClass">{{ strengthText }}</p>
        </div>
        <!-- 校验规则清单 -->
        <div v-if="pwdForm.newPassword" class="mb-2 rounded-md bg-[var(--muted)] p-3 text-xs">
          <p class="mb-1 font-medium">密码强度要求：</p>
          <p v-for="rule in passwordRules" :key="rule.label" :class="rule.valid ? 'text-[var(--bz-success)]' : 'text-[var(--muted-foreground)]'">
            {{ rule.valid ? '已满足' : '未满足' }} {{ rule.label }}
          </p>
        </div>
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
import { ChevronDown } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { changePasswordApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const nickName = computed(() => userStore.userInfo?.nickName || '用户')
const userInitial = computed(() => nickName.value.charAt(0).toUpperCase())

interface NavItem {
  title: string
  path: string
}

// 平铺导航 + 分组下拉导航，与首页 8 个快捷入口一一对应
const flatNavItems: NavItem[] = [
  { title: '工作台', path: '/' },
  { title: '通知公告', path: '/notices' },
  { title: '待办事项', path: '/todos' },
]

const dropdownGroups: { title: string; children: NavItem[] }[] = [
  {
    title: '考勤办公',
    children: [
      { title: '我的申请', path: '/attendance' },
      { title: '工作日志', path: '/worklogs' },
      { title: '日程安排', path: '/schedules' },
    ],
  },
  {
    title: '行政服务',
    children: [
      { title: '办公用品', path: '/supplies' },
      { title: '通讯录', path: '/contacts' },
    ],
  },
]

function isNavActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path === path || route.path.startsWith(path + '/')
}

function isGroupActive(group: { children: NavItem[] }): boolean {
  return group.children.some((child) => isNavActive(child.path))
}

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

// ---- 密码强度实时校验（仿大型网站） ----
interface PwdRule {
  label: string
  valid: boolean
}

function computePasswordRules(pwd: string): PwdRule[] {
  return [
    { label: '至少 8 位', valid: pwd.length >= 8 },
    { label: '包含大写字母', valid: /[A-Z]/.test(pwd) },
    { label: '包含小写字母', valid: /[a-z]/.test(pwd) },
    { label: '包含数字', valid: /\d/.test(pwd) },
    { label: '包含特殊字符', valid: /[^A-Za-z0-9]/.test(pwd) },
  ]
}

const passwordRules = computed(() => computePasswordRules(pwdForm.newPassword))

/** 已满足的规则数（0-5） */
const satisfiedCount = computed(() => passwordRules.value.filter((r) => r.valid).length)

/** 强度等级：0-2 弱，3-4 中，5 强 */
const strengthLevel = computed(() => {
  const n = satisfiedCount.value
  if (n >= 5) return 3
  if (n >= 3) return 2
  if (n >= 1) return 1
  return 0
})

const strengthText = computed(() => {
  const map = ['密码太弱', '强度较弱', '强度中等', '强度很强']
  return pwdForm.newPassword ? map[strengthLevel.value] : ''
})

const strengthTextClass = computed(() => {
  const map = ['text-red-500', 'text-orange-500', 'text-yellow-600', 'text-green-500']
  return map[strengthLevel.value]
})

function strengthBarClass(barIndex: number): string {
  const level = strengthLevel.value
  if (barIndex <= level) {
    const colors = ['bg-red-400', 'bg-orange-400', 'bg-yellow-500', 'bg-green-500']
    return colors[level]
  }
  return 'bg-[var(--border)]'
}

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
  } catch (e) {
    const msg = (e as Error)?.message || '密码修改失败'
    Message.error(msg)
  }
}
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--muted);
}

/* 顶部导航 */
.user-header {
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 32px;
  background: var(--card);
  border-bottom: 1px solid var(--border);
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
  display: flex;
  align-items: center;
  justify-content: center;
}
.user-header__brand {
  font-size: 16px;
  font-weight: 600;
  color: var(--foreground);
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
  color: var(--muted-foreground);
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
}
.user-header__nav-item:hover {
  color: var(--foreground);
  background: var(--muted);
}
.user-header__nav-item--active {
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  font-weight: 500;
}

.user-header__nav-group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.user-header__nav-group-icon {
  transition: transform 0.2s;
}
.user-header__nav-group:hover .user-header__nav-group-icon {
  transform: rotate(180deg);
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
  background: var(--muted);
}
.user-header__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--primary-foreground);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.user-header__username {
  font-size: 14px;
  color: var(--foreground);
}

/* 主内容 */
.user-main {
  flex: 1;
  padding: 24px 32px;
}

/* 页脚 */
.user-footer {
  background: var(--card);
  border-top: 1px solid var(--border);
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
  color: var(--muted-foreground);
}
.user-footer__logo {
  width: 24px;
  height: 24px;
}
.user-footer p {
  font-size: 13px;
  color: var(--muted-foreground);
}

/* ============ 移动端适配（<768px） ============ */
@media (max-width: 768px) {
  .user-header {
    padding: 0 12px;
  }
  .user-header__left {
    margin-right: 8px;
  }
  .user-header__brand {
    display: none; /* 窄屏隐藏品牌文字，仅保留 Logo */
  }
  /* 导航横向滚动，避免换行挤压布局 */
  .user-header__nav {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none; /* Firefox 隐藏滚动条 */
  }
  .user-header__nav::-webkit-scrollbar {
    display: none; /* Chrome/Safari 隐藏滚动条 */
  }
  .user-header__nav-item {
    padding: 6px 10px;
    font-size: 13px;
    white-space: nowrap;
    flex-shrink: 0;
  }
  .user-header__right {
    margin-left: 8px;
  }
  .user-header__username {
    display: none; /* 窄屏仅显示头像，姓名移入下拉菜单 */
  }
  .user-main {
    padding: 12px;
  }
  .user-footer {
    padding: 12px;
  }
  .user-footer__inner {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
