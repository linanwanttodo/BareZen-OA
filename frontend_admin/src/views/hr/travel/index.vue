<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="status" :label="$t('travel.status')">
          <a-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <a-option value="0">{{ $t('travel.statusPending') }}</a-option>
            <a-option value="1">{{ $t('travel.statusApproved') }}</a-option>
            <a-option value="2">{{ $t('travel.statusRejected') }}</a-option>
            <a-option value="3">{{ $t('travel.statusRevoked') }}</a-option>
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
        <span>{{ $t('travel.title') }}</span>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('travel.userName')" data-index="userName" :width="100" />
            <a-table-column :title="$t('travel.destination')" data-index="destination" :width="140" />
            <a-table-column :title="$t('travel.purpose')" data-index="purpose" ellipsis />
            <a-table-column :title="$t('travel.timeRange')" :width="180">
              <template #cell="{ record }">
                <span class="text-sm">{{ record.startTime }}<br />{{ record.endTime }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('travel.days')" data-index="days" :width="80" />
            <a-table-column :title="$t('common.status')" :width="90">
              <template #cell="{ record }">
                <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('travel.approverName')" data-index="approverName" :width="100">
              <template #cell="{ record }">{{ record.approverName || '-' }}</template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="200" align="right">
              <template #cell="{ record }">
                <a-space>
                  <template v-if="record.status === '0'">
                    <a-button v-permission="'hr:travel:approve'" size="small" type="primary" @click="openApprove(record)">
                      {{ $t('travel.approve') }}
                    </a-button>
                    <a-button v-permission="'hr:travel:edit'" size="small" @click="handleRevoke(record)">
                      {{ $t('travel.revoke') }}
                    </a-button>
                  </template>
                  <a-button v-if="record.approveRemark" size="small" @click="showRemark(record)">
                    {{ $t('travel.viewRemark') }}
                  </a-button>
                  <a-button size="small" @click="openFlow(record)">{{ $t('travel.flow') }}</a-button>
                  <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button v-permission="'hr:travel:delete'" size="small" status="danger">{{ $t('common.delete') }}</a-button>
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
    <a-modal v-model:visible="flowVisible" :title="$t('travel.flow')" :width="520" footer-only>
      <a-steps v-if="flowNodes.length" direction="vertical" :current="currentStep">
        <a-step
          v-for="node in flowNodes"
          :key="node.id"
          :title="node.nodeName"
          :description="`${node.operatorName || '-'} · ${node.createTime}${node.remark ? ' · ' + node.remark : ''}`"
        />
      </a-steps>
      <a-empty v-else :description="$t('travel.flowEmpty')" />
    </a-modal>

    <!-- 审批弹窗 -->
    <a-modal v-model:visible="approveVisible" :title="$t('travel.approve')" :width="480"
             @ok="handleApprove" @cancel="approveVisible = false" :ok-loading="approving">
      <a-form :model="approveForm" layout="vertical">
        <a-form-item :label="$t('travel.approveAction')" required>
          <a-radio-group v-model="approveForm.action" type="button">
            <a-radio value="pass">{{ $t('travel.approvePass') }}</a-radio>
            <a-radio value="reject">{{ $t('travel.approveReject') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="remark" :label="$t('travel.approveRemark')">
          <a-textarea v-model="approveForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getTravelPageApi, approveTravelApi, revokeTravelApi, deleteTravelApi, type AttTravelRequest } from '@/api/travel'
import { getFlowLogsApi, type ApprovalFlowNode } from '@/api/flowlog'

const loading = ref(false)
const approving = ref(false)
const tableData = ref<AttTravelRequest[]>([])
const approveVisible = ref(false)

const searchForm = reactive({ status: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const approveForm = reactive<{ id: string; action: 'pass' | 'reject'; remark: string }>({
  id: '', action: 'pass', remark: '',
})

function statusText(status: string | undefined): string {
  const map: Record<string, string> = { 0: '待审批', 1: '已通过', 2: '已驳回', 3: '已撤销' }
  return status != null ? map[status] || '-' : '-'
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
    const res = await getTravelPageApi({
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

function openApprove(record: AttTravelRequest) {
  approveForm.id = record.id
  approveForm.action = 'pass'
  approveForm.remark = ''
  approveVisible.value = true
}

async function handleApprove() {
  approving.value = true
  try {
    await approveTravelApi(approveForm.id, approveForm.action, approveForm.remark)
    Message.success('审批完成')
    approveVisible.value = false
    loadData()
  } finally {
    approving.value = false
  }
}

async function handleRevoke(record: AttTravelRequest) {
  await revokeTravelApi(record.id)
  Message.success('已撤销')
  loadData()
}

async function handleDelete(record: AttTravelRequest) {
  await deleteTravelApi(record.id)
  Message.success('删除成功')
  loadData()
}

function showRemark(record: AttTravelRequest) {
  Message.info(`${record.approveRemark || '-'}`, 5000)
}

// ============ 流程进度 ============
const flowVisible = ref(false)
const flowNodes = ref<ApprovalFlowNode[]>([])
const currentStep = ref(0)

async function openFlow(record: AttTravelRequest) {
  try {
    const res = await getFlowLogsApi('travel', record.id)
    flowNodes.value = res.data || []
    currentStep.value = Math.max(0, flowNodes.value.length - 1)
    flowVisible.value = true
  } catch (err) {
    console.error('travel flow error:', err)
  }
}

onMounted(loadData)
</script>
