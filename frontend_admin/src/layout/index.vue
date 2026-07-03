<template>
  <div class="bz-layout flex flex-col">
    <!-- 顶部栏：两种形态都保留，导航形态开关在此处 -->
    <header>
      <TopNavbar />
    </header>

    <div class="bz-body flex flex-1 overflow-hidden">
      <!-- 侧边形态：菜单树在左栏；顶部形态下不存在此节点 -->
      <SideMenu v-if="settings.navMode === 'side'" />

      <div class="bz-main flex flex-1 flex-col overflow-hidden">
        <!-- 多标签页（可在设置面板关闭） -->
        <TagsView v-if="settings.showTags" />

        <!-- 主内容区：留白统一由 layout 提供，页面自身不再加 padding -->
        <main class="bz-content flex-1 overflow-y-auto">
          <Breadcrumb v-if="settings.showBreadcrumb" class="bz-breadcrumb" />

          <router-view v-slot="{ Component, route }">
            <transition name="fade" mode="out-in">
              <keep-alive :max="10">
                <component :is="Component" :key="route.path" />
              </keep-alive>
            </transition>
          </router-view>
        </main>
      </div>
    </div>

    <!-- 布局设置面板 -->
    <SettingsPanel v-model="settingsVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref, provide } from 'vue'
import TopNavbar from './components/TopNavbar.vue'
import SideMenu from './components/SideMenu.vue'
import TagsView from './components/TagsView.vue'
import Breadcrumb from './components/Breadcrumb.vue'
import SettingsPanel from './components/SettingsPanel.vue'
import { useSettingsStore } from '@/stores/settings'

const settings = useSettingsStore()
const settingsVisible = ref(false)

// 暴露给 TopNavbar 打开设置面板（通过 provide 避免 props 层层下钻）
provide('openSettingsPanel', () => {
  settingsVisible.value = true
})
</script>

<style scoped>
/* 确定高度：对齐 Starter 的 -wrapper { height: 100vh }。
   此前用 Tailwind h-full(height:100%)，但父级 #app 只有 min-height，
   百分比高度解析为 auto → 整篇文档滚动、导航栏被卷走。 */
.bz-layout {
  height: 100vh;
}
.bz-content {
  padding: var(--td-comp-paddingTB-xl) var(--td-comp-paddingLR-xl);
  background: var(--td-bg-color-page);
}
.bz-content::-webkit-scrollbar {
  width: 8px;
  background: transparent;
}
.bz-content::-webkit-scrollbar-thumb {
  border-radius: 6px;
  border: 2px solid transparent;
  background-clip: content-box;
  background-color: var(--td-scrollbar-color);
}
.bz-breadcrumb {
  margin-bottom: var(--td-comp-margin-s);
}
</style>
