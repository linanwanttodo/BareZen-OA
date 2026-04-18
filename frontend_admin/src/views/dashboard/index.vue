<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <Card v-for="card in statCards" :key="card.title" class="p-5">
        <div class="stat-card__inner">
          <div class="stat-card__info">
            <p class="stat-card__title">{{ card.title }}</p>
            <p class="stat-card__value">{{ card.value }}</p>
          </div>
          <div class="stat-card__icon" :style="{ backgroundColor: card.bgColor }">
            <component :is="card.icon" :size="24" :style="{ color: card.iconColor }" />
          </div>
        </div>
      </Card>
    </div>

    <!-- 图表区域 -->
    <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-12">
      <Card class="p-5 lg:col-span-8">
        <div class="mb-2 flex items-baseline justify-between">
          <h3 class="text-base font-semibold text-[var(--foreground)]">访问趋势</h3>
          <span class="text-xs text-[var(--muted-foreground)]">近7天</span>
        </div>
        <div ref="lineChartRef" class="chart-container"></div>
      </Card>
      <Card class="p-5 lg:col-span-4">
        <div class="mb-2 flex items-baseline justify-between">
          <h3 class="text-base font-semibold text-[var(--foreground)]">用户分布</h3>
          <span class="text-xs text-[var(--muted-foreground)]">按角色</span>
        </div>
        <div ref="pieChartRef" class="chart-container"></div>
      </Card>
    </div>

    <!-- 公告 + 最近操作 -->
    <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card class="p-5">
        <div class="mb-3 flex items-center justify-between">
          <h3 class="text-base font-semibold text-[var(--foreground)]">最新公告</h3>
          <Button variant="ghost" size="sm" @click="router.push('/notice')">查看更多</Button>
        </div>
        <ul v-if="notices.length" class="divide-y divide-[var(--border)]">
          <li v-for="item in notices" :key="item.id" class="py-3">
            <div class="notice-title-row">
              <Badge :variant="item.noticeType === '1' ? 'default' : 'secondary'">
                {{ item.noticeType === '1' ? '通知' : '公告' }}
              </Badge>
              <span class="notice-title-text" @click="handleNoticeClick(item)">{{ item.noticeTitle }}</span>
            </div>
            <span class="mt-1 block text-xs text-[var(--muted-foreground)]">{{ formatTime(item.createTime) }}</span>
          </li>
        </ul>
        <Empty v-else description="暂无公告" />
      </Card>

      <Card class="p-5">
        <div class="mb-3 flex items-center justify-between">
          <h3 class="text-base font-semibold text-[var(--foreground)]">最近操作</h3>
          <Button variant="ghost" size="sm" @click="router.push('/monitor/oper-log')">查看更多</Button>
        </div>
        <ul v-if="operLogs.length" class="divide-y divide-[var(--border)]">
          <li v-for="item in operLogs" :key="item.id" class="py-3">
            <div class="operlog-title-row">
              <Badge :variant="businessTypeVariant(item.businessType)">{{ businessTypeText(item.businessType) }}</Badge>
              <span class="operlog-title-text">{{ item.title }}</span>
            </div>
            <div class="operlog-desc">
              <span>{{ item.operUser }}</span>
              <span class="operlog-dot">·</span>
              <span>{{ item.requestMethod }}</span>
              <span class="operlog-dot">·</span>
              <span :class="item.status === 0 ? 'text-green-600' : 'text-red-500'">{{ item.status === 0 ? '成功' : '失败' }}</span>
              <span class="operlog-dot">·</span>
              <span>{{ item.costTime }}ms</span>
            </div>
          </li>
        </ul>
        <Empty v-else description="暂无操作记录" />
      </Card>
    </div>

    <!-- 系统信息 -->
    <div class="mt-4">
      <Card class="p-5">
        <h3 class="mb-3 text-base font-semibold text-[var(--foreground)]">系统信息</h3>
        <dl class="grid grid-cols-2 gap-4 sm:grid-cols-3">
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">框架版本</dt>
            <dd class="mt-1"><Badge variant="default">1.0.0</Badge></dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">后端框架</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">Spring Boot 3.2+</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">前端框架</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">Vue 3 + shadcn</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">Java 版本</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">Java 21</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">数据库</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">MySQL 8.0</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">鉴权机制</dt>
            <dd class="mt-1"><Badge variant="default">JWT</Badge></dd>
          </div>
        </dl>
      </Card>
    </div>

    <!-- 公告详情弹窗 -->
    <Dialog :open="noticeDialogVisible" :title="currentNotice?.noticeTitle || '公告详情'" @update:open="noticeDialogVisible = $event">
      <div v-if="currentNotice" class="notice-detail">
        <div class="notice-detail-meta">
          <Badge :variant="currentNotice.noticeType === '1' ? 'default' : 'secondary'">
            {{ currentNotice.noticeType === '1' ? '通知' : '公告' }}
          </Badge>
          <Badge :variant="currentNotice.status === '1' ? 'default' : currentNotice.status === '0' ? 'outline' : 'destructive'">
            {{ currentNotice.status === '1' ? '已发布' : currentNotice.status === '0' ? '草稿' : '已下架' }}
          </Badge>
          <span class="notice-detail-time">{{ formatTime(currentNotice.createTime) }}</span>
        </div>
        <Separator class="my-4" />
        <div class="notice-detail-content" v-html="currentNotice.noticeContent"></div>
      </div>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
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
import Card from '@/components/ui/card/Card.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import Button from '@/components/ui/button/Button.vue'
import Empty from '@/components/ui/empty/Empty.vue'
import Separator from '@/components/ui/separator/Separator.vue'
import Dialog from '@/components/ui/dialog/Dialog.vue'

