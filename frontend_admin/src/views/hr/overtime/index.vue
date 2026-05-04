<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="status" :label="$t('overtime.status')">
          <a-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <a-option value="0">{{ $t('overtime.statusPending') }}</a-option>
            <a-option value="1">{{ $t('overtime.statusApproved') }}</a-option>
            <a-option value="2">{{ $t('overtime.statusRejected') }}</a-option>
            <a-option value="3">{{ $t('overtime.statusRevoked') }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">{{ $t('common.search') }}</a-button>
            <a-button @click="handleReset">{{ $t('common.reset') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 表格 -->
    <a-card>
      <template #title>
        <span>{{ $t('overtime.title') }}</span>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('overtime.userName')" data-index="userName" :width="100" />
            <a-table-column :title="$t('overtime.timeRange')">
              <template #cell="{ record }">
                <span class="text-sm">{{ record.startTime }} ~ {{ record.endTime }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('overtime.durationHours')" data-index="durationHours" :width="90" />
            <a-table-column :title="$t('overtime.reason')" data-index="reason" ellipsis />
            <a-table-column :title="$t('common.status')" :width="90">
              <template #cell="{ record }">
                <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('overtime.approverName')" data-index="approverName" :width="100">
              <template #cell="{ record }">{{ record.approverName || '-' }}</template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="200" align="right">
              <template #cell="{ record }">
                <a-space>
                  <template v-if="record.status === '0'">
                    <a-button v-permission="'hr:overtime:approve'" size="small" type="primary" @click="openApprove(record)">
                      {{ $t('overtime.approve') }}
                    </a-button>
                    <a-button v-permission="'hr:overtime:edit'" size="small" @click="handleRevoke(record)">
                      {{ $t('overtime.revoke') }}
                    </a-button>
                  </template>
                  <a-button v-if="record.approveRemark" size="small" @click="showRemark(record)">
                    {{ $t('overtime.viewRemark') }}
                  </a-button>
                  <a-button size="small" @click="openFlow(record)">{{ $t('overtime.flow') }}</a-button>
                  <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button v-permission="'hr:overtime:delete'" size="small" status="danger">{{ $t('common.delete') }}</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>

        <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
          <a-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      </a-spin>
    </a-card>

    <!-- 流程进度弹窗 -->
    <a-modal v-model:visible="flowVisible" :title="$t('overtime.flow')" :width="520" footer-only>
      <a-steps v-if="flowNodes.length" direction="vertical" :current="currentStep">
        <a-step
          v-for="node in flowNodes"
          :key="node.id"
          :title="node.nodeName"
          :description="`${node.operatorName || '-'} · ${node.createTime}${node.remark ? ' · ' + node.remark : ''}`"
        />
      </a-steps>
      <a-empty v-else :description="$t('overtime.flowEmpty')" />
    </a-modal>

    <!-- 审批弹窗 -->
    <a-modal v-model:visible="approveVisible" :title="$t('overtime.approve')" :width="480"
             @ok="handleApprove" @cancel="approveVisible = false" :ok-loading="approving">
      <a-form :model="approveForm" layout="vertical">
        <a-form-item :label="$t('overtime.approveAction')" required>
          <a-radio-group v-model="approveForm.action" type="button">
            <a-radio value="pass">{{ $t('overtime.approvePass') }}</a-radio>
            <a-radio value="reject">{{ $t('overtime.approveReject') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="remark" :label="$t('overtime.approveRemark')">
          <a-textarea v-model="approveForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getOvertimePageApi, approveOvertimeApi, revokeOvertimeApi, deleteOvertimeApi, type AttOvertimeRequest } from '@/api/overtime'
import { getFlowLogsApi, type ApprovalFlowNode } from '@/api/flowlog'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const loading = ref(false)
const approving = ref(false)
const tableData = ref<AttOvertimeRequest[]>([])
const approveVisible = ref(false)

const searchForm = reactive({ status: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const approveForm = reactive<{ id: string; action: 'pass' | 'reject'; remark: string }>({
  id: '', action: 'pass', remark: '',
})

function statusText(status: string | undefined): string {
  const map: Record<string, string> = { 0: 'enums.status.pending', 1: 'enums.status.approved', 2: 'enums.status.rejected', 3: 'enums.status.withdrawn' }
  return status != null ? t(map[status] || '-') : '-'
}
function statusColor(status: string | undefined): string {
  if (status === '0') return 'orange'
  if (status === '1') return 'green'
  if (status === '2') return 'red'
  return 'default'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getOvertimePageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      status: searchForm.status || undefined,
    })
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.status = ''; pagination.current = 1; loadData() }

function openApprove(record: AttOvertimeRequest) {
  approveForm.id = record.id
  approveForm.action = 'pass'
  approveForm.remark = ''
  approveVisible.value = true
}

async function handleApprove() {
  approving.value = true
  try {
    await approveOvertimeApi(approveForm.id, approveForm.action, approveForm.remark)
    Message.success(t('common.msg.approveDone'))
    approveVisible.value = false
    loadData()
  } finally {
    approving.value = false
  }
}

async function handleRevoke(record: AttOvertimeRequest) {
  await revokeOvertimeApi(record.id)
  Message.success(t('common.msg.withdrawn'))
  loadData()
}

async function handleDelete(record: AttOvertimeRequest) {
  await deleteOvertimeApi(record.id)
  Message.success(t('common.deleteSuccess'))
  loadData()
}

function showRemark(record: AttOvertimeRequest) {
  Message.info({ content: record.approveRemark || '-', duration: 5000 })
}

// ============ 流程进度 ============
const flowVisible = ref(false)
const flowNodes = ref<ApprovalFlowNode[]>([])
const currentStep = ref(0)

async function openFlow(record: AttOvertimeRequest) {
  try {
    const res = await getFlowLogsApi('overtime', record.id)
    flowNodes.value = res.data || []
    currentStep.value = Math.max(0, flowNodes.value.length - 1)
    flowVisible.value = true
  } catch (err) {
    console.error('overtime flow error:', err)
  }
}

onMounted(loadData)
</script>
