<template>
  <div class="mx-auto max-w-4xl px-6 py-8">
    <Button variant="ghost" size="sm" class="mb-6" @click="router.push('/notices')">
      <ChevronLeft :size="16" class="mr-1.5" />
      返回列表
    </Button>

    <div v-if="notice" class="rounded-lg border border-[var(--border)] bg-[var(--card)] p-8">
      <div class="mb-6 border-b border-[var(--border)] pb-6">
        <div class="mb-3 flex items-center gap-2">
          <Badge :variant="notice.noticeType === '1' ? 'outline' : 'default'">
            {{ notice.noticeType === '1' ? '通知' : '公告' }}
          </Badge>
        </div>
        <h1 class="mb-4 text-2xl font-bold text-[var(--foreground)]">{{ notice.noticeTitle }}</h1>
        <div class="flex items-center gap-4 text-sm text-[var(--muted-foreground)]">
          <span>发布时间：{{ formatDate(notice.createTime) }}</span>
        </div>
      </div>
      <div class="prose max-w-none" v-html="notice.noticeContent"></div>
    </div>
    <Empty v-else description="公告不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ChevronLeft } from 'lucide-vue-next'
import { getNoticeDetailApi } from '@/api/enterprise'
import type { Notice } from '@/types/api'
import Button from '@/components/ui/button/Button.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import Empty from '@/components/ui/empty/Empty.vue'

const router = useRouter()
const route = useRoute()
const notice = ref<Notice | null>(null)

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16)
}

async function loadNotice() {
  const id = String(route.params.id)
  if (!id) return
  try {
    const res = await getNoticeDetailApi(id)
    notice.value = res.data
  } catch {
    // 静默
  }
}

onMounted(() => {
  loadNotice()
})
</script>
