<template>
  <div class="top-navbar">
    <!-- 左侧：Logo + 品牌名 -->
    <div class="top-navbar__left">
      <img src="/logo.png" alt="BareZen" class="top-navbar__logo" />
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
            <icon-down class="top-navbar__arrow" :class="{ 'top-navbar__arrow--open': dropdownOpen[item.id] }" />
          </a>
          <div v-show="dropdownOpen[item.id]" class="top-navbar__dropdown-menu">
            <a
              v-for="child in item.children"
              :key="child.id"
              class="top-navbar__dropdown-item"
              :class="{ 'top-navbar__dropdown-item--active': isActive(child, item) }"
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
        <div v-if="pwdForm.newPassword" class="mb-2 rounded-wecom-md bg-[var(--wecom-bg-container)] p-3 text-xs">
          <p class="mb-1 font-medium">{{ $t('user.pwdPolicyTitle') }}</p>
          <p v-for="rule in passwordRules" :key="rule.label" class="flex items-center gap-1.5" :class="rule.valid ? 'text-[var(--wecom-success)]' : 'text-[var(--wecom-text-tertiary)]'">
            <icon-check v-if="rule.valid" :size="12" />
            <icon-minus v-else :size="12" />
            <span>{{ rule.label }}</span>
          </p>
        </div>
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
import { IconDown, IconCheck, IconMinus } from '@arco-design/web-vue/es/icon'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { changePasswordApi } from '@/api/auth'
import LangSwitch from '@/components/common/LangSwitch.vue'
import { resolveMenuPath } from '@/utils/menuPath'
import type { SysMenu } from '@/types/menu'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const nickName = computed(() => userStore.userInfo?.nickName || t('user.adminUser'))
const userInitial = computed(() => nickName.value.charAt(0).toUpperCase())

/**
 * 可见的菜单（仅 M=目录/C=菜单，排除按钮）。
 * 同时过滤 visible='1' 的隐藏菜单（如「个人中心」只通过头像下拉访问，
 * 不显示在导航栏），顶层与子菜单均过滤。
 */
