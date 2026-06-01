<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <a-row :gutter="16" class="mb-4">
      <a-col :span="8">
        <a-card class="stat-card">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-[var(--muted-foreground)]">{{ $t('todoCenter.total') }}</div>
              <div class="mt-1 text-2xl font-semibold">{{ summary.total ?? '-' }}</div>
            </div>
            <icon-notification class="text-2xl" style="color: var(--primary)" />
          </div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card class="stat-card">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-[var(--muted-foreground)]">{{ $t('todoCenter.leave') }}</div>
              <div class="mt-1 text-2xl font-semibold">{{ summary.leave ?? '-' }}</div>
            </div>
            <icon-calendar class="text-2xl" style="color: var(--primary)" />
          </div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card class="stat-card">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-[var(--muted-foreground)]">{{ $t('todoCenter.vehicle') }}</div>
              <div class="mt-1 text-2xl font-semibold">{{ summary.vehicle ?? '-' }}</div>
            </div>
            <icon-drive class="text-2xl" style="color: var(--primary)" />
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 待办列表 -->
    <a-card>
      <template #title>
        <span>{{ $t('todoCenter.pendingList') }}</span>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('todoCenter.type')" :width="110">
              <template #cell="{ record }">
                <a-tag :color="record.type === 'leave' ? 'blue' : 'orange'">{{ record.typeName }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('todoCenter.title')" data-index="title" />
            <a-table-column :title="$t('todoCenter.applicant')" data-index="applicantName" :width="110" />
            <a-table-column :title="$t('todoCenter.timeRange')" :width="180">
              <template #cell="{ record }">
                <span class="text-sm">{{ record.startTime }}<br />{{ record.endTime }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="180" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-permission="PERM.hr.leaveApprove" v-if="record.type === 'leave'" size="small" type="primary" @click="openApprove(record)">
                    {{ $t('todoCenter.approve') }}
                  </a-button>
                  <a-button v-permission="PERM.office.vehicleApprove" v-if="record.type === 'vehicle'" size="small" type="primary" @click="openApprove(record)">
                    {{ $t('todoCenter.approve') }}
                  </a-button>
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

    <!-- 审批弹窗 -->
    <a-modal v-model:visible="approveVisible" :title="$t('todoCenter.approve')" :width="520"
             @ok="handleApprove" @cancel="approveVisible = false" :ok-loading="approving"
             :ok-button-props="{ disabled: !approveForm.item }">
      <template v-if="approveForm.item">
        <a-card class="mb-4" :bordered="false" style="background: var(--color-fill-2)">
          <a-descriptions :column="2" size="small" layout="horizontal">
            <a-descriptions-item :label="$t('todoCenter.type')" :span="2">
              <a-tag :color="approveForm.item.type === 'leave' ? 'blue' : 'orange'">{{ approveForm.item.typeName }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('todoCenter.title')" :span="2">{{ approveForm.item.title }}</a-descriptions-item>
            <a-descriptions-item :label="$t('todoCenter.applicant')">{{ approveForm.item.applicantName }}</a-descriptions-item>
            <a-descriptions-item :label="$t('todoCenter.applyTime')">{{ approveForm.item.createTime }}</a-descriptions-item>
            <a-descriptions-item :label="$t('todoCenter.timeRange')" :span="2">
              {{ approveForm.item.startTime }} ~ {{ approveForm.item.endTime }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </template>
      <a-form :model="approveForm" layout="vertical">
        <a-form-item :label="$t('todoCenter.approveAction')" required>
          <a-radio-group v-model="approveForm.action" type="button">
            <a-radio value="pass">{{ $t('todoCenter.approvePass') }}</a-radio>
            <a-radio value="reject">{{ $t('todoCenter.approveReject') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="remark" :label="$t('todoCenter.approveRemark')">
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
import { getTodoPageApi, getTodoSummaryApi, type TodoItem } from '@/api/todo'
import { approveLeaveApi } from '@/api/leave'
import { approveVehicleApi } from '@/api/office'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const loading = ref(false)
const approving = ref(false)
const tableData = ref<TodoItem[]>([])
const approveVisible = ref(false)
const summary = reactive<{ leave?: number; vehicle?: number; total?: number }>({})

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const approveForm = reactive<{ item: TodoItem | null; action: 'pass' | 'reject'; remark: string }>({
  item: null,
  action: 'pass',
  remark: '',
})

async function loadData() {
  loading.value = true
  try {
    const res = await getTodoPageApi(pagination.current, pagination.pageSize)
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  const res = await getTodoSummaryApi()
  Object.assign(summary, res.data)
}

function openApprove(record: TodoItem) {
  approveForm.item = record
  approveForm.action = 'pass'
  approveForm.remark = ''
  approveVisible.value = true
}

async function handleApprove() {
  const item = approveForm.item
  if (!item) return
  approving.value = true
  try {
    if (item.type === 'leave') {
      await approveLeaveApi(item.id, approveForm.action, approveForm.remark)
    } else if (item.type === 'vehicle') {
      await approveVehicleApi(item.id, approveForm.action, approveForm.remark)
    }
    Message.success(t('common.msg.approveDone'))
    approveVisible.value = false
    loadData()
    loadSummary()
  } finally {
    approving.value = false
  }
}

onMounted(() => {
  loadData()
  loadSummary()
})
</script>
