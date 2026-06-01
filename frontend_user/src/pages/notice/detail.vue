<template>
  <div class="mx-auto max-w-4xl px-6 py-8">
    <a-button type="text" size="small" class="mb-6" @click="router.push('/notices')">
      <ChevronLeft :size="16" class="mr-1.5" />
      {{ $t('notice.backToList') }}
    </a-button>

    <div v-if="notice" class="rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-8">
      <div class="mb-6 border-b border-[var(--border)] pb-6">
        <div class="mb-3 flex items-center gap-2">
          <a-tag :color="notice.noticeType === '1' ? 'arcoblue' : 'green'">
            {{ notice.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}
          </a-tag>
        </div>
        <h1 class="mb-4 text-xl font-semibold text-[var(--wecom-text-primary)]">{{ notice.noticeTitle }}</h1>
        <div class="flex items-center gap-4 text-sm text-[var(--muted-foreground)]">
          <span>{{ $t('notice.publishTime') }}：{{ formatDateTime(notice.createTime) }}</span>
        </div>
      </div>
      <div class="prose max-w-none" v-html="sanitizeHtml(notice.noticeContent)"></div>
    </div>
    <a-empty v-else :description="$t('notice.notFound')" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ChevronLeft } from 'lucide-vue-next'
import { getNoticeDetailApi } from '@/api/enterprise'
import type { Notice } from '@/types/api'

import { sanitizeHtml } from '@/utils/dom'
import { formatDateTime } from '@/utils/format'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const notice = ref<Notice | null>(null)

async function loadNotice() {
  const id = String(route.params.id)
  if (!id) return
  try {
    const res = await getNoticeDetailApi(id)
    notice.value = res.data
  } catch (err) {
    console.error('notice detail load error:', err)
  }
}

onMounted(() => {
  loadNotice()
})
</script>
