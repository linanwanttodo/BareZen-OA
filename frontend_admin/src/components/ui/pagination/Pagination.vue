<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{ modelValue: number; total: number; pageSize: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: number]; change: [value: number] }>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

const pages = computed<number[]>(() => {
  const count = totalPages.value
  const current = props.modelValue
  const span = 2
  let start = Math.max(1, current - span)
  const end = Math.min(count, start + span * 2)
  start = Math.max(1, end - span * 2)
  const result: number[] = []
  for (let i = start; i <= end; i++) result.push(i)
  return result
})

function go(page: number) {
  if (page < 1 || page > totalPages.value || page === props.modelValue) return
  emit('update:modelValue', page)
  emit('change', page)
}
</script>

<template>
  <div v-if="totalPages > 1" class="flex items-center justify-center gap-1">
    <button
      type="button"
      :disabled="modelValue <= 1"
      @click="go(modelValue - 1)"
      :class="cn('h-8 min-w-8 rounded-md border border-[var(--border)] px-2 text-sm disabled:opacity-40', 'bg-[var(--background)] text-[var(--foreground)]')"
    >
      上一页
    </button>
    <button
      v-for="p in pages"
      :key="p"
      type="button"
      @click="go(p)"
      :class="cn(
        'h-8 min-w-8 rounded-md border px-2 text-sm',
        p === modelValue
          ? 'border-[var(--primary)] bg-[var(--primary)] text-[var(--primary-foreground)]'
          : 'border-[var(--border)] bg-[var(--background)] text-[var(--foreground)]',
      )"
    >
      {{ p }}
    </button>
    <button
      type="button"
      :disabled="modelValue >= totalPages"
      @click="go(modelValue + 1)"
      :class="cn('h-8 min-w-8 rounded-md border border-[var(--border)] px-2 text-sm disabled:opacity-40', 'bg-[var(--background)] text-[var(--foreground)]')"
    >
      下一页
    </button>
  </div>
</template>
