<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { cn } from '@/lib/utils'

interface MenuItem {
  label?: string
  value?: string
  divider?: boolean
}

const props = defineProps<{ items: MenuItem[] }>()
const emit = defineEmits<{ select: [value: string] }>()

const open = ref(false)
const root = ref<HTMLElement | null>(null)

function choose(item: MenuItem) {
  if (item.divider) return
  open.value = false
  emit('select', item.value as string)
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
    <div @click="open = !open" class="cursor-pointer select-none">
      <slot />
    </div>
    <div
      v-if="open"
      class="absolute right-0 z-50 mt-1 min-w-[160px] rounded-md border border-[var(--border)] bg-[var(--card)] p-1"
    >
      <template v-for="(item, i) in items" :key="i">
        <div v-if="item.divider" class="my-1 h-px bg-[var(--border)]" />
        <button
          v-else
          type="button"
          @click="choose(item)"
          :class="cn('block w-full rounded px-3 py-2 text-left text-sm text-[var(--foreground)] transition-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--ring)]')"
        >
          {{ item.label }}
        </button>
      </template>
    </div>
  </div>
</template>