const router = useRouter()

type BadgeVariant = 'default' | 'secondary' | 'destructive' | 'outline'

// ============ 统计卡片 ============
const statCards = ref([
  { title: '用户总数', value: '-', icon: IconUser, bgColor: '#E8F3FF', iconColor: '#3370FF' },
  { title: '角色数量', value: '-', icon: IconUserGroup, bgColor: '#E8FFEA', iconColor: '#00B42A' },
  { title: '菜单数量', value: '-', icon: IconApps, bgColor: '#FFF7E8', iconColor: '#FF7D00' },
  { title: '在线用户', value: '-', icon: IconEye, bgColor: '#F0E5FF', iconColor: '#722ED1' },
  { title: '文章总数', value: '-', icon: IconFile, bgColor: '#FFF7E8', iconColor: '#FF7D00' },
  { title: '公告数量', value: '-', icon: IconNotification, bgColor: '#E8F3FF', iconColor: '#3370FF' },
])

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

function businessTypeText(type: number) {
  const map: Record<number, string> = { 0: '其它', 1: '新增', 2: '修改', 3: '删除' }
  return map[type] || '其它'
}

function businessTypeVariant(type: number): BadgeVariant {
  const map: Record<number, BadgeVariant> = { 0: 'outline', 1: 'default', 2: 'secondary', 3: 'destructive' }
  return map[type] || 'outline'
}

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
    legend: { data: ['操作次数'], top: 0, right: 0, textStyle: { fontSize: 12, color: '#86909C' } },
    grid: { left: 40, right: 20, bottom: 30, top: 40 },
    xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: '#E5E6EB' } }, axisLabel: { color: '#86909C' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F2F3F5' } }, axisLabel: { color: '#86909C' } },
    series: [
      {
        name: '操作次数',
        type: 'line',
        smooth: true,
        data: counts,
        lineStyle: { width: 2, color: '#3370FF' },
        areaStyle: { color: 'rgba(51,112,255,0.12)' },
        itemStyle: { color: '#3370FF' },
        symbol: 'circle',
        symbolSize: 6,
      },
    ],
  })
}

function initPieChart(roleDistribution: { name: string; value: number }[]) {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  const colors = ['#3370FF', '#00B42A', '#FF7D00', '#722ED1', '#F53F3F', '#F7BA1E', '#0FC6C2']
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
function formatTime(time: string) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

async function loadStats() {
  try {
    const [usersRes, rolesRes, menusRes, onlineRes, chartsRes, noticesRes] = await Promise.allSettled([
      getUserListApi({ pageNum: 1, pageSize: 1 }),
      getRoleListApi({ pageNum: 1, pageSize: 1 }),
      getMenuListApi(),
      getOnlineUserCountApi(),
      getDashboardChartsApi(),
      getNoticePageApi({ pageNum: 1, pageSize: 1 }),
    ])

    if (usersRes.status === 'fulfilled') statCards.value[0].value = String(usersRes.value.data.total ?? '-')
    if (rolesRes.status === 'fulfilled') statCards.value[1].value = String(rolesRes.value.data.total ?? '-')
    if (menusRes.status === 'fulfilled') statCards.value[2].value = String((menusRes.value.data as any)?.length ?? '-')
    if (onlineRes.status === 'fulfilled') statCards.value[3].value = String(onlineRes.value.data ?? '-')
    if (chartsRes.status === 'fulfilled') {
      const chartData = chartsRes.value.data
      statCards.value[4].value = String(chartData?.articleCount ?? '-')
    }
    if (noticesRes.status === 'fulfilled') statCards.value[5].value = String(noticesRes.value.data?.total ?? '-')
  } catch {
    /* ignore */
  }
}

async function loadNotices() {
  try {
    const res = await getPublishedNoticesApi()
    notices.value = (res.data || []).slice(0, 6)
  } catch {
    /* ignore */
  }
}

async function loadOperLogs() {
  try {
    const res = await getOperLogPageApi({ pageNum: 1, pageSize: 6 })
    operLogs.value = res.data?.records || []
  } catch {
    /* ignore */
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
  } catch {
    /* ignore */
  }
}

// ============ 生命周期 ============
let resizeHandler: (() => void) | null = null

onMounted(async () => {
  loadStats()
  loadNotices()
  loadOperLogs()
  loadCharts()

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

/* ---- 统计卡片 ---- */
.stat-card__inner {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.stat-card__title {
  font-size: 13px;
  color: var(--muted-foreground);
  margin-bottom: 8px;
  font-weight: 500;
}
.stat-card__value {
  font-size: 28px;
  font-weight: 700;
  color: var(--foreground);
  line-height: 1.2;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}
.stat-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
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
