<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

type Variant = 'default' | 'secondary' | 'destructive' | 'outline'
type Size = 'sm' | 'default'

const props = withDefaults(defineProps<{ variant?: Variant; size?: Size }>(), { variant: 'default', size: 'default' })

const variants: Record<Variant, string> = {
  default: 'bg-[var(--primary)] text-[var(--primary-foreground)]',
  secondary: 'bg-[var(--secondary)] text-[var(--secondary-foreground)]',
  destructive: 'bg-[var(--destructive)] text-[var(--destructive-foreground)]',
  outline: 'border border-[var(--border)] text-[var(--foreground)]',
}

const sizes: Record<Size, string> = {
  sm: 'px-1.5 py-0 text-[10px] leading-4',
  default: 'px-2 py-0.5 text-xs',
}

const cls = computed(() => cn('inline-flex items-center rounded-md font-medium', sizes[props.size], variants[props.variant]))
</script>

<template>
  <span :class="cls"><slot /></span>
</template>
