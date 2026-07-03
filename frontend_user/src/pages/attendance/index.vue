<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('attendance.title') }}</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('attendance.subtitle') }}</p>
      </div>
    </div>

    <!-- Tab 切换 -->
    <t-tabs v-model:active-key="activeTab" class="mb-6" @change="onTabChange">
      <t-tab-panel value="leave" :label="$t('attendance.leave')" />
      <t-tab-panel value="overtime" :label="$t('attendance.overtime')" />
      <t-tab-panel value="travel" :label="$t('attendance.travel')" />
    </t-tabs>

    <div v-if="activeTab === 'leave'">
      <!-- 请假申请表单 -->
      <t-card class="mb-6" :title="$t('attendance.applyLeave')">
        <t-form ref="leaveFormRef" :data="leaveForm" layout="vertical">
          <t-row :gutter="16">
            <t-col :span="8">
              <t-form-item :label="$t('attendance.leaveType')" required name="leaveType" :rules="[{ required: true, message: $t('validation.required', { field: $t('attendance.leaveType') }) }]">
                <t-select v-model="leaveForm.leaveType" :placeholder="$t('common.selectPlaceholder')">
                  <t-option value="annual">{{ $t('attendance.leaveTypeAnnual') }}</t-option>
                  <t-option value="sick">{{ $t('attendance.leaveTypeSick') }}</t-option>
                  <t-option value="personal">{{ $t('attendance.leaveTypePersonal') }}</t-option>
                  <t-option value="overtime">{{ $t('attendance.leaveTypeOvertime') }}</t-option>
                  <t-option value="marriage">{{ $t('attendance.leaveTypeMarriage') }}</t-option>
                  <t-option value="maternity">{{ $t('attendance.leaveTypeMaternity') }}</t-option>
                  <t-option value="other">{{ $t('attendance.leaveTypeOther') }}</t-option>
                </t-select>
              </t-form-item>
            </t-col>
            <t-col :span="16">
              <t-form-item :label="$t('attendance.timeRange')" required name="startTime" :rules="[{ required: true, message: $t('validation.required', { field: $t('attendance.timeRange') }) }]">
                
<t-date-picker v-model="leaveForm.startTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('attendance.startTime')" />
                <span class="mx-2">~</span>
                
<t-date-picker v-model="leaveForm.endTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('attendance.endTime')" />
              </t-form-item>
            </t-col>
          </t-row>
          <t-form-item :label="$t('attendance.reason')" required name="reason" :rules="[{ required: true, message: $t('validation.required', { field: $t('attendance.reason') }) }]">
            <t-textarea v-model="leaveForm.reason" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
          </t-form-item>
          <t-button theme="primary" :loading="submitting" @click="handleSubmitLeave">
            {{ $t('attendance.submit') }}
          </t-button>
        </t-form>
      </t-card>

      <!-- 我的请假申请列表 -->
      <t-card :title="$t('attendance.myLeaves')">
        <t-loading :loading="leaveLoading">
          <div v-if="leaves.length" class="space-y-3">
            <div v-for="item in leaves" :key="item.id" class="flex items-start gap-4 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5">
              <div class="min-w-0 flex-1">
                <div class="mb-1 flex items-center gap-2">
                  <t-tag :theme="statusTheme(item.status)">{{ statusText(item.status) }}</t-tag>
                  <span class="font-medium text-[var(--foreground)]">{{ leaveTypeText(item.leaveType) }}</span>
                  <span class="text-sm text-[var(--muted-foreground)]">{{ item.startTime }} ~ {{ item.endTime }}</span>
                </div>
                <p class="text-sm text-[var(--foreground)]">{{ item.reason || '-' }}</p>
                <p v-if="item.approveRemark" class="mt-1 text-xs text-[var(--muted-foreground)]">
                  {{ $t('attendance.approveRemark') }}：{{ item.approveRemark }}
                </p>
              </div>
              <t-button v-if="item.status === '0'" size="small" theme="danger" @click="handleRevokeLeave(item)">
                {{ $t('attendance.revoke') }}
              </t-button>
              <t-button size="small" @click="openFlow('leave', item.id)">{{ $t('attendance.flow') }}</t-button>
            </div>
          </div>
          <t-empty v-else :description="$t('attendance.noData')" />
        </t-loading>
      </t-card>
    </div>

    <div v-if="activeTab === 'overtime'">
      <!-- 加班申请表单 -->
      <t-card class="mb-6" :title="$t('attendance.applyOvertime')">
        <t-form ref="overtimeFormRef" :data="overtimeForm" layout="vertical">
          <t-row :gutter="16">
            <t-col :span="12">
              <t-form-item :label="$t('attendance.timeRange')" required name="startTime" :rules="[{ required: true, message: $t('validation.required', { field: $t('attendance.timeRange') }) }]">
                
