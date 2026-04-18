<script setup lang="ts" generic="T extends string | number">
import { computed, ref, watch, onUnmounted } from 'vue'
import { cn } from '@/lib/utils'

interface Option<T> {
  label: string
  value: T
}

const props = defineProps<{
  modelValue?: T
  options: Option<T>[]
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: T | undefined]
  change: [value: T | undefined]
}>()

const open = ref(false)
const root = ref<HTMLElement | null>(null)

const selectedLabel = computed(() => {
  const found = props.options.find((o) => o.value === props.modelValue)
  return found ? found.label : (props.placeholder ?? '')
})

const isPlaceholder = computed(() => !props.options.some((o) => o.value === props.modelValue))

function choose(value: T) {
  open.value = false
  emit('update:modelValue', value)
  emit('change', value)
}

function onDocClick(e: MouseEvent) {
  if (root.value && !root.value.contains(e.target as Node)) open.value = false
}

watch(open, (v) => {
  if (v) document.addEventListener('click', onDocClick)
  else document.removeEventListener('click', onDocClick)
})
onUnmounted(() => document.removeEventListener('click', onDocClick))
</script>

<template>
  <div ref="root" class="relative">
    <button
      type="button"
      @click="open = !open"
      :class="cn(
        'flex h-9 w-full items-center justify-between rounded border border-[var(--border)] bg-white px-3 text-sm focus:border-[var(--primary)]',
        isPlaceholder ? 'text-[var(--muted-foreground)]' : 'text-[var(--foreground)]',
      )"
    >
      <span class="truncate">{{ selectedLabel }}</span>
      <svg
        class="ml-2 h-4 w-4 shrink-0 text-[var(--muted-foreground)]"
        :class="open ? 'rotate-180' : ''"
        viewBox="0 0 20 20"
        fill="none"
        stroke="currentColor"
        stroke-width="1.5"
      >
        <path d="M6 8l4 4 4-4" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    </button>

    <div
      v-if="open"
      class="absolute left-0 z-50 mt-1 max-h-60 w-full overflow-y-auto rounded-md border border-[var(--border)] bg-[var(--card)] p-1"
    >
      <button
        v-for="opt in options"
        :key="String(opt.value)"
        type="button"
        @click="choose(opt.value)"
        :class="cn(
          'flex w-full items-center justify-between rounded px-3 py-2 text-left text-sm',
          opt.value === modelValue
            ? 'bg-[var(--muted)] font-medium text-[var(--primary)]'
            : 'text-[var(--foreground)]',
        )"
      >
        <span class="truncate">{{ opt.label }}</span>
        <svg
          v-if="opt.value === modelValue"
          class="ml-2 h-4 w-4 shrink-0 text-[var(--primary)]"
          viewBox="0 0 20 20"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="M5 10l3 3 7-7" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
    </div>
  </div>
</template>
