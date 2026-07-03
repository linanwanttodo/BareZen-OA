<template>
  <div v-if="loading" class="flex items-center justify-center h-64">
    <t-loading size="28px" :text="$t('common.loading')" />
  </div>
  <div v-else class="dashboard-container">
    <!-- 关键指标：t-row/t-col 栅格 + 首卡反色 + 圆形图标徽标（对齐 TDesign Starter dashboard/top-panel） -->
    <t-row :gutter="[16, 16]">
      <t-col v-for="(card, i) in statCards" :key="card.key" :xs="6" :xl="3">
        <t-card
          :title="$t(card.titleKey)"
          :bordered="false"
          class="metric-card"
          :class="{ 'metric-card--main': i === 0 }"
          @click="router.push(card.route)"
        >
          <div class="metric-card__value">{{ card.value }}</div>
          <span class="metric-card__badge">
            <component :is="card.icon" />
          </span>
          <template #footer>
            <div class="metric-card__footer">
              <span class="metric-card__hint">{{ $t('common.detail') }}</span>
              <chevron-right-icon class="metric-card__arrow" />
            </div>
          </template>
        </t-card>
      </t-col>
    </t-row>

    <!-- 图表区域 -->
    <t-row :gutter="[16, 16]" class="mt-4">
      <t-col :xs="12" :lg="8">
        <t-card
          class="panel-card"
          :bordered="false"
          :title="$t('dashboard.visitTrend')"
          :subtitle="$t('dashboard.last7Days')"
        >
          <div ref="lineChartRef" class="chart-container"></div>
        </t-card>
      </t-col>
      <t-col :xs="12" :lg="4">
        <t-card
          class="panel-card"
          :bordered="false"
          :title="$t('dashboard.userDistribution')"
          :subtitle="$t('dashboard.byRole')"
        >
          <div ref="pieChartRef" class="chart-container"></div>
        </t-card>
      </t-col>
    </t-row>

    <!-- 审批效率 -->
    <t-card
      class="mt-4 panel-card"
      :bordered="false"
      :title="$t('dashboard.approvalStats')"
      :subtitle="$t('dashboard.approvalStatsHint')"
    >
      <t-table
        v-if="approvalStats.length"
        row-key="type"
        :data="approvalStats"
        :columns="approvalColumns"
        :pagination="false"
        :hover="true"
        vertical-align="top"
      >
        <template #pending="{ row }">
          <span :class="row.pending > 0 ? 'approval-pending--active' : ''">{{ row.pending }}</span>
        </template>
        <template #avgHours="{ row }">{{ row.avgHours }}h</template>
        <template #rejectRate="{ row }">{{ row.rejectRate }}%</template>
      </t-table>
      <p v-else class="py-6 text-center text-sm text-[var(--td-text-color-placeholder)]">{{ $t('dashboard.noApprovalStats') }}</p>
    </t-card>

    <!-- 公告 + 最近操作 -->
    <t-row :gutter="[16, 16]" class="mt-4">
      <t-col :xs="12" :lg="6">
        <t-card class="panel-card" :bordered="false" :title="$t('dashboard.latestNotices')">
          <template #actions>
            <t-link theme="primary" hover="color" class="text-xs" @click="router.push('/notice')">{{ $t('common.viewAll') }}</t-link>
          </template>
          <div v-if="notices.length" class="divide-y divide-[var(--border)]">
            <div v-for="item in notices" :key="item.id" class="flex items-center justify-between py-3">
              <div class="flex items-center gap-2 min-w-0">
                <t-tag :theme="item.noticeType === '1' ? 'primary' : 'default'" variant="light" class="shrink-0 text-xs">
                  {{ item.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}
                </t-tag>
                <span class="truncate text-sm text-[var(--foreground)]" @click="handleNoticeClick(item)">{{ item.noticeTitle }}</span>
              </div>
              <span class="shrink-0 text-xs text-[var(--muted-foreground)]">{{ formatDateTime(item.createTime) }}</span>
            </div>
          </div>
          <p v-else class="py-8 text-center text-sm text-[var(--muted-foreground)]">{{ $t('dashboard.noNotices') }}</p>
        </t-card>
      </t-col>

      <t-col :xs="12" :lg="6">
        <t-card class="panel-card" :bordered="false" :title="$t('dashboard.recentLogs')">
          <template #actions>
            <t-link theme="primary" hover="color" class="text-xs" @click="router.push('/monitor/oper-log')">{{ $t('common.viewAll') }}</t-link>
          </template>
          <div v-if="operLogs.length" class="divide-y divide-[var(--border)]">
            <div v-for="item in operLogs" :key="item.id" class="py-3">
              <div class="flex items-center gap-2">
                <t-tag :theme="businessTypeVariant(item.businessType)" variant="light" class="text-xs">{{ businessTypeText(item.businessType) }}</t-tag>
                <span class="text-sm text-[var(--foreground)]">{{ item.title }}</span>
              </div>
              <p class="mt-1 text-xs text-[var(--muted-foreground)]">
                {{ item.operUser }} · {{ item.requestMethod }} ·
                <span :class="item.status === 0 ? 'text-[var(--td-success-color)]' : 'text-[var(--td-error-color)]'">{{ operStatusText(item.status) }}</span>
                · {{ item.costTime }}ms
              </p>
            </div>
          </div>
          <p v-else class="py-8 text-center text-sm text-[var(--muted-foreground)]">{{ $t('dashboard.noLogs') }}</p>
        </t-card>
      </t-col>
    </t-row>


    <!-- 公告详情弹窗 -->
    <t-dialog v-model:visible="noticeDialogVisible" :header="currentNotice?.noticeTitle || $t('dashboard.noticeDetail')" @close="noticeDialogVisible = false">
      <div v-if="currentNotice" class="notice-detail">
        <div class="notice-detail-meta">
          <t-tag :theme="currentNotice.noticeType === '1' ? 'primary' : 'default'">
            {{ currentNotice.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}
          </t-tag>
          <t-tag v-if="currentNotice.status === '1'" theme="primary">{{ $t('notice.noticeStatusPublished') }}</t-tag>
          <t-tag v-else-if="currentNotice.status === '0'" bordered>{{ $t('notice.noticeStatusDraft') }}</t-tag>
          <t-tag v-else theme="danger">{{ $t('notice.noticeStatusRevoked') }}</t-tag>
          <span class="notice-detail-time">{{ formatDateTime(currentNotice.createTime) }}</span>
        </div>
        <t-divider class="my-4" />
        <div class="notice-detail-content" v-html="sanitizeHtml(currentNotice.noticeContent)"></div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, onActivated, nextTick } from 'vue'
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
  ChevronRightIcon,
} from 'tdesign-icons-vue-next'
echarts.use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])
import { formatDateTime, businessTypeText, operStatusText, businessTypeVariant } from '@/utils/format'
import { getUserListApi } from '@/api/user'
import { getRoleListApi } from '@/api/role'
import { getMenuListApi } from '@/api/menu'
import { getOnlineUserCountApi } from '@/api/online'
import { getPublishedNoticesApi, getNoticePageApi, type SysNotice } from '@/api/notice'
import { getOperLogPageApi, type SysOperLog } from '@/api/operLog'
import { getDashboardChartsApi, type ApprovalStat, type ChartData } from '@/api/dashboard'
import { sanitizeHtml } from '@/utils/dom'

