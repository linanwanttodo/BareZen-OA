<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <!-- 头部 -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-[var(--foreground)]">{{ $t('clock.title') }}</h1>
      <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('clock.subtitle') }}</p>
    </div>

    <!-- 今日打卡 -->
    <div class="mb-6 rounded-xl border border-[var(--border)] bg-[var(--card)] p-6">
      <div class="flex flex-col items-center justify-between gap-6 md:flex-row">
        <div class="text-center md:text-left">
          <p class="text-sm text-[var(--muted-foreground)]">{{ todayText }}</p>
          <p class="mt-2 font-mono text-4xl font-semibold tracking-wide text-[var(--foreground)]">{{ nowTime }}</p>
          <div class="mt-3 flex items-center justify-center gap-2 md:justify-start">
            <span class="h-2.5 w-2.5 rounded-full" :class="todayDotClass"></span>
            <span class="text-sm" :class="todayStatusTextClass">{{ todayStatusText }}</span>
          </div>
        </div>
        <div class="flex flex-col items-center gap-3 sm:flex-row">
          <a-button
            size="large"
            type="primary"
            class="min-w-32"
            :disabled="!!todayRecord?.clockInTime"
            :loading="submitting"
            @click="handleClockIn"
          >
            {{ $t('clock.clockIn') }}
          </a-button>
          <a-button
            size="large"
            class="min-w-32"
            :disabled="!todayRecord?.clockInTime || !!todayRecord?.clockOutTime"
            :loading="submitting"
            @click="handleClockOut"
          >
            {{ $t('clock.clockOut') }}
          </a-button>
        </div>
      </div>

      <!-- 今日打卡详情 -->
      <div class="mt-6 grid grid-cols-2 gap-4 border-t border-[var(--border)] pt-5 sm:grid-cols-4">
        <div>
          <p class="text-xs text-[var(--muted-foreground)]">{{ $t('clock.clockedInAt') }}</p>
          <p class="mt-1 text-lg font-medium text-[var(--foreground)]">{{ clockInDisplay }}</p>
        </div>
        <div>
          <p class="text-xs text-[var(--muted-foreground)]">{{ $t('clock.clockedOutAt') }}</p>
          <p class="mt-1 text-lg font-medium text-[var(--foreground)]">{{ clockOutDisplay }}</p>
        </div>
        <div>
          <p class="text-xs text-[var(--muted-foreground)]">{{ $t('clock.late') }}</p>
          <p class="mt-1 text-lg font-medium" :class="lateMinutes > 0 ? 'text-[var(--bz-warning)]' : 'text-[var(--foreground)]'">
            {{ lateDisplay }}
          </p>
        </div>
        <div>
          <p class="text-xs text-[var(--muted-foreground)]">{{ $t('clock.early') }}</p>
          <p class="mt-1 text-lg font-medium" :class="earlyMinutes > 0 ? 'text-[var(--bz-warning)]' : 'text-[var(--foreground)]'">
            {{ earlyDisplay }}
          </p>
        </div>
      </div>
    </div>

    <!-- 月度回顾 -->
    <div class="rounded-xl border border-[var(--border)] bg-[var(--card)] p-6">
      <div class="mb-5 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-[var(--foreground)]">{{ $t('clock.monthly') }}</h2>
        <div class="flex items-center gap-2">
          <a-button size="small" @click="shiftMonth(-1)" aria-label="prev-month">
            <template #icon><icon-left /></template>
          </a-button>
          <span class="w-24 text-center text-sm font-medium text-[var(--foreground)]">{{ monthLabel }}</span>
          <a-button size="small" @click="shiftMonth(1)" aria-label="next-month">
            <template #icon><icon-right /></template>
          </a-button>
          <a-button v-if="!isCurrentMonth" size="small" @click="backToCurrentMonth">{{ $t('clock.backToday') }}</a-button>
        </div>
      </div>

      <!-- 月度统计 -->
      <div class="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div class="rounded-lg border border-[var(--border)] bg-[var(--muted)] p-4">
          <p class="text-xs text-[var(--muted-foreground)]">{{ $t('clock.workDays') }}</p>
          <p class="mt-1 text-2xl font-semibold text-[var(--foreground)]">{{ monthStats.workDays }}</p>
        </div>
        <div class="rounded-lg border border-[var(--border)] bg-[var(--muted)] p-4">
          <p class="text-xs text-[var(--muted-foreground)]">{{ $t('clock.lateCount') }}</p>
          <p class="mt-1 text-2xl font-semibold" :class="monthStats.lateCount > 0 ? 'text-[var(--bz-warning)]' : 'text-[var(--foreground)]'">
            {{ monthStats.lateCount }}
          </p>
        </div>
        <div class="rounded-lg border border-[var(--border)] bg-[var(--muted)] p-4">
          <p class="text-xs text-[var(--muted-foreground)]">{{ $t('clock.earlyCount') }}</p>
          <p class="mt-1 text-2xl font-semibold" :class="monthStats.earlyCount > 0 ? 'text-[var(--bz-warning)]' : 'text-[var(--foreground)]'">
            {{ monthStats.earlyCount }}
          </p>
        </div>
        <div class="rounded-lg border border-[var(--border)] bg-[var(--muted)] p-4">
          <p class="text-xs text-[var(--muted-foreground)]">{{ $t('clock.absentDays') }}</p>
          <p class="mt-1 text-2xl font-semibold" :class="monthStats.absentDays > 0 ? 'text-[var(--bz-danger)]' : 'text-[var(--foreground)]'">
            {{ monthStats.absentDays }}
          </p>
        </div>
      </div>

      <!-- 日历网格：周一为每周第一天 -->
      <div class="grid grid-cols-7 gap-1 sm:gap-2">
        <div v-for="w in weekHeader" :key="w" class="pb-2 text-center text-xs text-[var(--muted-foreground)]">{{ w }}</div>
        <div
          v-for="cell in calendarCells"
          :key="cell.dateStr"
          class="flex aspect-square flex-col items-center justify-center rounded-lg border text-sm"
          :class="cellClass(cell)"
        >
          <span>{{ cell.day }}</span>
          <span class="mt-1 h-1.5 w-1.5 rounded-full sm:h-2 sm:w-2" :class="cellDotClass(cell)"></span>
        </div>
      </div>

      <!-- 图例 -->
      <div class="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 border-t border-[var(--border)] pt-4">
        <span v-for="lg in legends" :key="lg.label" class="flex items-center gap-1.5 text-xs text-[var(--muted-foreground)]">
          <span class="h-2 w-2 rounded-full" :class="lg.dotClass"></span>
          {{ lg.label }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { clockInApi, clockOutApi, getTodayClockApi, getMonthClockApi } from '@/api/clock'
import type { AttClockRecord } from '@/types/api'

const { t } = useI18n()
/** 页面内 i18n 直取 */
function i18nText(key: string): string {
  return t(`clock.${key}`)
}

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']

/** 当前月份（yyyy-MM），用户可切换回顾月份 */
const viewMonth = ref(currentMonthStr())
const monthRecords = ref<AttClockRecord[]>([])
const todayRecord = ref<AttClockRecord | null>(null)
const submitting = ref(false)

// ---- 实时时钟 ----
const now = ref(new Date())
let timer: ReturnType<typeof setInterval> | null = null

function currentMonthStr(d = new Date()): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

const todayText = computed(() => {
  const d = now.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${WEEKDAYS[d.getDay()]}`
})

const nowTime = computed(() => {
  const d = now.value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})

// ---- 今日打卡状态 ----
const clockInDisplay = computed(() => (todayRecord.value?.clockInTime ?? '').slice(11, 16) || '-')
const clockOutDisplay = computed(() => (todayRecord.value?.clockOutTime ?? '').slice(11, 16) || '-')
const lateMinutes = computed(() => todayRecord.value?.lateMinutes || 0)
const earlyMinutes = computed(() => todayRecord.value?.earlyMinutes || 0)
const lateDisplay = computed(() => (lateMinutes.value > 0 ? `${lateMinutes.value} 分钟` : '-'))
const earlyDisplay = computed(() => (earlyMinutes.value > 0 ? `${earlyMinutes.value} 分钟` : '-'))

const todayStatusText = computed(() => {
  const r = todayRecord.value
  if (r?.clockInTime && r?.clockOutTime) return i18nText('done')
  if (r?.clockInTime) return i18nText('statusWorking')
  return i18nText('statusWaiting')
})

const todayDotClass = computed(() => {
  const r = todayRecord.value
  if (r?.clockInTime && r?.clockOutTime) return 'bg-[var(--bz-success)]'
  if (r?.clockInTime) return 'bg-[var(--primary)]'
  return 'bg-[var(--muted-foreground)]'
})

const todayStatusTextClass = computed(() => {
  const r = todayRecord.value
  if (r?.clockInTime && r?.clockOutTime) return 'text-[var(--bz-success)]'
  if (r?.clockInTime) return 'text-[var(--primary)]'
  return 'text-[var(--muted-foreground)]'
})

// ---- 打卡动作 ----
async function handleClockIn() {
  submitting.value = true
  try {
    const res = await clockInApi()
    todayRecord.value = res.data
    await loadMonth()
    Message.success(i18nText('clockInSuccess'))
  } catch (e) {
    // 拦截器已 Toast 后端错误信息
    console.error('clock-in error:', e)
  } finally {
    submitting.value = false
  }
}

async function handleClockOut() {
  submitting.value = true
  try {
    const res = await clockOutApi()
    todayRecord.value = res.data
    await loadMonth()
    Message.success(i18nText('clockOutSuccess'))
  } catch (e) {
    console.error('clock-out error:', e)
  } finally {
    submitting.value = false
  }
}

// ---- 月度日历 ----
const weekHeader = ['一', '二', '三', '四', '五', '六', '日']

interface CalendarCell {
  day: number
  dateStr: string
  isToday: boolean
  isPast: boolean // 早于今天
  isWeekend: boolean
  record?: AttClockRecord
}

const monthLabel = computed(() => {
  const [y, m] = viewMonth.value.split('-')
  return `${y}年${Number(m)}月`
})

const isCurrentMonth = computed(() => viewMonth.value === currentMonthStr())

const calendarCells = computed<CalendarCell[]>(() => {
  const [y, m] = viewMonth.value.split('-').map(Number)
  const first = new Date(y, m - 1, 1)
  const daysInMonth = new Date(y, m, 0).getDate()
  // 周一为一周起点：周日 getDay()=0 → 偏移 (0+6)%7=6
  const leadingBlanks = (first.getDay() + 6) % 7
  const recordMap = new Map(monthRecords.value.map((r) => [r.clockDate, r]))

  const cells: CalendarCell[] = []
  for (let i = 0; i < leadingBlanks; i++) {
    cells.push({ day: 0, dateStr: '', isToday: false, isPast: false, isWeekend: false })
  }
  const todayStr = currentMonthStr() + '-' + String(now.value.getDate()).padStart(2, '0')
  for (let day = 1; day <= daysInMonth; day++) {
    const date = new Date(y, m - 1, day)
    const dateStr = `${viewMonth.value}-${String(day).padStart(2, '0')}`
    const weekday = date.getDay()
    cells.push({
      day,
      dateStr,
      isToday: dateStr === todayStr,
      isPast: date < new Date(now.value.getFullYear(), now.value.getMonth(), now.value.getDate()),
      isWeekend: weekday === 0 || weekday === 6,
      record: recordMap.get(dateStr),
    })
  }
  return cells
})

/** 格子底色：空占位透明；今天描边高亮；未来/周末淡化 */
function cellClass(cell: CalendarCell): string {
  if (!cell.dateStr) return 'border-transparent'
  const base = 'border-[var(--border)]'
  if (cell.isToday) return `${base} border-[var(--primary)] ring-1 ring-[var(--primary)]`
  if (cell.record || (cell.isPast && !cell.isWeekend)) return `${base} bg-[var(--muted)]`
  return `${base} bg-transparent opacity-60`
}

/** 格子状态圆点：normal 绿 / late 橙 / early 琥珀 / absent·异常 红；过去无记录工作日视为缺卡 */
function cellDotClass(cell: CalendarCell): string {
  const status = cell.record?.status
  if (cell.record) {
    if (status === 'late') return 'bg-[var(--bz-warning)]'
    if (status === 'early') return 'bg-amber-500'
    if (status === 'absent' || status === 'abnormal') return 'bg-[var(--bz-danger)]'
    return 'bg-[var(--bz-success)]'
  }
  if (cell.isPast && !cell.isWeekend && !cell.isToday) return 'bg-[var(--bz-danger)] opacity-60'
  return 'bg-transparent'
}

// ---- 月度统计 ----
const monthStats = computed(() => {
  let workDays = 0
  let lateCount = 0
  let earlyCount = 0
  let absentDays = 0
  for (const r of monthRecords.value) {
    if (r.clockInTime) workDays++
    if (r.status === 'late') lateCount++
    if (r.status === 'early') earlyCount++
    if (r.status === 'absent' || r.status === 'abnormal') absentDays++
  }
  // 过去的工作日无任何打卡记录 → 缺卡
  for (const cell of calendarCells.value) {
    if (cell.dateStr && cell.isPast && !cell.isWeekend && !cell.record) absentDays++
  }
  return { workDays, lateCount, earlyCount, absentDays }
})

const legends = computed(() => [
  { label: i18nText('statusNormal'), dotClass: 'bg-[var(--bz-success)]' },
  { label: i18nText('statusLate'), dotClass: 'bg-[var(--bz-warning)]' },
  { label: i18nText('statusEarly'), dotClass: 'bg-amber-500' },
  { label: i18nText('statusAbsent'), dotClass: 'bg-[var(--bz-danger)]' },
])

// ---- 月份切换 ----
let monthSeq = 0 // 请求序号，防止竞态

function shiftMonth(offset: number) {
  const [y, m] = viewMonth.value.split('-').map(Number)
  const d = new Date(y, m - 1 + offset, 1)
  viewMonth.value = currentMonthStr(d)
  loadMonth()
}

function backToCurrentMonth() {
  viewMonth.value = currentMonthStr()
  loadMonth()
}

// ---- 数据加载 ----
async function loadToday() {
  try {
    const res = await getTodayClockApi()
    todayRecord.value = res.data
  } catch (e) {
    console.error('load today clock error:', e)
  }
}

async function loadMonth() {
  const seq = ++monthSeq // 捕获当前序号
  try {
    const res = await getMonthClockApi(viewMonth.value)
    // 检查是否是最新请求
    if (seq !== monthSeq) return
    monthRecords.value = res.data || []
  } catch (e) {
    if (seq === monthSeq) {
      console.error('load month clock error:', e)
    }
  }
}

/** 页面内 i18n 直取（避免在 script 中大量 useI18n 解构） */

let disposed = false // 组件卸载标志

onMounted(async () => {
  disposed = false
  await Promise.all([loadToday(), loadMonth()])
  
  // 只有在组件未卸载时才启动定时器
  if (disposed) return
  
  timer = setInterval(() => {
    now.value = new Date()
  }, 1000)
})

onUnmounted(() => {
  disposed = true
  if (timer) clearInterval(timer)
})
</script>