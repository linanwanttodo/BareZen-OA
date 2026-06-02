<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <div class="pt-6">
        <div class="flex flex-wrap items-end gap-4">
          <t-form-item :label="$t('monitor.operLog.title')">
            <t-input v-model="searchForm.title" :placeholder="$t('monitor.operLog.title')" style="width: 176px" />
          </t-form-item>
          <t-form-item :label="$t('monitor.operLog.operName')">
            <t-input v-model="searchForm.operUser" :placeholder="$t('monitor.operLog.operName')" style="width: 144px" />
          </t-form-item>
          <t-form-item :label="$t('monitor.operLog.businessType')">
            <t-select
              v-model="searchForm.businessType"
              :options="bizOptions"
              :placeholder="$t('common.selectPlaceholder')"
              style="width: 128px"
              allow-clear
            />
          </t-form-item>
          <t-form-item :label="$t('common.status')">
            <t-select
              v-model="searchForm.status"
              :options="statusOptions"
              :placeholder="$t('common.selectPlaceholder')"
              style="width: 128px"
              allow-clear
            />
          </t-form-item>
          <div class="flex gap-2">
            <t-button @click="handleSearch">{{ $t('common.search') }}</t-button>
            <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
          </div>
        </div>
      </div>
    </t-card>

    <!-- 表格区域 -->
    <t-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('monitor.operLog.title') }}</span>
          <t-space>
            <t-button @click="loadTable">
              <template #icon><icon-refresh /></template>
              {{ $t('monitor.operLog.refresh') }}
            </t-button>
            <t-button v-permission="PERM.monitor.operlogDelete" theme="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">
              <template #icon><icon-delete /></template>
              {{ $t('common.batchDelete') }}
            </t-button>
            <t-popconfirm v-permission="PERM.monitor.operlogDelete" :content="$t('monitor.operLog.cleanConfirm', { beforeDays: 30 })" @confirm="handleClean">
              <t-button theme="danger">
                <template #icon><icon-delete /></template>
                {{ $t('monitor.operLog.clean') }}
              </t-button>
            </t-popconfirm>
          </t-space>
        </div>
      </template>
        <div v-if="loading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--primary)]"></div>
          <span class="ml-2 text-[var(--muted-foreground)]">{{ $t('common.loading') }}</span>
        </div>
        <template v-else-if="tableData.length">
            <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
    <template #businessType="{ row }">
                  <t-tag :color="tagColor(businessTypeVariant(row.businessType))" :bordered="tagBordered(businessTypeVariant(row.businessType))">{{ businessTypeText(row.businessType) }}</t-tag>
                </template>
    <template #requestMethod="{ row }">
                  <t-tag :color="tagColor(methodVariant(row.requestMethod))" :bordered="tagBordered(methodVariant(row.requestMethod))">{{ row.requestMethod }}</t-tag>
                </template>
    <template #status="{ row }">
                  <t-tag :color="tagColor(operStatusVariant(row.status))" :bordered="tagBordered(operStatusVariant(row.status))">{{ operStatusText(row.status) }}</t-tag>
                </template>
    <template #costTime="{ row }">
                  <span :class="row.costTime > 1000 ? 'text-wecom-warning' : 'text-wecom-success'">{{ row.costTime }}ms</span>
                </template>
    <template #col_9="{ row }">
                  <t-space>
                    <t-popconfirm :content="$t('monitor.operLog.deleteConfirm')" @confirm="handleDelete(row)">
                      <t-button v-permission="PERM.monitor.operlogDelete" type="text" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                    </t-popconfirm>
                    <t-button v-permission="PERM.monitor.operlogList" type="text" size="small" @click="handleDetail(row)">{{ $t('monitor.operLog.detail') }}</t-button>
                  </t-space>
                </template>
  </t-table>

          <div class="mt-4 flex justify-end">
            <t-pagination
              v-model:current="pagination.current"
              :total="pagination.total"
              :page-size="pagination.pageSize"
              @change="onPageChange"
            />
          </div>
        </template>
        <t-empty v-else :description="$t('monitor.operLog.noLogs')" />
    </t-card>

    <!-- 详情弹窗 -->
    <t-dialog v-model="detailVisible" :header="$t('monitor.operLog.detailTitle')" @close="detailVisible = false">
      <div v-if="detailData" class="log-detail">
        <dl class="grid grid-cols-2 gap-x-8 gap-y-4">
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('monitor.operLog.title') }}</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ detailData.title }}</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('monitor.operLog.businessType') }}</dt>
            <dd class="mt-1"><t-tag :color="tagColor(businessTypeVariant(detailData.businessType))" :bordered="tagBordered(businessTypeVariant(detailData.businessType))">{{ businessTypeText(detailData.businessType) }}</t-tag></dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('monitor.operLog.operName') }}</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ detailData.operUser }}</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('monitor.operLog.url') }}</dt>
            <dd class="mt-1"><span class="mono-text">{{ detailData.operIp }}</span></dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('monitor.operLog.requestMethod') }}</dt>
            <dd class="mt-1"><t-tag :color="tagColor(methodVariant(detailData.requestMethod))" :bordered="tagBordered(methodVariant(detailData.requestMethod))">{{ detailData.requestMethod }}</t-tag></dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('monitor.operLog.costTime') }}</dt>
            <dd class="mt-1">
              <span :class="detailData.costTime > 1000 ? 'text-wecom-warning' : 'text-wecom-success'">{{ detailData.costTime }}ms</span>
            </dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('common.status') }}</dt>
            <dd class="mt-1"><t-tag :color="tagColor(operStatusVariant(detailData.status))" :bordered="tagBordered(operStatusVariant(detailData.status))">{{ operStatusText(detailData.status) }}</t-tag></dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('monitor.operLog.operTime') }}</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ formatDateTime(detailData.operTime) }}</dd>
          </div>
        </dl>

        <div class="log-detail-section">
          <h4 class="log-detail-section__title">{{ $t('monitor.operLog.requestInfo') }}</h4>
          <dl class="grid grid-cols-1 gap-4">
            <div>
              <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('monitor.operLog.operUrl') }}</dt>
              <dd class="mt-1"><span class="mono-text">{{ detailData.operUrl }}</span></dd>
            </div>
            <div>
              <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('monitor.operLog.className') }}</dt>
              <dd class="mt-1"><span class="mono-text">{{ detailData.className }}.{{ detailData.methodName }}</span></dd>
            </div>
          </dl>
        </div>

        <div v-if="detailData.operParam" class="log-detail-section">
          <h4 class="log-detail-section__title">{{ $t('monitor.operLog.operParam') }}</h4>
          <div class="log-detail-code">{{ formatJson(detailData.operParam) }}</div>
        </div>

        <div v-if="detailData.jsonResult" class="log-detail-section">
          <h4 class="log-detail-section__title">{{ $t('monitor.operLog.jsonResult') }}</h4>
          <div class="log-detail-code">{{ formatJson(detailData.jsonResult) }}</div>
        </div>

        <div v-if="detailData.errorMsg" class="log-detail-section">
          <h4 class="log-detail-section__title log-detail-section__title--error">{{ $t('monitor.operLog.errorMsg') }}</h4>
          <div class="log-detail-code log-detail-code--error">{{ detailData.errorMsg }}</div>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {  MessagePlugin  } from 'tdesign-vue-next'
