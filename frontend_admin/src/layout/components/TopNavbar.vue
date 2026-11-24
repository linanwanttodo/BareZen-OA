<template>
  <div class="top-navbar" :class="{ 'top-navbar--dark': settings.darkNavbar }">
    <!-- 左侧：Logo + 品牌名 + 侧边栏折叠 + 布局设置入口 -->
    <div class="top-navbar__left">
      <img src="/logo.png" alt="BareZen" class="top-navbar__logo" />
      <span class="top-navbar__brand">BareZen</span>

      <t-tooltip v-if="settings.navMode === 'side'" :content="settings.sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
        <t-button
          variant="text"
          shape="square"
          size="small"
          class="top-navbar__collapse"
          @click="settings.sidebarCollapsed = !settings.sidebarCollapsed"
        >
          <template #icon><view-list-icon /></template>
        </t-button>
      </t-tooltip>

      <t-tooltip content="布局设置">
        <t-button variant="text" shape="square" size="small" class="top-navbar__settings" @click="openSettings?.()">
          <template #icon><icon-apps /></template>
        </t-button>
      </t-tooltip>
    </div>

    <!-- 中间：顶部形态下渲染横向导航；侧边形态下菜单在左栏 -->
    <nav v-if="settings.navMode === 'top'" class="top-navbar__nav">
      <template v-for="item in visibleMenus" :key="item.id">
        <!-- 有子菜单 → 下拉 -->
        <t-dropdown
          v-if="item.children && item.children.length > 0"
          :options="dropdownOptions(item)"
          trigger="hover"
          @click="onDropdownClick($event, item)"
        >
          <a class="top-navbar__nav-item" :class="{ 'top-navbar__nav-item--active': isActive(item) }">
            {{ label(item) }}
            <icon-down class="top-navbar__arrow" />
          </a>
        </t-dropdown>

        <!-- 无子菜单 → 直接链接 -->
        <a
          v-else
          class="top-navbar__nav-item"
          :class="{ 'top-navbar__nav-item--active': isActive(item) }"
          @click="navigate(item)"
        >
          {{ label(item) }}
        </a>
      </template>
    </nav>

    <div v-else class="top-navbar__crumb">
      <span class="top-navbar__crumb-title">{{ currentSectionTitle }}</span>
    </div>

    <!-- 右侧：导航形态开关 + 主题切换 + 语言切换 + 用户信息 -->
    <div class="top-navbar__right">
      <t-tooltip :content="settings.navMode === 'top' ? '切换为侧边导航' : '切换为顶部导航'">
        <t-button
          variant="text"
          shape="square"
          size="small"
          class="top-navbar__mode"
          @click="settings.toggleNavMode()"
        >
          <template #icon>
            <menu-icon v-if="settings.navMode === 'top'" />
            <tab-icon v-else />
          </template>
        </t-button>
      </t-tooltip>
      <ThemeToggle />
      <LangSwitch />
      <t-dropdown :options="userMenuOptions" trigger="hover" @click="onUserMenuClick">
        <div class="top-navbar__user">
          <div class="top-navbar__avatar">{{ userInitial }}</div>
          <span class="top-navbar__username">{{ nickName }}</span>
        </div>
      </t-dropdown>
    </div>

    <!-- 修改密码弹窗 -->
    <t-dialog
      v-model:visible="pwdDialogVisible"
      :header="$t('profile.changePasswordTitle')"
      :width="480"
      :close-on-overlay-click="false"
      :confirm-btn="{ content: $t('common.confirm'), loading: pwdSubmitting }"
      @confirm="onPwdSubmit"
      @close="pwdDialogVisible = false"
    >
      <t-form :data="pwdForm" label-align="top">
        <t-form-item labelWidth="auto" :label="$t('profile.oldPassword')" name="oldPassword">
          <t-input type="password" v-model="pwdForm.oldPassword" :placeholder="$t('profile.oldPasswordPlaceholder')" clearable />
        </t-form-item>
        <t-form-item :label="$t('profile.newPassword')" name="newPassword">
          <t-input type="password" v-model="pwdForm.newPassword" :placeholder="$t('profile.newPasswordPlaceholder')" clearable />
        </t-form-item>
        <PasswordStrength :value="pwdForm.newPassword" show-bars />
        <t-form-item :label="$t('profile.confirmPassword')" name="confirmPassword">
          <t-input type="password" v-model="pwdForm.confirmPassword" :placeholder="$t('profile.confirmPasswordPlaceholder')" clearable />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import { MenuIcon, TabIcon, ViewListIcon } from 'tdesign-icons-vue-next'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { useMenuNav } from '@/composables/useMenuNav'
