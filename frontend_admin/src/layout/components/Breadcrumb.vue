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

interface Crumb {
  path: string
  title: string
}

const route = useRoute()
const { t, te } = useI18n()

/**
 * 面包屑层级：取 route.matched 中有 title 的记录，
 * title 优先走 i18n（route.* 键名约定，与 TagsView/菜单一致），无翻译则用原值。
 */
const crumbs = computed<Crumb[]>(() => {
  return route.matched
    .filter((r) => r.meta?.title)
    .map((r) => {
      const raw = r.meta.title as string
      const key = `route.${String(r.name)}`
      const title = te(key) ? t(key) : raw
      return { path: r.path, title }
    })
})
</script>

<style scoped>
.page-breadcrumb {
  padding: var(--wecom-spacing-sm) var(--wecom-spacing-xl);
  background: var(--wecom-bg-card);
  border-bottom: 1px solid var(--wecom-border);
}
</style>