<t-date-picker v-model="overtimeForm.startTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('attendance.startTime')" />
                <span class="mx-2">~</span>
                
<t-date-picker v-model="overtimeForm.endTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('attendance.endTime')" />
              </t-form-item>
            </t-col>
            <t-col :span="6">
              <t-form-item :label="$t('attendance.durationHours')">
                <t-input-number v-model="overtimeForm.durationHours" :min="0.5" :step="0.5" style="width: 100%" />
              </t-form-item>
            </t-col>
            <t-col :span="6">
              <t-form-item :label="$t('attendance.overtimeDate')">
                
<t-date-picker v-model="overtimeForm.overtimeDate" value-type="YYYY-MM-DD" style="width: 100%" />
              </t-form-item>
            </t-col>
          </t-row>
          <t-form-item :label="$t('attendance.reason')" required name="reason" :rules="[{ required: true, message: $t('validation.required', { field: $t('attendance.reason') }) }]">
            <t-textarea v-model="overtimeForm.reason" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
          </t-form-item>
          <t-button theme="primary" :loading="submitting" @click="handleSubmitOvertime">
            {{ $t('attendance.submit') }}
          </t-button>
        </t-form>
      </t-card>

      <!-- 我的加班申请列表 -->
      <t-card :title="$t('attendance.myOvertimes')">
        <t-loading :loading="overtimeLoading">
          <div v-if="overtimes.length" class="space-y-3">
            <div v-for="item in overtimes" :key="item.id" class="flex items-start gap-4 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5">
              <div class="min-w-0 flex-1">
                <div class="mb-1 flex items-center gap-2">
                  <t-tag :theme="statusTheme(item.status)">{{ statusText(item.status) }}</t-tag>
                  <span class="text-sm text-[var(--muted-foreground)]">{{ item.startTime }} ~ {{ item.endTime }}</span>
                  <span v-if="item.durationHours" class="text-sm text-[var(--muted-foreground)]">{{ item.durationHours }}h</span>
                </div>
                <p class="text-sm text-[var(--foreground)]">{{ item.reason || '-' }}</p>
                <p v-if="item.approveRemark" class="mt-1 text-xs text-[var(--muted-foreground)]">
                  {{ $t('attendance.approveRemark') }}：{{ item.approveRemark }}
                </p>
              </div>
              <t-button v-if="item.status === '0'" size="small" theme="danger" @click="handleRevokeOvertime(item)">
                {{ $t('attendance.revoke') }}
              </t-button>
              <t-button size="small" @click="openFlow('overtime', item.id)">{{ $t('attendance.flow') }}</t-button>
            </div>
          </div>
          <t-empty v-else :description="$t('attendance.noData')" />
        </t-loading>
      </t-card>
    </div>

    <div v-else-if="activeTab === 'travel'">
      <!-- 出差申请表单 -->
      <t-card class="mb-6" :title="$t('attendance.applyTravel')">
        <t-form ref="travelFormRef" :data="travelForm" layout="vertical">
          <t-row :gutter="16">
            <t-col :span="12">
              <t-form-item :label="$t('attendance.destination')" required name="destination" :rules="[{ required: true, message: $t('validation.required', { field: $t('attendance.destination') }) }]">
                <t-input v-model="travelForm.destination" :placeholder="$t('common.inputPlaceholder')" />
              </t-form-item>
            </t-col>
            <t-col :span="12">
              <t-form-item :label="$t('attendance.timeRange')" required name="startTime" :rules="[{ required: true, message: $t('validation.required', { field: $t('attendance.timeRange') }) }]">
                
<t-date-picker v-model="travelForm.startTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('attendance.startTime')" />
                <span class="mx-2">~</span>
                
