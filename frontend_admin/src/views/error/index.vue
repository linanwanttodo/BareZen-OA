<template>
  <div class="error-page min-h-[500px] flex flex-col items-center justify-center py-16">
    <div class="error-code-wrapper mb-6">
      <span class="error-code">{{ code }}</span>
    </div>
    <p class="text-lg text-[var(--muted-foreground)] mb-2 font-medium">{{ message }}</p>
    <p class="text-sm text-[var(--muted-foreground)] mb-8">请检查地址是否正确，或返回首页</p>
    <div class="flex items-center gap-3">
      <Button variant="primary" @click="router.push('/')">
        <Home :size="16" class="mr-1.5" />
        返回首页
      </Button>
      <Button variant="outline" @click="router.back()">
        <ArrowLeft :size="16" class="mr-1.5" />
        返回上页
      </Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Home, ArrowLeft } from 'lucide-vue-next'
import Button from '@/components/ui/button/Button.vue'

defineProps<{
  code: number | string
  message: string
}>()

const router = useRouter()
</script>

<style scoped>
.error-code-wrapper {
  position: relative;
}

.error-code {
  font-size: 120px;
  font-weight: 800;
  line-height: 1;
  color: var(--primary);
  opacity: 0.15;
}

.error-code-wrapper::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 4px;
  border-radius: 2px;
  background: var(--primary);
}
</style>
