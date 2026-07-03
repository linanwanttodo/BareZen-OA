<template>
  <t-breadcrumb class="page-breadcrumb">
    <t-breadcrumb-item
      v-for="(item, index) in crumbs"
      :key="item.path"
      :to="index < crumbs.length - 1 ? item.path : undefined"
    >
      {{ item.title }}
    </t-breadcrumb-item>
  </t-breadcrumb>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { labelFromPath } from '@/utils/menuLabel'

interface Crumb {
  path: string
  title: string
}

const route = useRoute()
const { t, te } = useI18n()

/**
 * 面包屑层级：取 route.matched 中有 title 的记录，
 * 文案按 path 末段推导 route.* 键（与导航栏、标签页同一套约定），无翻译则用原值。
 */
const crumbs = computed<Crumb[]>(() => {
  return route.matched
    .filter((r) => r.meta?.title)
    .map((r) => {
      const raw = r.meta.title as string
      return { path: r.path, title: labelFromPath(r.path, raw, t, te) }
    })
})
</script>

<style scoped>
/* 位于 .bz-content 内部，随内容区一起留白，故不设底色与边框 */
.page-breadcrumb {
  font-size: var(--td-font-size-body-small);
  line-height: var(--td-line-height-body-small);
}
/* TDesign 在 __inner 上重复声明了 font: body-medium，只改根节点会被内层覆盖 */
.page-breadcrumb :deep(.t-breadcrumb__inner) {
  font-size: var(--td-font-size-body-small);
  line-height: var(--td-line-height-body-small);
}
/* 分隔符图标默认 body-large(16px)，比文字还大，一并收小 */
.page-breadcrumb :deep(.t-breadcrumb__item .t-icon) {
  font-size: var(--td-font-size-body-small);
}
</style>
