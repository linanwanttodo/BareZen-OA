<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  label: string
  value: string | number
  icon?: Component
}>()
</script>

<template>
  <div class="flex items-center gap-3 rounded-lg border border-[var(--border)] bg-[var(--card)] p-5">
    <div v-if="icon" class="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-[var(--muted)]">
      <component :is="icon" class="h-5 w-5 text-[var(--foreground)]" />
    </div>
    <div>
      <span class="text-sm text-[var(--muted-foreground)]">{{ label }}</span>
      <p class="text-2xl font-bold text-[var(--foreground)]">{{ value }}</p>
    </div>
  </div>
</template>
