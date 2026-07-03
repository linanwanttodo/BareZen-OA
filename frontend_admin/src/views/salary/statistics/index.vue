<template>
  <div class="page-container">
    <!-- 筛选 -->
    <t-card class="mb-4">
      <t-form :data="searchForm" :label-width="80" :on-submit="loadData">
        <t-form-item name="paymentMonth" :label="$t('salary.paymentMonth')">
          
<t-date-picker v-model="searchForm.paymentMonth" mode="month" format="YYYY-MM" value-type="YYYY-MM" clearable :placeholder="$t('common.all')" />
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" html-type="submit">{{ $t('common.search') }}</t-button>
            <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-card>

    <!-- 汇总卡片 -->
    <t-row :gutter="16" class="mb-4">
      <t-col :span="3">
        <t-card>
          <t-statistic :title="$t('salary.statHeadcount')" :value="stats.summary?.headcount ?? 0" />
        </t-card>
      </t-col>
      <t-col :span="3">
        <t-card>
          <t-statistic :title="$t('salary.statTotalIncome')" :value="money(stats.summary?.totalIncome)" :precision="2" prefix="¥" />
        </t-card>
      </t-col>
      <t-col :span="3">
        <t-card>
          <t-statistic :title="$t('salary.statTotalDeduction')" :value="money(stats.summary?.totalDeduction)" :precision="2" prefix="¥" />
        </t-card>
      </t-col>
      <t-col :span="3">
        <t-card>
          <t-statistic :title="$t('salary.statTotalActual')" :value="money(stats.summary?.totalActual)" :precision="2" prefix="¥" />
        </t-card>
      </t-col>
    </t-row>

    <!-- 图表 + 明细 -->
    <t-row :gutter="16">
      <t-col :span="7">
        <t-card :title="$t('salary.monthlyTrend')">
          <div ref="chartRef" style="height: 360px"></div>
        </t-card>
      </t-col>
      <t-col :span="5">
        <t-card :title="$t('salary.monthlyDetail')">
          <t-table row-key="paymentMonth" :data="stats.monthly || []" :columns="tableColumns" :pagination="false" size="small" :hover="true" vertical-align="top">
    <template #paidRatio="{ row }">
                <t-tag :theme="row.headcount > 0 && row.paidCount === row.headcount ? 'success' : row.paidCount > 0 ? 'warning' : 'default'" variant="light">
                  {{ row.headcount > 0 ? Math.round((row.paidCount / row.headcount) * 100) : 0 }}%
                </t-tag>
              </template>
  </t-table>
        </t-card>
      </t-col>
    </t-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted, onActivated, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getSalaryStatisticsApi, type SalaryStatistics } from '@/api/salary'

echarts.use([BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const { t } = useI18n()
const loading = ref(false)
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const searchForm = reactive({ paymentMonth: '' })
const stats = ref<SalaryStatistics>({ summary: { headcount: 0, paidCount: 0, unpaidCount: 0, totalIncome: 0, totalDeduction: 0, totalActual: 0 }, monthly: [] })

function money(v: number | undefined): number {
  return Number(v ?? 0)
}

/** ECharts 画布不消费 CSS 变量，运行时解析主题 token */
function cssVar(name: string, fallback: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}

const tableColumns = computed(() => [
  { colKey: 'paymentMonth', title: t('salary.paymentMonth'), width: 90 },
  { colKey: 'headcount', title: t('salary.statHeadcount'), width: 70, align: 'center' },
  { colKey: 'totalActual', title: t('salary.statTotalActual'), align: 'right' },
  { colKey: 'paidRatio', title: t('salary.paidRatio'), width: 80 },
])

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const primary = cssVar('--td-brand-color', '#0052d9')
  const success = cssVar('--td-success-color', '#2ba471')
  const text = cssVar('--td-text-color-secondary', 'rgba(0,0,0,0.6)')
  const axis = cssVar('--td-border-level-2-color', '#e7e7e7')
  const monthly = stats.value.monthly || []
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: [t('salary.totalIncome'), t('salary.statTotalActual')], textStyle: { color: text } },
    grid: { left: 60, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: monthly.map((m) => m.paymentMonth), axisLine: { lineStyle: { color: axis } }, axisLabel: { color: text } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: axis } }, axisLabel: { color: text } },
    series: [
      { name: t('salary.totalIncome'), type: 'bar', data: monthly.map((m) => Number(m.totalIncome)), itemStyle: { color: primary, borderRadius: [4, 4, 0, 0] } },
      { name: t('salary.statTotalActual'), type: 'bar', data: monthly.map((m) => Number(m.totalActual)), itemStyle: { color: success, borderRadius: [4, 4, 0, 0] } },
    ],
  })
}

async function loadData() {
  loading.value = true
  try {
    const res = await getSalaryStatisticsApi(searchForm.paymentMonth || undefined)
    stats.value = res.data || stats.value
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

function handleReset() {
  searchForm.paymentMonth = ''
  loadData()
}

function handleResize() {
  chart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})
onActivated(() => {
  loadData()
  nextTick(() => chart?.resize())
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>
