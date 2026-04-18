<script setup lang="ts">
import { computed, watch, onUnmounted } from 'vue'
import { X } from 'lucide-vue-next'

type Size = 'sm' | 'default' | 'lg' | 'xl'

const props = withDefaults(
  defineProps<{ open: boolean; title?: string; size?: Size }>(),
  { size: 'default' },
)
const emit = defineEmits<{ 'update:open': [value: boolean] }>()

const maxWidth = computed(() => {
  const map: Record<Size, string> = {
    sm: 'max-w-md',
    default: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
  }
  return map[props.size]
})

function close() {
  emit('update:open', false)
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') close()
}

watch(
  () => props.open,
  (v) => {
    if (v) window.addEventListener('keydown', onKey)
    else window.removeEventListener('keydown', onKey)
  },
)

onUnmounted(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/40" @click="close" />
      <div
        role="dialog"
        aria-modal="true"
        :class="[
          'relative z-10 max-h-[calc(100vh-4rem)] w-full overflow-y-auto rounded-lg border border-[var(--border)] bg-[var(--card)] p-6',
          maxWidth,
        ]"
      >
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-lg font-semibold text-[var(--foreground)]">{{ title }}</h3>
          <button
            type="button"
            class="text-[var(--muted-foreground)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--ring)]"
            aria-label="关闭"
            @click="close"
          >
            <X class="h-5 w-5" />
          </button>
        </div>
        <div class="text-sm text-[var(--foreground)]">
          <slot />
        </div>
        <div v-if="$slots.footer" class="mt-6 flex justify-end gap-2">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </Teleport>
</template>
