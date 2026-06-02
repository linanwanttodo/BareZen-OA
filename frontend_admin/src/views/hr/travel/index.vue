<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <t-form-item field="status" :label="$t('travel.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <t-option value="0">{{ $t('travel.statusPending') }}</t-option>
            <t-option value="1">{{ $t('travel.statusApproved') }}</t-option>
            <t-option value="2">{{ $t('travel.statusRejected') }}</t-option>
            <t-option value="3">{{ $t('travel.statusRevoked') }}</t-option>
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
          <span>{{ $t('travel.title') }}</span>
          <t-button v-permission="PERM.hr.travelAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('travel.apply') }}
          </t-button>
        </div>
      </template>

      <t-loading :loading="loading">
          <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
    <template #startTime="{ row }">
                <span class="text-sm">{{ row.startTime }}<br />{{ row.endTime }}</span>
              </template>
    <template #status="{ row }">
                <t-tag :color="statusColor(row.status)">{{ statusText(row.status) }}</t-tag>
              </template>
    <template #approverName="{ row }">{{ row.approverName || '-' }}</template>
    <template #operate="{ row }">
                <t-space>
                  <template v-if="row.status === '0'">
                    <t-button v-permission="PERM.hr.travelApprove" size="small" theme="primary" @click="openApprove(row)">
                      {{ $t('travel.approve') }}
                    </t-button>
                    <t-button v-permission="PERM.hr.travelEdit" size="small" @click="handleRevoke(row)">
                      {{ $t('travel.revoke') }}
                    </t-button>
                  </template>
                  <t-button v-if="row.approveRemark" size="small" @click="showRemark(row)">
                    {{ $t('travel.viewRemark') }}
                  </t-button>
                  <t-button size="small" @click="openFlow(row)">{{ $t('travel.flow') }}</t-button>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button v-permission="PERM.hr.travelDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
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
    <t-dialog v-model="flowVisible" :title="$t('travel.flow')" :width="520" footer-only>
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
      <t-empty v-else :description="$t('travel.flowEmpty')" />
    </t-dialog>

    <!-- 出差申请弹窗 -->
    <t-dialog v-model="addVisible" :title="$t('travel.apply')" :width="560"
             @confirm="handleSubmit" @cancel="addVisible = false" :ok-loading="submitting">
      <t-form :model="form" layout="vertical">
        <t-form-item field="destination" :label="$t('travel.destination')" required>
          <t-input v-model="form.destination" :placeholder="$t('common.inputPlaceholder')" :max-length="100" />
        </t-form-item>
        <t-form-item field="timeRange" :label="$t('travel.timeRange')" required>
          
<!-- TODO(date-picker): 核对返回值格式（原 Arco 为字符串） --><t-date-picker v-model="form.startTime" show-time style="width: 260px" :placeholder="$t('travel.startTime')" />
          <span class="mx-2">~</span>
          
<!-- TODO(date-picker): 核对返回值格式（原 Arco 为字符串） --><t-date-picker v-model="form.endTime" show-time style="width: 260px" :placeholder="$t('travel.endTime')" />
        </t-form-item>
        <t-form-item field="days" :label="$t('travel.days')">
          <t-input-number v-model="form.days" :min="0.5" :step="0.5" style="width: 100%" :placeholder="$t('travel.daysPlaceholder')" />
        </t-form-item>
        <t-form-item field="purpose" :label="$t('travel.purpose')" required>
          <t-textarea v-model="form.purpose" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 审批弹窗 -->
    <t-dialog v-model="approveVisible" :title="$t('travel.approve')" :width="480"
             @confirm="handleApprove" @cancel="approveVisible = false" :ok-loading="approving">
      <t-form :model="approveForm" layout="vertical">
        <t-form-item :label="$t('travel.approveAction')" required>
          <t-radio-group v-model="approveForm.action" variant="default-filled">
            <t-radio value="pass">{{ $t('travel.approvePass') }}</t-radio>
            <t-radio value="reject">{{ $t('travel.approveReject') }}</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item field="remark" :label="$t('travel.approveRemark')">
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

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'userName', title: t('travel.userName'), width: 100 },
    { colKey: 'destination', title: t('travel.destination'), width: 140 },
    { colKey: 'purpose', title: t('travel.purpose'), ellipsis: true },
    { colKey: 'startTime', title: t('travel.timeRange'), width: 180 },
    { colKey: 'days', title: t('travel.days'), width: 80 },
    { colKey: 'status', title: t('common.status'), width: 90 },
    { colKey: 'approverName', title: t('travel.approverName'), width: 100 },
    { colKey: 'operate', title: t('common.operate'), width: 200, align: 'right' },
  ])

</script>
