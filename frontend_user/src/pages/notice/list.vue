<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <PageHeader title="通知公告" description="查看企业发布的通知与公告" />

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
            <Badge :variant="notice.noticeType === '1' ? 'outline' : 'default'">
              {{ notice.noticeType === '1' ? '通知' : '公告' }}
            </Badge>
            <h3 class="truncate text-base font-semibold text-[var(--foreground)]">{{ notice.noticeTitle }}</h3>
          </div>
          <p class="mb-3 line-clamp-2 leading-relaxed text-sm text-[var(--muted-foreground)]">{{ stripHtml(notice.noticeContent) }}</p>
          <div class="flex items-center gap-4 text-xs text-[var(--muted-foreground)]">
            <span>{{ formatDate(notice.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>
    <Empty v-else description="暂无通知公告" />

    <div v-if="total > pageSize" class="mt-6">
      <Pagination v-model="currentPage" :total="total" :page-size="pageSize" @change="loadNotices" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, FileText } from 'lucide-vue-next'
import { getNoticeListApi } from '@/api/enterprise'
import type { Notice } from '@/types/api'
import PageHeader from '@/components/common/PageHeader.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import Pagination from '@/components/ui/pagination/Pagination.vue'
import Empty from '@/components/ui/empty/Empty.vue'

const router = useRouter()
const notices = ref<Notice[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

function stripHtml(html?: string) {
  if (!html) return ''
  return html.replace(/<[^>]+>/g, '').substring(0, 120)
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  return dateStr.substring(0, 10)
}

async function loadNotices() {
  try {
    const res = await getNoticeListApi({ page: currentPage.value, pageSize: pageSize.value })
    notices.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    // 静默
  }
}

onMounted(() => {
  loadNotices()
})
</script>
