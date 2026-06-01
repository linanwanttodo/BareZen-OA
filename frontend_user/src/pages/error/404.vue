<template>
  <div class="flex min-h-[60vh] flex-col items-center justify-center">
    <p class="text-sm font-medium text-[var(--wecom-text-tertiary)]">404</p>
    <p class="mt-3 text-base font-medium text-[var(--wecom-text-primary)]">{{ $t('error.404') }}</p>
    <p class="mb-8 mt-1 text-sm text-[var(--wecom-text-tertiary)]">您访问的页面可能已被移动或删除</p>
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
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const router = useRouter()
</script>
