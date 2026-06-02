<template>
  <t-dropdown trigger="click" @select="handleSelect">
    <div class="lang-switch">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"/>
        <line x1="2" y1="12" x2="22" y2="12"/>
        <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
      </svg>
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
  gap: 4px;
  padding: 4px 8px;
  cursor: pointer;
  border-radius: var(--wecom-radius-md);
  color: var(--wecom-text-secondary);
  transition: background var(--wecom-transition-fast), color var(--wecom-transition-fast);
}
.lang-switch:hover {
  background: var(--wecom-bg-hover);
  color: var(--wecom-text-primary);
}
.lang-switch__label {
  font-size: 13px;
}
</style>
