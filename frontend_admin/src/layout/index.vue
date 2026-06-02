<template>
  <div class="bz-layout h-full flex flex-col">
    <!-- 顶部导航 -->
    <header>
      <TopNavbar />
    </header>

    <!-- 多标签页（可在设置面板关闭） -->
    <TagsView v-if="settings.showTags" />

    <!-- 面包屑（可在设置面板关闭） -->
    <Breadcrumb v-if="settings.showBreadcrumb" />

    <!-- 主内容区 -->
    <main class="bz-content flex-1 overflow-y-auto">
      <router-view v-slot="{ Component, route }">
        <transition name="fade" mode="out-in">
          <keep-alive :max="10">
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </transition>
      </router-view>
    </main>

    <!-- 布局设置面板 -->
    <SettingsPanel v-model="settingsVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TopNavbar from './components/TopNavbar.vue'
import TagsView from './components/TagsView.vue'
import Breadcrumb from './components/Breadcrumb.vue'
import SettingsPanel from './components/SettingsPanel.vue'
import { useSettingsStore } from '@/stores/settings'

const settings = useSettingsStore()
const settingsVisible = ref(false)

// 暴露给 TopNavbar 打开设置面板（通过 provide 避免 props 层层下钻）
import { provide } from 'vue'
provide('openSettingsPanel', () => {
  settingsVisible.value = true
})
</script>

<style scoped>
.bz-content {
  padding: 0;
  background: var(--wecom-bg-page);
}
</style>
