<template>
  <div class="mb-8 flex items-end justify-between gap-4">
    <div>
      <h1 class="text-2xl font-bold text-[var(--foreground)]">{{ title }}</h1>
      <p v-if="description" class="mt-1 text-sm text-[var(--muted-foreground)]">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="shrink-0">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; description?: string }>()
</script>
