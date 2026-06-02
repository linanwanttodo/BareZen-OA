import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

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
  /** 侧边栏/顶部导航深色模式 */
  darkNavbar: boolean
}

function loadSettings(): LayoutSettings {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      return { showBreadcrumb: true, showTags: true, darkNavbar: false, ...JSON.parse(raw) }
    }
  } catch {
    // 损坏数据按默认处理
  }
  return { showBreadcrumb: true, showTags: true, darkNavbar: false }
}

export const useSettingsStore = defineStore('settings', () => {
  const initial = loadSettings()
  const showBreadcrumb = ref(initial.showBreadcrumb)
  const showTags = ref(initial.showTags)
  const darkNavbar = ref(initial.darkNavbar)

  watch(
    [showBreadcrumb, showTags, darkNavbar],
    () => {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ showBreadcrumb: showBreadcrumb.value, showTags: showTags.value, darkNavbar: darkNavbar.value })
      )
    },
    { deep: true }
  )

  return { showBreadcrumb, showTags, darkNavbar }
})
