<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-card-content class="pt-6">
        <div class="flex flex-wrap items-end gap-4">
          <a-form-item :label="$t('monitor.operLog.title')">
            <a-input v-model="searchForm.title" :placeholder="$t('monitor.operLog.title')" style="width: 176px" />
          </a-form-item>
          <a-form-item :label="$t('monitor.operLog.operName')">
            <a-input v-model="searchForm.operUser" :placeholder="$t('monitor.operLog.operName')" style="width: 144px" />
          </a-form-item>
          <a-form-item :label="$t('monitor.operLog.businessType')">
            <a-select
              v-model="searchForm.businessType"
              :options="bizOptions"
              :placeholder="$t('common.selectPlaceholder')"
              style="width: 128px"
              allow-clear
            />
          </a-form-item>
          <a-form-item :label="$t('common.status')">
            <a-select
              v-model="searchForm.status"
              :options="statusOptions"
              :placeholder="$t('common.selectPlaceholder')"
              style="width: 128px"
              allow-clear
            />
          </a-form-item>
          <div class="flex gap-2">
            <a-button @click="handleSearch">{{ $t('common.search') }}</a-button>
            <a-button @click="handleReset">{{ $t('common.reset') }}</a-button>
          </div>
        </div>
      </a-card-content>
    </a-card>

    <!-- 表格区域 -->
    <a-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('monitor.operLog.title') }}</span>
          <a-space>
            <a-button @click="loadTable">
              <template #icon><icon-refresh /></template>
              {{ $t('monitor.operLog.refresh') }}
            </a-button>
            <a-button v-permission="PERM.monitor.operlogDelete" status="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">
              <template #icon><icon-delete /></template>
              {{ $t('common.batchDelete') }}
            </a-button>
            <a-popconfirm v-permission="PERM.monitor.operlogDelete" :content="$t('monitor.operLog.cleanConfirm', { beforeDays: 30 })" @ok="handleClean">
              <a-button status="danger">
                <template #icon><icon-delete /></template>
                {{ $t('monitor.operLog.clean') }}
              </a-button>
            </a-popconfirm>
          </a-space>
        </div>
      </template>
      <a-card-content>
        <div v-if="loading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--primary)]"></div>
          <span class="ml-2 text-[var(--muted-foreground)]">{{ $t('common.loading') }}</span>
        </div>
        <template v-else-if="tableData.length">
          <a-table
            :data="tableData"
            :pagination="false"
            row-key="id"
            :row-selection="{ type: 'checkbox', selectedRowKeys: selectedIds }"
            @selection-change="onSelectionChange"
          >
            <template #columns>
              <a-table-column :title="$t('common.id')" data-index="id" />
              <a-table-column :title="$t('monitor.operLog.title')" data-index="title" />
              <a-table-column :title="$t('monitor.operLog.businessType')">
                <template #cell="{ record }">
                  <a-tag :color="tagColor(businessTypeVariant(record.businessType))" :bordered="tagBordered(businessTypeVariant(record.businessType))">{{ businessTypeText(record.businessType) }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column :title="$t('monitor.operLog.requestMethod')">
                <template #cell="{ record }">
                  <a-tag :color="tagColor(methodVariant(record.requestMethod))" :bordered="tagBordered(methodVariant(record.requestMethod))">{{ record.requestMethod }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column :title="$t('monitor.operLog.operName')" data-index="operUser" />
              <a-table-column :title="$t('monitor.operLog.operUrl')" data-index="operUrl" />
              <a-table-column :title="$t('common.status')">
                <template #cell="{ record }">
                  <a-tag :color="tagColor(operStatusVariant(record.status))" :bordered="tagBordered(operStatusVariant(record.status))">{{ operStatusText(record.status) }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column :title="$t('monitor.operLog.costTime')">
                <template #cell="{ record }">
                  <span :class="record.costTime > 1000 ? 'text-wecom-warning' : 'text-wecom-success'">{{ record.costTime }}ms</span>
                </template>
              </a-table-column>
              <a-table-column :title="$t('monitor.operLog.operTime')" data-index="operTime" />
              <a-table-column :title="$t('common.operate')" align="right">
                <template #cell="{ record }">
                  <a-space>
                    <a-popconfirm :content="$t('monitor.operLog.deleteConfirm')" @ok="handleDelete(record)">
                      <a-button v-permission="PERM.monitor.operlogDelete" type="text" size="small" status="danger">{{ $t('common.delete') }}</a-button>
                    </a-popconfirm>
                    <a-button v-permission="PERM.monitor.operlogList" type="text" size="small" @click="handleDetail(record)">{{ $t('monitor.operLog.detail') }}</a-button>
                  </a-space>
                </template>
              </a-table-column>
            </template>
          </a-table>

          <div class="mt-4 flex justify-end">
            <a-pagination
              v-model:current="pagination.current"
              :total="pagination.total"
              :page-size="pagination.pageSize"
              @change="onPageChange"
            />
          </div>
        </template>
        <a-empty v-else :description="$t('monitor.operLog.noLogs')" />
      </a-card-content>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal v-model:visible="detailVisible" :title="$t('monitor.operLog.detailTitle')" @cancel="detailVisible = false">
      <div v-if="detailData" class="log-detail">
        <dl class="grid grid-cols-2 gap-x-8 gap-y-4">
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('monitor.operLog.title') }}</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ detailData.title }}</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('monitor.operLog.businessType') }}</dt>
            <dd class="mt-1"><a-tag :color="tagColor(businessTypeVariant(detailData.businessType))" :bordered="tagBordered(businessTypeVariant(detailData.businessType))">{{ businessTypeText(detailData.businessType) }}</a-tag></dd>
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
            <dd class="mt-1"><a-tag :color="tagColor(methodVariant(detailData.requestMethod))" :bordered="tagBordered(methodVariant(detailData.requestMethod))">{{ detailData.requestMethod }}</a-tag></dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('monitor.operLog.costTime') }}</dt>
            <dd class="mt-1">
              <span :class="detailData.costTime > 1000 ? 'text-wecom-warning' : 'text-wecom-success'">{{ detailData.costTime }}ms</span>
            </dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('common.status') }}</dt>
            <dd class="mt-1"><a-tag :color="tagColor(operStatusVariant(detailData.status))" :bordered="tagBordered(operStatusVariant(detailData.status))">{{ operStatusText(detailData.status) }}</a-tag></dd>
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
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconRefresh, IconDelete } from '@arco-design/web-vue/es/icon'
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
  Message.success(t('monitor.operLog.deleteSuccess'))
  loadTable()
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  await batchDeleteOperLogApi(selectedIds.value)
  Message.success(t('monitor.operLog.batchDeleteSuccess', { count: selectedIds.value.length }))
  selectedIds.value = []
  loadTable()
}

async function handleClean() {
  await cleanOperLogApi()
  Message.success(t('monitor.operLog.cleanSuccess'))
  selectedIds.value = []
  loadTable()
}

// ============ 初始化 ============
onMounted(() => {
  loadTable()
})
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
