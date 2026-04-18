<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

type Variant = 'default' | 'secondary' | 'destructive' | 'outline'

const props = withDefaults(defineProps<{ variant?: Variant }>(), { variant: 'default' })

const variants: Record<Variant, string> = {
  default: 'bg-[var(--primary)] text-[var(--primary-foreground)]',
  secondary: 'bg-[var(--secondary)] text-[var(--secondary-foreground)]',
  destructive: 'bg-[var(--destructive)] text-[var(--destructive-foreground)]',
  outline: 'border border-[var(--border)] text-[var(--foreground)]',
}

const cls = computed(() => cn('inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium', variants[props.variant]))
</script>

<template>
  <span :class="cls"><slot /></span>
</template>
