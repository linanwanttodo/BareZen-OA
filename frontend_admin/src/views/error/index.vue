<template>
  <div class="error-page flex min-h-[500px] flex-col items-center justify-center py-16">
    <p class="text-sm font-medium text-[var(--wecom-text-tertiary)]">{{ code }}</p>
    <p class="mt-3 text-base font-medium text-[var(--wecom-text-primary)]">{{ message }}</p>
    <p class="mb-8 mt-1 text-sm text-[var(--wecom-text-tertiary)]">{{ $t('error.checkUrl') }}</p>
    <div class="flex items-center gap-3">
      <a-button type="primary" @click="router.push('/')">
        <icon-home :size="16" class="mr-1.5" />
        {{ $t('error.backHome') }}
      </a-button>
      <a-button @click="router.back()">
        <icon-left :size="16" class="mr-1.5" />
        {{ $t('error.backPrev') }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

defineProps<{
  code: number | string
  message: string
}>()

const router = useRouter()
</script>
