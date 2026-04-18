<script setup lang="ts">
import { Inbox } from 'lucide-vue-next'
defineProps<{ description?: string }>()
</script>

<template>
  <div class="flex flex-col items-center justify-center gap-2 py-10 text-center">
    <Inbox class="h-8 w-8 text-[var(--muted-foreground)]" />
    <p class="text-sm text-[var(--muted-foreground)]">{{ description || '暂无数据' }}</p>
  </div>
</template>
