<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <t-form-item field="status" :label="$t('overtime.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <t-option value="0">{{ $t('overtime.statusPending') }}</t-option>
            <t-option value="1">{{ $t('overtime.statusApproved') }}</t-option>
            <t-option value="2">{{ $t('overtime.statusRejected') }}</t-option>
            <t-option value="3">{{ $t('overtime.statusRevoked') }}</t-option>
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
          <span>{{ $t('overtime.title') }}</span>
          <t-button v-permission="PERM.hr.overtimeAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('overtime.apply') }}
          </t-button>
        </div>
      </template>

      <t-loading :loading="loading">
          <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
    <template #startTime="{ row }">
                <span class="text-sm">{{ row.startTime }} ~ {{ row.endTime }}</span>
              </template>
    <template #status="{ row }">
                <t-tag :color="statusColor(row.status)">{{ statusText(row.status) }}</t-tag>
              </template>
    <template #approverName="{ row }">{{ row.approverName || '-' }}</template>
    <template #operate="{ row }">
                <t-space>
                  <template v-if="row.status === '0'">
                    <t-button v-permission="PERM.hr.overtimeApprove" size="small" theme="primary" @click="openApprove(row)">
                      {{ $t('overtime.approve') }}
                    </t-button>
                    <t-button v-permission="PERM.hr.overtimeEdit" size="small" @click="handleRevoke(row)">
                      {{ $t('overtime.revoke') }}
                    </t-button>
                  </template>
                  <t-button v-if="row.approveRemark" size="small" @click="showRemark(row)">
                    {{ $t('overtime.viewRemark') }}
                  </t-button>
                  <t-button size="small" @click="openFlow(row)">{{ $t('overtime.flow') }}</t-button>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button v-permission="PERM.hr.overtimeDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
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
    <t-dialog v-model="flowVisible" :title="$t('overtime.flow')" :width="520" footer-only>
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
      <t-empty v-else :description="$t('overtime.flowEmpty')" />
    </t-dialog>

    <!-- 加班申请弹窗 -->
    <t-dialog v-model="addVisible" :title="$t('overtime.apply')" :width="560"
             @confirm="handleSubmit" @cancel="addVisible = false" :ok-loading="submitting">
      <t-form :model="form" layout="vertical">
        <t-form-item field="timeRange" :label="$t('overtime.timeRange')" required>
          
<!-- TODO(date-picker): 核对返回值格式（原 Arco 为字符串） --><t-date-picker v-model="form.startTime" show-time style="width: 260px" :placeholder="$t('overtime.startTime')" />
          <span class="mx-2">~</span>
          
<!-- TODO(date-picker): 核对返回值格式（原 Arco 为字符串） --><t-date-picker v-model="form.endTime" show-time style="width: 260px" :placeholder="$t('overtime.endTime')" />
        </t-form-item>
        <t-form-item field="durationHours" :label="$t('overtime.durationHours')">
          <t-input-number v-model="form.durationHours" :min="0.5" :step="0.5" style="width: 100%" :placeholder="$t('overtime.durationHoursPlaceholder')" />
        </t-form-item>
        <t-form-item field="reason" :label="$t('overtime.reason')" required>
          <t-textarea v-model="form.reason" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 审批弹窗 -->
    <t-dialog v-model="approveVisible" :title="$t('overtime.approve')" :width="480"
             @confirm="handleApprove" @cancel="approveVisible = false" :ok-loading="approving">
      <t-form :model="approveForm" layout="vertical">
        <t-form-item :label="$t('overtime.approveAction')" required>
          <t-radio-group v-model="approveForm.action" variant="default-filled">
            <t-radio value="pass">{{ $t('overtime.approvePass') }}</t-radio>
            <t-radio value="reject">{{ $t('overtime.approveReject') }}</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item field="remark" :label="$t('overtime.approveRemark')">
          <t-textarea v-model="approveForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
import { MessagePlugin as Message } from 'tdesign-vue-next'
import { getOvertimePageApi, submitOvertimeApi, approveOvertimeApi, revokeOvertimeApi, deleteOvertimeApi, type AttOvertimeRequest } from '@/api/overtime'
import { getFlowLogsApi, getFlowTasksApi, type ApprovalFlowNode, type ApprovalFlowTask } from '@/api/flowlog'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const approving = ref(false)
const tableData = ref<AttOvertimeRequest[]>([])
const addVisible = ref(false)
const approveVisible = ref(false)

const searchForm = reactive({ status: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

// 表单与接口类型分离：接口 AttOvertimeRequest.durationHours 为 string（后端回显），
// 表单内为 number（a-input-number 双向绑定），提交时再转字符串
interface OvertimeForm {
  durationHours: number
  userName: string
  startTime: string
  endTime: string
  reason: string
}

const form = reactive<OvertimeForm>({
  durationHours: 1,
  userName: userStore.userInfo?.nickName || '',
  startTime: '',
  endTime: '',
  reason: '',
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

// ============ 新增申请 ============
function handleAdd() {
  form.durationHours = 1
  form.startTime = ''
  form.endTime = ''
  form.reason = ''
  form.userName = userStore.userInfo?.nickName || ''
  addVisible.value = true
}

async function handleSubmit() {
  if (!form.startTime || !form.endTime) {
    Message.warning(t('overtime.timeRequired'))
    return
  }
  if (!form.reason) {
    Message.warning(t('overtime.reasonRequired'))
    return
  }
  submitting.value = true
  try {
    await submitOvertimeApi({
      userName: form.userName,
      startTime: form.startTime,
      endTime: form.endTime,
      durationHours: String(form.durationHours),
      reason: form.reason,
    })
    Message.success(t('common.msg.submitSuccess'))
    addVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

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

async function openFlow(record: AttOvertimeRequest) {
  try {
    const [taskRes, logRes] = await Promise.all([
      getFlowTasksApi('overtime', record.id),
      getFlowLogsApi('overtime', record.id),
    ])
    flowTasks.value = taskRes.data || []
    flowNodes.value = logRes.data || []
    const pendingIdx = flowTasks.value.findIndex((task) => task.status === '0')
    taskCurrent.value = pendingIdx >= 0 ? pendingIdx : Math.max(0, flowTasks.value.length - 1)
    currentStep.value = Math.max(0, flowNodes.value.length - 1)
    flowVisible.value = true
  } catch (err) {
    console.error('overtime flow error:', err)
  }
}

onMounted(loadData)

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'userName', title: t('overtime.userName'), width: 100 },
    { colKey: 'startTime', title: t('overtime.timeRange') },
    { colKey: 'durationHours', title: t('overtime.durationHours'), width: 90 },
    { colKey: 'reason', title: t('overtime.reason'), ellipsis: true },
    { colKey: 'status', title: t('common.status'), width: 90 },
    { colKey: 'approverName', title: t('overtime.approverName'), width: 100 },
    { colKey: 'operate', title: t('common.operate'), width: 200, align: 'right' },
  ])

</script>
