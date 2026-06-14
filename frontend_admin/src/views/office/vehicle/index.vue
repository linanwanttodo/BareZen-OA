<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <t-form-item field="applicantName" :label="$t('office.applicantName')">
          <t-input v-model="searchForm.applicantName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 180px" />
        </t-form-item>
        <t-form-item field="status" :label="$t('office.vehicleStatus')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <t-option value="0">{{ $t('office.statusPending') }}</t-option>
            <t-option value="1">{{ $t('office.statusApproved') }}</t-option>
            <t-option value="2">{{ $t('office.statusRejected') }}</t-option>
            <t-option value="3">{{ $t('office.statusCancelled') }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" html-type="submit">{{ $t('common.search') }}</t-button>
            <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-card>

    <!-- 表格 -->
    <t-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('office.vehicleList') }}</span>
          <t-button v-permission="PERM.office.vehicleAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('office.applyVehicle') }}
          </t-button>
        </div>
      </template>

      <t-loading :loading="loading">
          <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
    <template #vehicleType="{ row }">{{ vehicleTypeText(row.vehicleType) }}</template>
    <template #timeRange="{ row }">
                <span class="text-sm">{{ row.startTime }} ~ {{ row.endTime }}</span>
              </template>
    <template #status="{ row }">
                <t-tag :color="statusColor(row.status)">{{ statusText(row.status) }}</t-tag>
              </template>
    <template #approverName="{ row }">{{ row.approverName || '-' }}</template>
    <template #operate="{ row }">
                <t-space>
                  <template v-if="row.status === '0'">
                    <t-button v-permission="PERM.office.vehicleApprove" size="small" theme="primary" @click="openApprove(row)">
                      {{ $t('office.approve') }}
                    </t-button>
                    <t-button size="small" @click="handleCancel(row)">
                      {{ $t('office.cancel') }}
                    </t-button>
                  </template>
                  <t-button v-if="row.approveRemark" size="small" @click="showRemark(row)">
                    {{ $t('office.viewRemark') }}
                  </t-button>
                  <t-button size="small" @click="openFlow(row)">{{ $t('office.flow') }}</t-button>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button v-permission="PERM.office.vehicleDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
  </t-table>

        <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
          <t-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      </t-loading>
    </t-card>

    <!-- 流程进度弹窗：优先渲染引擎节点链（任务态），未接入引擎回退事件流 -->
    <t-dialog v-model="flowVisible" :title="$t('office.flow')" :width="520" footer-only>
      <t-steps v-if="flowTasks.length" direction="vertical" :current="taskCurrent">
        <t-step-item
          v-for="task in flowTasks"
          :key="task.id"
          :status="taskStepStatus(task.status)"
          :title="task.nodeName"
          :description="taskStepDesc(task)"
        />
      </t-steps>
      <t-steps v-else-if="flowNodes.length" direction="vertical" :current="currentStep">
        <t-step-item
          v-for="node in flowNodes"
          :key="node.id"
          :title="node.nodeName"
          :description="`${node.operatorName || '-'} · ${node.createTime}${node.remark ? ' · ' + node.remark : ''}`"
        />
      </t-steps>
      <t-empty v-else :description="$t('office.flowEmpty')" />
    </t-dialog>

    <!-- 用车申请弹窗 -->
    <t-dialog v-model="addVisible" :title="$t('office.applyVehicle')" :width="560"
             @confirm="handleSubmit" @cancel="addVisible = false" :ok-loading="submitting">
      <t-form :model="form" layout="vertical">
        <t-form-item field="vehicleType" :label="$t('office.vehicleType')" required>
          <t-select v-model="form.vehicleType">
            <t-option value="car">{{ $t('office.typeCar') }}</t-option>
            <t-option value="van">{{ $t('office.typeVan') }}</t-option>
            <t-option value="bus">{{ $t('office.typeBus') }}</t-option>
            <t-option value="other">{{ $t('office.typeOther') }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item field="timeRange" :label="$t('office.timeRange')" required>
          
<t-date-picker v-model="form.startTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 240px" :placeholder="$t('office.startTime')" />
          <span class="mx-2">~</span>
          
<t-date-picker v-model="form.endTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 240px" :placeholder="$t('office.endTime')" />
        </t-form-item>
        <t-form-item field="purpose" :label="$t('office.purpose')" required>
          <t-textarea v-model="form.purpose" :placeholder="$t('common.inputPlaceholder')" :max-length="200" show-word-limit />
        </t-form-item>
        <t-form-item field="destination" :label="$t('office.destination')">
          <t-input v-model="form.destination" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item field="passengerCount" :label="$t('office.passengerCount')">
          <t-input-number v-model="form.passengerCount" :min="1" style="width: 100%" />
        </t-form-item>
        <t-form-item field="deptName" :label="$t('office.deptName')">
          <t-input v-model="form.deptName" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 审批弹窗 -->
    <t-dialog v-model="approveVisible" :title="$t('office.approve')" :width="480"
             @confirm="handleApprove" @cancel="approveVisible = false" :ok-loading="approving">
      <t-form :model="approveForm" layout="vertical">
        <t-form-item :label="$t('office.approveAction')" required>
          <t-radio-group v-model="approveForm.action" variant="default-filled">
            <t-radio value="pass">{{ $t('office.approvePass') }}</t-radio>
            <t-radio value="reject">{{ $t('office.approveReject') }}</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item field="remark" :label="$t('office.approveRemark')">
          <t-textarea v-model="approveForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
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
    MessagePlugin.warning(t('common.msg.vehicleTimeRequired'))
    return
  }
  if (!form.purpose) {
    MessagePlugin.warning(t('common.msg.vehicleReasonRequired'))
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
    MessagePlugin.success(t('common.msg.submitSuccess'))
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
    MessagePlugin.success(t('common.msg.approveDone'))
    approveVisible.value = false
    loadData()
  } finally {
    approving.value = false
  }
}

async function handleCancel(record: OfficeVehicleRequest) {
  await cancelVehicleApi(record.id)
  MessagePlugin.success(t('common.msg.cancelled'))
  loadData()
}

async function handleDelete(record: OfficeVehicleRequest) {
  await deleteVehicleApi(record.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadData()
}

function showRemark(record: OfficeVehicleRequest) {
  MessagePlugin.info({ content: record.approveRemark || '-', duration: 5000 })
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

/** 表格列定义（自定义单元格见 t-table 同名插槽） */
const tableColumns = computed(() => [
  { colKey: 'applicantName', title: t('office.applicantName'), width: 100 },
  { colKey: 'deptName', title: t('office.deptName'), width: 120 },
  { colKey: 'vehicleType', title: t('office.vehicleType'), width: 90 },
  { colKey: 'timeRange', title: t('office.timeRange'), width: 250 },
  { colKey: 'destination', title: t('office.destination'), width: 120, ellipsis: true },
  { colKey: 'purpose', title: t('office.purpose'), ellipsis: true },
  { colKey: 'status', title: t('office.vehicleStatus'), width: 90 },
  { colKey: 'approverName', title: t('leave.approverName'), width: 100 },
  { colKey: 'operate', title: t('common.operate'), width: 230, align: 'right' },
])

</script>
