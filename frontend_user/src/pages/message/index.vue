<template>
  <div class="mx-auto max-w-4xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('message.title') }}</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('message.subtitle') }}</p>
      </div>
      <t-button theme="primary" :disabled="unreadCount === 0" @click="onMarkAllRead">
        <template #icon><icon-check-circle /></template>
        {{ $t('message.markAllRead') }}
      </t-button>
    </div>

    <!-- 已读/未读筛选 -->
    <div class="mb-6">
      <t-radio-group v-model="readFilter" variant="default-filled" @change="onReadFilter">
        <t-radio :value="undefined">{{ $t('message.all') }}</t-radio>
        <t-radio :value="0">{{ $t('message.unread') }}（{{ unreadCount }}）</t-radio>
        <t-radio :value="1">{{ $t('message.read') }}</t-radio>
      </t-radio-group>
    </div>

    <!-- 消息列表 -->
    <t-loading :loading="loading">
      <div v-if="messages.length" class="space-y-3">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="cursor-pointer rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5 transition-colors hover:border-[var(--primary)]"
          :class="[msg.isRead === 1 ? 'border-[var(--wecom-border)]' : 'border-[var(--wecom-primary-light)] shadow-wecom-sm']"
          @click="goDetail(msg)"
        >
          <div class="mb-1 flex items-center gap-2">
            <span
              class="inline-block h-2 w-2 shrink-0 rounded-full"
              :class="msg.isRead === 1 ? 'bg-[var(--border)]' : 'bg-[var(--primary)]'"
            />
            <h3 class="min-w-0 flex-1 truncate font-semibold text-[var(--foreground)]" :class="{ 'opacity-70': msg.isRead === 1 }">
              {{ msg.title }}
            </h3>
            <t-tag :color="msgTypeColor(msg.msgType)" size="small">{{ msgTypeLabel(msg.msgType) }}</t-tag>
            <span class="shrink-0 text-xs text-[var(--muted-foreground)]">{{ formatDate(msg.createTime) }}</span>
          </div>
          <p class="line-clamp-2 pl-4 text-sm text-[var(--muted-foreground)]" :class="{ 'opacity-60': msg.isRead === 1 }">
            {{ msg.content }}
          </p>
        </div>
      </div>
      <t-empty v-else :description="$t('message.noData')" />
    </t-loading>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="mt-6 flex justify-end">
      <t-pagination
        v-model:current="currentPage"
        :total="total"
        :page-size="pageSize"
        show-total
        @change="loadMessages"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MessagePlugin as Message } from 'tdesign-vue-next'
import { getMyMessagesApi, getUnreadCountApi, markMessageReadApi, markAllMessagesReadApi } from '@/api/message'
import type { SysMessage } from '@/types/api'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const messages = ref<SysMessage[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const unreadCount = ref(0)
const readFilter = ref<number | undefined>(undefined)
const { t } = useI18n()
const router = useRouter()

function msgTypeLabel(type: string) {
  const map: Record<string, string> = { approval: 'message.approval', system: 'message.system', notice: 'message.notice' }
  return t(map[type] || 'message.notification')
}

function msgTypeColor(type: string) {
  return { approval: 'arcoblue', system: 'gray', notice: 'green' }[type] || 'gray'
}

function goDetail(msg: SysMessage) {
  router.push(`/messages/${msg.id}`)
}

async function loadUnreadCount() {
  try {
    const res = await getUnreadCountApi()
    unreadCount.value = res.data ?? 0
  } catch {
    // 未读数失败不影响列表展示
  }
}

async function loadMessages() {
  loading.value = true
  try {
    const res = await getMyMessagesApi({
      page: currentPage.value,
      pageSize: pageSize.value,
      isRead: readFilter.value,
    })
    messages.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (err) {
    console.error('message list load error:', err)
  } finally {
    loading.value = false
  }
}

function onReadFilter() {
  currentPage.value = 1
  loadMessages()
}

async function onMarkRead(msg: SysMessage) {
  if (msg.isRead === 1) return
  try {
    await markMessageReadApi(msg.id)
    msg.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    // 未读筛选下，已读后刷新列表
    if (readFilter.value === 0) {
      loadMessages()
    }
  } catch (err) {
    console.error('mark read error:', err)
  }
}

async function onMarkAllRead() {
  try {
    const count = await markAllMessagesReadApi()
    Message.success(count.data > 0 ? `已标记 ${count.data} 条为已读` : '没有未读消息')
    unreadCount.value = 0
    if (readFilter.value === 1) {
      // 已读筛选下全部已读无变化，保持当前列表
      messages.value.forEach((m) => (m.isRead = 1))
    } else {
      loadMessages()
    }
  } catch (err) {
    console.error('mark all read error:', err)
  }
}

onMounted(() => {
  loadMessages()
  loadUnreadCount()
})
</script>