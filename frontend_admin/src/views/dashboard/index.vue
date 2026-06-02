<template>
  <div v-if="loading" class="flex items-center justify-center h-64">
    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--primary)]"></div>
    <span class="ml-2 text-[var(--muted-foreground)]">{{ $t('common.loading') }}</span>
  </div>
  <div v-else class="dashboard-container">
    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <t-card v-for="card in statCards" :key="card.title" class="p-5">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-[var(--wecom-text-tertiary)]">{{ $t(card.titleKey) }}</p>
            <p class="mt-1 text-2xl font-semibold text-[var(--wecom-text-primary)]">{{ card.value }}</p>
          </div>
          <component :is="card.icon" size="20px" class="text-[var(--wecom-text-disabled)]" />
        </div>
      </t-card>
    </div>

    <!-- 图表区域 -->
    <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-12">
      <t-card class="p-5 lg:col-span-8">
        <div class="mb-2 flex items-baseline justify-between">
          <h3 class="text-base font-semibold text-[var(--foreground)]">{{ $t('dashboard.visitTrend') }}</h3>
          <span class="text-xs text-[var(--muted-foreground)]">{{ $t('dashboard.last7Days') }}</span>
        </div>
        <div ref="lineChartRef" class="chart-container"></div>
      </t-card>
      <t-card class="p-5 lg:col-span-4">
        <div class="mb-2 flex items-baseline justify-between">
          <h3 class="text-base font-semibold text-[var(--foreground)]">{{ $t('dashboard.userDistribution') }}</h3>
          <span class="text-xs text-[var(--muted-foreground)]">{{ $t('dashboard.byRole') }}</span>
        </div>
        <div ref="pieChartRef" class="chart-container"></div>
      </t-card>
    </div>

    <!-- 审批效率 -->
    <t-card class="mt-4 p-5">
      <div class="mb-3 flex items-center justify-between">
        <h3 class="text-base font-semibold text-[var(--foreground)]">{{ $t('dashboard.approvalStats') }}</h3>
        <span class="text-xs text-[var(--muted-foreground)]">{{ $t('dashboard.approvalStatsHint') }}</span>
      </div>
      <div v-if="approvalStats.length" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <div v-for="s in approvalStats" :key="s.type" class="rounded-wecom-md border border-[var(--wecom-border)] p-4">
          <p class="text-sm font-medium text-[var(--wecom-text-primary)]">{{ s.typeName }}</p>
          <p class="mt-2 flex items-baseline gap-3">
            <span class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ s.pending }}</span>
            <span class="text-xs text-[var(--wecom-text-tertiary)]">{{ $t('dashboard.pendingApproval') }}</span>
          </p>
          <p class="mt-2 text-xs text-[var(--wecom-text-tertiary)]">
            {{ $t('dashboard.avgHours') }}: <span class="text-[var(--wecom-text-primary)]">{{ s.avgHours }}h</span>
            · {{ $t('dashboard.rejectRate') }}: <span class="text-[var(--wecom-text-primary)]">{{ s.rejectRate }}%</span>
          </p>
        </div>
      </div>
      <p v-else class="py-6 text-center text-sm text-[var(--wecom-text-tertiary)]">{{ $t('dashboard.noApprovalStats') }}</p>
    </t-card>

    <!-- 公告 + 最近操作 -->
    <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
      <t-card class="p-5">
        <div class="mb-3 flex items-center justify-between">
          <h3 class="text-sm font-medium text-[var(--foreground)]">{{ $t('dashboard.latestNotices') }}</h3>
          <button class="text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]" @click="router.push('/notice')">{{ $t('common.viewAll') }}</button>
        </div>
        <div v-if="notices.length" class="divide-y divide-[var(--border)]">
          <div v-for="item in notices" :key="item.id" class="flex items-center justify-between py-3">
            <div class="flex items-center gap-2 min-w-0">
              <t-tag :color="item.noticeType === '1' ? 'arcoblue' : 'gray'" class="shrink-0 text-xs">
                {{ item.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}
              </t-tag>
              <span class="truncate text-sm text-[var(--foreground)]" @click="handleNoticeClick(item)">{{ item.noticeTitle }}</span>
            </div>
            <span class="shrink-0 text-xs text-[var(--muted-foreground)]">{{ formatDateTime(item.createTime) }}</span>
          </div>
        </div>
        <p v-else class="py-8 text-center text-sm text-[var(--muted-foreground)]">{{ $t('dashboard.noNotices') }}</p>
      </t-card>

      <t-card class="p-5">
        <div class="mb-3 flex items-center justify-between">
          <h3 class="text-sm font-medium text-[var(--foreground)]">{{ $t('dashboard.recentLogs') }}</h3>
          <button class="text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]" @click="router.push('/monitor/oper-log')">{{ $t('common.viewAll') }}</button>
        </div>
        <div v-if="operLogs.length" class="divide-y divide-[var(--border)]">
          <div v-for="item in operLogs" :key="item.id" class="py-3">
            <div class="flex items-center gap-2">
              <t-tag :color="businessTypeVariant(item.businessType) === 'default' ? 'arcoblue' : businessTypeVariant(item.businessType) === 'destructive' ? 'red' : 'arcoblue'" class="text-xs">{{ businessTypeText(item.businessType) }}</t-tag>
              <span class="text-sm text-[var(--foreground)]">{{ item.title }}</span>
            </div>
            <p class="mt-1 text-xs text-[var(--muted-foreground)]">
              {{ item.operUser }} · {{ item.requestMethod }} ·
              <span :class="item.status === 0 ? 'text-[var(--wecom-success)]' : 'text-[var(--wecom-error)]'">{{ operStatusText(item.status) }}</span>
              · {{ item.costTime }}ms
            </p>
          </div>
        </div>
        <p v-else class="py-8 text-center text-sm text-[var(--muted-foreground)]">{{ $t('dashboard.noLogs') }}</p>
      </t-card>
    </div>


    <!-- 公告详情弹窗 -->
    <t-dialog v-model="noticeDialogVisible" :header="currentNotice?.noticeTitle || $t('dashboard.noticeDetail')" @close="noticeDialogVisible = false">
      <div v-if="currentNotice" class="notice-detail">
        <div class="notice-detail-meta">
          <t-tag :color="currentNotice.noticeType === '1' ? 'arcoblue' : 'gray'">
            {{ currentNotice.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}
          </t-tag>
          <t-tag v-if="currentNotice.status === '1'" color="arcoblue">{{ $t('notice.noticeStatusPublished') }}</t-tag>
          <t-tag v-else-if="currentNotice.status === '0'" bordered>{{ $t('notice.noticeStatusDraft') }}</t-tag>
          <t-tag v-else color="red">{{ $t('notice.noticeStatusRevoked') }}</t-tag>
          <span class="notice-detail-time">{{ formatDateTime(currentNotice.createTime) }}</span>
        </div>
        <t-divider class="my-4" />
        <div class="notice-detail-content" v-html="sanitizeHtml(currentNotice.noticeContent)"></div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, onActivated, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  UserIcon as IconUser,
  UsergroupIcon as IconUserGroup,
  AppIcon as IconApps,
  BrowseIcon as IconEye,
  NotificationIcon as IconNotification,
} from 'tdesign-icons-vue-next'
echarts.use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])
import { formatDateTime, businessTypeText, operStatusText, businessTypeVariant } from '@/utils/format'
import { getUserListApi } from '@/api/user'
import { getRoleListApi } from '@/api/role'
import { getMenuListApi } from '@/api/menu'
import { getOnlineUserCountApi } from '@/api/online'
import { getPublishedNoticesApi, getNoticePageApi, type SysNotice } from '@/api/notice'
import { getOperLogPageApi, type SysOperLog } from '@/api/operLog'
import { getDashboardChartsApi, type ApprovalStat } from '@/api/dashboard'
import { sanitizeHtml } from '@/utils/dom'