import { formatDateTime, businessTypeText, operStatusText, operStatusVariant, businessTypeVariant, methodVariant } from '@/utils/format'
import {
  getOperLogPageApi,
  getOperLogApi,
  deleteOperLogApi,
  batchDeleteOperLogApi,
  cleanOperLogApi,
  type SysOperLog,
  type OperLogListParams,
} from '@/api/operLog'

const { t } = useI18n()

function tagColor(v: string): string | undefined {
  if (v === 'default') return 'arcoblue'
  if (v === 'destructive') return 'red'
  if (v === 'secondary') return 'gray'
  return undefined
}

function tagBordered(v: string): boolean {
  return v === 'outline'
}

const bizOptions = [
  { label: t('common.other'), value: 0 },
  { label: t('common.add'), value: 1 },
  { label: t('common.edit'), value: 2 },
  { label: t('common.delete'), value: 3 },
]
const statusOptions = [
  { label: t('monitor.operLog.statusSuccess'), value: 0 },
  { label: t('monitor.operLog.statusFail'), value: 1 },
]

// ============ 搜索 ============
const searchForm = reactive<OperLogListParams>({
  title: '',
  operUser: '',
  businessType: undefined,
  status: undefined,
})

function handleSearch() {
  pagination.current = 1
  loadTable()
}

