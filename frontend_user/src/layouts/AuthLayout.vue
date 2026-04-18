<template>
  <div class="flex min-h-screen items-center justify-center bg-[var(--background)]">
    <div class="w-full max-w-md px-6">
      <div class="mb-10 text-center">
        <div class="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-md bg-[var(--primary)]">
          <AppLogo size="lg" />
        </div>
        <h1 class="text-2xl font-bold text-[var(--foreground)]">BareZen</h1>
        <p class="mt-2 text-sm text-[var(--muted-foreground)]">企业级数字化底座</p>
      </div>
      <div class="rounded-lg border border-[var(--border)] bg-[var(--card)] p-8">
        <router-view />
      </div>
      <div class="mt-6 text-center">
        <Button variant="ghost" size="sm" @click="router.push('/')">返回工作台</Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import AppLogo from '@/components/common/AppLogo.vue'
import Button from '@/components/ui/button/Button.vue'

const router = useRouter()
</script>
