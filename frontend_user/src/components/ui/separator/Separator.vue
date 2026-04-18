<script setup lang="ts">
withDefaults(defineProps<{ orientation?: 'horizontal' | 'vertical' }>(), { orientation: 'horizontal' })
</script>

<template>
  <div
    v-if="orientation === 'horizontal'"
    class="h-px w-full bg-[var(--border)]"
    role="separator"
  />
  <div v-else class="h-full w-px bg-[var(--border)]" role="separator" />
</template>
