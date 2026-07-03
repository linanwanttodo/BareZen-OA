<template>
  <t-menu
    class="side-menu"
    :class="{ 'side-menu--dark': settings.darkNavbar }"
    :value="activePath"
    :theme="settings.darkNavbar ? 'dark' : 'light'"
    :collapsed="settings.sidebarCollapsed"
    :expanded="expandedKeys"
    :expand-mutex="false"
    :expand-type="settings.sidebarCollapsed ? 'popup' : 'normal'"
    @change="onSelect"
    @expand="onExpand"
  >
    <template v-for="item in visibleMenus" :key="item.id">
      <t-submenu
        v-if="item.children && item.children.length"
        :value="pathOf(item)"
        :title="label(item)"
      >
        <template #icon><component :is="resolveMenuIcon(item.icon)" /></template>
        <t-menu-item
          v-for="child in item.children"
          :key="child.id"
          :value="pathOf(child, item)"
        >
          <template #icon><component :is="resolveMenuIcon(child.icon)" /></template>
          {{ label(child) }}
        </t-menu-item>
      </t-submenu>

      <t-menu-item v-else :value="pathOf(item)">
        <template #icon><component :is="resolveMenuIcon(item.icon)" /></template>
        {{ label(item) }}
      </t-menu-item>
    </template>
  </t-menu>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSettingsStore } from '@/stores/settings'
import { useMenuNav } from '@/composables/useMenuNav'
import { resolveMenuIcon } from '@/utils/menuIcon'
import { resolveMenuPath } from '@/utils/menuPath'
import type { SysMenu } from '@/types/menu'

const router = useRouter()
const settings = useSettingsStore()
const { visibleMenus, label, activePath, activeRootValue } = useMenuNav()

const pathOf = (item: SysMenu, parent?: SysMenu) => resolveMenuPath(item, parent)

/** 分组展开态由组件自持：菜单树只有两级，展开态不需要持久化 */
const expandedKeys = ref<string[]>([])

// 跳转后自动展开目标所属分组，避免当前页在收起的分组里失去高亮
watch(
  activeRootValue,
  (root) => {
    if (root && !expandedKeys.value.includes(root)) expandedKeys.value = [...expandedKeys.value, root]
  },
  { immediate: true }
)

function onExpand(value: Array<string | number>) {
  expandedKeys.value = value.map(String)
}

/** t-menu 的 value 即解析后的路由 path，直接用于跳转 */
function onSelect(value: string | number) {
  const path = String(value)
  if (!path || path === router.currentRoute.value.path) return
  router.push(path)
}
</script>

<style scoped>
/* 对齐 Starter：展开 232px / 折叠 64px 固定宽，满高且自身滚动。
   菜单项多于视口时不得把页面顶高，否则折叠后下方会留一大片空白。 */
.side-menu {
  flex-shrink: 0;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: 1px solid var(--td-component-stroke);
}
.side-menu::-webkit-scrollbar {
  width: 6px;
  background: transparent;
}
.side-menu::-webkit-scrollbar-thumb {
  border-radius: 6px;
  border: 2px solid transparent;
  background-clip: content-box;
  background-color: var(--td-scrollbar-color);
}
</style>