import { changePasswordApi } from '@/api/auth'
import LangSwitch from '@/components/common/LangSwitch.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import PasswordStrength from '@/components/common/PasswordStrength.vue'
import { isPasswordPolicyValid } from '@/utils/passwordRules'
import type { SysMenu } from '@/types/menu'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const settings = useSettingsStore()
const { visibleMenus, label, isActive, navigate } = useMenuNav()
const openSettings = inject<(() => void) | undefined>('openSettingsPanel', undefined)

const nickName = computed(() => userStore.userInfo?.nickName || t('user.adminUser'))
const userInitial = computed(() => nickName.value.charAt(0).toUpperCase())

/** 侧边形态下顶部栏只显示当前所属模块名，避免与左栏重复 */
const currentSectionTitle = computed(() => {
  const hit = visibleMenus.value.find((item) => isActive(item))
  return hit ? label(hit) : ''
})

/** 菜单项 → TDesign Dropdown options */
function dropdownOptions(item: SysMenu) {
  return (item.children || []).map((child) => ({
    content: label(child),
    value: child.id as string,
  }))
}

function onDropdownClick(data: { value: string | number }, parent: SysMenu) {
  const child = (parent.children || []).find((c) => String(c.id) === String(data.value))
  if (child) navigate(child, parent)
}

const userMenuOptions = [
  { content: t('route.profile'), value: 'profile' },
  { content: t('profile.changePwd'), value: 'pwd' },
  { content: t('login.logout'), value: 'logout', theme: 'error' as const },
]

function onUserMenuClick(data: { value: string | number }) {
  switch (data.value) {
    case 'profile':
      router.push('/profile')
      break
    case 'pwd':
      pwdDialogVisible.value = true
      break
    case 'logout':
      onLogout()
      break
  }
}

async function onLogout() {
  await userStore.logout()
  await router.push('/login')
  MessagePlugin.success(t('login.logoutSuccess'))
}

// ---- 修改密码 ----
const pwdDialogVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})


