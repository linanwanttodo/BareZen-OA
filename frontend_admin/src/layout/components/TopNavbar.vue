<template>
  <div class="top-navbar">
    <!-- 左侧：Logo + 品牌名 -->
    <div class="top-navbar__left">
      <img src="/logo-icon.svg" alt="BareZen" class="top-navbar__logo" />
      <span class="top-navbar__brand">BareZen</span>
    </div>

    <!-- 中间：导航菜单（从后端动态渲染） -->
    <nav class="top-navbar__nav">
      <template v-for="item in visibleMenus" :key="item.id">
        <!-- 有子菜单 → 下拉 -->
        <div
          v-if="item.children && item.children.length > 0"
          class="top-navbar__dropdown"
          @mouseenter="openDropdown(item.id)"
          @mouseleave="closeDropdown(item.id)"
        >
          <a
            class="top-navbar__nav-item"
            :class="{ 'top-navbar__nav-item--active': isActive(item) }"
          >
            {{ getMenuTranslation(item) }}
            <span class="top-navbar__arrow" :class="{ 'top-navbar__arrow--open': dropdownOpen[item.id] }">›</span>
          </a>
          <div v-show="dropdownOpen[item.id]" class="top-navbar__dropdown-menu">
            <a
              v-for="child in item.children"
              :key="child.id"
              class="top-navbar__dropdown-item"
              :class="{ 'top-navbar__dropdown-item--active': isActive(child) }"
              @click="navigate(child, item)"
            >
              {{ getMenuTranslation(child) }}
            </a>
          </div>
        </div>

        <!-- 无子菜单 → 直接链接 -->
        <a
          v-else
          class="top-navbar__nav-item"
          :class="{ 'top-navbar__nav-item--active': isActive(item) }"
          @click="navigate(item)"
        >
          {{ getMenuTranslation(item) }}
        </a>
      </template>
    </nav>

    <!-- 右侧：语言切换 + 用户信息 -->
    <div class="top-navbar__right">
      <LangSwitch />
      <a-dropdown>
        <div class="top-navbar__user">
          <div class="top-navbar__avatar">{{ userInitial }}</div>
          <span class="top-navbar__username">{{ nickName }}</span>
        </div>
        <template #content>
          <a-doption @click="router.push('/profile')">{{ $t('route.profile') }}</a-doption>
          <a-doption @click="pwdDialogVisible = true">{{ $t('profile.changePwd') }}</a-doption>
          <a-doption @click="onLogout" status="danger">{{ $t('login.logout') }}</a-doption>
        </template>
      </a-dropdown>
    </div>

    <!-- 修改密码弹窗 -->
    <a-modal
      v-model:visible="pwdDialogVisible"
      :title="$t('profile.changePasswordTitle')"
      :width="400"
      :mask-closable="false"
      @ok="onPwdSubmit"
      @cancel="pwdDialogVisible = false"
    >
      <a-form :model="pwdForm" layout="vertical">
        <a-form-item field="oldPassword" :label="$t('profile.oldPassword')">
          <a-input-password v-model="pwdForm.oldPassword" :placeholder="$t('profile.oldPasswordPlaceholder')" />
        </a-form-item>
        <a-form-item field="newPassword" :label="$t('profile.newPassword')">
          <a-input-password v-model="pwdForm.newPassword" :placeholder="$t('profile.newPasswordPlaceholder')" />
        </a-form-item>
        <a-form-item field="confirmPassword" :label="$t('profile.confirmPassword')">
          <a-input-password v-model="pwdForm.confirmPassword" :placeholder="$t('profile.confirmPasswordPlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { changePasswordApi } from '@/api/auth'
import LangSwitch from '@/components/common/LangSwitch.vue'
import type { SysMenu } from '@/types/menu'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const nickName = computed(() => userStore.userInfo?.nickName || t('user.adminUser'))
const userInitial = computed(() => nickName.value.charAt(0).toUpperCase())

/** 可见的菜单（仅 M=目录/C=菜单，排除按钮） */
const visibleMenus = computed<SysMenu[]>(() => {
  return permissionStore.menuList.filter(m => m.menuType !== 'F')
})

/**
 * 根据菜单项路径获取翻译文本
 * 尝试从 i18n 的 route.* 中查找翻译，找不到则使用原始 menuName
 */