const { t } = useI18n()
const router = useRouter()
const loading = ref(true)

// ============ 统计卡片 ============
// route 取自 sys_menu 中真实存在的菜单 path，整卡点击直达对应管理页
const statCards = ref([
  { title: t('dashboard.userCount'), titleKey: 'dashboard.userCount', key: 'userCount', value: '-', icon: IconUser, route: '/system/user' },
  { title: t('dashboard.roleCount'), titleKey: 'dashboard.roleCount', key: 'roleCount', value: '-', icon: IconUserGroup, route: '/system/role' },
  { title: t('dashboard.menuCount'), titleKey: 'dashboard.menuCount', key: 'menuCount', value: '-', icon: IconApps, route: '/system/menu' },
  { title: t('dashboard.onlineCount'), titleKey: 'dashboard.onlineCount', key: 'onlineCount', value: '-', icon: IconEye, route: '/monitor/online' },
  { title: t('dashboard.noticeCount'), titleKey: 'dashboard.noticeCount', key: 'noticeCount', value: '-', icon: IconNotification, route: '/notice' },
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

// 插槽名必须与 colKey 同名，否则 TDesign 静默不渲染该列的自定义内容
const approvalColumns = computed(() => [
  { colKey: 'typeName', title: t('dashboard.approvalType'), ellipsis: true },
  { colKey: 'pending', title: t('dashboard.pendingApproval'), width: 120 },
  { colKey: 'avgHours', title: t('dashboard.avgHours'), width: 180 },
  { colKey: 'rejectRate', title: t('dashboard.rejectRate'), width: 120 },
])

// ============ 图表（企业微信色板，ECharts 不解析 CSS 变量，使用字面量） ============
const lineChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

// 图表色值从 TDesign token 运行时解析（ECharts 画布无法直接消费 CSS 变量），
// 保持与全局主题单一来源一致；解析失败时回退到同值常量。
// 每次调用重新 getComputedStyle，暗色切换后可取到更新后的 token。
function cssVar(name: string, fallback: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}
/** hex → rgba（用于面积图淡色填充） */
function withAlpha(hex: string, alpha: number): string {
  const n = hex.replace('#', '')
  const r = parseInt(n.slice(0, 2), 16)
  const g = parseInt(n.slice(2, 4), 16)
  const b = parseInt(n.slice(4, 6), 16)
  return `rgba(${r},${g},${b},${alpha})`
}

function chartPalette() {
  return {
    primary: cssVar('--td-brand-color-7', '#2B7CE9'),
    text: cssVar('--td-text-color-placeholder', '#8C8C8C'),
    textStrong: cssVar('--td-text-color-primary', '#262626'),
    border: cssVar('--td-component-stroke', '#E8E8E8'),
    split: cssVar('--td-bg-color-page', '#F5F5F5'),
    bg: cssVar('--td-bg-color-container', '#FFFFFF'),
    // 饼图：品牌色阶渐进 + 中性灰收尾，避免彩虹配色
    series: [
      cssVar('--td-brand-color-7', '#2B7CE9'),
      cssVar('--td-brand-color-5', '#5595EC'),
      cssVar('--td-brand-color-3', '#A9CDF7'),
      cssVar('--td-brand-color-1', '#F0F7FF'),
      cssVar('--td-text-color-secondary', '#595959'),
      cssVar('--td-text-color-placeholder', '#8C8C8C'),
      cssVar('--td-text-color-disabled', '#BFBFBF'),
    ],
  }
}

function initLineChart(visitTrend: { date: string; count: number }[]) {
  if (!lineChartRef.value) return
  lineChart = echarts.init(lineChartRef.value)
  const P = chartPalette()
  const days = visitTrend.map((d) => d.date.substring(5))
  const counts = visitTrend.map((d) => d.count)
  lineChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: P.bg, borderColor: P.border, textStyle: { color: P.textStrong } },
    legend: { data: [t('dashboard.visitTrend')], top: 0, right: 0, textStyle: { fontSize: 12, color: P.text } },
    grid: { left: 40, right: 20, bottom: 30, top: 40 },
    xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: P.border } }, axisLabel: { color: P.text } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: P.split } }, axisLabel: { color: P.text } },
    series: [
      {
        name: t('dashboard.visitTrend'),
        type: 'line',
        smooth: true,
        data: counts,
        lineStyle: { width: 2, color: P.primary },
        areaStyle: { color: withAlpha(P.primary, 0.08) },
        itemStyle: { color: P.primary },
        symbol: 'circle',
        symbolSize: 6,
      },
    ],
  })
}

