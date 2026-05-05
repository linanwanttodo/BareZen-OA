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
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('travel.title') }}</span>
          <a-button v-permission="'hr:travel:add'" type="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('travel.apply') }}
          </a-button>
        </div>
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

    <!-- 流程进度弹窗：优先渲染引擎节点链（任务态），未接入引擎回退事件流 -->
    <a-modal v-model:visible="flowVisible" :title="$t('travel.flow')" :width="520" footer-only>
      <a-steps v-if="flowTasks.length" direction="vertical" :current="taskCurrent">
        <a-step
          v-for="task in flowTasks"
          :key="task.id"
          :status="taskStepStatus(task.status)"
          :title="task.nodeName"
          :description="taskStepDesc(task)"
        />
      </a-steps>
      <a-steps v-else-if="flowNodes.length" direction="vertical" :current="currentStep">
        <a-step
          v-for="node in flowNodes"
          :key="node.id"
          :title="node.nodeName"
          :description="`${node.operatorName || '-'} · ${node.createTime}${node.remark ? ' · ' + node.remark : ''}`"
        />
      </a-steps>
      <a-empty v-else :description="$t('travel.flowEmpty')" />
    </a-modal>

    <!-- 出差申请弹窗 -->
    <a-modal v-model:visible="addVisible" :title="$t('travel.apply')" :width="560"
             @ok="handleSubmit" @cancel="addVisible = false" :ok-loading="submitting">
      <a-form :model="form" layout="vertical">
        <a-form-item field="destination" :label="$t('travel.destination')" required>
          <a-input v-model="form.destination" :placeholder="$t('common.inputPlaceholder')" :max-length="100" />
        </a-form-item>
        <a-form-item field="timeRange" :label="$t('travel.timeRange')" required>
          <a-date-picker v-model="form.startTime" show-time style="width: 260px" :placeholder="$t('travel.startTime')" />
          <span class="mx-2">~</span>
          <a-date-picker v-model="form.endTime" show-time style="width: 260px" :placeholder="$t('travel.endTime')" />
        </a-form-item>
        <a-form-item field="days" :label="$t('travel.days')">
          <a-input-number v-model="form.days" :min="0.5" :step="0.5" style="width: 100%" :placeholder="$t('travel.daysPlaceholder')" />
        </a-form-item>
        <a-form-item field="purpose" :label="$t('travel.purpose')" required>
          <a-textarea v-model="form.purpose" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
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
import { IconPlus } from '@arco-design/web-vue/es/icon'
import { getTravelPageApi, submitTravelApi, approveTravelApi, revokeTravelApi, deleteTravelApi, type AttTravelRequest } from '@/api/travel'
import { getFlowLogsApi, getFlowTasksApi, type ApprovalFlowNode, type ApprovalFlowTask } from '@/api/flowlog'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const approving = ref(false)
const tableData = ref<AttTravelRequest[]>([])
const addVisible = ref(false)
const approveVisible = ref(false)

const searchForm = reactive({ status: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

// 表单与接口类型分离：接口 AttTravelRequest.days 为 string（后端回显），
// 表单内为 number（a-input-number 双向绑定），提交时再转字符串
interface TravelForm {
  destination: string
  days: number
  userName: string
  startTime: string
  endTime: string
  purpose: string
}

const form = reactive<TravelForm>({
  destination: '',
  days: 1,
  userName: userStore.userInfo?.nickName || '',
  startTime: '',
  endTime: '',
  purpose: '',
})

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

function handleAdd() {
  form.destination = ''
  form.days = 1
  form.startTime = ''
  form.endTime = ''
  form.purpose = ''
  addVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    // days 表单内为 number（a-input-number），提交时转 string 与后端字段对齐
    await submitTravelApi({ ...form, days: String(form.days) })
    Message.success(t('common.msg.submitDone'))
    addVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

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
    Message.success(t('common.msg.approveDone'))
    approveVisible.value = false
    loadData()
  } finally {
    approving.value = false
  }
}

async function handleRevoke(record: AttTravelRequest) {
  await revokeTravelApi(record.id)
  Message.success(t('common.msg.withdrawn'))
  loadData()
}

async function handleDelete(record: AttTravelRequest) {
  await deleteTravelApi(record.id)
  Message.success(t('common.deleteSuccess'))
  loadData()
}

function showRemark(record: AttTravelRequest) {
  Message.info({ content: record.approveRemark || '-', duration: 5000 })
}

// ============ 流程进度 ============
const flowVisible = ref(false)
const flowNodes = ref<ApprovalFlowNode[]>([])
const flowTasks = ref<ApprovalFlowTask[]>([])
const currentStep = ref(0)
const taskCurrent = ref(0)

/** 节点任务态 → 步骤状态：待审批=process，通过=finish，驳回=error，取消=wait */
function taskStepStatus(status: string | undefined): 'process' | 'finish' | 'error' | 'wait' {
  if (status === '1') return 'finish'
  if (status === '2') return 'error'
  if (status === '0') return 'process'
  return 'wait'
}

/** 节点任务描述：审批人 · 时间 · 意见（未审批节点展示待审批） */
function taskStepDesc(task: ApprovalFlowTask): string {
  const who = task.status === '0'
    ? t('common.waitingApproval')
    : (task.approverName || '-')
  const when = task.approveTime || task.createTime || ''
  return `${who} · ${when}${task.approveRemark ? ' · ' + task.approveRemark : ''}`
}

async function openFlow(record: AttTravelRequest) {
  try {
    const [taskRes, logRes] = await Promise.all([
      getFlowTasksApi('travel', record.id),
      getFlowLogsApi('travel', record.id),
    ])
    flowTasks.value = taskRes.data || []
    flowNodes.value = logRes.data || []
    const pendingIdx = flowTasks.value.findIndex((task) => task.status === '0')
    taskCurrent.value = pendingIdx >= 0 ? pendingIdx : Math.max(0, flowTasks.value.length - 1)
    currentStep.value = Math.max(0, flowNodes.value.length - 1)
    flowVisible.value = true
  } catch (err) {
    console.error('travel flow error:', err)
  }
}

onMounted(loadData)
</script>
