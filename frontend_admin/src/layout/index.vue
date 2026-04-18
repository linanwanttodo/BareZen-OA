<template>
  <div class="bz-layout h-full flex flex-col">
    <!-- 顶部导航 -->
    <header>
      <TopNavbar />
    </header>

    <!-- 多标签页 -->
    <TagsView />

    <!-- 主内容区 -->
    <main class="bz-content flex-1 overflow-y-auto">
      <router-view v-slot="{ Component, route }">
        <transition name="fade" mode="out-in">
          <keep-alive>
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import TopNavbar from './components/TopNavbar.vue'
import TagsView from './components/TagsView.vue'
</script>

<style scoped>
.bz-content {
  padding: 0;
  background: #F7F8FA;
}
</style>