function initPieChart(roleDistribution: { name: string; value: number }[]) {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  const P = chartPalette()
  const data = roleDistribution.map((item, i) => ({
    value: item.value,
    name: item.name,
    itemStyle: { color: P.series[i % P.series.length] },
  }))
  pieChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: P.bg, borderColor: P.border, textStyle: { color: P.textStrong } },
    legend: { orient: 'horizontal', bottom: 0, textStyle: { fontSize: 12, color: P.text } },
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

// 图表数据缓存，主题切换后可用新 token 重绘
const chartData = ref<ChartData | null>(null)

async function loadCharts() {
  try {
    const res = await getDashboardChartsApi()
    const data = res.data
    if (data) {
      approvalStats.value = data.approvalStats || []
      chartData.value = data
      await nextTick()
      renderCharts()
    }
  } catch (err) {
    console.error('dashboard load error:', err)
  }
}

/** 用缓存数据重绘图表（主题切换后 CSS token 变化，需重新解析色值） */
function renderCharts() {
  const data = chartData.value
  if (!data) return
  lineChart?.dispose()
  pieChart?.dispose()
  lineChart = null
  pieChart = null
  initLineChart(data.visitTrend || [])
  initPieChart(data.roleDistribution || [])
}

// ============ 生命周期 ============
let resizeHandler: (() => void) | null = null
let themeHandler: (() => void) | null = null

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
  // 主题切换时以新 token 重绘图表（ECharts 画布不响应 CSS 变量变化）
  themeHandler = () => { renderCharts() }
  window.addEventListener('bz-theme-change', themeHandler)
})

onActivated(async () => {
  // keep-alive 缓存后重新进入时，容器尺寸可能变化，重算图表布局
  await nextTick()
  lineChart?.resize()
  pieChart?.resize()
})

onUnmounted(() => {
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  if (themeHandler) window.removeEventListener('bz-theme-change', themeHandler)
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 0;
  min-height: 100%;
}

/* ---- 审批效率：待办数大于 0 时给出行提示 ---- */
.approval-pending--active {
  color: var(--td-brand-color);
  font-weight: 600;
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
