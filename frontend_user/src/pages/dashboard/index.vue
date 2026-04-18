<template>
  <div class="dashboard-page">
    <!-- 欢迎区域 -->
    <section class="px-6 py-10">
      <div class="mx-auto max-w-6xl">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="mb-2 text-2xl font-bold text-[var(--foreground)]">{{ greeting }}，{{ nickName }}</h1>
            <p class="text-[var(--muted-foreground)]">今天是 {{ todayStr }}，祝你工作顺利</p>
          </div>
          <div class="hidden md:block">
            <Button @click="router.push('/todos')">
              <CheckSquare :size="16" class="mr-1.5" />
              我的待办
            </Button>
          </div>
        </div>
      </div>
    </section>

    <!-- 统计卡片 -->
    <section class="px-6">
      <div class="mx-auto grid max-w-6xl grid-cols-1 gap-4 md:grid-cols-3">
        <StatCard label="待办事项" :value="dashboard.todoCount || 0" :icon="Clock" />
        <StatCard label="已完成" :value="dashboard.doneCount || 0" :icon="CheckCheck" />
        <StatCard label="紧急待办" :value="dashboard.urgentCount || 0" :icon="AlertCircle" />
      </div>
    </section>

    <!-- 双栏内容 -->
    <section class="px-6 py-8">
      <div class="mx-auto grid max-w-6xl grid-cols-1 gap-6 lg:grid-cols-2">
        <!-- 最新通知 -->
        <Card>
          <CardHeader class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <Bell :size="20" class="text-[var(--foreground)]" />
              <CardTitle>最新通知</CardTitle>
            </div>
            <Button variant="ghost" size="sm" @click="router.push('/notices')">查看全部</Button>
          </CardHeader>
          <CardContent>
            <div v-if="dashboard.latestNotices?.length" class="space-y-3">
              <div
                v-for="notice in dashboard.latestNotices"
                :key="notice.id"
                class="flex cursor-pointer items-start gap-3 rounded-lg border border-[var(--border)] p-3"
                @click="router.push(`/notices/${notice.id}`)"
              >
                <Badge :variant="notice.noticeType === '1' ? 'outline' : 'default'">
                  {{ notice.noticeType === '1' ? '通知' : '公告' }}
                </Badge>
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-medium text-[var(--foreground)]">{{ notice.noticeTitle }}</p>
                  <p class="mt-1 text-xs text-[var(--muted-foreground)]">{{ formatDate(notice.createTime) }}</p>
                </div>
              </div>
            </div>
            <Empty v-else description="暂无通知" />
          </CardContent>
        </Card>

        <!-- 即将到期待办 -->
        <Card>
          <CardHeader class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <CheckSquare :size="20" class="text-[var(--foreground)]" />
              <CardTitle>即将到期</CardTitle>
            </div>
            <Button variant="ghost" size="sm" @click="router.push('/todos')">全部待办</Button>
          </CardHeader>
          <CardContent>
            <div v-if="dashboard.upcomingTodos?.length" class="space-y-3">
              <div v-for="todo in dashboard.upcomingTodos" :key="todo.id" class="flex items-center gap-3 rounded-lg border border-[var(--border)] p-3">
                <Badge :variant="priorityVariant(todo.priority)">{{ priorityLabel(todo.priority) }}</Badge>
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-medium text-[var(--foreground)]">{{ todo.title }}</p>
                  <p class="mt-1 text-xs text-[var(--muted-foreground)]">截止: {{ todo.dueDate || '无期限' }}</p>
                </div>
                <Badge :variant="statusVariant(todo.status)">{{ statusLabel(todo.status) }}</Badge>
              </div>
            </div>
            <Empty v-else description="暂无待办" />
          </CardContent>
        </Card>
      </div>
    </section>

    <!-- 快捷入口 -->
    <section class="px-6 pb-8">
      <div class="mx-auto max-w-6xl">
        <Card>
          <CardHeader>
            <div class="flex items-center gap-2">
              <Grid :size="20" class="text-[var(--foreground)]" />
              <CardTitle>快捷入口</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <div class="grid grid-cols-2 gap-4 md:grid-cols-4">
              <button
                v-for="entry in quickEntries"
                :key="entry.path"
                type="button"
                class="flex flex-col items-center gap-2 rounded-lg border border-[var(--border)] p-4 text-[var(--foreground)]"
                @click="router.push(entry.path)"
              >
                <div class="flex h-12 w-12 items-center justify-center rounded-md bg-[var(--muted)]">
                  <component :is="entry.icon" :size="24" />
                </div>
                <span class="text-sm font-medium">{{ entry.title }}</span>
              </button>
            </div>
          </CardContent>
        </Card>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Clock,
  CheckCheck,
  AlertCircle,
  Bell,
  CheckSquare,
  Grid,
  Users,
  FileText,
} from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { getDashboardApi } from '@/api/enterprise'
import type { DashboardData } from '@/types/api'
import Card from '@/components/ui/card/Card.vue'
import CardHeader from '@/components/ui/card/CardHeader.vue'
import CardTitle from '@/components/ui/card/CardTitle.vue'
import CardContent from '@/components/ui/card/CardContent.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import Button from '@/components/ui/button/Button.vue'
import StatCard from '@/components/common/StatCard.vue'
import Empty from '@/components/ui/empty/Empty.vue'

const router = useRouter()
const userStore = useUserStore()
const dashboard = ref<Partial<DashboardData>>({})

const nickName = computed(() => userStore.userInfo?.nickName || '同事')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const todayStr = computed(() => {
  const d = new Date()
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${weekdays[d.getDay()]}`
})

const quickEntries = [
  { title: '通知公告', path: '/notices', icon: Bell },
  { title: '通讯录', path: '/contacts', icon: Users },
  { title: '待办事项', path: '/todos', icon: CheckSquare },
  { title: '个人中心', path: '/user/profile', icon: FileText },
]

function priorityLabel(p?: number) {
  return { 0: '低', 1: '中', 2: '高', 3: '紧急' }[p ?? 1] || '中'
}
function priorityVariant(p?: number): 'secondary' | 'outline' | 'default' | 'destructive' {
  return ({ 0: 'secondary', 1: 'outline', 2: 'default', 3: 'destructive' } as const)[p ?? 1] || 'outline'
}
function statusLabel(s?: number) {
  return { 0: '待办', 1: '进行中', 2: '已完成', 3: '已取消' }[s ?? 0] || '待办'
}
function statusVariant(s?: number): 'secondary' | 'outline' | 'default' | 'destructive' {
  return ({ 0: 'outline', 1: 'default', 2: 'secondary', 3: 'outline' } as const)[s ?? 0] || 'outline'
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  return dateStr.substring(0, 10)
}

async function loadDashboard() {
  try {
    const res = await getDashboardApi()
    dashboard.value = res.data || {}
  } catch {
    // 静默处理
  }
}

onMounted(() => {
  loadDashboard()
})
</script>
