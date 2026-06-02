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
        :label="$t(getTagTitleKey(tag))"
        :closable="!tag.affix"
      />
    </t-tabs>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import type { TagItem } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

/**
 * 根据 tag 信息获取 i18n 翻译 key
 * 优先使用 route 表中的 key 映射
 */
function getTagTitleKey(tag: TagItem): string {
  const routeKeyMap: Record<string, string> = {
    'Dashboard': 'route.dashboard',
    'User': 'route.user',
    'Role': 'route.role',
    'Menu': 'route.menu',
    'Dept': 'route.dept',
    'Dict': 'route.dict',
    'Notice': 'route.notice',
    'Config': 'route.config',
    'Online': 'route.online',
    'OperLog': 'route.operLog',
    'Logininfor': 'route.logininfor',
    'Jobs': 'route.jobs',
    'JobLog': 'route.jobLog',
    'Gen': 'route.gen',
    'Profile': 'route.profile',
    'Backup': 'route.backup',
    'RoleDept': 'route.roleDept',
  }
  const key = routeKeyMap[tag.name]
  if (key) return key
  // 如果 tag.title 直接匹配某个 i18n key，使用它
  const knownKeys = ['route.dashboard', 'route.user', 'route.role', 'route.menu',
    'route.dept', 'route.dict', 'route.notice', 'route.config',
    'route.online', 'route.operLog', 'route.logininfor', 'route.jobs',
    'route.gen', 'route.profile', 'route.backup', 'route.roleDept']
  for (const k of knownKeys) {
    // getTagTitleKey 在模板中经 $t() 调用，此处直接返回原 key 或标题
    if (knownKeys.includes(k) && tag.title === k) return k
  }
  return tag.title
}

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
  background: var(--wecom-bg-card);
  border-bottom: 1px solid var(--wecom-border);
  padding: 0 var(--wecom-spacing-xl);
}

/* 对齐企微风格：标签条扁平、紧凑 */
.tags-view :deep(.t-tabs__nav-item) {
  font-size: 12px;
}
.tags-view :deep(.t-tabs__content) {
  display: none;
}
</style>
