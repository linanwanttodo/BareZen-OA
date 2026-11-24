<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('vehicleApply.title') }}</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('vehicleApply.subtitle') }}</p>
      </div>
      <t-button theme="primary" @click="openApply">
        <template #icon><add-icon /></template>
        {{ $t('vehicleApply.apply') }}
      </t-button>
    </div>

    <!-- 状态筛选 -->
    <div class="mb-4 flex items-center gap-3">
      <span class="text-sm text-[var(--muted-foreground)]">{{ $t('vehicleApply.statusLabel') }}</span>
      <t-select v-model="filterStatus" :placeholder="$t('common.selectPlaceholder')" style="width: 160px" @change="loadRequests">
        <t-option value="">{{ $t('vehicleApply.statusAll') }}</t-option>
        <t-option value="0">{{ $t('vehicleApply.statusPending') }}</t-option>
        <t-option value="1">{{ $t('vehicleApply.statusApproved') }}</t-option>
        <t-option value="2">{{ $t('vehicleApply.statusRejected') }}</t-option>
        <t-option value="3">{{ $t('vehicleApply.statusCancelled') }}</t-option>
      </t-select>
    </div>

    <!-- 我的用车申请列表 -->
    <t-loading :loading="requestLoading">
      <div v-if="requests.length" class="space-y-3">
        <div v-for="item in requests" :key="item.id" class="flex items-start gap-4 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5">
          <div class="min-w-0 flex-1">
            <div class="mb-1 flex items-center gap-2">
              <t-tag :theme="statusTheme(item.status)">{{ statusText(item.status) }}</t-tag>
              <span class="font-medium text-[var(--foreground)]">{{ vehicleTypeText(item.vehicleType) }}</span>
              <span class="text-sm text-[var(--muted-foreground)]">{{ item.startTime }} ~ {{ item.endTime }}</span>
            </div>
            <p class="text-sm text-[var(--foreground)]">{{ routeText(item) }}</p>
            <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ item.purpose || '-' }}</p>
            <p v-if="item.approveRemark" class="mt-1 text-xs text-[var(--muted-foreground)]">
              {{ $t('vehicleApply.approveRemark') }}：{{ item.approveRemark }}
            </p>
          </div>
          <t-button v-if="item.status === '0'" size="small" theme="danger" @click="handleRevoke(item)">
            {{ $t('vehicleApply.revoke') }}
          </t-button>
          <t-button size="small" @click="openFlow(item.id)">{{ $t('vehicleApply.flow') }}</t-button>
        </div>
      </div>
      <t-empty v-else :description="$t('vehicleApply.noData')" />
    </t-loading>

    <!-- 申请用车弹窗 -->
    <t-dialog v-model:visible="applyVisible" :header="$t('vehicleApply.apply')" :width="520"
             :confirm-btn="{ content: $t('common.submit'), loading: submitting }"
             @confirm="handleSubmit" @cancel="applyVisible = false" :close-on-overlay-click="false">
      <t-form ref="applyFormRef" :data="applyForm" layout="vertical">
        <t-form-item :label="$t('vehicleApply.vehicleType')" name="vehicleType">
          <t-select v-model="applyForm.vehicleType" :placeholder="$t('common.selectPlaceholder')">
            <t-option value="car">{{ $t('vehicleApply.typeCar') }}</t-option>
            <t-option value="van">{{ $t('vehicleApply.typeVan') }}</t-option>
            <t-option value="bus">{{ $t('vehicleApply.typeBus') }}</t-option>
            <t-option value="other">{{ $t('vehicleApply.typeOther') }}</t-option>
          </t-select>
        </t-form-item>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item :label="$t('vehicleApply.departure')" name="departure">
              <t-input v-model="applyForm.departure" :placeholder="$t('common.inputPlaceholder')" :max-length="200" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item :label="$t('vehicleApply.destination')" name="destination">
              <t-input v-model="applyForm.destination" :placeholder="$t('common.inputPlaceholder')" :max-length="200" />
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item :label="$t('vehicleApply.timeRange')" required name="startTime" :rules="[{ required: true, message: $t('validation.required', { field: $t('vehicleApply.timeRange') }) }]">
          <div class="flex w-full items-center">
            <t-date-picker v-model="applyForm.startTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('vehicleApply.startTime')" />
            <span class="mx-2">~</span>
            <t-date-picker v-model="applyForm.endTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('vehicleApply.endTime')" />
          </div>
        </t-form-item>
        <t-form-item :label="$t('vehicleApply.passengerCount')" name="passengerCount">
          <t-input-number v-model="applyForm.passengerCount" :min="1" :max="50" style="width: 160px" />
        </t-form-item>
        <t-form-item :label="$t('vehicleApply.purpose')" required name="purpose" :rules="[{ required: true, message: $t('validation.required', { field: $t('vehicleApply.purpose') }) }]">
          <t-textarea v-model="applyForm.purpose" :placeholder="$t('common.inputPlaceholder')" :max-length="200" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 流程进度弹窗：优先渲染引擎节点链（任务态），未接入引擎回退事件流 -->
    <t-dialog v-model:visible="flowVisible" :header="$t('vehicleApply.flow')" :width="520" footer-only>
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
      <t-empty v-else :description="$t('vehicleApply.flowEmpty')" />
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { AddIcon } from 'tdesign-icons-vue-next'
import { useI18n } from 'vue-i18n'
import {
  getMyVehiclesApi, submitVehicleApi, revokeVehicleApi,
  getFlowLogsApi, getFlowTasksApi,
  type VehicleRequest, type FlowLog, type FlowTask,
} from '@/api/enterprise'

