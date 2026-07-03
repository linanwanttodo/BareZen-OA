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
            <icon-copy class="text-2xl" style="color: var(--primary)" />
          </div>
        </t-card>
      </t-col>
    </t-row>

    <!-- 待办列表 -->
    <t-card>
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
                  <t-link theme="primary" v-permission="PERM.hr.leaveApprove" v-if="row.type === 'leave'" @click="openApprove(row)">
                    {{ $t('todoCenter.approve') }}
                  </t-link>
                  <t-link theme="primary" v-permission="PERM.office.vehicleApprove" v-if="row.type === 'vehicle'" @click="openApprove(row)">
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

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'type', title: t('todoCenter.type'), width: 110 },
    { colKey: 'title', title: t('todoCenter.title') },
    { colKey: 'applicantName', title: t('todoCenter.applicant'), width: 110 },
    { colKey: 'startTime', title: t('todoCenter.timeRange'), width: 180 },
    { colKey: 'operate', title: t('common.operate'), width: 180, align: 'right' },
  ])

</script>
