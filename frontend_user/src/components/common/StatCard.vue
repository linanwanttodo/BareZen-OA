<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  label: string
  value: string | number
  icon?: Component
}>()
</script>

<template>
  <div class="flex items-center gap-3 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5 shadow-wecom-sm">
    <div v-if="icon" class="flex h-10 w-10 shrink-0 items-center justify-center rounded-wecom-sm bg-[var(--wecom-bg-container)]">
      <component :is="icon" class="h-5 w-5 text-[var(--wecom-text-tertiary)]" />
    </div>
    <div>
      <span class="text-sm text-[var(--wecom-text-tertiary)]">{{ label }}</span>
      <p class="text-2xl font-semibold text-[var(--wecom-text-primary)]">{{ value }}</p>
    </div>
  </div>
</template>
