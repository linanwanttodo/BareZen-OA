<template>
  <div class="flex min-h-screen items-center justify-center bg-[var(--background)] px-4">
    <div class="w-full max-w-[400px]">
      <div class="rounded-xl border border-[var(--border)] bg-[var(--card)] p-8 shadow-sm">
        <router-view />
      </div>
      <div class="mt-4 text-center">
        <button class="text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)] transition-colors" @click="router.push('/')">
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