function handleReset() {
  searchForm.title = ''
  searchForm.operUser = ''
  searchForm.businessType = undefined
  searchForm.status = undefined
  pagination.current = 1
  loadTable()
}

// ============ 表格 ============
const loading = ref(false)
const tableData = ref<SysOperLog[]>([])

// 行选中（批量删除）
const selectedIds = ref<string[]>([])

function onSelectionChange(rowKeys: (string | number)[]) {
  selectedIds.value = rowKeys.map(String)
}

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

function onPageChange(page: number) {
  pagination.current = page
  loadTable()
}

function formatJson(str: string) {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

async function loadTable() {
  loading.value = true
  try {
    const res = await getOperLogPageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      title: searchForm.title || undefined,
      operUser: searchForm.operUser || undefined,
      businessType: searchForm.businessType,
      status: searchForm.status,
    })
    tableData.value = res.data?.list || []
    pagination.total = res.data?.total || 0
  } catch (err) {
    console.error('oper-log load error:', err)
  } finally {
    loading.value = false
  }
}

// ============ 详情 ============
const detailVisible = ref(false)
const detailData = ref<SysOperLog | null>(null)

async function handleDetail(row: SysOperLog) {
  try {
    const res = await getOperLogApi(row.id)
    detailData.value = res.data
  } catch (err) {
    console.error('oper-log detail error:', err)
    detailData.value = row
  }
  detailVisible.value = true
}

// ============ 删除 / 批量删除 / 清空 ============
async function handleDelete(row: SysOperLog) {
  await deleteOperLogApi(String(row.id))
  MessagePlugin.success(t('monitor.operLog.deleteSuccess'))
  loadTable()
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  await batchDeleteOperLogApi(selectedIds.value)
  MessagePlugin.success(t('monitor.operLog.batchDeleteSuccess', { count: selectedIds.value.length }))
  selectedIds.value = []
  loadTable()
}

async function handleClean() {
  await cleanOperLogApi()
  MessagePlugin.success(t('monitor.operLog.cleanSuccess'))
  selectedIds.value = []
  loadTable()
}

// ============ 初始化 ============
onMounted(() => {
  loadTable()
})

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'id', title: t('common.id') },
    { colKey: 'title', title: t('monitor.operLog.title') },
    { colKey: 'businessType', title: t('monitor.operLog.businessType') },
    { colKey: 'requestMethod', title: t('monitor.operLog.requestMethod') },
    { colKey: 'operUser', title: t('monitor.operLog.operName') },
    { colKey: 'operUrl', title: t('monitor.operLog.operUrl') },
    { colKey: 'status', title: t('common.status') },
    { colKey: 'costTime', title: t('monitor.operLog.costTime') },
    { colKey: 'operTime', title: t('monitor.operLog.operTime') },
    { colKey: 'col_9', title: t('common.operate'), align: 'right' },
  ])

</script>

<style scoped>
.mono-text {
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
}

.log-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.log-detail-section {
  margin-top: 4px;
}
.log-detail-section__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--foreground);
  margin-bottom: 8px;
}
.log-detail-code {
  background: var(--muted);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 12px 16px;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--foreground);
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 240px;
  overflow-y: auto;
}
.log-detail-code--error {
  color: var(--destructive);
}
</style>
