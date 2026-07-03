<template>
  <t-drawer v-model:visible="visible" header="布局设置" :footer="false" size="300px">
    <div class="settings-panel">
      <div class="settings-panel__item settings-panel__item--stack">
        <span>导航栏模式</span>
        <t-radio-group v-model="settings.navMode" variant="default-filled" size="small">
          <t-radio-button value="top">顶部</t-radio-button>
          <t-radio-button value="side">侧边</t-radio-button>
        </t-radio-group>
      </div>
      <div v-if="settings.navMode === 'side'" class="settings-panel__item">
        <span>侧边栏收起</span>
        <t-switch v-model="settings.sidebarCollapsed" />
      </div>
      <div class="settings-panel__item">
        <span>显示面包屑</span>
        <t-switch v-model="settings.showBreadcrumb" />
      </div>
      <div class="settings-panel__item">
        <span>显示多标签页</span>
        <t-switch v-model="settings.showTags" />
      </div>
      <div class="settings-panel__item">
        <span>深色导航栏</span>
        <t-switch v-model="settings.darkNavbar" />
      </div>
    </div>
  </t-drawer>
</template>

<script setup lang="ts">
import { useSettingsStore } from '@/stores/settings'

const visible = defineModel<boolean>({ required: true })
const settings = useSettingsStore()
</script>

<style scoped>
.settings-panel {
  display: flex;
  flex-direction: column;
  gap: var(--td-size-6);
}
.settings-panel__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--td-font-size-body-medium);
  color: var(--td-text-color-primary);
}
.settings-panel__item--stack {
  flex-direction: column;
  align-items: flex-start;
  gap: var(--td-size-4);
}
</style>
