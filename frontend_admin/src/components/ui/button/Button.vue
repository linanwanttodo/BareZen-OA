<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

type Variant = 'primary' | 'default' | 'outline' | 'ghost' | 'destructive'
type Size = 'sm' | 'default' | 'lg' | 'icon'

const props = withDefaults(
  defineProps<{
    variant?: Variant
    size?: Size
    disabled?: boolean
    type?: 'button' | 'submit' | 'reset'
  }>(),
  { variant: 'primary', size: 'default', type: 'button' },
)

// Oracle Redwood 风格：实心色块、无阴影、无渐变、干净边框
const base =
  'inline-flex items-center justify-center gap-2 rounded font-medium select-none disabled:cursor-not-allowed disabled:opacity-40'

const variants: Record<Variant, string> = {
  primary: 'bg-[var(--primary)] text-white',
  default: 'bg-[var(--muted)] text-[var(--foreground)]',
  outline: 'border border-[var(--border)] text-[var(--foreground)] bg-transparent',
  ghost: 'text-[var(--foreground)] bg-transparent',
  destructive: 'bg-[var(--destructive)] text-white',
}

const sizes: Record<Size, string> = {
  sm: 'h-8 px-3 text-xs',
  default: 'h-9 px-4 text-sm',
  lg: 'h-10 px-6 text-sm',
  icon: 'h-9 w-9',
}

const cls = computed(() => cn(base, variants[props.variant], sizes[props.size]))
</script>

<template>
  <button :type="type" :disabled="disabled" :class="cls">
    <slot />
  </button>
</template>