const { t } = useI18n()
const router = useRouter()
const loading = ref(true)

// ============ 统计卡片 ============
const statCards = ref([
  { title: t('dashboard.userCount'), titleKey: 'dashboard.userCount', key: 'userCount', value: '-', icon: IconUser },
  { title: t('dashboard.roleCount'), titleKey: 'dashboard.roleCount', key: 'roleCount', value: '-', icon: IconUserGroup },
  { title: t('dashboard.menuCount'), titleKey: 'dashboard.menuCount', key: 'menuCount', value: '-', icon: IconApps },
  { title: t('dashboard.onlineCount'), titleKey: 'dashboard.onlineCount', key: 'onlineCount', value: '-', icon: IconEye },
  { title: t('dashboard.noticeCount'), titleKey: 'dashboard.noticeCount', key: 'noticeCount', value: '-', icon: IconNotification },
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

// ============ 审批效率 ============
const approvalStats = ref<ApprovalStat[]>([])

// ============ 图表（企业微信色板，ECharts 不解析 CSS 变量，使用字面量） ============
const lineChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const CHART_PRIMARY = '#2B7CE9'
const CHART_TEXT = '#8C8C8C'
const CHART_TEXT_STRONG = '#262626'
const CHART_BORDER = '#E8E8E8'
const CHART_SPLIT = '#F5F5F5'
// 饼图：蓝色系渐进 + 中性灰收尾，避免彩虹配色
const CHART_SERIES_COLORS = ['#2B7CE9', '#5C9CEF', '#8FBCF5', '#C0DBFA', '#595959', '#8C8C8C', '#BFBFBF']

function initLineChart(visitTrend: { date: string; count: number }[]) {
  if (!lineChartRef.value) return
  lineChart = echarts.init(lineChartRef.value)
  const days = visitTrend.map((d) => d.date.substring(5))
  const counts = visitTrend.map((d) => d.count)
  lineChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: '#FFFFFF', borderColor: CHART_BORDER, textStyle: { color: CHART_TEXT_STRONG } },
    legend: { data: [t('dashboard.visitTrend')], top: 0, right: 0, textStyle: { fontSize: 12, color: CHART_TEXT } },
    grid: { left: 40, right: 20, bottom: 30, top: 40 },
    xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: CHART_BORDER } }, axisLabel: { color: CHART_TEXT } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: CHART_SPLIT } }, axisLabel: { color: CHART_TEXT } },
    series: [
      {
        name: t('dashboard.visitTrend'),
        type: 'line',
        smooth: true,
        data: counts,
        lineStyle: { width: 2, color: CHART_PRIMARY },
        areaStyle: { color: 'rgba(43,124,233,0.08)' },
        itemStyle: { color: CHART_PRIMARY },
        symbol: 'circle',
        symbolSize: 6,
      },
    ],
  })
}

function initPieChart(roleDistribution: { name: string; value: number }[]) {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  const data = roleDistribution.map((item, i) => ({
    value: item.value,
    name: item.name,
    itemStyle: { color: CHART_SERIES_COLORS[i % CHART_SERIES_COLORS.length] },
  }))
  pieChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: '#FFFFFF', borderColor: CHART_BORDER, textStyle: { color: CHART_TEXT_STRONG } },
    legend: { orient: 'horizontal', bottom: 0, textStyle: { fontSize: 12, color: CHART_TEXT } },
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
      approvalStats.value = data.approvalStats || []
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
    ])
  } catch (err) {
    console.error('dashboard load error:', err)
  } finally {
    loading.value = false
  }
  
  // loading 完成后等待 DOM 更新再初始化图表
  await nextTick()
  await loadCharts()

  resizeHandler = () => {
    lineChart?.resize()
    pieChart?.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

onActivated(async () => {
  // keep-alive 缓存后重新进入时，容器尺寸可能变化，重算图表布局
  await nextTick()
  lineChart?.resize()
  pieChart?.resize()
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