<t-date-picker v-model="travelForm.endTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('attendance.endTime')" />
              </t-form-item>
            </t-col>
            <t-col :span="6">
              <t-form-item :label="$t('attendance.days')">
                <t-input-number v-model="travelForm.days" :min="0.5" :step="0.5" style="width: 100%" />
              </t-form-item>
            </t-col>
          </t-row>
          <t-form-item :label="$t('attendance.purpose')" required name="purpose" :rules="[{ required: true, message: $t('validation.required', { field: $t('attendance.purpose') }) }]">
            <t-textarea v-model="travelForm.purpose" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
          </t-form-item>
          <t-button theme="primary" :loading="submitting" @click="handleSubmitTravel">
            {{ $t('attendance.submit') }}
          </t-button>
        </t-form>
      </t-card>

      <!-- 我的出差申请列表 -->
      <t-card :title="$t('attendance.myTravels')">
        <t-loading :loading="travelLoading">
          <div v-if="travels.length" class="space-y-3">
            <div v-for="item in travels" :key="item.id" class="flex items-start gap-4 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5">
              <div class="min-w-0 flex-1">
                <div class="mb-1 flex items-center gap-2">
                  <t-tag :theme="statusTheme(item.status)">{{ statusText(item.status) }}</t-tag>
                  <span class="font-medium text-[var(--foreground)]">{{ item.destination }}</span>
                  <span class="text-sm text-[var(--muted-foreground)]">{{ item.startTime }} ~ {{ item.endTime }}</span>
                </div>
                <p class="text-sm text-[var(--foreground)]">{{ item.purpose || '-' }}</p>
                <p v-if="item.approveRemark" class="mt-1 text-xs text-[var(--muted-foreground)]">
                  {{ $t('attendance.approveRemark') }}：{{ item.approveRemark }}
                </p>
              </div>
              <t-button v-if="item.status === '0'" size="small" theme="danger" @click="handleRevokeTravel(item)">
                {{ $t('attendance.revoke') }}
              </t-button>
              <t-button size="small" @click="openFlow('travel', item.id)">{{ $t('attendance.flow') }}</t-button>
            </div>
          </div>
          <t-empty v-else :description="$t('attendance.noData')" />
        </t-loading>
      </t-card>
    </div>

    <!-- 流程进度弹窗：优先渲染引擎节点链（任务态），未接入引擎回退事件流 -->
    <t-dialog v-model:visible="flowVisible" :header="$t('attendance.flow')" :width="520" footer-only>
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
      <t-empty v-else :description="$t('attendance.flowEmpty')" />
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import {
  getMyLeavesApi, submitLeaveApi, revokeLeaveApi,
  getMyOvertimesApi, submitOvertimeApi, revokeOvertimeApi,
  getMyTravelsApi, submitTravelApi, revokeTravelApi,
  getFlowLogsApi, getFlowTasksApi,
  type LeaveRequest, type OvertimeRequest, type TravelRequest, type FlowLog, type FlowTask,
} from '@/api/enterprise'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const activeTab = ref('leave')
const submitting = ref(false)
const leaveLoading = ref(false)
const overtimeLoading = ref(false)
const travelLoading = ref(false)
const leaves = ref<LeaveRequest[]>([])
const overtimes = ref<OvertimeRequest[]>([])
const travels = ref<TravelRequest[]>([])

const leaveFormRef = ref<FormInstanceFunctions>()

const leaveForm = reactive<Partial<LeaveRequest>>({
  userName: userStore.userInfo?.nickName || '',
})
const overtimeFormRef = ref<FormInstanceFunctions>()

const overtimeForm = reactive<Partial<OvertimeRequest>>({
  userName: userStore.userInfo?.nickName || '',
  durationHours: 1,
})
const travelFormRef = ref<FormInstanceFunctions>()

const travelForm = reactive<Partial<TravelRequest>>({
  userName: userStore.userInfo?.nickName || '',
  days: 1,
})

const LEAVE_TYPE_TEXT: Record<string, string> = {
  annual: '年假', sick: '病假', personal: '事假', overtime: '调休',
  marriage: '婚假', maternity: '产假', other: '其他',
}
function leaveTypeText(type: string | undefined): string {
  return type != null ? LEAVE_TYPE_TEXT[type] || type : '-'
}

function statusText(status: string | undefined): string {
  const map: Record<string, string> = { 0: '待审批', 1: '已通过', 2: '已驳回', 3: '已撤销' }
  return status != null ? map[status] || '-' : '-'
}
function statusTheme(status: string | undefined): 'default' | 'warning' | 'success' | 'danger' {
  if (status === '0') return 'warning'
  if (status === '1') return 'success'
  if (status === '2') return 'danger'
  return 'default'
}

async function loadLeaves() {
  leaveLoading.value = true
  try {
    const res = await getMyLeavesApi({ page: 1, pageSize: 20 })
    leaves.value = res.data.list || []
  } finally {
    leaveLoading.value = false
  }
}

async function loadOvertimes() {
  overtimeLoading.value = true
  try {
    const res = await getMyOvertimesApi({ page: 1, pageSize: 20 })
    overtimes.value = res.data.list || []
  } finally {
    overtimeLoading.value = false
  }
}

async function loadTravels() {
  travelLoading.value = true
  try {
    const res = await getMyTravelsApi({ page: 1, pageSize: 20 })
    travels.value = res.data.list || []
  } finally {
    travelLoading.value = false
  }
}

function onTabChange(key: string | number) {
  if (key === 'leave') loadLeaves()
  else if (key === 'overtime') loadOvertimes()
  else loadTravels()
}

