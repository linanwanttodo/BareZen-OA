<template>
  <div class="page-container">
    <t-card :bordered="false" class="bz-list-panel">
      <!-- 筛选区：标签左置 + 栅格等宽，查询/重置固定在右端（对齐 Starter list/base） -->
      <t-form :data="searchForm" :label-width="80" colon @submit-success="handleSearch">
        <t-row :gutter="[24, 24]">
          <t-col :span="3">
            <t-form-item name="status" :label="$t('monitor.jobLog.status')">
              <t-select v-model="searchForm.status" class="form-item-content" :placeholder="$t('common.all')" allow-clear>
                <t-option value="0">{{ $t('monitor.jobLog.success') }}</t-option>
                <t-option value="1">{{ $t('monitor.jobLog.failed') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="3" class="bz-list-ops">
            <t-form-item>
              <t-space>
                <t-button theme="primary" html-type="submit">{{ $t('common.search') }}</t-button>
                <t-button theme="default" variant="base" @click="handleReset">{{ $t('common.reset') }}</t-button>
              </t-space>
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>

      <t-table
        row-key="id"
        :data="tableData"
        :columns="tableColumns"
        :loading="loading"
        :pagination="false"
        :hover="true"
        vertical-align="top"
      >
        <template #status="{ row }">
          <t-tag :theme="row.status === '0' ? 'success' : 'danger'" variant="light">
            {{ row.status === '0' ? $t('monitor.jobLog.success') : $t('monitor.jobLog.failed') }}
          </t-tag>
        </template>
        <template #invokeTarget="{ row }">
          <span class="font-mono text-xs">{{ row.invokeTarget }}</span>
        </template>
        <template #duration="{ row }">
          {{ row.duration ?? 0 }} ms
        </template>
        <template #errorMsg="{ row }">
          <t-link v-if="row.errorMsg" theme="danger" hover="color" @click="showError(row)">
            {{ $t('monitor.jobLog.viewError') }}
          </t-link>
          <span v-else class="text-[var(--td-text-color-placeholder)]">{{ $t('monitor.jobLog.noError') }}</span>
        </template>
      </t-table>

      <div class="bz-list-pager">
        <t-pagination
          v-model:current="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          show-total
          show-jumper
          @change="onPageChange"
        />
      </div>
    </t-card>

    <!-- 失败详情 -->
    <t-dialog
      v-model:visible="errorDialogVisible"
      :header="$t('monitor.jobLog.errorMsg')"
      :width="640"
      :footer="false"
    >
      <t-descriptions :column="1" border>
        <t-descriptions-item :label="$t('monitor.jobLog.jobName')">{{ currentLog?.jobName }}</t-descriptions-item>
        <t-descriptions-item :label="$t('monitor.jobLog.startTime')">{{ currentLog?.startTime }}</t-descriptions-item>
      </t-descriptions>
      <pre class="error-detail">{{ currentLog?.errorMsg }}</pre>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onActivated } from 'vue'
import { useI18n } from 'vue-i18n'
import { getJobLogPageApi, type SysJobLog } from '@/api/job'

const { t } = useI18n()

const loading = ref(false)
const tableData = ref<SysJobLog[]>([])
const searchForm = reactive<{ status?: string }>({})
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const errorDialogVisible = ref(false)
const currentLog = ref<SysJobLog | null>(null)

const tableColumns = computed(() => [
  { colKey: 'jobName', title: t('monitor.jobLog.jobName'), width: 160, ellipsis: true },
  { colKey: 'invokeTarget', title: t('monitor.jobLog.invokeTarget'), ellipsis: true },
  { colKey: 'status', title: t('monitor.jobLog.status'), width: 100 },
  { colKey: 'startTime', title: t('monitor.jobLog.startTime'), width: 170 },
  { colKey: 'stopTime', title: t('monitor.jobLog.stopTime'), width: 170 },
  { colKey: 'duration', title: t('monitor.jobLog.duration'), width: 110 },
  { colKey: 'errorMsg', title: t('monitor.jobLog.errorMsg'), width: 110 },
])

async function loadList() {
  loading.value = true
  try {
    const res = await getJobLogPageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      status: searchForm.status,
    })
    tableData.value = res.data?.list || []
    pagination.total = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadList()
}

function handleReset() {
  searchForm.status = undefined
  handleSearch()
}

function onPageChange() {
  loadList()
}

function showError(row: SysJobLog) {
  currentLog.value = row
  errorDialogVisible.value = true
}

onMounted(loadList)
// keep-alive 缓存后重新进入时刷新，避免看到上次的旧数据
onActivated(loadList)
</script>

<style scoped>
.error-detail {
  margin-top: var(--td-comp-margin-l);
  padding: var(--td-comp-paddingLR-l);
  border-radius: var(--td-radius-default);
  background: var(--td-bg-color-secondarycontainer);
  color: var(--td-error-color);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: var(--td-font-size-body-small);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 320px;
  overflow-y: auto;
}
</style>
