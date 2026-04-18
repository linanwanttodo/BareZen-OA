<script setup lang="ts">
withDefaults(defineProps<{ size?: 'sm' | 'md' | 'lg' }>(), { size: 'md' })
</script>

<template>
  <div
    :class="{
      'h-6 w-6 text-sm': size === 'sm',
      'h-8 w-8 text-base': size === 'md',
      'h-12 w-12 text-xl': size === 'lg',
    }"
    class="flex items-center justify-center rounded-md bg-[var(--primary)] font-bold text-white"
  >
    B
  </div>
</template>