async function handleSubmitLeave() {
  // 校验不通过则中止提交，字段错误就地显示
  if (await leaveFormRef.value?.validate() !== true) return
  if (!leaveForm.leaveType) {
    MessagePlugin.warning('请选择请假类型')
    return
  }
  if (!leaveForm.startTime || !leaveForm.endTime) {
    MessagePlugin.warning('请选择请假时间')
    return
  }
  if (!leaveForm.reason) {
    MessagePlugin.warning('请填写请假事由')
    return
  }
  submitting.value = true
  try {
    await submitLeaveApi({
      userName: leaveForm.userName,
      leaveType: leaveForm.leaveType,
      startTime: leaveForm.startTime,
      endTime: leaveForm.endTime,
      reason: leaveForm.reason,
    })
    MessagePlugin.success('提交成功，等待审批')
    leaveForm.leaveType = ''
    leaveForm.startTime = ''
    leaveForm.endTime = ''
    leaveForm.reason = ''
    loadLeaves()
  } finally {
    submitting.value = false
  }
}

async function handleSubmitOvertime() {
  // 校验不通过则中止提交，字段错误就地显示
  if (await overtimeFormRef.value?.validate() !== true) return
  if (!overtimeForm.startTime || !overtimeForm.endTime) {
    MessagePlugin.warning('请选择加班时间')
    return
  }
  if (!overtimeForm.reason) {
    MessagePlugin.warning('请填写加班事由')
    return
  }
  submitting.value = true
  try {
    await submitOvertimeApi({
      userName: overtimeForm.userName,
      overtimeDate: overtimeForm.overtimeDate,
      startTime: overtimeForm.startTime,
      endTime: overtimeForm.endTime,
      durationHours: Number(overtimeForm.durationHours) || 1,
      reason: overtimeForm.reason,
    })
    MessagePlugin.success('提交成功，等待审批')
    overtimeForm.startTime = ''
    overtimeForm.endTime = ''
    overtimeForm.overtimeDate = ''
    overtimeForm.reason = ''
    loadOvertimes()
  } finally {
    submitting.value = false
  }
}

async function handleSubmitTravel() {
  // 校验不通过则中止提交，字段错误就地显示
  if (await travelFormRef.value?.validate() !== true) return
  if (!travelForm.destination) {
    MessagePlugin.warning('请填写目的地')
    return
  }
  if (!travelForm.startTime || !travelForm.endTime) {
    MessagePlugin.warning('请选择出差时间')
    return
  }
  if (!travelForm.purpose) {
    MessagePlugin.warning('请填写出差事由')
    return
  }
  submitting.value = true
  try {
    await submitTravelApi({
      userName: travelForm.userName,
      destination: travelForm.destination,
      startTime: travelForm.startTime,
      endTime: travelForm.endTime,
      days: Number(travelForm.days) || 1,
      purpose: travelForm.purpose,
    })
    MessagePlugin.success('提交成功，等待审批')
    travelForm.destination = ''
    travelForm.startTime = ''
    travelForm.endTime = ''
    travelForm.purpose = ''
    loadTravels()
  } finally {
    submitting.value = false
  }
}

async function handleRevokeLeave(item: LeaveRequest) {
  await revokeLeaveApi(item.id)
  MessagePlugin.success('已撤销')
  loadLeaves()
}

async function handleRevokeOvertime(item: OvertimeRequest) {
  await revokeOvertimeApi(item.id)
  MessagePlugin.success('已撤销')
  loadOvertimes()
}

async function handleRevokeTravel(item: TravelRequest) {
  await revokeTravelApi(item.id)
  MessagePlugin.success('已撤销')
  loadTravels()
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
  const who = task.status === '0' ? '待审批' : (task.approverName || '-')
  const when = task.approveTime || task.createTime || ''
  return `${who} · ${when}${task.approveRemark ? ' · ' + task.approveRemark : ''}`
}

async function openFlow(bizType: string, bizId: string) {
  try {
    const [taskRes, logRes] = await Promise.all([
      getFlowTasksApi(bizType, bizId),
      getFlowLogsApi(bizType, bizId),
    ])
    flowTasks.value = taskRes.data || []
    flowNodes.value = logRes.data || []
    const pendingIdx = flowTasks.value.findIndex((task) => task.status === '0')
    taskCurrent.value = pendingIdx >= 0 ? pendingIdx : Math.max(0, flowTasks.value.length - 1)
    currentStep.value = Math.max(0, flowNodes.value.length - 1)
    flowVisible.value = true
  } catch (err) {
    console.error('attendance flow error:', err)
  }
}

onMounted(loadLeaves)
</script>
