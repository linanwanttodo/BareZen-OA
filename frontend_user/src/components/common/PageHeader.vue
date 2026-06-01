<template>
  <div class="mb-6 flex items-end justify-between gap-4">
    <div>
      <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ title }}</h1>
      <p v-if="description" class="mt-1 text-sm text-[var(--wecom-text-tertiary)]">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="shrink-0">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; description?: string }>()
</script>
