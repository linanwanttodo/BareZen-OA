<script setup lang="ts">
import { computed } from 'vue'

interface Option {
  label: string
  value: string | number
}

const props = defineProps<{ modelValue: string | number | ''; options: Option[]; placeholder?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string | number | '']; change: [value: string | number | ''] }>()

const value = computed({
  get: () => props.modelValue,
  set: (v) => {
    emit('update:modelValue', v)
    emit('change', v)
  },
})
</script>

<template>
  <select
    v-model="value"
    class="h-9 w-full rounded border border-[var(--border)] bg-white px-3 text-sm text-[var(--foreground)] focus:border-[var(--primary)]"
  >
    <option v-if="placeholder" value="">{{ placeholder }}</option>
    <option v-for="opt in options" :key="String(opt.value)" :value="opt.value">{{ opt.label }}</option>
  </select>
</template>
