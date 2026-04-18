<template>
  <div class="user-layout">
    <!-- 顶部导航 -->
    <header class="user-header">
      <div class="user-header__left">
        <div class="user-header__logo">B</div>
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
  background: #F7F8FA;
}

.user-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 32px;
  background: #FFFFFF;
  border-bottom: 1px solid #E5E6EB;
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
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background: #3370FF;
  color: #FFFFFF;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.user-header__brand {
  font-size: 16px;
  font-weight: 600;
  color: #1D2129;
}

.user-main {
  flex: 1;
  padding: 24px 32px;
}
</style>