function getMenuTranslation(item: SysMenu): string {
  const path = item.path
  if (!path) return item.menuName
  // 提取路径最后一段作为 route key
  const segments = path.replace(/^\//, '').split('/')
  const key = segments[segments.length - 1]
  // 尝试各种可能的 key 映射
  const routeKeyMap: Record<string, string> = {
    'dashboard': 'dashboard',
    'system': 'system',
    'user': 'user',
    'role': 'role',
    'menu': 'menu',
    'dept': 'dept',
    'dict': 'dict',
    'notice': 'notice',
    'config': 'config',
    'monitor': 'monitor',
    'online': 'online',
    'oper-log': 'operLog',
    'logininfor': 'logininfor',
    'jobs': 'jobs',
    'job-log': 'jobLog',
    'tool': 'tool',
    'gen': 'gen',
    'profile': 'profile',
    'backup': 'backup',
    'role-dept': 'roleDept',
  }
  const routeKey = routeKeyMap[key]
  if (routeKey) {
    const translation = t('route.' + routeKey)
    // i18n 的 t() 在 key 不存在时返回 key 本身，所以需要判断
    if (translation !== 'route.' + routeKey) {
      return translation
    }
  }
  return item.menuName
}

// 下拉展开状态
const dropdownOpen = reactive<Record<string, boolean>>({})
function openDropdown(id: string) { dropdownOpen[id] = true }
function closeDropdown(id: string) { dropdownOpen[id] = false }

function isActive(item: SysMenu): boolean {
  const path = item.path
  if (!path) return false
  if (path.startsWith('http')) return false
  const normalized = path.startsWith('/') ? path : '/' + path
  return route.path === normalized || route.path.startsWith(normalized + '/')
}

function navigate(item: SysMenu, parent?: SysMenu) {
  const path = item.path
  if (!path) return
  if (path.startsWith('http')) {
    window.open(path, '_blank')
    return
  }
  // 拼接完整路径：/parent/child
  let fullPath = path
  if (parent && parent.path && item.menuType === 'C') {
    fullPath = parent.path + '/' + path
  }
  const normalized = fullPath.startsWith('/') ? fullPath : '/' + fullPath
  // 尝试 push，如果路由不存在则 fallback 到静态路由
  router.push(normalized).catch(() => {
    // 如果动态路由不存在，尝试静态路由
    router.push(normalized)
  })
}

async function onLogout() {
  await userStore.logout()
  await router.push('/login')
  Message.success(t('login.logoutSuccess'))
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
    Message.warning(t('login.passwordInfoIncomplete'))
    return
  }
  if (pwdForm.newPassword.length < 6) {
    Message.warning(t('login.passwordTooShort'))
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    Message.warning(t('login.passwordMismatch'))
    return
  }

  try {
    await changePasswordApi({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    Message.success(t('login.passwordChangeSuccess'))
    pwdDialogVisible.value = false
    userStore.logout()
    router.push('/login')
  } catch (e) {
    const msg = (e as Error)?.message || '密码修改失败'
    Message.error(msg)
  }
}
</script>

<style scoped>
.top-navbar {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-color, #fff);
}
.top-navbar__left {
  display: flex;
  align-items: center;
  margin-right: 24px;
}
.top-navbar__logo {
  width: 32px;
  height: 32px;
  margin-right: 10px;
}
.top-navbar__brand {
  font-weight: 600;
  font-size: 16px;
  color: var(--foreground);
}
.top-navbar__nav {
  display: flex;
  align-items: center;
  flex: 1;
  gap: 2px;
}
.top-navbar__nav-item {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  color: var(--muted-foreground, #666);
  text-decoration: none;
  transition: background .15s, color .15s;
}
.top-navbar__nav-item:hover {
  background: var(--hover-bg, #f0f0f0);
  color: var(--foreground, #333);
}
.top-navbar__nav-item--active,
.top-navbar__nav-item--active:hover {
  background: #E8F3FF;
  color: #3370FF;
  font-weight: 500;
}
.top-navbar__arrow {
  margin-left: 4px;
  font-size: 14px;
  transition: transform .2s;
}
.top-navbar__arrow--open {
  transform: rotate(90deg);
}
.top-navbar__dropdown {
  position: relative;
}
.top-navbar__dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  min-width: 160px;
  padding: 4px;
  border: 1px solid var(--border, #e5e6eb);
  border-radius: 8px;
  background: var(--bg-color, #fff);
  box-shadow: 0 4px 12px rgba(0,0,0,.08);
  z-index: 1000;
}
.top-navbar__dropdown-item {
  display: block;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  color: var(--foreground, #333);
  text-decoration: none;
  white-space: nowrap;
}
.top-navbar__dropdown-item:hover {
  background: var(--hover-bg, #f5f5f5);
}
.top-navbar__dropdown-item--active {
  color: #3370FF;
  background: #E8F3FF;
}
.top-navbar__right {
  display: flex;
  align-items: center;
}
.top-navbar__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
}
.top-navbar__user:hover {
  background: var(--hover-bg, #f5f5f5);
}
.top-navbar__avatar {
  width: 28px; height: 28px;
  border-radius: 50%;
  background: #3370FF;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}
.top-navbar__username {
  font-size: 14px;
  color: var(--foreground);
}
</style>
