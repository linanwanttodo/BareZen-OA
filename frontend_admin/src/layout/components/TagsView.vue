<template>
  <div class="tags-view">
    <t-tabs
      theme="card"
      :value="appStore.activeTag"
      @change="onChange"
      @remove="onRemove"
    >
      <t-tab-panel
        v-for="tag in appStore.tagsList"
        :key="tag.path"
        :value="tag.path"
        :label="tagLabel(tag)"
        :closable="!tag.affix"
      />
    </t-tabs>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/stores/app'
import { labelFromPath } from '@/utils/menuLabel'
import type { TagItem } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const { t, te } = useI18n()
const appStore = useAppStore()

/** 标签文案按路由 path 末段推导 route.* 键，与导航栏共用同一套约定 */
const tagLabel = (tag: TagItem) => labelFromPath(tag.path, tag.title, t, te)

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

/** 切换标签 → 跳转路由 */
function onChange(value: unknown) {
  const path = value as string
  if (path && path !== route.path) {
    router.push(path)
  }
}

/** 关闭标签（t-tabs @remove），affix 标签不可关闭不会触发 */
function onRemove({ value }: { value: unknown }) {
  const nextPath = appStore.closeTag(value as string)
  if (nextPath) {
    router.push(nextPath)
  }
}
</script>

<style scoped>
.tags-view {
  background: var(--td-bg-color-container);
  border-bottom: 1px solid var(--td-component-stroke);
  padding: 0 var(--td-size-8);
}

/* 对齐企微风格：标签条扁平、紧凑 */
.tags-view :deep(.t-tabs__nav-item) {
  font-size: var(--td-font-size-body-small);
  /* card 主题的标签项默认直角，加圆角后与卡片语言一致 */
  border-radius: var(--td-radius-default);
}
.tags-view :deep(.t-tabs--card .t-tabs__nav-item) {
  padding: 0 var(--td-comp-margin-l);
}
.tags-view :deep(.t-tabs__content) {
  display: none;
}
</style>
