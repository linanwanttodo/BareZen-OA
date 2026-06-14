<template>
  <div class="flex min-h-screen items-center justify-center bg-[var(--background)] px-4">
    <div class="w-full max-w-[400px]">
      <div class="rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-8 shadow-wecom-sm">
        <router-view />
      </div>
      <div class="mt-4 text-center">
        <t-link theme="default" hover="color" size="small" @click="router.push('/')">
          <template #prefix-icon><arrow-left-icon size="16px" /></template>
          <span>返回工作台</span>
        </t-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeftIcon } from 'tdesign-icons-vue-next'
const router = useRouter()
</script>
