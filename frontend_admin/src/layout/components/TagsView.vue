<template>
  <div class="tags-view">
    <div
      v-for="tag in appStore.tagsList"
      :key="tag.path"
      class="tags-view__item"
      :class="{ 'tags-view__item--active': tag.path === appStore.activeTag }"
      @click="router.push(tag.path)"
    >
      <span>{{ tag.title }}</span>
      <span
        v-if="!tag.affix"
        class="tags-view__close"
        @click.stop="onTagRemove(tag.path)"
      >×</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

watch(
  () => route.path,
  (path) => {
    if (route.meta?.title && !route.meta?.hidden) {
      appStore.addTag({
        path,
        name: route.name as string,
        title: route.meta.title as string,
        affix: route.meta.affix,
      })
    }
  },
  { immediate: true }
)

function onTagRemove(path: string) {
  const nextPath = appStore.closeTag(path)
  if (nextPath) {
    router.push(nextPath)
  }
}
</script>

<style scoped>
.tags-view {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 32px;
  background: #FFFFFF;
  border-bottom: 1px solid #E5E6EB;
}

.tags-view__item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  font-size: 12px;
  color: #86909C;
  background: #F7F8FA;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
}
.tags-view__item:hover {
  color: #4E5969;
}
.tags-view__item--active {
  color: #3370FF;
  background: #E8F3FF;
}

.tags-view__close {
  font-size: 14px;
  line-height: 1;
  opacity: 0.5;
}
.tags-view__close:hover {
  opacity: 1;
}
</style>
