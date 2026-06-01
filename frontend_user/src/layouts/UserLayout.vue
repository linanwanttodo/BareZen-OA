<template>
  <div class="user-layout">
    <!-- 顶部导航 -->
    <header class="user-header">
      <div class="user-header__left">
        <img src="/logo.png" alt="BareZen" class="user-header__logo" />
        <span class="user-header__brand">BareZen</span>
      </div>
      <a-button size="small" @click="router.push('/')">返回工作台</a-button>
    </header>

    <!-- 主内容 -->
    <main class="user-main">
      <div class="mx-auto max-w-4xl">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--wecom-bg-page);
}

.user-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--wecom-header-height);
  padding: 0 32px;
  background: var(--wecom-bg-card);
  border-bottom: 1px solid var(--wecom-border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.user-header__left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-header__logo {
  width: 28px;
  height: 28px;
}
.user-header__brand {
  font-size: 16px;
  font-weight: 600;
  color: var(--wecom-text-primary);
}

.user-main {
  flex: 1;
  padding: var(--wecom-spacing-xl) 32px;
}
</style>
