import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

/** 导航形态：top=顶部横向导航（腾讯云控制台式），side=左侧边栏导航 */
export type NavMode = 'top' | 'side'

/**
 * 布局设置（对齐 TDesign Starter 的设置面板）
 *
 * 状态持久化到 localStorage，刷新后保持。
 */
const STORAGE_KEY = 'bz_layout_settings'

interface LayoutSettings {
  /** 显示面包屑 */
  showBreadcrumb: boolean
  /** 显示多标签页 */
  showTags: boolean
  /** 导航栏深色主题 */
  darkNavbar: boolean
  /** 导航形态 */
  navMode: NavMode
  /** 侧边形态下是否折叠成图标条 */
  sidebarCollapsed: boolean
}

const DEFAULTS: LayoutSettings = {
  showBreadcrumb: true,
  showTags: true,
  darkNavbar: false,
  // 对齐 TDesign Starter：默认侧边导航，顶部横向形态仍可一键切换
  navMode: 'side',
  sidebarCollapsed: false,
}

function loadSettings(): LayoutSettings {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<LayoutSettings>
      // navMode 来自持久化数据，必须校验：脏值会同时绕过两种形态的 v-if
      return {
        ...DEFAULTS,
        ...parsed,
        navMode: parsed.navMode === 'side' || parsed.navMode === 'top' ? parsed.navMode : DEFAULTS.navMode,
      }
    }
  } catch {
    // 损坏数据按默认处理
  }
  return DEFAULTS
}

export const useSettingsStore = defineStore('settings', () => {
  const initial = loadSettings()
  const showBreadcrumb = ref(initial.showBreadcrumb)
  const showTags = ref(initial.showTags)
  const darkNavbar = ref(initial.darkNavbar)
  const navMode = ref<NavMode>(initial.navMode)
  const sidebarCollapsed = ref(initial.sidebarCollapsed)

  watch(
    [showBreadcrumb, showTags, darkNavbar, navMode, sidebarCollapsed],
    () => {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          showBreadcrumb: showBreadcrumb.value,
          showTags: showTags.value,
          darkNavbar: darkNavbar.value,
          navMode: navMode.value,
          sidebarCollapsed: sidebarCollapsed.value,
        })
      )
    },
    { deep: true }
  )

  /** 在顶部/侧边两种形态间切换 */
  function toggleNavMode() {
    navMode.value = navMode.value === 'top' ? 'side' : 'top'
  }

  return {
    showBreadcrumb,
    showTags,
    darkNavbar,
    navMode,
    sidebarCollapsed,
    toggleNavMode,
  }
})
