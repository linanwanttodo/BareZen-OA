<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <t-row :gutter="16" class="mb-4">
      <t-col :span="4">
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
      <t-col :span="4">
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
      <t-col :span="4">
        <t-card class="stat-card">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-[var(--muted-foreground)]">{{ $t('todoCenter.overtime') }}</div>
              <div class="mt-1 text-2xl font-semibold">{{ summary.overtime ?? '-' }}</div>
            </div>
            <icon-command class="text-2xl" style="color: var(--primary)" />
          </div>
        </t-card>
      </t-col>
      <t-col :span="4">
        <t-card class="stat-card">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-[var(--muted-foreground)]">{{ $t('todoCenter.travel') }}</div>
              <div class="mt-1 text-2xl font-semibold">{{ summary.travel ?? '-' }}</div>
            </div>
            <icon-ticket class="text-2xl" style="color: var(--primary)" />
          </div>
        </t-card>
      </t-col>
      <t-col :span="4">
        <t-card class="stat-card">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-[var(--muted-foreground)]">{{ $t('todoCenter.supply') }}</div>
              <div class="mt-1 text-2xl font-semibold">{{ summary.supply ?? '-' }}</div>
            </div>
            <icon-file class="text-2xl" style="color: var(--primary)" />
          </div>
        </t-card>
      </t-col>
      <t-col :span="4">
        <t-card class="stat-card">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm text-[var(--muted-foreground)]">{{ $t('todoCenter.vehicle') }}</div>
              <div class="mt-1 text-2xl font-semibold">{{ summary.vehicle ?? '-' }}</div>
            </div>
            <icon-copy class="text-2xl" style="color: var(--primary)" />
          </div>
        </t-card>
      </t-col>
    </t-row>

    <!-- 待办列表 -->
    <t-card :bordered="false" class="bz-list-panel">
      <template #title>
        <span>{{ $t('todoCenter.pendingList') }}</span>
      </template>

      
          <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #type="{ row }">
                <t-tag :theme="row.type === 'leave' ? 'primary' : 'warning'" variant="light">{{ row.typeName }}</t-tag>
              </template>
    <template #startTime="{ row }">
                <span class="text-sm">{{ row.startTime }}<br />{{ row.endTime }}</span>
              </template>
    <template #operate="{ row }">
                <t-space>
                  <!-- 审批按钮按待办类型分发。后端 TodoAggregationServiceImpl 产出 5 种类型：
                       leave / overtime / travel / supply / vehicle。
                       此前只覆盖 leave 与 vehicle，其余 3 种待办在列表里没有审批入口，
                       表现为「这条待办点不动、处理不了」。 -->
                  <t-link v-if="row.type === 'leave'" theme="primary" v-permission="PERM.hr.leaveApprove" @click="openApprove(row)">
                    {{ $t('todoCenter.approve') }}
                  </t-link>
                  <t-link v-if="row.type === 'overtime'" theme="primary" v-permission="PERM.hr.overtimeApprove" @click="openApprove(row)">
                    {{ $t('todoCenter.approve') }}
                  </t-link>
                  <t-link v-if="row.type === 'travel'" theme="primary" v-permission="PERM.hr.travelApprove" @click="openApprove(row)">
                    {{ $t('todoCenter.approve') }}
                  </t-link>
                  <t-link v-if="row.type === 'supply'" theme="primary" v-permission="PERM.office.supplyApprove" @click="openApprove(row)">
                    {{ $t('todoCenter.approve') }}
                  </t-link>
                  <t-link v-if="row.type === 'vehicle'" theme="primary" v-permission="PERM.office.vehicleApprove" @click="openApprove(row)">
                    {{ $t('todoCenter.approve') }}
                  </t-link>
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

    <!-- 审批弹窗 -->
    <t-dialog v-model:visible="approveVisible" :header="$t('todoCenter.approve')" width="520px"
             @confirm="handleApprove" @close="approveVisible = false" :ok-loading="approving"
             :ok-button-props="{ disabled: !approveForm.item }">
      <template v-if="approveForm.item">
        <t-card class="mb-4" :bordered="false" style="background: var(--wecom-bg-container)">
          <t-descriptions :column="2" size="small" layout="horizontal">
            <t-descriptions-item :label="$t('todoCenter.type')" :span="2">
              <t-tag :theme="approveForm.item.type === 'leave' ? 'primary' : 'warning'">{{ approveForm.item.typeName }}</t-tag>
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
      <t-form ref="approveFormRef" :data="approveForm" layout="vertical">
        <t-form-item :label="$t('todoCenter.approveAction')">
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
import { ref, computed, reactive, onMounted, onActivated } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { getTodoPageApi, getTodoSummaryApi, type TodoItem, type TodoSummary } from '@/api/todo'
import { approveLeaveApi } from '@/api/leave'
import { approveOvertimeApi } from '@/api/overtime'
import { approveTravelApi } from '@/api/travel'
import { approveSupplyRequestApi } from '@/api/supply'
import { approveVehicleApi } from '@/api/office'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const loading = ref(false)
const approving = ref(false)
const tableData = ref<TodoItem[]>([])
const approveVisible = ref(false)
// 与后端 TodoCenterController.summary 返回的 5 类计数保持一致
const summary = reactive<TodoSummary>({ total: 0, leave: 0, overtime: 0, travel: 0, supply: 0, vehicle: 0 })

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const approveFormRef = ref<FormInstanceFunctions>()
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
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await approveFormRef.value?.validate()) !== true) return
  const item = approveForm.item
  if (!item) return
  approving.value = true
  try {
    // 与后端 TodoAggregationServiceImpl 的 5 种类型一一对应。
    // 此前只处理 leave/vehicle，其余类型走完分支什么都不做却仍提示「审批完成」，
    // 属于静默失败 —— 现在每类都有明确分支，未知类型显式报错。
    switch (item.type) {
      case 'leave':
        await approveLeaveApi(item.id, approveForm.action, approveForm.remark)
        break
      case 'overtime':
        await approveOvertimeApi(item.id, approveForm.action, approveForm.remark)
        break
      case 'travel':
        await approveTravelApi(item.id, approveForm.action, approveForm.remark)
        break
      case 'supply':
        await approveSupplyRequestApi(item.id, approveForm.action, approveForm.remark)
        break
      case 'vehicle':
        await approveVehicleApi(item.id, approveForm.action, approveForm.remark)
        break
      default:
        MessagePlugin.error(t('todoCenter.unsupportedType', { type: item.type }))
        return
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

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'type', title: t('todoCenter.type'), width: 110 },
    { colKey: 'title', title: t('todoCenter.title') },
    { colKey: 'applicantName', title: t('todoCenter.applicant'), width: 110 },
    { colKey: 'startTime', title: t('todoCenter.timeRange'), width: 180 },
    { colKey: 'operate', title: t('common.operate'), width: 180, align: 'right' },
  ])

</script>
