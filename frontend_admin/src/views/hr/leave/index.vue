<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :data="searchForm" :label-width="80" @submit-success="handleSearch">
        <t-form-item name="status" :label="$t('leave.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear>
            <t-option value="0">{{ $t('leave.statusPending') }}</t-option>
            <t-option value="1">{{ $t('leave.statusApproved') }}</t-option>
            <t-option value="2">{{ $t('leave.statusRejected') }}</t-option>
            <t-option value="3">{{ $t('leave.statusRevoked') }}</t-option>
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
          <span>{{ $t('leave.title') }}</span>
          <t-button v-permission="PERM.hr.leaveAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('leave.apply') }}
          </t-button>
        </div>
      </template>

      
          <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #leaveType="{ row }">{{ leaveTypeText(row.leaveType) }}</template>
    <template #timeRange="{ row }">
                <span class="text-sm">{{ row.startTime }} ~ {{ row.endTime }}</span>
              </template>
    <template #status="{ row }">
                <t-tag :color="statusColor(row.status)" variant="light">{{ statusText(row.status) }}</t-tag>
              </template>
    <template #approverName="{ row }">{{ row.approverName || '-' }}</template>
    <template #operate="{ row }">
                <t-space>
                  <template v-if="row.status === '0'">
                    <t-link theme="primary" v-permission="PERM.hr.leaveApprove" @click="openApprove(row)">
                      {{ $t('leave.approve') }}
                    </t-link>
                    <t-link theme="primary" @click="handleRevoke(row)">
                      {{ $t('leave.revoke') }}
                    </t-link>
                  </template>
                  <t-button v-if="row.approveRemark" size="small" @click="showRemark(row)">
                    {{ $t('leave.viewRemark') }}
                  </t-button>
                  <t-button size="small" @click="openFlow(row)">{{ $t('leave.flow') }}</t-button>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button v-permission="PERM.hr.leaveDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
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
      
    </t-card>

    <!-- 流程进度弹窗：优先渲染引擎节点链（任务态），未接入引擎回退事件流 -->
    <t-dialog v-model:visible="flowVisible" :title="$t('leave.flow')" :width="520" footer-only>
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
      <t-empty v-else :description="$t('leave.flowEmpty')" />
    </t-dialog>

    <!-- 请假申请弹窗 -->
    <t-dialog v-model:visible="addVisible" :title="$t('leave.apply')" :width="560"
             @confirm="handleSubmit" @cancel="addVisible = false" :ok-loading="submitting">
      <t-form ref="formRef" :data="form" layout="vertical">
        <t-form-item name="leaveType" :label="$t('leave.leaveType')" required>
          <t-select v-model="form.leaveType">
            <t-option value="annual">{{ $t('leave.typeAnnual') }}</t-option>
            <t-option value="sick">{{ $t('leave.typeSick') }}</t-option>
            <t-option value="personal">{{ $t('leave.typePersonal') }}</t-option>
            <t-option value="overtime">{{ $t('leave.typeOvertime') }}</t-option>
            <t-option value="marriage">{{ $t('leave.typeMarriage') }}</t-option>
            <t-option value="maternity">{{ $t('leave.typeMaternity') }}</t-option>
            <t-option value="other">{{ $t('leave.typeOther') }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item name="timeRange" :label="$t('leave.timeRange')" required>
          
<t-date-picker v-model="form.startTime" show-time value-type="YYYY-MM-DD HH:mm" :placeholder="$t('leave.startTime')" />
          <span class="mx-2">~</span>
          
<t-date-picker v-model="form.endTime" show-time value-type="YYYY-MM-DD HH:mm" :placeholder="$t('leave.endTime')" />
        </t-form-item>
        <t-form-item name="leaveDays" :label="$t('leave.leaveDays')">
          <t-input-number v-model="form.leaveDays" :min="0.5" :step="0.5" style="width: 100%" :placeholder="$t('leave.leaveDaysPlaceholder')" />
        </t-form-item>
        <t-form-item name="reason" :label="$t('leave.reason')" required>
          <t-textarea v-model="form.reason" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 审批弹窗 -->
    <t-dialog v-model:visible="approveVisible" :title="$t('leave.approve')" :width="480"
             @confirm="handleApprove" @cancel="approveVisible = false" :ok-loading="approving">
      <t-form ref="approveFormRef" :data="approveForm" layout="vertical">
        <t-form-item :label="$t('leave.approveAction')" required>
          <t-radio-group v-model="approveForm.action" variant="default-filled">
            <t-radio value="pass">{{ $t('leave.approvePass') }}</t-radio>
            <t-radio value="reject">{{ $t('leave.approveReject') }}</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item name="remark" :label="$t('leave.approveRemark')">
          <t-textarea v-model="approveForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted, onActivated } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { getLeavePageApi, submitLeaveApi, approveLeaveApi, revokeLeaveApi, deleteLeaveApi, type AttLeaveRequest } from '@/api/leave'
import { getFlowLogsApi, getFlowTasksApi, type ApprovalFlowNode, type ApprovalFlowTask } from '@/api/flowlog'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const approving = ref(false)
const tableData = ref<AttLeaveRequest[]>([])
const addVisible = ref(false)
const approveVisible = ref(false)

const searchForm = reactive({ status: '' })

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

// 表单与接口类型分离：接口 AttLeaveRequest.leaveDays 为 string（后端回显），
// 表单内为 number（a-input-number 双向绑定），提交时再转字符串
interface LeaveForm {
  leaveType: string
  leaveDays: number
  userName: string
  startTime: string
  endTime: string
  reason: string
}

const formRef = ref<FormInstanceFunctions>()
const form = reactive<LeaveForm>({
  leaveType: 'annual',
  leaveDays: 1,
  userName: userStore.userInfo?.nickName || '',
  startTime: '',
  endTime: '',
  reason: '',
})

const approveFormRef = ref<FormInstanceFunctions>()
const approveForm = reactive<{ id: string; action: 'pass' | 'reject'; remark: string }>({
  id: '',
  action: 'pass',
  remark: '',
})

const leaveTypeMap: Record<string, string> = {
  annual: 'enums.leaveType.annual', sick: 'enums.leaveType.sick', personal: 'enums.leaveType.personal', overtime: 'enums.leaveType.overtime',
  marriage: 'enums.leaveType.marriage', maternity: 'enums.leaveType.maternity', other: 'enums.leaveType.other',
}

function leaveTypeText(type: string | undefined): string {
  return type != null ? t(leaveTypeMap[type] || '-') : '-'
}

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
    const res = await getLeavePageApi({
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

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  searchForm.status = ''
  pagination.current = 1
  loadData()
}

function handleAdd() {
  form.leaveType = 'annual'
  form.leaveDays = 1
  form.startTime = ''
  form.endTime = ''
  form.reason = ''
  form.userName = userStore.userInfo?.nickName || ''
  addVisible.value = true
}

async function handleSubmit() {
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await formRef.value?.validate()) !== true) return
  if (!form.startTime || !form.endTime) {
    MessagePlugin.warning(t('common.msg.leaveTimeRequired'))
    return
  }
  if (!form.reason) {
    MessagePlugin.warning(t('common.msg.leaveReasonRequired'))
    return
  }
  submitting.value = true
  try {
    await submitLeaveApi({
      userName: form.userName,
      leaveType: form.leaveType,
      startTime: form.startTime,
      endTime: form.endTime,
      leaveDays: String(form.leaveDays),
      reason: form.reason,
    })
    MessagePlugin.success(t('common.msg.submitSuccess'))
    addVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function openApprove(record: AttLeaveRequest) {
  approveForm.id = record.id
  approveForm.action = 'pass'
  approveForm.remark = ''
  approveVisible.value = true
}

async function handleApprove() {
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await approveFormRef.value?.validate()) !== true) return
  approving.value = true
  try {
    await approveLeaveApi(approveForm.id, approveForm.action, approveForm.remark)
    MessagePlugin.success(t('common.msg.approveDone'))
    approveVisible.value = false
    loadData()
  } finally {
    approving.value = false
  }
}

