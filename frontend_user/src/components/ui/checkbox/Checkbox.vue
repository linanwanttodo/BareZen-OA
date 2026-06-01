<script setup lang="ts">
import { Check } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

const props = defineProps<{ modelValue?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; change: [value: boolean] }>()

function toggle() {
  const next = !props.modelValue
  emit('update:modelValue', next)
  emit('change', next)
}
</script>

<template>
  <button
    type="button"
    role="checkbox"
    :aria-checked="modelValue"
    @click="toggle"
    :class="cn(
      'flex h-5 w-5 items-center justify-center rounded border',
      modelValue ? 'border-[var(--primary)] bg-[var(--primary)] text-white' : 'border-[var(--border)] bg-[var(--wecom-bg-card)]',
    )"
  >
    <Check v-if="modelValue" class="h-3.5 w-3.5" />
  </button>
</template>
