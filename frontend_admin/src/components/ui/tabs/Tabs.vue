<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

interface Tab {
  label: string
  value: string | number
}

const props = defineProps<{ modelValue: string | number; tabs: Tab[] }>()
const emit = defineEmits<{ 'update:modelValue': [value: string | number]; change: [value: string | number] }>()

const value = computed({
  get: () => props.modelValue,
  set: (v) => {
    emit('update:modelValue', v)
    emit('change', v)
  },
})
</script>

<template>
  <div class="flex gap-1 border-b border-[var(--border)]">
    <button
      v-for="tab in tabs"
      :key="String(tab.value)"
      type="button"
      @click="value = tab.value"
      :class="cn(
        'border-b-2 px-4 py-2 text-sm',
        modelValue === tab.value
          ? 'border-[var(--primary)] text-[var(--foreground)] font-medium'
          : 'border-transparent text-[var(--muted-foreground)]',
      )"
    >
      {{ tab.label }}
    </button>
  </div>
</template>
