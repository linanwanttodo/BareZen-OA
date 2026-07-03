<template>
  <div class="tags-view">
    <t-tabs
      theme="card"
      :value="appStore.activeTag"
      drag-sort
      @change="onChange"
      @remove="onRemove"
      @drag-sort="onDragSort"
    >
      <t-tab-panel
        v-for="tag in appStore.tagsList"
        :key="tag.path"
        :value="tag.path"
        :removable="!tag.affix"
        :draggable="!tag.affix"
      >
        <template #label>
          <!-- 右键菜单：刷新 / 关闭左侧 / 关闭右侧 / 关闭其他 -->
          <t-dropdown
            trigger="context-menu"
            :min-column-width="140"
            :popup-props="popupProps(tag.path)"
          >
            <span class="tag-label">{{ tagLabel(tag) }}</span>
            <template #dropdown>
              <t-dropdown-menu>
                <t-dropdown-item @click="handleRefresh(tag)">
                  <refresh-icon />
                  {{$t('tagTabs.refresh') }}
                </t-dropdown-item>
                <t-dropdown-item
                  v-if="appStore.tagsList.findIndex((t) => t.path === tag.path) > 1"
                  @click="handleCloseLeft(tag.path)"
                >
                  <arrow-left-icon />
                  {{$t('tagTabs.closeLeft') }}
                </t-dropdown-item>
                <t-dropdown-item
                  v-if="appStore.tagsList.findIndex((t) => t.path === tag.path) < appStore.tagsList.length - 1"
                  @click="handleCloseRight(tag.path)"
                >
                  <arrow-right-icon />
                  {{$t('tagTabs.closeRight') }}
                </t-dropdown-item>
                <t-dropdown-item
                  v-if="appStore.tagsList.length > 2"
                  @click="handleCloseOther(tag.path)"
                >
                  <close-circle-icon />
                  {{$t('tagTabs.closeOther') }}
                </t-dropdown-item>
              </t-dropdown-menu>
            </template>
          </t-dropdown>
        </template>
      </t-tab-panel>
    </t-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeftIcon, ArrowRightIcon, CloseCircleIcon, RefreshIcon } from 'tdesign-icons-vue-next'
import { useAppStore } from '@/stores/app'
import { labelFromPath } from '@/utils/menuLabel'
import type { TagItem } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const { t, te } = useI18n()
const appStore = useAppStore()

/** 当前打开右键菜单的标签 path（null = 未打开） */
const activeMenuPath = ref<string | null>(null)

/**
 * 右键菜单的 popup 配置。
 * 点击菜单外（document 触发）时收起菜单，否则关闭后菜单会一直挂着。
 */
function popupProps(path: string) {
  return {
    overlayClassName: 'route-tabs-dropdown',
    onVisibleChange: (visible: boolean, ctx: { trigger?: string }) => {
      if (ctx?.trigger === 'document') activeMenuPath.value = null
      if (visible) activeMenuPath.value = path
    },
    visible: activeMenuPath.value === path,
  }
}

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

/** 拖拽排序：把 currentIndex 处的标签移到 targetIndex */
function onDragSort({ currentIndex, targetIndex }: { currentIndex: number; targetIndex: number }) {
  appStore.moveTag(currentIndex, targetIndex)
}

/** 刷新当前标签页 */
function handleRefresh(tag: TagItem) {
  activeMenuPath.value = null
  if (route.path === tag.path) {
    // 已在该页：先销毁 keep-alive 缓存再跳回自身，触发组件重建
    appStore.reloadTag(tag.path)
    router.replace({ path: tag.path })
  } else {
    router.push(tag.path)
  }
}

function handleCloseLeft(path: string) {
  activeMenuPath.value = null
  appStore.closeLeftTags(path)
  if (route.path !== path) router.push(path)
}

function handleCloseRight(path: string) {
  activeMenuPath.value = null
  appStore.closeRightTags(path)
  if (route.path !== path) router.push(path)
}

function handleCloseOther(path: string) {
  activeMenuPath.value = null
  appStore.closeOtherTags(path)
  if (route.path !== path) router.push(path)
}
</script>

<style scoped>
.tags-view {
  background: var(--td-bg-color-container);
  border-bottom: 1px solid var(--td-component-stroke);
  padding: 0 var(--td-size-8);
}

/* 对齐 Starter：标签条扁平紧凑，card 主题默认直角加圆角与卡片语言一致 */
.tags-view :deep(.t-tabs__nav-item) {
  font-size: var(--td-font-size-body-small);
  border-radius: var(--td-radius-default);
}
.tags-view :deep(.t-tabs--card .t-tabs__nav-item) {
  padding: 0 var(--td-comp-margin-l);
}
/* 拖拽中的标签给明确反馈 */
.tags-view :deep(.t-tabs__nav-item--dragging) {
  opacity: 0.5;
}
.tag-label {
  white-space: nowrap;
}
.tags-view :deep(.t-tabs__content) {
  display: none;
}
</style>

<style>
/* 右键菜单项里的图标间距（popup 渲染在 body 上，scoped 样式覆盖不到） */
.route-tabs-dropdown .t-icon {
  margin-right: 8px;
}
</style>
