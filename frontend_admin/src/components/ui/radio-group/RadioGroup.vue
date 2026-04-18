<script setup lang="ts" generic="T extends string | number">
import { cn } from '@/lib/utils'

interface Option<T> {
  label?: string
  value: T
  icon?: any
}

const props = defineProps<{
  modelValue?: T
  options: Option<T>[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: T]
  change: [value: T]
}>()

function select(value: T) {
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<template>
  <div class="inline-flex h-8 items-stretch rounded-md bg-[var(--muted)] p-0.5">
    <button
      v-for="(opt, i) in options"
      :key="String(opt.value)"
      type="button"
      @click="select(opt.value)"
      :class="cn(
        'inline-flex items-center justify-center rounded px-3 text-sm',
        i > 0 ? 'border-l border-[var(--border)]' : '',
        opt.value === modelValue
          ? 'bg-[var(--card)] font-medium text-[var(--primary)]'
          : 'bg-transparent text-[var(--muted-foreground)]',
      )"
    >
      <component v-if="opt.icon" :is="opt.icon" :size="16" class="shrink-0" />
      <span v-if="opt.label">{{ opt.label }}</span>
    </button>
  </div>
</template>
