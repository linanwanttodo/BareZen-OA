<template>
  <div class="mx-auto max-w-4xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-[var(--foreground)]">消息中心</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">离线通知兜底，登录后可查看未读消息</p>
      </div>
      <a-button type="primary" :disabled="unreadCount === 0" @click="onMarkAllRead">
        <template #icon><icon-check-circle /></template>
        全部已读
      </a-button>
    </div>

    <!-- 已读/未读筛选 -->
    <div class="mb-6">
      <a-radio-group v-model="readFilter" type="button" @change="onReadFilter">
        <a-radio :value="undefined">全部</a-radio>
        <a-radio :value="0">未读（{{ unreadCount }}）</a-radio>
        <a-radio :value="1">已读</a-radio>
      </a-radio-group>
    </div>

    <!-- 消息列表 -->
    <a-spin :loading="loading">
      <div v-if="messages.length" class="space-y-3">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="cursor-pointer rounded-lg border bg-[var(--card)] p-5 transition-colors hover:border-[var(--primary)]"
          :class="[msg.isRead === 1 ? 'border-[var(--border)]' : 'border-[var(--primary-light)] shadow-sm']"
          @click="onMarkRead(msg)"
        >
          <div class="mb-1 flex items-center gap-2">
            <span
              class="inline-block h-2 w-2 shrink-0 rounded-full"
              :class="msg.isRead === 1 ? 'bg-[var(--border)]' : 'bg-[var(--primary)]'"
            />
            <h3 class="min-w-0 flex-1 truncate font-semibold text-[var(--foreground)]" :class="{ 'opacity-70': msg.isRead === 1 }">
              {{ msg.title }}
            </h3>
            <a-tag :color="msgTypeColor(msg.msgType)" size="small">{{ msgTypeLabel(msg.msgType) }}</a-tag>
            <span class="shrink-0 text-xs text-[var(--muted-foreground)]">{{ formatDate(msg.createTime) }}</span>
          </div>
          <p class="line-clamp-2 pl-4 text-sm text-[var(--muted-foreground)]" :class="{ 'opacity-60': msg.isRead === 1 }">
            {{ msg.content }}
          </p>
        </div>
      </div>
      <a-empty v-else description="暂无消息" />
    </a-spin>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="mt-6 flex justify-end">
      <a-pagination
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
import { Message } from '@arco-design/web-vue'
import { IconCheckCircle } from '@arco-design/web-vue/es/icon'
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

function msgTypeLabel(type: string) {
  return { approval: '审批', system: '系统', notice: '公告' }[type] || '通知'
}

function msgTypeColor(type: string) {
  return { approval: 'arcoblue', system: 'gray', notice: 'green' }[type] || 'gray'
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