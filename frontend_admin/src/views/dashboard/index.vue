<template>
  <div v-if="loading" class="flex items-center justify-center h-64">
    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--primary)]"></div>
    <span class="ml-2 text-[var(--muted-foreground)]">{{ $t('common.loading') }}</span>
  </div>
  <div v-else class="dashboard-container">
    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <a-card v-for="card in statCards" :key="card.title" class="p-5" :style="{ borderLeft: `3px solid ${card.color}` }">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-[var(--muted-foreground)]">{{ $t(card.titleKey) }}</p>
            <p class="mt-1 text-2xl font-semibold tracking-tight text-[var(--foreground)]">{{ card.value }}</p>
          </div>
          <component :is="card.icon" :size="20" :style="{ color: card.color }" />
        </div>
      </a-card>
    </div>

    <!-- 图表区域 -->
    <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-12">
      <a-card class="p-5 lg:col-span-8">
        <div class="mb-2 flex items-baseline justify-between">
          <h3 class="text-base font-semibold text-[var(--foreground)]">{{ $t('dashboard.visitTrend') }}</h3>
          <span class="text-xs text-[var(--muted-foreground)]">{{ $t('dashboard.last7Days') }}</span>
        </div>
        <div ref="lineChartRef" class="chart-container"></div>
      </a-card>
      <a-card class="p-5 lg:col-span-4">
        <div class="mb-2 flex items-baseline justify-between">
          <h3 class="text-base font-semibold text-[var(--foreground)]">{{ $t('dashboard.userDistribution') }}</h3>
          <span class="text-xs text-[var(--muted-foreground)]">{{ $t('dashboard.byRole') }}</span>
        </div>
        <div ref="pieChartRef" class="chart-container"></div>
      </a-card>
    </div>

    <!-- 公告 + 最近操作 -->
    <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
      <a-card class="p-5">
        <div class="mb-3 flex items-center justify-between">
          <h3 class="text-sm font-medium text-[var(--foreground)]">{{ $t('dashboard.latestNotices') }}</h3>
          <button class="text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]" @click="router.push('/notice')">{{ $t('common.viewAll') }}</button>
        </div>
        <div v-if="notices.length" class="divide-y divide-[var(--border)]">
          <div v-for="item in notices" :key="item.id" class="flex items-center justify-between py-3">
            <div class="flex items-center gap-2 min-w-0">
              <a-tag :color="item.noticeType === '1' ? 'arcoblue' : 'purple'" class="shrink-0 text-xs">
                {{ item.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}
              </a-tag>
              <span class="truncate text-sm text-[var(--foreground)]" @click="handleNoticeClick(item)">{{ item.noticeTitle }}</span>
            </div>
            <span class="shrink-0 text-xs text-[var(--muted-foreground)]">{{ formatDateTime(item.createTime) }}</span>
          </div>
        </div>
        <p v-else class="py-8 text-center text-sm text-[var(--muted-foreground)]">{{ $t('dashboard.noNotices') }}</p>
      </a-card>

      <a-card class="p-5">
        <div class="mb-3 flex items-center justify-between">
          <h3 class="text-sm font-medium text-[var(--foreground)]">{{ $t('dashboard.recentLogs') }}</h3>
          <button class="text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]" @click="router.push('/monitor/oper-log')">{{ $t('common.viewAll') }}</button>
        </div>
        <div v-if="operLogs.length" class="divide-y divide-[var(--border)]">
          <div v-for="item in operLogs" :key="item.id" class="py-3">
            <div class="flex items-center gap-2">
              <a-tag :color="businessTypeVariant(item.businessType) === 'default' ? 'arcoblue' : businessTypeVariant(item.businessType) === 'destructive' ? 'red' : 'arcoblue'" class="text-xs">{{ businessTypeText(item.businessType) }}</a-tag>
              <span class="text-sm text-[var(--foreground)]">{{ item.title }}</span>
            </div>
            <p class="mt-1 text-xs text-[var(--muted-foreground)]">
              {{ item.operUser }} · {{ item.requestMethod }} ·
              <span :class="item.status === 0 ? 'text-green-600' : 'text-red-500'">{{ operStatusText(item.status) }}</span>
              · {{ item.costTime }}ms
            </p>
          </div>
        </div>
        <p v-else class="py-8 text-center text-sm text-[var(--muted-foreground)]">{{ $t('dashboard.noLogs') }}</p>
      </a-card>
    </div>


    <!-- 公告详情弹窗 -->
    <a-modal v-model:visible="noticeDialogVisible" :title="currentNotice?.noticeTitle || $t('dashboard.noticeDetail')" @cancel="noticeDialogVisible = false">
      <div v-if="currentNotice" class="notice-detail">
        <div class="notice-detail-meta">
          <a-tag :color="currentNotice.noticeType === '1' ? 'arcoblue' : 'purple'">
            {{ currentNotice.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}
          </a-tag>
          <a-tag v-if="currentNotice.status === '1'" color="arcoblue">{{ $t('notice.noticeStatusPublished') }}</a-tag>
          <a-tag v-else-if="currentNotice.status === '0'" bordered>{{ $t('notice.noticeStatusDraft') }}</a-tag>
          <a-tag v-else color="red">{{ $t('notice.noticeStatusRevoked') }}</a-tag>
          <span class="notice-detail-time">{{ formatDateTime(currentNotice.createTime) }}</span>
        </div>
        <a-divider class="my-4" />
        <div class="notice-detail-content" v-html="sanitizeHtml(currentNotice.noticeContent)"></div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import { formatDateTime, businessTypeText, operStatusText, businessTypeVariant } from '@/utils/format'
import {
  IconUser,
  IconUserGroup,
  IconApps,
  IconEye,
  IconFile,
  IconNotification,
} from '@arco-design/web-vue/es/icon'
import { getUserListApi } from '@/api/user'
import { getRoleListApi } from '@/api/role'
import { getMenuListApi } from '@/api/menu'
import { getOnlineUserCountApi } from '@/api/online'
import { getPublishedNoticesApi, getNoticePageApi, type SysNotice } from '@/api/notice'
import { getOperLogPageApi, type SysOperLog } from '@/api/operLog'
import { getDashboardChartsApi } from '@/api/dashboard'
import { sanitizeHtml } from '@/utils/dom'

const { t } = useI18n()
const router = useRouter()
const loading = ref(true)

// ============ 统计卡片 ============
const statCards = ref([
  { title: t('dashboard.userCount'), titleKey: 'dashboard.userCount', key: 'userCount', value: '-', icon: IconUser, color: 'var(--primary)' },
  { title: t('dashboard.roleCount'), titleKey: 'dashboard.roleCount', key: 'roleCount', value: '-', icon: IconUserGroup, color: '#00B42A' },
  { title: t('dashboard.menuCount'), titleKey: 'dashboard.menuCount', key: 'menuCount', value: '-', icon: IconApps, color: '#FF7D00' },
  { title: t('dashboard.onlineCount'), titleKey: 'dashboard.onlineCount', key: 'onlineCount', value: '-', icon: IconEye, color: '#722ED1' },
  { title: t('dashboard.noticeCount'), titleKey: 'dashboard.noticeCount', key: 'noticeCount', value: '-', icon: IconNotification, color: '#F7BA1E' },
])

function setStat(key: string, val: string) {
  const card = statCards.value.find((c) => c.key === key)
  if (card) card.value = val
}

// ============ 公告 ============
const notices = ref<SysNotice[]>([])
const noticeDialogVisible = ref(false)
const currentNotice = ref<SysNotice | null>(null)

function handleNoticeClick(item: SysNotice) {
  currentNotice.value = item
  noticeDialogVisible.value = true
}

// ============ 操作日志 ============
const operLogs = ref<SysOperLog[]>([])

// ============ 图表 ============
const lineChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

function initLineChart(visitTrend: { date: string; count: number }[]) {
  if (!lineChartRef.value) return
  lineChart = echarts.init(lineChartRef.value)
  const days = visitTrend.map((d) => d.date.substring(5))
  const counts = visitTrend.map((d) => d.count)
  lineChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: '#FFFFFF', borderColor: '#E5E6EB', textStyle: { color: '#1D2129' } },
    legend: { data: [t('dashboard.visitTrend')], top: 0, right: 0, textStyle: { fontSize: 12, color: '#86909C' } },
    grid: { left: 40, right: 20, bottom: 30, top: 40 },
    xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: '#E5E6EB' } }, axisLabel: { color: '#86909C' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F2F3F5' } }, axisLabel: { color: '#86909C' } },
    series: [
      {
        name: t('dashboard.visitTrend'),
        type: 'line',
        smooth: true,
        data: counts,
        lineStyle: { width: 2, color: 'var(--primary)' },
        areaStyle: { color: 'rgba(51,112,255,0.12)' },
        itemStyle: { color: 'var(--primary)' },
        symbol: 'circle',
        symbolSize: 6,
      },
    ],
  })
}

