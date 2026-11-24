<template>
  <div class="page-container">
    <t-card class="bz-list-panel">
      <t-form :data="searchForm" :label-width="90" :on-submit="handleSearch">
        <t-row :gutter="[24, 24]">
          <t-col :span="3">
            <t-form-item name="bizType" :label="$t('flowLog.bizType')" :rules="[{ required: true, message: t('validation.required', { field: t('flowLog.bizType') }) }]">
              <t-select v-model="searchForm.bizType" :placeholder="$t('common.selectPlaceholder')" clearable>
                <t-option value="leave">{{ $t('flowTemplate.bizLeave') }}</t-option>
                <t-option value="overtime">{{ $t('flowTemplate.bizOvertime') }}</t-option>
                <t-option value="travel">{{ $t('flowTemplate.bizTravel') }}</t-option>
                <t-option value="vehicle">{{ $t('flowTemplate.bizVehicle') }}</t-option>
                <t-option value="supply">{{ $t('flowLog.bizSupply') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="3">
            <t-form-item name="bizId" :label="$t('flowLog.bizId')" :rules="[{ required: true, message: t('validation.required', { field: t('flowLog.bizId') }) }]">
              <t-input v-model="searchForm.bizId" :placeholder="$t('flowLog.bizIdPlaceholder')" allow-clear />
            </t-form-item>
          </t-col>
          <t-col :span="3" class="bz-list-ops">
            <t-form-item>
              <t-space>
                <t-button v-permission="PERM.system.flowlogList" theme="primary" type="submit">{{ $t('common.search') }}</t-button>
                <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
              </t-space>
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>

      <!-- 审批节点链（多级审批引擎） -->
      <div class="bz-list-toolbar bz-list-toolbar--first">
        <div class="flex items-center gap-2">
          <span>{{ $t('flowLog.taskTitle') }}</span>
          <t-tag v-if="searched" theme="primary" variant="light">{{ bizTypeLabel }}</t-tag>
          <t-tag v-if="searched" variant="outline">#{{ searchForm.bizId }}</t-tag>
        </div>
      </div>
      <t-table
        :loading="loading"
        row-key="id"
        :data="pagedTasks"
        :columns="taskColumns"
        :pagination="false"
        size="small"
        :hover="true"
        vertical-align="top"
      >
        <template #status="{ row }">
          <t-tag :theme="flowTagTheme(row.status)" variant="light">{{ flowStatusText(row.status) }}</t-tag>
        </template>
      </t-table>
      <t-empty v-if="!loading && searched && !tasks.length" :description="$t('flowLog.taskEmpty')" />
      <div v-if="tasks.length > taskPagination.pageSize" class="mt-3 flex justify-end">
        <t-pagination
          v-model:current="taskPagination.current"
          :total="tasks.length"
          :page-size="taskPagination.pageSize"
          size="small"
        />
      </div>

      <!-- 事件流水（审计日志，时间正序） -->
      <div class="bz-list-toolbar bz-list-toolbar--first">
        <span>{{ $t('flowLog.logTitle') }}</span>
      </div>
      <t-table
        :loading="loading"
        row-key="id"
        :data="pagedLogs"
        :columns="logColumns"
        :pagination="false"
        size="small"
        :hover="true"
        vertical-align="top"
      >
        <template #nodeStatus="{ row }">
          <t-tag :theme="flowTagTheme(row.nodeStatus)" variant="light">{{ flowStatusText(row.nodeStatus) }}</t-tag>
        </template>
      </t-table>
      <t-empty v-if="!loading && searched && !logs.length" :description="$t('flowLog.eventEmpty')" />
      <div v-if="logs.length > logPagination.pageSize" class="mt-3 flex justify-end">
        <t-pagination
          v-model:current="logPagination.current"
          :total="logs.length"
          :page-size="logPagination.pageSize"
          size="small"
        />
      </div>
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import { getFlowLogsApi, getFlowTasksApi, type ApprovalFlowNode, type ApprovalFlowTask } from '@/api/flowlog'

const { t } = useI18n()
const loading = ref(false)
const searched = ref(false)

const searchForm = reactive({ bizType: '' as string, bizId: '' })
const tasks = ref<ApprovalFlowTask[]>([])
const logs = ref<ApprovalFlowNode[]>([])

const taskPagination = reactive({ current: 1, pageSize: 10 })
const logPagination = reactive({ current: 1, pageSize: 10 })

const bizTypeLabel = computed(() => {
  const map: Record<string, string> = {
    leave: t('flowTemplate.bizLeave'),
    overtime: t('flowTemplate.bizOvertime'),
    travel: t('flowTemplate.bizTravel'),
    vehicle: t('flowTemplate.bizVehicle'),
    supply: t('flowLog.bizSupply'),
  }
  return map[searchForm.bizType] || searchForm.bizType
})

/** 节点状态（0=待审批 1=已通过 2=已驳回 3=已取消，见 V22 迁移注释） */
function flowStatusText(status: string | undefined): string {
  const map: Record<string, string> = {
    '0': t('enums.status.pending'),
    '1': t('enums.status.approved'),
    '2': t('enums.status.rejected'),
    '3': t('enums.status.cancelled'),
  }
  return (status && map[status]) || status || '-'
}

function flowTagTheme(status: string | undefined): 'success' | 'warning' | 'danger' | 'primary' | 'default' {
  if (status === '1') return 'success'
  if (status === '0') return 'warning'
  if (status === '2') return 'danger'
  return 'default'
}

const pagedTasks = computed(() => {
  const start = (taskPagination.current - 1) * taskPagination.pageSize
  return tasks.value.slice(start, start + taskPagination.pageSize)
})

const pagedLogs = computed(() => {
  const start = (logPagination.current - 1) * logPagination.pageSize
  return logs.value.slice(start, start + logPagination.pageSize)
})

async function handleSearch() {
  if (!searchForm.bizType || !searchForm.bizId.trim()) return
  const bizId = searchForm.bizId.trim()
  if (!/^\d+$/.test(bizId)) {
    MessagePlugin.warning(t('flowLog.bizIdInvalid'))
    return
  }
  loading.value = true
  searched.value = true
  taskPagination.current = 1
  logPagination.current = 1
  try {
    const [taskRes, logRes] = await Promise.all([
      getFlowTasksApi(searchForm.bizType, bizId),
      getFlowLogsApi(searchForm.bizType, bizId),
    ])
    tasks.value = taskRes.data || []
    logs.value = logRes.data || []
  } finally {
    loading.value = false
  }
}

function handleReset() {
  searchForm.bizType = ''
  searchForm.bizId = ''
  searched.value = false
  tasks.value = []
  logs.value = []
}

const taskColumns = computed(() => [
  { colKey: 'nodeSeq', title: t('flowLog.nodeSeq'), width: 80 },
  { colKey: 'nodeName', title: t('flowLog.nodeName'), width: 160 },
  { colKey: 'approverName', title: t('flowLog.approver'), width: 120 },
  { colKey: 'status', title: t('common.status'), width: 100 },
  { colKey: 'approveRemark', title: t('flowLog.approveRemark'), ellipsis: true },
  { colKey: 'approveTime', title: t('flowLog.approveTime'), width: 170 },
])

const logColumns = computed(() => [
  { colKey: 'nodeName', title: t('flowLog.nodeName'), width: 160 },
  { colKey: 'operatorName', title: t('flowLog.operator'), width: 120 },
  { colKey: 'nodeStatus', title: t('flowLog.nodeStatus'), width: 100 },
  { colKey: 'remark', title: t('common.remark'), ellipsis: true },
  { colKey: 'createTime', title: t('common.createTime'), width: 170 },
])
</script>
