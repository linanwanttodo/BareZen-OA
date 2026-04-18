<script setup lang="ts">
import { cn } from '@/lib/utils'

const props = defineProps<{ modelValue?: boolean; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; change: [value: boolean] }>()

function toggle() {
  if (props.disabled) return
  const next = !props.modelValue
  emit('update:modelValue', next)
  emit('change', next)
}
</script>

<template>
  <button
    type="button"
    role="switch"
    :aria-checked="modelValue"
    :disabled="disabled"
    @click="toggle"
    :class="cn(
      'relative inline-flex h-5 w-9 shrink-0 items-center rounded-full border disabled:opacity-50',
      modelValue ? 'border-[var(--primary)] bg-[var(--primary)]' : 'border-[var(--border)] bg-[var(--muted)]',
    )"
  >
    <span
      :class="cn(
        'inline-block h-3.5 w-3.5 rounded-full bg-white transition-none',
        modelValue ? 'translate-x-4' : 'translate-x-0.5',
      )"
    />
  </button>
</template>