function initPieChart(roleDistribution: { name: string; value: number }[]) {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  const colors = ['var(--primary)', '#00B42A', '#FF7D00', '#722ED1', '#F53F3F', '#F7BA1E', '#0FC6C2']
  const data = roleDistribution.map((item, i) => ({
    value: item.value,
    name: item.name,
    itemStyle: { color: colors[i % colors.length] },
  }))
  pieChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: '#FFFFFF', borderColor: '#E5E6EB', textStyle: { color: '#1D2129' } },
    legend: { orient: 'horizontal', bottom: 0, textStyle: { fontSize: 12, color: '#4E5969' } },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' },
        },
        data,
      },
    ],
  })
}

// ============ 数据加载 ============

async function loadStats() {
  try {
    const [usersRes, rolesRes, menusRes, onlineRes, noticesRes] = await Promise.allSettled([
      getUserListApi({ pageNum: 1, pageSize: 1 }),
      getRoleListApi({ pageNum: 1, pageSize: 1 }),
      getMenuListApi(),
      getOnlineUserCountApi(),
      getNoticePageApi({ pageNum: 1, pageSize: 1 }),
    ])

    if (usersRes.status === 'fulfilled') setStat('userCount', String(usersRes.value.data.total ?? '-'))
    if (rolesRes.status === 'fulfilled') setStat('roleCount', String(rolesRes.value.data.total ?? '-'))
    if (menusRes.status === 'fulfilled') setStat('menuCount', String(Array.isArray(menusRes.value.data) ? menusRes.value.data.length : '-'))
    if (onlineRes.status === 'fulfilled') setStat('onlineCount', String(onlineRes.value.data ?? '-'))
    if (noticesRes.status === 'fulfilled') setStat('noticeCount', String(noticesRes.value.data?.total ?? '-'))
  } catch (err) {
    console.error('dashboard load error:', err)
  }
}

