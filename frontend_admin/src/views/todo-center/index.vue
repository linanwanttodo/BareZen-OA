<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <t-row :gutter="16" class="mb-4">
      <t-col :span="8">
        <t-card class="stat-card">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-[var(--muted-foreground)]">{{ $t('todoCenter.total') }}</div>
              <div class="mt-1 text-2xl font-semibold">{{ summary.total ?? '-' }}</div>
            </div>
            <icon-notification class="text-2xl" style="color: var(--primary)" />
          </div>
        </t-card>
      </t-col>
      <t-col :span="8">
        <t-card class="stat-card">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-[var(--muted-foreground)]">{{ $t('todoCenter.leave') }}</div>
              <div class="mt-1 text-2xl font-semibold">{{ summary.leave ?? '-' }}</div>
            </div>
            <icon-calendar class="text-2xl" style="color: var(--primary)" />
          </div>
        </t-card>
      </t-col>
      <t-col :span="8">
        <t-card class="stat-card">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-[var(--muted-foreground)]">{{ $t('todoCenter.vehicle') }}</div>
              <div class="mt-1 text-2xl font-semibold">{{ summary.vehicle ?? '-' }}</div>
            </div>
            <icon-drive class="text-2xl" style="color: var(--primary)" />
          </div>
        </t-card>
      </t-col>
    </t-row>

    <!-- 待办列表 -->
    <t-card>
      <template #title>
        <span>{{ $t('todoCenter.pendingList') }}</span>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('todoCenter.type')" :width="110">
              <template #cell="{ record }">
                <t-tag :color="record.type === 'leave' ? 'blue' : 'orange'">{{ record.typeName }}</t-tag>
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
                <t-space>
                  <t-button v-permission="PERM.hr.leaveApprove" v-if="record.type === 'leave'" size="small" theme="primary" @click="openApprove(record)">
                    {{ $t('todoCenter.approve') }}
                  </t-button>
                  <t-button v-permission="PERM.office.vehicleApprove" v-if="record.type === 'vehicle'" size="small" theme="primary" @click="openApprove(record)">
                    {{ $t('todoCenter.approve') }}
                  </t-button>
                </t-space>
              </template>
            </a-table-column>
          </template>
        </a-table>

        <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
          <t-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      </a-spin>
    </t-card>

    <!-- 审批弹窗 -->
    <t-dialog v-model="approveVisible" :header="$t('todoCenter.approve')" width="520px"
             @confirm="handleApprove" @close="approveVisible = false" :ok-loading="approving"
             :ok-button-props="{ disabled: !approveForm.item }">
      <template v-if="approveForm.item">
        <t-card class="mb-4" :bordered="false" style="background: var(--wecom-bg-container)">
          <t-descriptions :column="2" size="small" layout="horizontal">
            <t-descriptions-item :label="$t('todoCenter.type')" :span="2">
              <t-tag :color="approveForm.item.type === 'leave' ? 'blue' : 'orange'">{{ approveForm.item.typeName }}</t-tag>
            </t-descriptions-item>
            <t-descriptions-item :label="$t('todoCenter.title')" :span="2">{{ approveForm.item.title }}</t-descriptions-item>
            <t-descriptions-item :label="$t('todoCenter.applicant')">{{ approveForm.item.applicantName }}</t-descriptions-item>
            <t-descriptions-item :label="$t('todoCenter.applyTime')">{{ approveForm.item.createTime }}</t-descriptions-item>
            <t-descriptions-item :label="$t('todoCenter.timeRange')" :span="2">
              {{ approveForm.item.startTime }} ~ {{ approveForm.item.endTime }}
            </t-descriptions-item>
          </t-descriptions>
        </t-card>
      </template>
      <t-form :model="approveForm" layout="vertical">
        <t-form-item :label="$t('todoCenter.approveAction')" required>
          <t-radio-group v-model="approveForm.action" variant="default-filled">
            <t-radio value="pass">{{ $t('todoCenter.approvePass') }}</t-radio>
            <t-radio value="reject">{{ $t('todoCenter.approveReject') }}</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item name="remark" :label="$t('todoCenter.approveRemark')">
          <t-textarea v-model="approveForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, onMounted, onActivated } from 'vue'
import {  MessagePlugin  } from 'tdesign-vue-next'
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
    MessagePlugin.success(t('common.msg.approveDone'))
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
onActivated(() => { loadData(); loadSummary() })
</script>
