<template>
  <div class="px-6 py-8 max-w-5xl mx-auto">
    <div v-if="loading" class="flex items-center justify-center h-64">
      <t-loading size="28px" :text="$t('common.loading')" />
    </div>
    <template v-else>
      <!-- 欢迎区 -->
      <div class="mb-8">
        <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('dashboard.welcome', { name: nickName }) }}</h1>
        <p class="mt-1 text-sm text-[var(--wecom-text-tertiary)]">{{ todayStr }}</p>
      </div>

      <!-- 统计卡片 -->
      <div class="stat-grid">
        <t-card :title="$t('dashboard.todayTask')" :header-bordered="false">
          <p class="stat-value">{{ dashboard.todoCount || 0 }}</p>
        </t-card>
        <t-card :title="$t('dashboard.doneCount')" :header-bordered="false">
          <p class="stat-value">{{ dashboard.doneCount || 0 }}</p>
        </t-card>
        <t-card :title="$t('dashboard.urgent')" :header-bordered="false">
          <p class="stat-value">{{ dashboard.urgentCount || 0 }}</p>
        </t-card>
      </div>

      <!-- 双栏内容 -->
      <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <!-- 最新通知 -->
        <t-card :title="$t('dashboard.latestNotices')" :header-bordered="false">
          <template #actions>
            <t-link theme="primary" hover="color" class="text-xs" @click="router.push('/notices')">{{ $t('dashboard.viewAll') }}</t-link>
          </template>
          <div v-if="dashboard.latestNotices?.length" class="space-y-3">
            <div
              v-for="notice in dashboard.latestNotices"
              :key="notice.id"
              class="list-item"
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
        </t-card>

        <!-- 待办事项 -->
        <t-card :title="$t('dashboard.upcomingTodos')" :header-bordered="false">
          <template #actions>
            <t-link theme="primary" hover="color" class="text-xs" @click="router.push('/todos')">{{ $t('dashboard.viewAll') }}</t-link>
          </template>
          <div v-if="dashboard.upcomingTodos?.length" class="space-y-3">
            <div v-for="todo in dashboard.upcomingTodos" :key="todo.id" class="list-item">
              <div class="flex items-center gap-3 min-w-0">
                <span class="h-2 w-2 rounded-full shrink-0" :class="todo.priority >= 2 ? 'bg-[var(--wecom-error)]' : 'bg-[var(--wecom-text-disabled)]'"></span>
                <span class="truncate text-sm text-[var(--wecom-text-primary)]">{{ todo.title }}</span>
              </div>
              <span v-if="todo.dueDate" class="shrink-0 text-xs text-[var(--wecom-text-tertiary)]">{{ todo.dueDate }}</span>
            </div>
          </div>
          <div v-else class="py-8 text-center text-sm text-[var(--wecom-text-tertiary)]">{{ $t('dashboard.noUpcoming') }}</div>
        </t-card>
      </div>

      <!-- 快捷入口 -->
      <t-card class="quick-entry-card" :title="$t('dashboard.quickEntry')" :header-bordered="false">
        <div class="quick-grid">
          <t-button
            v-for="entry in quickEntries"
            :key="entry.path"
            variant="outline"
            theme="default"
            size="large"
            class="quick-entry-btn"
            @click="router.push(entry.path)"
          >
            <template #icon><component :is="entry.icon" size="20px" /></template>
            {{ entry.title }}
          </t-button>
        </div>
      </t-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NotificationIcon, CheckRectangleIcon, UsergroupIcon, FileIcon, TimeIcon, EditIcon, CalendarIcon, CartIcon } from 'tdesign-icons-vue-next'
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
  { title: t('dashboard.noticeCount'), path: '/notices', icon: NotificationIcon },
  { title: t('dashboard.contact'), path: '/contacts', icon: UsergroupIcon },
  { title: t('dashboard.todayTask'), path: '/todos', icon: CheckRectangleIcon },
  { title: t('dashboard.myProfile'), path: '/user/profile', icon: FileIcon },
  { title: t('dashboard.attendance'), path: '/attendance', icon: TimeIcon },
  { title: t('dashboard.worklog'), path: '/worklogs', icon: EditIcon },
  { title: t('dashboard.schedule'), path: '/schedules', icon: CalendarIcon },
  { title: t('dashboard.supply'), path: '/supplies', icon: CartIcon },
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

<style scoped>
/* 统计卡片栅格 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}
.stat-value {
  margin-top: 4px;
  font-size: 24px;
  font-weight: 600;
  color: var(--wecom-text-primary);
}
/* 列表行（通知/待办共用） */
.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid var(--wecom-border);
  border-radius: var(--td-radius-small);
  padding: 12px 16px;
}
.list-item:hover {
  background: var(--wecom-bg-hover);
}
/* 快捷入口卡片 */
.quick-entry-card {
  margin-top: 24px;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.quick-entry-btn {
  height: auto;
  padding: 16px;
  flex-direction: column;
  gap: 8px;
  white-space: normal;
}
.quick-entry-btn:hover {
  border-color: var(--td-brand-color);
  color: var(--td-brand-color);
}
@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
