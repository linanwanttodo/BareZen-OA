<template>
  <div class="flex min-h-screen items-center justify-center bg-[var(--background)] px-4">
    <div class="w-full max-w-[400px]">
      <div class="rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-8 shadow-wecom-sm">
        <router-view />
      </div>
      <div class="mt-4 text-center">
        <button class="text-xs text-[var(--wecom-text-tertiary)] hover:text-[var(--wecom-text-primary)] transition-colors" @click="router.push('/')">
          ← 返回工作台
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>
