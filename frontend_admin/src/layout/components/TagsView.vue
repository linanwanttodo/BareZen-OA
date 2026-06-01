<template>
  <div class="tags-view">
    <div
      v-for="tag in appStore.tagsList"
      :key="tag.path"
      class="tags-view__item"
      :class="{ 'tags-view__item--active': tag.path === appStore.activeTag }"
      @click="router.push(tag.path)"
    >
      <span>{{ $t(getTagTitleKey(tag)) }}</span>
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
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/stores/app'
import type { TagItem } from '@/stores/app'

const { t } = useI18n()
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
    if (t(k) === tag.title) return k
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
  padding: 8px var(--wecom-spacing-xl);
  background: var(--wecom-bg-card);
  border-bottom: 1px solid var(--wecom-border);
}

.tags-view__item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  font-size: 12px;
  color: var(--wecom-text-tertiary);
  background: var(--wecom-bg-container);
  border: 1px solid transparent;
  border-radius: var(--wecom-radius-sm);
  cursor: pointer;
  user-select: none;
  transition: color var(--wecom-transition-fast), background var(--wecom-transition-fast);
}
.tags-view__item:hover {
  color: var(--wecom-text-secondary);
  background: var(--wecom-bg-active);
}
.tags-view__item--active {
  color: var(--wecom-primary);
  background: var(--wecom-primary-light);
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