const { t } = useI18n()
const requestLoading = ref(false)
const submitting = ref(false)
const requests = ref<VehicleRequest[]>([])
const filterStatus = ref('')
const applyVisible = ref(false)

const applyFormRef = ref<FormInstanceFunctions>()

const applyForm = reactive<{ vehicleType: string; departure: string; destination: string; startTime: string; endTime: string; passengerCount: number; purpose: string }>({
  vehicleType: 'car', departure: '', destination: '', startTime: '', endTime: '', passengerCount: 1, purpose: '',
})

function statusText(status: string | undefined): string {
  const map: Record<string, string> = {
    0: t('vehicleApply.statusPending'),
    1: t('vehicleApply.statusApproved'),
    2: t('vehicleApply.statusRejected'),
    3: t('vehicleApply.statusCancelled'),
  }
  return status != null ? map[status] || '-' : '-'
}
function statusTheme(status: string | undefined): 'default' | 'warning' | 'success' | 'danger' {
  if (status === '0') return 'warning'
  if (status === '1') return 'success'
  if (status === '2') return 'danger'
  return 'default'
}

const VEHICLE_TYPE_TEXT: Record<string, string> = {
  car: 'vehicleApply.typeCar',
  van: 'vehicleApply.typeVan',
  bus: 'vehicleApply.typeBus',
  other: 'vehicleApply.typeOther',
}
function vehicleTypeText(type: string | undefined): string {
  return type != null && VEHICLE_TYPE_TEXT[type] ? t(VEHICLE_TYPE_TEXT[type]) : (type || '-')
}

function routeText(item: VehicleRequest): string {
  const from = item.departure || '-'
  const to = item.destination || '-'
  return `${from} → ${to}`
}

async function loadRequests() {
  requestLoading.value = true
  try {
    const res = await getMyVehiclesApi({ page: 1, pageSize: 20, status: filterStatus.value || undefined })
    requests.value = res.data.list || []
  } finally {
    requestLoading.value = false
  }
}

function openApply() {
  applyForm.vehicleType = 'car'
  applyForm.departure = ''
  applyForm.destination = ''
  applyForm.startTime = ''
  applyForm.endTime = ''
  applyForm.passengerCount = 1
  applyForm.purpose = ''
  applyVisible.value = true
}

async function handleSubmit() {
  // 校验不通过则中止提交，字段错误就地显示
  if (await applyFormRef.value?.validate() !== true) return
  if (!applyForm.startTime || !applyForm.endTime) {
    MessagePlugin.warning(t('vehicleApply.timeRequired'))
    return
  }
  if (applyForm.startTime >= applyForm.endTime) {
    MessagePlugin.warning(t('vehicleApply.timeReversed'))
    return
  }
  if (!applyForm.purpose) {
    MessagePlugin.warning(t('vehicleApply.purposeRequired'))
    return
  }
  submitting.value = true
  try {
    await submitVehicleApi({
      vehicleType: applyForm.vehicleType,
      departure: applyForm.departure,
      destination: applyForm.destination,
      startTime: applyForm.startTime,
      endTime: applyForm.endTime,
      passengerCount: applyForm.passengerCount,
      purpose: applyForm.purpose,
    })
    MessagePlugin.success(t('vehicleApply.submitSuccess'))
    applyVisible.value = false
    loadRequests()
  } finally {
    submitting.value = false
  }
}

async function handleRevoke(item: VehicleRequest) {
  await revokeVehicleApi(item.id)
  MessagePlugin.success(t('vehicleApply.revoked'))
  loadRequests()
}

// ============ 流程进度 ============
const flowVisible = ref(false)
const flowNodes = ref<FlowLog[]>([])
const flowTasks = ref<FlowTask[]>([])
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
function taskStepDesc(task: FlowTask): string {
  const who = task.status === '0' ? t('vehicleApply.statusPending') : (task.approverName || '-')
  const when = task.approveTime || task.createTime || ''
  return `${who} · ${when}${task.approveRemark ? ' · ' + task.approveRemark : ''}`
}

async function openFlow(bizId: string) {
  try {
    const [taskRes, logRes] = await Promise.all([
      getFlowTasksApi('vehicle', bizId),
      getFlowLogsApi('vehicle', bizId),
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

onMounted(loadRequests)
</script>
