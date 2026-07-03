<template>
  <t-dropdown trigger="click" @select="handleSelect">
    <div class="lang-switch">
      <EarthIcon size="16px" />
      <span class="lang-switch__label">{{ currentLangLabel }}</span>
    </div>
    <template #content>
      <t-dropdown-menu>
        <t-dropdown-item value="zh-CN">简体中文</t-dropdown-item>
        <t-dropdown-item value="en-US">English</t-dropdown-item>
      </t-dropdown-menu>
    </template>
  </t-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { EarthIcon } from 'tdesign-icons-vue-next'

const { locale } = useI18n()

const currentLangLabel = computed(() => {
  return locale.value === 'zh-CN' ? '简体中文' : 'English'
})

function handleSelect(value: string | number | Record<string, any> | undefined) {
  if (typeof value === 'string') {
    locale.value = value
    localStorage.setItem('lang', value)
  }
}
</script>

<style scoped>
.lang-switch {
  display: flex;
  align-items: center;
  gap: var(--td-size-2);
  padding: var(--td-size-2) var(--td-size-4);
  cursor: pointer;
  border-radius: var(--td-radius-default);
  color: var(--td-text-color-secondary);
  transition: background var(--wecom-transition-fast), color var(--wecom-transition-fast);
}
.lang-switch:hover {
  background: var(--td-bg-color-secondarycontainer-hover);
  color: var(--td-text-color-primary);
}
.lang-switch__label {
  font-size: var(--td-font-size-body-medium);
}
</style>
