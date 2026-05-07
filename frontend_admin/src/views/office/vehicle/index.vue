<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="applicantName" :label="$t('office.applicantName')">
          <a-input v-model="searchForm.applicantName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 180px" />
        </a-form-item>
        <a-form-item field="status" :label="$t('office.vehicleStatus')">
          <a-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <a-option value="0">{{ $t('office.statusPending') }}</a-option>
            <a-option value="1">{{ $t('office.statusApproved') }}</a-option>
            <a-option value="2">{{ $t('office.statusRejected') }}</a-option>
            <a-option value="3">{{ $t('office.statusCancelled') }}</a-option>
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
          <span>{{ $t('office.vehicleList') }}</span>
          <a-button v-permission="PERM.office.vehicleAdd" type="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('office.applyVehicle') }}
          </a-button>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('office.applicantName')" data-index="applicantName" :width="100" />
            <a-table-column :title="$t('office.vehicleType')" :width="90">
              <template #cell="{ record }">
                {{ vehicleTypeText(record.vehicleType) }}
              </template>
            </a-table-column>
            <a-table-column :title="$t('office.purpose')" data-index="purpose" ellipsis />
            <a-table-column :title="$t('office.timeRange')" :width="180">
              <template #cell="{ record }">
                <span class="text-sm">{{ record.startTime }}<br />{{ record.endTime }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('office.destination')" data-index="destination" :width="120" />
            <a-table-column :title="$t('office.passengerCount')" data-index="passengerCount" :width="80" />
            <a-table-column :title="$t('common.status')" :width="90">
              <template #cell="{ record }">
                <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="200" align="right">
              <template #cell="{ record }">
                <a-space>
                  <template v-if="record.status === '0'">
                    <a-button v-permission="PERM.office.vehicleApprove" size="small" type="primary" @click="openApprove(record)">
                      {{ $t('office.approve') }}
                    </a-button>
                    <a-button v-permission="PERM.office.vehicleEdit" size="small" @click="handleCancel(record)">
                      {{ $t('office.cancel') }}
                    </a-button>
                  </template>
                  <a-button v-if="record.approveRemark" size="small" @click="showRemark(record)">
                    {{ $t('office.viewRemark') }}
                  </a-button>
                  <a-button size="small" @click="openFlow(record)">{{ $t('office.flow') }}</a-button>
                  <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button v-permission="PERM.office.vehicleDelete" size="small" status="danger">{{ $t('common.delete') }}</a-button>
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
    <a-modal v-model:visible="flowVisible" :title="$t('office.flow')" :width="520" footer-only>
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
      <a-empty v-else :description="$t('office.flowEmpty')" />
    </a-modal>

    <!-- 用车申请弹窗 -->
    <a-modal v-model:visible="addVisible" :title="$t('office.applyVehicle')" :width="560"
             @ok="handleSubmit" @cancel="addVisible = false" :ok-loading="submitting">
      <a-form :model="form" layout="vertical">
        <a-form-item field="vehicleType" :label="$t('office.vehicleType')" required>
          <a-select v-model="form.vehicleType">
            <a-option value="car">{{ $t('office.typeCar') }}</a-option>
            <a-option value="van">{{ $t('office.typeVan') }}</a-option>
            <a-option value="bus">{{ $t('office.typeBus') }}</a-option>
            <a-option value="other">{{ $t('office.typeOther') }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="timeRange" :label="$t('office.timeRange')" required>
          <a-date-picker v-model="form.startTime" show-time style="width: 240px" :placeholder="$t('office.startTime')" />
          <span class="mx-2">~</span>
          <a-date-picker v-model="form.endTime" show-time style="width: 240px" :placeholder="$t('office.endTime')" />
        </a-form-item>
        <a-form-item field="purpose" :label="$t('office.purpose')" required>
          <a-textarea v-model="form.purpose" :placeholder="$t('common.inputPlaceholder')" :max-length="200" show-word-limit />
        </a-form-item>
        <a-form-item field="destination" :label="$t('office.destination')">
          <a-input v-model="form.destination" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-form-item field="passengerCount" :label="$t('office.passengerCount')">
          <a-input-number v-model="form.passengerCount" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item field="deptName" :label="$t('office.deptName')">
          <a-input v-model="form.deptName" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 审批弹窗 -->
    <a-modal v-model:visible="approveVisible" :title="$t('office.approve')" :width="480"
             @ok="handleApprove" @cancel="approveVisible = false" :ok-loading="approving">
      <a-form :model="approveForm" layout="vertical">
        <a-form-item :label="$t('office.approveAction')" required>
          <a-radio-group v-model="approveForm.action" type="button">
            <a-radio value="pass">{{ $t('office.approvePass') }}</a-radio>
            <a-radio value="reject">{{ $t('office.approveReject') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="remark" :label="$t('office.approveRemark')">
          <a-textarea v-model="approveForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getVehiclePageApi, submitVehicleApi, approveVehicleApi, cancelVehicleApi, deleteVehicleApi, type OfficeVehicleRequest } from '@/api/office'
import { getFlowLogsApi, getFlowTasksApi, type ApprovalFlowNode, type ApprovalFlowTask } from '@/api/flowlog'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const approving = ref(false)
const tableData = ref<OfficeVehicleRequest[]>([])
const addVisible = ref(false)
const approveVisible = ref(false)

const searchForm = reactive({ applicantName: '', status: '' })

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const form = reactive<Partial<OfficeVehicleRequest>>({
  vehicleType: 'car',
  passengerCount: 1,
  applicantName: userStore.userInfo?.nickName || '',
})

const approveForm = reactive<{ id: string; action: 'pass' | 'reject'; remark: string }>({
  id: '',
  action: 'pass',
  remark: '',
})

const vehicleTypeMap: Record<string, string> = { car: 'enums.vehicleType.car', van: 'enums.vehicleType.van', bus: 'enums.vehicleType.bus', other: 'enums.vehicleType.other' }

function vehicleTypeText(type: string | undefined): string {
  return type != null ? t(vehicleTypeMap[type] || '-') : '-'
}

function statusText(status: string | undefined): string {
  const map: Record<string, string> = { 0: 'enums.status.pending', 1: 'enums.status.approved', 2: 'enums.status.rejected', 3: 'enums.status.cancelled' }
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
    const res = await getVehiclePageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      status: searchForm.status || undefined,
      applicantName: searchForm.applicantName || undefined,
    })
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  searchForm.applicantName = ''
  searchForm.status = ''
  pagination.current = 1
  loadData()
}

function handleAdd() {
  form.vehicleType = 'car'
  form.passengerCount = 1
  form.startTime = ''
  form.endTime = ''
  form.purpose = ''
  form.destination = ''
  form.deptName = ''
  form.applicantName = userStore.userInfo?.nickName || ''
  addVisible.value = true
}

async function handleSubmit() {
  if (!form.startTime || !form.endTime) {
    Message.warning(t('common.msg.vehicleTimeRequired'))
    return
  }
  if (!form.purpose) {
    Message.warning(t('common.msg.vehicleReasonRequired'))
    return
  }
  submitting.value = true
  try {
    await submitVehicleApi({
      applicantName: form.applicantName,
      deptName: form.deptName,
      vehicleType: form.vehicleType,
      purpose: form.purpose,
      startTime: form.startTime,
      endTime: form.endTime,
      destination: form.destination,
      passengerCount: Number(form.passengerCount) || 1,
    })
    Message.success(t('common.msg.submitSuccess'))
    addVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function openApprove(record: OfficeVehicleRequest) {
  approveForm.id = record.id
  approveForm.action = 'pass'
  approveForm.remark = ''
  approveVisible.value = true
}

async function handleApprove() {
  approving.value = true
  try {
    await approveVehicleApi(approveForm.id, approveForm.action, approveForm.remark)
    Message.success(t('common.msg.approveDone'))
    approveVisible.value = false
    loadData()
  } finally {
    approving.value = false
  }
}

async function handleCancel(record: OfficeVehicleRequest) {
  await cancelVehicleApi(record.id)
  Message.success(t('common.msg.cancelled'))
  loadData()
}

async function handleDelete(record: OfficeVehicleRequest) {
  await deleteVehicleApi(record.id)
  Message.success(t('common.deleteSuccess'))
  loadData()
}

function showRemark(record: OfficeVehicleRequest) {
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

async function openFlow(record: OfficeVehicleRequest) {
  try {
    const [taskRes, logRes] = await Promise.all([
      getFlowTasksApi('vehicle', record.id),
      getFlowLogsApi('vehicle', record.id),
    ])
    flowTasks.value = taskRes.data || []
    flowNodes.value = logRes.data || []
    const pendingIdx = flowTasks.value.findIndex((task) => task.status === '0')
    taskCurrent.value = pendingIdx >= 0 ? pendingIdx : Math.max(0, flowTasks.value.length - 1)
    currentStep.value = Math.max(0, flowNodes.value.length - 1)
    flowVisible.value = true
  } catch (err) {
    console.error('vehicle flow error:', err)
  }
}

onMounted(loadData)
</script>
