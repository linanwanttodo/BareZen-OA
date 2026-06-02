<template>
  <div class="px-6 py-8 max-w-5xl mx-auto">
    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--primary)]"></div>
      <span class="ml-2 text-[var(--muted-foreground)]">{{ $t('common.loading') }}</span>
    </div>
    <template v-else>
      <!-- 欢迎区 -->
      <div class="mb-8">
      <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('dashboard.welcome', { name: nickName }) }}</h1>
      <p class="mt-1 text-sm text-[var(--wecom-text-tertiary)]">{{ todayStr }}</p>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-3 gap-4 mb-8">
      <div class="rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5 shadow-wecom-sm">
        <p class="text-sm text-[var(--wecom-text-tertiary)]">{{ $t('dashboard.todayTask') }}</p>
        <p class="mt-2 text-2xl font-semibold text-[var(--wecom-text-primary)]">{{ dashboard.todoCount || 0 }}</p>
      </div>
      <div class="rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5 shadow-wecom-sm">
        <p class="text-sm text-[var(--wecom-text-tertiary)]">{{ $t('dashboard.doneCount') }}</p>
        <p class="mt-2 text-2xl font-semibold text-[var(--wecom-text-primary)]">{{ dashboard.doneCount || 0 }}</p>
      </div>
      <div class="rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5 shadow-wecom-sm">
        <p class="text-sm text-[var(--wecom-text-tertiary)]">{{ $t('dashboard.urgent') }}</p>
        <p class="mt-2 text-2xl font-semibold text-[var(--wecom-text-primary)]">{{ dashboard.urgentCount || 0 }}</p>
      </div>
    </div>

    <!-- 双栏内容 -->
    <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
      <!-- 最新通知 -->
      <div class="rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] shadow-wecom-sm">
        <div class="flex items-center justify-between border-b border-[var(--wecom-border)] px-5 py-4">
          <h2 class="text-sm font-medium text-[var(--wecom-text-primary)]">{{ $t('dashboard.latestNotices') }}</h2>
          <button class="text-xs text-[var(--wecom-primary)]" @click="router.push('/notices')">{{ $t('dashboard.viewAll') }}</button>
        </div>
        <div class="p-5">
          <div v-if="dashboard.latestNotices?.length" class="space-y-3">
            <div
              v-for="notice in dashboard.latestNotices"
              :key="notice.id"
              class="flex items-center justify-between rounded-wecom-sm border border-[var(--wecom-border)] px-4 py-3 cursor-pointer hover:bg-[var(--wecom-bg-hover)] transition-colors"
              @click="router.push(`/notices/${notice.id}`)"
            >
              <div class="flex items-center gap-3 min-w-0">
                <t-tag :bordered="notice.noticeType === '1'" class="shrink-0 text-xs" theme="primary">
                  {{ notice.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}
                </t-tag>
                <span class="truncate text-sm text-[var(--wecom-text-primary)]">{{ notice.noticeTitle }}</span>
              </div>
              <span class="shrink-0 text-xs text-[var(--wecom-text-tertiary)]">{{ formatShortDate(notice.createTime) }}</span>
            </div>
          </div>
          <div v-else class="py-8 text-center text-sm text-[var(--wecom-text-tertiary)]">{{ $t('dashboard.noNotices') }}</div>
        </div>
      </div>

      <!-- 待办事项 -->
      <div class="rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] shadow-wecom-sm">
        <div class="flex items-center justify-between border-b border-[var(--wecom-border)] px-5 py-4">
          <h2 class="text-sm font-medium text-[var(--wecom-text-primary)]">{{ $t('dashboard.upcomingTodos') }}</h2>
          <button class="text-xs text-[var(--wecom-primary)]" @click="router.push('/todos')">{{ $t('dashboard.viewAll') }}</button>
        </div>
        <div class="p-5">
          <div v-if="dashboard.upcomingTodos?.length" class="space-y-3">
            <div v-for="todo in dashboard.upcomingTodos" :key="todo.id" class="flex items-center justify-between rounded-wecom-sm border border-[var(--wecom-border)] px-4 py-3">
              <div class="flex items-center gap-3 min-w-0">
                <span class="h-2 w-2 rounded-full shrink-0" :class="todo.priority >= 2 ? 'bg-[var(--wecom-error)]' : 'bg-[var(--wecom-text-disabled)]'"></span>
                <span class="truncate text-sm text-[var(--wecom-text-primary)]">{{ todo.title }}</span>
              </div>
              <span v-if="todo.dueDate" class="shrink-0 text-xs text-[var(--wecom-text-tertiary)]">{{ todo.dueDate }}</span>
            </div>
          </div>
          <div v-else class="py-8 text-center text-sm text-[var(--wecom-text-tertiary)]">{{ $t('dashboard.noUpcoming') }}</div>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="mt-6 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5 shadow-wecom-sm">
      <h2 class="mb-4 text-sm font-medium text-[var(--wecom-text-primary)]">{{ $t('dashboard.quickEntry') }}</h2>
      <div class="grid grid-cols-4 gap-3">
        <button
          v-for="entry in quickEntries"
          :key="entry.path"
          type="button"
          class="flex flex-col items-center gap-2 rounded-wecom-sm border border-[var(--wecom-border)] px-4 py-4 text-sm text-[var(--wecom-text-primary)] hover:bg-[var(--wecom-bg-hover)] transition-colors"
          @click="router.push(entry.path)"
        >
          <component :is="entry.icon" :size="20" class="text-[var(--wecom-text-tertiary)]" />
          <span>{{ entry.title }}</span>
        </button>
      </div>
    </div>
  </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Bell, CheckSquare, Users, FileText, Clock, NotebookPen, Calendar, Package } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { formatShortDate } from '@/utils/format'
import { getDashboardApi } from '@/api/enterprise'
import type { DashboardData } from '@/types/api'


const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const dashboard = ref<Partial<DashboardData>>({})

const nickName = computed(() => userStore.userInfo?.nickName || '同事')

const todayStr = computed(() => {
  const d = new Date()
  const weekdays: Record<number, string> = { 0: t('dashboard.sunday'), 1: t('dashboard.monday'), 2: t('dashboard.tuesday'), 3: t('dashboard.wednesday'), 4: t('dashboard.thursday'), 5: t('dashboard.friday'), 6: t('dashboard.saturday') }
  return t('dashboard.dateFormat', { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate(), weekday: weekdays[d.getDay()] })
})

const quickEntries = computed(() => [
  { title: t('dashboard.noticeCount'), path: '/notices', icon: Bell },
  { title: t('dashboard.contact'), path: '/contacts', icon: Users },
  { title: t('dashboard.todayTask'), path: '/todos', icon: CheckSquare },
  { title: t('dashboard.myProfile'), path: '/user/profile', icon: FileText },
  { title: t('dashboard.attendance'), path: '/attendance', icon: Clock },
  { title: t('dashboard.worklog'), path: '/worklogs', icon: NotebookPen },
  { title: t('dashboard.schedule'), path: '/schedules', icon: Calendar },
  { title: t('dashboard.supply'), path: '/supplies', icon: Package },
])

async function loadDashboard() {
  loading.value = true
  try {
    const res = await getDashboardApi()
    dashboard.value = res.data || {}
  } catch (err) {
    console.error('dashboard load error:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboard()
})
</script>