async function onPwdSubmit() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
    MessagePlugin.warning(t('login.passwordInfoIncomplete'))
    return
  }
  // 与后端 PasswordUtil.validatePasswordPolicy 完全一致的五项校验
  if (!isPasswordPolicyValid(pwdForm.newPassword)) {
    MessagePlugin.warning(t('user.batchPwdError', { name: '' }))
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    MessagePlugin.warning(t('login.passwordMismatch'))
    return
  }

  pwdSubmitting.value = true
  try {
    await changePasswordApi({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    MessagePlugin.success(t('login.passwordChangeSuccess'))
    pwdDialogVisible.value = false
    // 改密成功后后端已拉黑当前 token：再调登出接口必然 401，
    // 直接同步清理本地状态（token/用户信息/动态路由），避免与 401 处理互相竞争
    userStore.resetState()
    router.push('/login')
  } catch (e) {
    const msg = (e as Error)?.message || '密码修改失败'
    MessagePlugin.error(msg)
  } finally {
    pwdSubmitting.value = false
  }
}
</script>

<style scoped>
.top-navbar {
  display: flex;
  align-items: center;
  height: var(--td-size-14);
  padding: 0 var(--td-size-8);
  border-bottom: 1px solid var(--td-component-stroke);
  background: var(--td-bg-color-container);
}
.top-navbar--dark {
  background: var(--bz-navbar-dark);
  border-bottom-color: var(--bz-navbar-dark);
}
.top-navbar--dark .top-navbar__brand,
.top-navbar--dark .top-navbar__username,
.top-navbar--dark .top-navbar__nav-item,
.top-navbar--dark .top-navbar__crumb-title,
.top-navbar--dark .top-navbar__collapse,
.top-navbar--dark .top-navbar__settings,
.top-navbar--dark .top-navbar__mode {
  color: var(--td-text-color-anti);
}
.top-navbar--dark .top-navbar__nav-item:hover {
  background: var(--td-bg-color-container-select);
}
.top-navbar--dark :deep(.t-button__text),
.top-navbar--dark :deep(.t-design-icon) {
  color: var(--td-text-color-anti);
}
.top-navbar__left {
  display: flex;
  align-items: center;
  margin-right: var(--td-size-8);
}
.top-navbar__logo {
  width: var(--td-size-9);
  height: var(--td-size-9);
  margin-right: 10px;
}
.top-navbar__brand {
  font-weight: 600;
  font-size: var(--td-font-size-body-large);
  color: var(--td-text-color-primary);
}
.top-navbar__collapse,
.top-navbar__settings {
  margin-left: var(--td-size-4);
  color: var(--td-text-color-placeholder);
}
.top-navbar__nav {
  display: flex;
  align-items: center;
  flex: 1;
  gap: var(--td-size-1);
  /* 导航区可收缩，超出时横向滚动，避免挤压右侧工具栏 */
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}
.top-navbar__nav::-webkit-scrollbar {
  display: none;
}
.top-navbar__crumb {
  flex: 1;
}
.top-navbar__crumb-title {
  font-size: var(--td-font-size-body-large);
  font-weight: 500;
  color: var(--td-text-color-primary);
}
.top-navbar__nav-item {
  display: inline-flex;
  align-items: center;
  /* 中文菜单名不得从词中间折行（曾因 flex 收缩出现「仪表/盘」断行） */
  white-space: nowrap;
  flex-shrink: 0;
  padding: var(--td-size-3) var(--td-size-5);
  border-radius: var(--td-radius-default);
  font-size: var(--td-font-size-body-medium);
  cursor: pointer;
  color: var(--td-text-color-secondary);
  text-decoration: none;
  transition: background var(--wecom-transition-fast), color var(--wecom-transition-fast);
}
.top-navbar__nav-item:hover {
  background: var(--td-bg-color-container-hover);
  color: var(--td-text-color-primary);
}
.top-navbar__nav-item--active,
.top-navbar__nav-item--active:hover {
  background: var(--td-brand-color-light);
  color: var(--td-brand-color);
  font-weight: 500;
}
.top-navbar__arrow {
  margin-left: var(--td-size-2);
  font-size: var(--td-font-size-body-small);
  transition: transform var(--wecom-transition-base);
}
.top-navbar__right {
  display: flex;
  align-items: center;
  margin-left: auto;
}
.top-navbar__mode {
  color: var(--td-text-color-placeholder);
}
.top-navbar__user {
  display: flex;
  align-items: center;
  gap: var(--td-size-4);
  cursor: pointer;
  padding: var(--td-size-2) var(--td-size-4);
  border-radius: var(--td-radius-default);
}
.top-navbar__user:hover {
  background: var(--td-bg-color-container-hover);
}
.top-navbar__avatar {
  width: var(--td-size-9); height: var(--td-size-9);
  border-radius: var(--td-radius-default);
  background: var(--td-brand-color);
  color: var(--td-text-color-anti);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--td-font-size-body-medium);
  font-weight: 600;
}
.top-navbar__username {
  font-size: var(--td-font-size-body-medium);
  color: var(--td-text-color-primary);
}
</style>
