<script setup lang="ts">
import { cn } from '@/lib/utils'

type Variant = 'default' | 'info' | 'warning' | 'destructive'

const props = withDefaults(defineProps<{ variant?: Variant }>(), { variant: 'default' })

const variants: Record<Variant, string> = {
  default: 'border-[var(--border)] bg-[var(--muted)] text-[var(--foreground)]',
  info: 'border-[var(--primary)] bg-[var(--muted)] text-[var(--foreground)]',
  warning: 'border-[var(--border)] bg-[var(--muted)] text-[var(--foreground)]',
  destructive: 'border-[var(--destructive)] bg-[var(--muted)] text-[var(--foreground)]',
}

const cls = cn('rounded-md border p-3 text-sm', variants[props.variant])
</script>

<template>
  <div :class="cls" role="alert">
    <slot />
  </div>
</template>
