<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <PageHeader :title="$t('notice.title')" description="" />

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--primary)]"></div>
      <span class="ml-2 text-[var(--muted-foreground)]">加载中...</span>
    </div>
    <template v-else>
      <div v-if="notices.length" class="space-y-4">
      <div
        v-for="notice in notices"
        :key="notice.id"
        class="flex cursor-pointer items-start gap-4 rounded-lg border border-[var(--border)] bg-[var(--card)] p-6"
        @click="router.push(`/notices/${notice.id}`)"
      >
        <div class="mt-1 flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-[var(--muted)]">
          <component :is="notice.noticeType === '1' ? Bell : FileText" :size="20" class="text-[var(--foreground)]" />
        </div>
        <div class="min-w-0 flex-1">
          <div class="mb-1 flex items-center gap-2">
            <a-tag :color="notice.noticeType === '1' ? 'arcoblue' : 'green'">
              {{ notice.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}
            </a-tag>
            <h3 class="truncate text-base font-semibold text-[var(--foreground)]">{{ notice.noticeTitle }}</h3>
            <span v-if="notice.requireRead === 'Y' && notice.isRead === false" class="shrink-0 rounded bg-red-500 px-1.5 py-0.5 text-[10px] font-medium text-white">NEW</span>
          </div>
          <p class="mb-3 line-clamp-2 leading-relaxed text-sm text-[var(--muted-foreground)]">{{ stripHtml(notice.noticeContent) }}</p>
          <div class="flex items-center gap-4 text-xs text-[var(--muted-foreground)]">
            <span>{{ formatDate(notice.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>
    <a-empty v-else :description="$t('common.noData')" />

    <div v-if="total > pageSize" class="mt-6">
      <a-pagination v-model:current="currentPage" :total="total" :page-size="pageSize" @change="loadNotices" />
    </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Bell, FileText } from 'lucide-vue-next'
import { getNoticeListApi } from '@/api/enterprise'
import type { Notice } from '@/types/api'
import PageHeader from '@/components/common/PageHeader.vue'

import { formatDate } from '@/utils/format'
import { sanitizeHtml } from '@/utils/dom'

const { t } = useI18n()
const router = useRouter()
const loading = ref(true)
const notices = ref<Notice[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

function stripHtml(html?: string) {
  if (!html) return ''
  return sanitizeHtml(html).replace(/<[^>]+>/g, '').substring(0, 120)
}

async function loadNotices() {
  loading.value = true
  try {
    const res = await getNoticeListApi({ page: currentPage.value, pageSize: pageSize.value })
    notices.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (err) {
    console.error('notice list load error:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadNotices()
})
</script>
