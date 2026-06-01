<script setup lang="ts">
import { cn } from '@/lib/utils'

interface Option {
  label: string
  value: string | number
}

const props = defineProps<{ modelValue: string | number | ''; options: Option[] }>()
const emit = defineEmits<{ 'update:modelValue': [value: string | number | '']; change: [value: string | number | ''] }>()

function select(value: string | number | '') {
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<template>
  <div class="flex flex-wrap gap-2">
    <button
      v-for="opt in options"
      :key="String(opt.value)"
      type="button"
      @click="select(opt.value)"
      :class="cn(
        'rounded border px-3 py-1.5 text-sm',
        modelValue === opt.value
          ? 'border-[var(--primary)] bg-[var(--primary)] text-[var(--primary-foreground)]'
          : 'border-[var(--border)] bg-[var(--wecom-bg-card)] text-[var(--foreground)]',
      )"
    >
      {{ opt.label }}
    </button>
  </div>
</template>
