<template>
  <div class="flex min-h-[60vh] flex-col items-center justify-center">
    <div class="error-code mb-6">404</div>
    <p class="mb-2 text-lg font-medium text-[var(--foreground)]">{{ $t('error.404') }}</p>
    <p class="mb-8 text-sm text-[var(--muted-foreground)]">您访问的页面可能已被移动或删除</p>
    <div class="flex items-center gap-3">
      <a-button type="primary" @click="router.push('/')">
        <Home :size="16" class="mr-1.5" />
        {{ $t('error.backHome') }}
      </a-button>
      <a-button @click="router.back()">
        <Undo2 :size="16" class="mr-1.5" />
        {{ $t('error.backPrev') }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Home, Undo2 } from 'lucide-vue-next'

const { t } = useI18n()
const router = useRouter()
</script>

<style scoped>
.error-code {
  font-size: 120px;
  font-weight: 800;
  line-height: 1;
  color: var(--primary);
  opacity: 0.15;
}
</style>
