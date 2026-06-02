<template>
  <t-drawer v-model="visible" header="布局设置" :footer="false" style="width: 300px">
    <div class="settings-panel">
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
import { ref } from 'vue'
import { useSettingsStore } from '@/stores/settings'

const visible = defineModel<boolean>({ required: true })
const settings = useSettingsStore()
</script>

<style scoped>
.settings-panel {
  display: flex;
  flex-direction: column;
  gap: var(--wecom-spacing-lg);
}
.settings-panel__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  color: var(--wecom-text-primary);
}
</style>
