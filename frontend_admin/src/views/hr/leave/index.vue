<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="status" :label="$t('leave.status')">
          <a-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <a-option value="0">{{ $t('leave.statusPending') }}</a-option>
            <a-option value="1">{{ $t('leave.statusApproved') }}</a-option>
            <a-option value="2">{{ $t('leave.statusRejected') }}</a-option>
            <a-option value="3">{{ $t('leave.statusRevoked') }}</a-option>
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
          <span>{{ $t('leave.title') }}</span>
          <a-button v-permission="'hr:leave:add'" type="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('leave.apply') }}
          </a-button>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('leave.userName')" data-index="userName" :width="100" />
            <a-table-column :title="$t('leave.leaveType')" :width="100">
              <template #cell="{ record }">
                <a-tag>{{ leaveTypeText(record.leaveType) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('leave.timeRange')">
              <template #cell="{ record }">
                <span class="text-sm">{{ record.startTime }} ~ {{ record.endTime }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('leave.leaveDays')" data-index="leaveDays" :width="80" />
            <a-table-column :title="$t('leave.reason')" data-index="reason" ellipsis />
            <a-table-column :title="$t('common.status')" :width="90">
              <template #cell="{ record }">
                <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('leave.approverName')" data-index="approverName" :width="100">
              <template #cell="{ record }">{{ record.approverName || '-' }}</template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="200" align="right">
              <template #cell="{ record }">
                <a-space>
                  <!-- 待审批：管理员可审批，申请人可撤销 -->
                  <template v-if="record.status === '0'">
                    <a-button v-permission="'hr:leave:approve'" size="small" type="primary" @click="openApprove(record)">
                      {{ $t('leave.approve') }}
                    </a-button>
                    <a-button v-permission="'hr:leave:edit'" size="small" @click="handleRevoke(record)">
                      {{ $t('leave.revoke') }}
                    </a-button>
                  </template>
                  <!-- 已审批：展示审批意见 -->
                  <a-button v-if="record.approveRemark" size="small" @click="showRemark(record)">
                    {{ $t('leave.viewRemark') }}
                  </a-button>
                  <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button v-permission="'hr:leave:delete'" size="small" status="danger">{{ $t('common.delete') }}</a-button>
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

    <!-- 请假申请弹窗 -->
    <a-modal v-model:visible="addVisible" :title="$t('leave.apply')" :width="560"
             @ok="handleSubmit" @cancel="addVisible = false" :ok-loading="submitting">
      <a-form :model="form" layout="vertical">
        <a-form-item field="leaveType" :label="$t('leave.leaveType')" required>
          <a-select v-model="form.leaveType">
            <a-option value="annual">{{ $t('leave.typeAnnual') }}</a-option>
            <a-option value="sick">{{ $t('leave.typeSick') }}</a-option>
            <a-option value="personal">{{ $t('leave.typePersonal') }}</a-option>
            <a-option value="overtime">{{ $t('leave.typeOvertime') }}</a-option>
            <a-option value="marriage">{{ $t('leave.typeMarriage') }}</a-option>
            <a-option value="maternity">{{ $t('leave.typeMaternity') }}</a-option>
            <a-option value="other">{{ $t('leave.typeOther') }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="timeRange" :label="$t('leave.timeRange')" required>
          <a-date-picker v-model="form.startTime" show-time style="width: 260px" :placeholder="$t('leave.startTime')" />
          <span class="mx-2">~</span>
          <a-date-picker v-model="form.endTime" show-time style="width: 260px" :placeholder="$t('leave.endTime')" />
        </a-form-item>
        <a-form-item field="leaveDays" :label="$t('leave.leaveDays')">
          <a-input-number v-model="form.leaveDays" :min="0.5" :step="0.5" style="width: 100%" :placeholder="$t('leave.leaveDaysPlaceholder')" />
        </a-form-item>
        <a-form-item field="reason" :label="$t('leave.reason')" required>
          <a-textarea v-model="form.reason" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 审批弹窗 -->
    <a-modal v-model:visible="approveVisible" :title="$t('leave.approve')" :width="480"
             @ok="handleApprove" @cancel="approveVisible = false" :ok-loading="approving">
      <a-form :model="approveForm" layout="vertical">
        <a-form-item :label="$t('leave.approveAction')" required>
          <a-radio-group v-model="approveForm.action" type="button">
            <a-radio value="pass">{{ $t('leave.approvePass') }}</a-radio>
            <a-radio value="reject">{{ $t('leave.approveReject') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="remark" :label="$t('leave.approveRemark')">
          <a-textarea v-model="approveForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getLeavePageApi, submitLeaveApi, approveLeaveApi, revokeLeaveApi, deleteLeaveApi, type AttLeaveRequest } from '@/api/leave'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const approving = ref(false)
const tableData = ref<AttLeaveRequest[]>([])
const addVisible = ref(false)
const approveVisible = ref(false)

const searchForm = reactive({ status: '' })

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const form = reactive<Partial<AttLeaveRequest>>({
  leaveType: 'annual',
  leaveDays: 1,
  userName: userStore.userInfo?.nickName || '',
})

const approveForm = reactive<{ id: string; action: 'pass' | 'reject'; remark: string }>({
  id: '',
  action: 'pass',
  remark: '',
})

const leaveTypeMap: Record<string, string> = {
  annual: '年假', sick: '病假', personal: '事假', overtime: '调休',
  marriage: '婚假', maternity: '产假', other: '其他',
}

function leaveTypeText(type: string | undefined): string {
  return type != null ? leaveTypeMap[type] || '-' : '-'
}

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
  if (!form.startTime || !form.endTime) {
    Message.warning('请选择请假起止时间')
    return
  }
  if (!form.reason) {
    Message.warning('请填写请假事由')
    return
  }
  submitting.value = true
  try {
    await submitLeaveApi({
      userName: form.userName,
      leaveType: form.leaveType,
      startTime: form.startTime,
      endTime: form.endTime,
      leaveDays: Number(form.leaveDays) || 1,
      reason: form.reason,
    })
    Message.success('提交成功')
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
  approving.value = true
  try {
    await approveLeaveApi(approveForm.id, approveForm.action, approveForm.remark)
    Message.success('审批完成')
    approveVisible.value = false
    loadData()
  } finally {
    approving.value = false
  }
}

async function handleRevoke(record: AttLeaveRequest) {
  await revokeLeaveApi(record.id)
  Message.success('已撤销')
  loadData()
}

async function handleDelete(record: AttLeaveRequest) {
  await deleteLeaveApi(record.id)
  Message.success('删除成功')
  loadData()
}

function showRemark(record: AttLeaveRequest) {
  Message.info(`${record.approveRemark || '-'}`, 5000)
}

onMounted(loadData)
</script>
