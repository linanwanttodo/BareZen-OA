<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

type Size = 'sm' | 'default' | 'lg'

const props = withDefaults(defineProps<{ src?: string; fallback?: string; size?: Size }>(), {
  fallback: '',
  size: 'default',
})

const sizeMap: Record<Size, string> = {
  sm: 'h-8 w-8 text-xs',
  default: 'h-9 w-9 text-sm',
  lg: 'h-14 w-14 text-lg',
}

const cls = computed(() => cn('relative flex shrink-0 items-center justify-center overflow-hidden rounded-full border border-[var(--border)] bg-[var(--muted)] font-medium text-[var(--foreground)]', sizeMap[props.size]))
</script>

<template>
  <div :class="cls">
    <img v-if="src" :src="src" class="h-full w-full object-cover" alt="" />
    <span v-else>{{ fallback }}</span>
  </div>
</template>