async function handleRevoke(record: AttLeaveRequest) {
  await revokeLeaveApi(record.id)
  MessagePlugin.success(t('common.msg.withdrawn'))
  loadData()
}

async function handleDelete(record: AttLeaveRequest) {
  await deleteLeaveApi(record.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadData()
}

function showRemark(record: AttLeaveRequest) {
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

async function openFlow(record: AttLeaveRequest) {
  try {
    const [taskRes, logRes] = await Promise.all([
      getFlowTasksApi('leave', record.id),
      getFlowLogsApi('leave', record.id),
    ])
    flowTasks.value = taskRes.data || []
    flowNodes.value = logRes.data || []
    // 节点链优先：当前待审节点序号即步骤进度；空链回退事件流（-1）
    const pendingIdx = flowTasks.value.findIndex((t) => t.status === '0')
    taskCurrent.value = pendingIdx >= 0 ? pendingIdx : Math.max(0, flowTasks.value.length - 1)
    currentStep.value = Math.max(0, flowNodes.value.length - 1)
    flowVisible.value = true
  } catch (err) {
    console.error('leave flow error:', err)
  }
}

onMounted(loadData)
onActivated(loadData)

/** 表格列定义（自定义单元格见 t-table 同名插槽） */
const tableColumns = computed(() => [
  { colKey: 'userName', title: t('leave.userName'), width: 100 },
  { colKey: 'deptName', title: t('leave.deptName'), width: 120 },
  { colKey: 'leaveType', title: t('leave.leaveType'), width: 100 },
  { colKey: 'timeRange', title: t('leave.timeRange'), width: 250 },
  { colKey: 'leaveDays', title: t('leave.leaveDays'), width: 80 },
  { colKey: 'reason', title: t('leave.reason'), ellipsis: true },
  { colKey: 'status', title: t('common.status'), width: 90 },
  { colKey: 'approverName', title: t('leave.approverName'), width: 100 },
  { colKey: 'operate', title: t('common.operate'), width: 230, align: 'right' },
])

</script>