async function loadNotices() {
  try {
    const res = await getPublishedNoticesApi()
    notices.value = (res.data || []).slice(0, 6)
  } catch (err) {
    console.error('dashboard load error:', err)
  }
}

async function loadOperLogs() {
  try {
    const res = await getOperLogPageApi({ pageNum: 1, pageSize: 6 })
    operLogs.value = res.data?.list || []
  } catch (err) {
    console.error('dashboard load error:', err)
  }
}

async function loadCharts() {
  try {
    const res = await getDashboardChartsApi()
    const data = res.data
    if (data) {
      await nextTick()
      initLineChart(data.visitTrend)
      initPieChart(data.roleDistribution)
    }
  } catch (err) {
    console.error('dashboard load error:', err)
  }
}

// ============ 生命周期 ============
let resizeHandler: (() => void) | null = null

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      loadStats(),
      loadNotices(),
      loadOperLogs(),
      loadCharts(),
    ])
  } catch (err) {
    console.error('dashboard load error:', err)
  } finally {
    loading.value = false
  }

  resizeHandler = () => {
    lineChart?.resize()
    pieChart?.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 0;
  min-height: 100%;
}

/* ---- 图表 ---- */
.chart-container {
  width: 100%;
  height: 300px;
}

/* ---- 公告列表 ---- */
.notice-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.notice-title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

/* ---- 操作日志列表 ---- */
.operlog-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.operlog-title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}
.operlog-desc {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--muted-foreground);
  margin-top: 4px;
}
.operlog-dot {
  color: var(--muted-foreground);
}

/* ---- 公告详情弹窗 ---- */
.notice-detail-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
.notice-detail-time {
  font-size: 12px;
  color: var(--muted-foreground);
}
.notice-detail-content {
  line-height: 1.8;
  color: var(--foreground);
  font-size: 14px;
}
</style>
