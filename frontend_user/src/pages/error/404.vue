<template>
  <div class="flex min-h-[60vh] flex-col items-center justify-center">
    <div class="error-code mb-6">404</div>
    <p class="mb-2 text-lg font-medium text-[var(--foreground)]">页面不存在</p>
    <p class="mb-8 text-sm text-[var(--muted-foreground)]">您访问的页面可能已被移动或删除</p>
    <div class="flex items-center gap-3">
      <Button @click="router.push('/')">
        <Home :size="16" class="mr-1.5" />
        返回首页
      </Button>
      <Button variant="outline" @click="router.back()">
        <Undo2 :size="16" class="mr-1.5" />
        返回上页
      </Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Home, Undo2 } from 'lucide-vue-next'
import Button from '@/components/ui/button/Button.vue'

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
