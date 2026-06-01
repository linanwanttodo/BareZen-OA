<template>
  <div class="mx-auto max-w-4xl px-6 py-8">
    <div class="mb-6 flex items-center gap-4">
      <a-button size="small" @click="router.back()">
        <template #icon><icon-left /></template>
        返回
      </a-button>
      <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('message.detail') }}</h1>
    </div>

    <a-spin :loading="loading">
      <div v-if="message" class="rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-6">
        <div class="mb-4 flex items-center gap-2">
          <a-tag :color="tagColor" size="small">{{ tagLabel }}</a-tag>
          <span class="text-xs text-[var(--muted-foreground)]">{{ formatDate(message.createTime) }}</span>
          <span v-if="message.isRead === 1" class="text-xs text-[var(--muted-foreground)]">{{ $t('message.read') }}</span>
          <span v-else class="text-xs text-[var(--primary)]">{{ $t('message.unread') }}</span>
        </div>
        <h2 class="mb-4 text-xl font-semibold text-[var(--foreground)]">{{ message.title }}</h2>
        <div class="whitespace-pre-wrap text-sm leading-relaxed text-[var(--foreground)]">{{ message.content }}</div>
      </div>
      <a-empty v-else-if="!loading" description="消息不存在" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconLeft } from '@arco-design/web-vue/es/icon'
import { getMessageDetailApi } from '@/api/message'
import type { SysMessage } from '@/types/api'
import { formatDate } from '@/utils/format'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const message = ref<SysMessage | null>(null)

const tagColor = computed(() => {
  return { approval: 'arcoblue', system: 'gray', notice: 'green' }[message.value?.msgType || ''] || 'gray'
})

const tagLabel = computed(() => {
  const map: Record<string, string> = {
    approval: 'message.approval',
    system: 'message.system',
    notice: 'message.notice',
  }
  const key = map[message.value?.msgType || ''] || 'message.notification'
  return t(key)
})

async function loadMessage() {
  const id = route.params.id as string
  if (!id) return
  loading.value = true
  try {
    // 直接调用单条查询接口（后端校验属主 + 自动标记已读）
    const res = await getMessageDetailApi(Number(id))
    if (res.code === 200 && res.data) {
      message.value = res.data
    } else {
      Message.error(res.msg || t('message.notFound'))
    }
  } catch (e) {
    console.error('load message detail error:', e)
    Message.error(t('message.notFound'))
  } finally {
    loading.value = false
  }
}

onMounted(loadMessage)
</script>