const visibleMenus = computed<SysMenu[]>(() => {
  return permissionStore.menuList
    .filter(m => m.menuType !== 'F' && m.visible !== '1')
    .map(m => ({
      ...m,
      children: (m.children || []).filter(c => c.menuType !== 'F' && c.visible !== '1'),
    }))
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
    'file': 'file',
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

function isActive(item: SysMenu, parent?: SysMenu): boolean {
  const normalized = resolveMenuPath(item, parent)
  if (!normalized || normalized.startsWith('http')) return false
  return route.path === normalized || route.path.startsWith(normalized + '/')
}

function navigate(item: SysMenu, parent?: SysMenu) {
  const fullPath = resolveMenuPath(item, parent)
  if (!fullPath) return
  if (fullPath.startsWith('http')) {
    window.open(fullPath, '_blank')
    return
  }
  // 尝试 push，如果路由不存在则 fallback 到静态路由
  router.push(fullPath).catch(() => {
    // 如果动态路由不存在，尝试静态路由
    router.push(fullPath)
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

// ---- 密码强度实时校验（仿大型网站） ----
interface PwdRule {
  label: string
  valid: boolean
}

function computePasswordRules(pwd: string): PwdRule[] {
  return [
    { label: t('user.pwdRuleLength'), valid: pwd.length >= 8 },
    { label: t('user.pwdRuleUpper'), valid: /[A-Z]/.test(pwd) },
    { label: t('user.pwdRuleLower'), valid: /[a-z]/.test(pwd) },
    { label: t('user.pwdRuleDigit'), valid: /\d/.test(pwd) },
    { label: t('user.pwdRuleSpecial'), valid: /[^A-Za-z0-9]/.test(pwd) },
  ]
}

const passwordRules = computed(() => computePasswordRules(pwdForm.newPassword))

const satisfiedCount = computed(() => passwordRules.value.filter((r) => r.valid).length)

const strengthLevel = computed(() => {
  const n = satisfiedCount.value
  if (n >= 5) return 3
  if (n >= 3) return 2
  if (n >= 1) return 1
  return 0
})

const strengthText = computed(() => {
  const map = [t('user.pwdStrength0'), t('user.pwdStrength1'), t('user.pwdStrength2'), t('user.pwdStrength3')]
  return pwdForm.newPassword ? map[strengthLevel.value] : ''
})

const strengthTextClass = computed(() => {
  const map = [
    'text-[var(--wecom-error)]',
    'text-[var(--wecom-warning)]',
    'text-[var(--wecom-warning)]',
    'text-[var(--wecom-success)]',
  ]
  return map[strengthLevel.value]
})

function strengthBarClass(barIndex: number): string {
  const level = strengthLevel.value
  if (barIndex <= level) {
    const colors = [
      'bg-[var(--wecom-error)]',
      'bg-[var(--wecom-warning)]',
      'bg-[var(--wecom-warning)]',
      'bg-[var(--wecom-success)]',
    ]
    return colors[level]
  }
  return 'bg-[var(--wecom-border)]'
}

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
    // 改密成功后后端已拉黑当前 token：再调登出接口必然 401，
    // 直接同步清理本地状态（token/用户信息/动态路由），避免与 401 处理互相竞争
    userStore.resetState()
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
  height: var(--wecom-header-height);
  padding: 0 var(--wecom-spacing-xl);
  border-bottom: 1px solid var(--wecom-border);
  background: var(--wecom-bg-card);
}
.top-navbar__left {
  display: flex;
  align-items: center;
  margin-right: var(--wecom-spacing-xl);
}
.top-navbar__logo {
  width: 28px;
  height: 28px;
  margin-right: 10px;
}
.top-navbar__brand {
  font-weight: 600;
  font-size: 16px;
  color: var(--wecom-text-primary);
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
  padding: 6px 12px;
  border-radius: var(--wecom-radius-md);
  font-size: 14px;
  cursor: pointer;
  color: var(--wecom-text-secondary);
  text-decoration: none;
  transition: background var(--wecom-transition-fast), color var(--wecom-transition-fast);
}
.top-navbar__nav-item:hover {
  background: var(--wecom-bg-hover);
  color: var(--wecom-text-primary);
}
.top-navbar__nav-item--active,
.top-navbar__nav-item--active:hover {
  background: var(--wecom-primary-light);
  color: var(--wecom-primary);
  font-weight: 500;
}
.top-navbar__arrow {
  margin-left: 4px;
  font-size: 12px;
  transition: transform var(--wecom-transition-base);
}
.top-navbar__arrow--open {
  transform: rotate(180deg);
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
  border: 1px solid var(--wecom-border);
  border-radius: var(--wecom-radius-md);
  background: var(--wecom-bg-card);
  box-shadow: var(--wecom-shadow-md);
  z-index: 1000;
}
.top-navbar__dropdown-item {
  display: block;
  padding: 8px 12px;
  border-radius: var(--wecom-radius-sm);
  font-size: 14px;
  cursor: pointer;
  color: var(--wecom-text-primary);
  text-decoration: none;
  white-space: nowrap;
}
.top-navbar__dropdown-item:hover {
  background: var(--wecom-bg-hover);
}
.top-navbar__dropdown-item--active {
  color: var(--wecom-primary);
  background: var(--wecom-primary-light);
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
  border-radius: var(--wecom-radius-md);
}
.top-navbar__user:hover {
  background: var(--wecom-bg-hover);
}
.top-navbar__avatar {
  width: 28px; height: 28px;
  border-radius: var(--wecom-radius-md);
  background: var(--wecom-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}
.top-navbar__username {
  font-size: 14px;
  color: var(--wecom-text-primary);
}
</style>
