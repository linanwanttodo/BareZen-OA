<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="applicantName" :label="$t('office.applicantName')">
          <a-input v-model="searchForm.applicantName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 180px" />
        </a-form-item>
        <a-form-item field="status" :label="$t('office.vehicleStatus')">
          <a-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <a-option value="0">{{ $t('office.statusPending') }}</a-option>
            <a-option value="1">{{ $t('office.statusApproved') }}</a-option>
            <a-option value="2">{{ $t('office.statusRejected') }}</a-option>
            <a-option value="3">{{ $t('office.statusCancelled') }}</a-option>
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
          <span>{{ $t('office.vehicleList') }}</span>
          <a-button v-permission="'office:vehicle:add'" type="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('office.applyVehicle') }}
          </a-button>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('office.applicantName')" data-index="applicantName" :width="100" />
            <a-table-column :title="$t('office.vehicleType')" :width="90">
              <template #cell="{ record }">
                {{ vehicleTypeText(record.vehicleType) }}
              </template>
            </a-table-column>
            <a-table-column :title="$t('office.purpose')" data-index="purpose" ellipsis />
            <a-table-column :title="$t('office.timeRange')" :width="180">
              <template #cell="{ record }">
                <span class="text-sm">{{ record.startTime }}<br />{{ record.endTime }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('office.destination')" data-index="destination" :width="120" />
            <a-table-column :title="$t('office.passengerCount')" data-index="passengerCount" :width="80" />
            <a-table-column :title="$t('common.status')" :width="90">
              <template #cell="{ record }">
                <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="200" align="right">
              <template #cell="{ record }">
                <a-space>
                  <template v-if="record.status === '0'">
                    <a-button v-permission="'office:vehicle:approve'" size="small" type="primary" @click="openApprove(record)">
                      {{ $t('office.approve') }}
                    </a-button>
                    <a-button v-permission="'office:vehicle:edit'" size="small" @click="handleCancel(record)">
                      {{ $t('office.cancel') }}
                    </a-button>
                  </template>
                  <a-button v-if="record.approveRemark" size="small" @click="showRemark(record)">
                    {{ $t('office.viewRemark') }}
                  </a-button>
                  <a-button size="small" @click="openFlow(record)">{{ $t('office.flow') }}</a-button>
                  <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button v-permission="'office:vehicle:delete'" size="small" status="danger">{{ $t('common.delete') }}</a-button>
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

    <!-- 流程进度弹窗 -->
    <a-modal v-model:visible="flowVisible" :title="$t('office.flow')" :width="520" footer-only>
      <a-steps v-if="flowNodes.length" direction="vertical" :current="currentStep">
        <a-step
          v-for="node in flowNodes"
          :key="node.id"
          :title="node.nodeName"
          :description="`${node.operatorName || '-'} · ${node.createTime}${node.remark ? ' · ' + node.remark : ''}`"
        />
      </a-steps>
      <a-empty v-else :description="$t('office.flowEmpty')" />
    </a-modal>

    <!-- 用车申请弹窗 -->
    <a-modal v-model:visible="addVisible" :title="$t('office.applyVehicle')" :width="560"
             @ok="handleSubmit" @cancel="addVisible = false" :ok-loading="submitting">
      <a-form :model="form" layout="vertical">
        <a-form-item field="vehicleType" :label="$t('office.vehicleType')" required>
          <a-select v-model="form.vehicleType">
            <a-option value="car">{{ $t('office.typeCar') }}</a-option>
            <a-option value="van">{{ $t('office.typeVan') }}</a-option>
            <a-option value="bus">{{ $t('office.typeBus') }}</a-option>
            <a-option value="other">{{ $t('office.typeOther') }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="timeRange" :label="$t('office.timeRange')" required>
          <a-date-picker v-model="form.startTime" show-time style="width: 240px" :placeholder="$t('office.startTime')" />
          <span class="mx-2">~</span>
          <a-date-picker v-model="form.endTime" show-time style="width: 240px" :placeholder="$t('office.endTime')" />
        </a-form-item>
        <a-form-item field="purpose" :label="$t('office.purpose')" required>
          <a-textarea v-model="form.purpose" :placeholder="$t('common.inputPlaceholder')" :max-length="200" show-word-limit />
        </a-form-item>
        <a-form-item field="destination" :label="$t('office.destination')">
          <a-input v-model="form.destination" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-form-item field="passengerCount" :label="$t('office.passengerCount')">
          <a-input-number v-model="form.passengerCount" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item field="deptName" :label="$t('office.deptName')">
          <a-input v-model="form.deptName" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 审批弹窗 -->
    <a-modal v-model:visible="approveVisible" :title="$t('office.approve')" :width="480"
             @ok="handleApprove" @cancel="approveVisible = false" :ok-loading="approving">
      <a-form :model="approveForm" layout="vertical">
        <a-form-item :label="$t('office.approveAction')" required>
          <a-radio-group v-model="approveForm.action" type="button">
            <a-radio value="pass">{{ $t('office.approvePass') }}</a-radio>
            <a-radio value="reject">{{ $t('office.approveReject') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="remark" :label="$t('office.approveRemark')">
          <a-textarea v-model="approveForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getVehiclePageApi, submitVehicleApi, approveVehicleApi, cancelVehicleApi, deleteVehicleApi, type OfficeVehicleRequest } from '@/api/office'
import { getFlowLogsApi, type ApprovalFlowNode } from '@/api/flowlog'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const approving = ref(false)
const tableData = ref<OfficeVehicleRequest[]>([])
const addVisible = ref(false)
const approveVisible = ref(false)

const searchForm = reactive({ applicantName: '', status: '' })

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const form = reactive<Partial<OfficeVehicleRequest>>({
  vehicleType: 'car',
  passengerCount: 1,
  applicantName: userStore.userInfo?.nickName || '',
})

const approveForm = reactive<{ id: string; action: 'pass' | 'reject'; remark: string }>({
  id: '',
  action: 'pass',
  remark: '',
})

const vehicleTypeMap: Record<string, string> = { car: '轿车', van: '商务车', bus: '客车', other: '其他' }

function vehicleTypeText(type: string | undefined): string {
  return type != null ? vehicleTypeMap[type] || '-' : '-'
}

function statusText(status: string | undefined): string {
  const map: Record<string, string> = { 0: '待审批', 1: '已通过', 2: '已驳回', 3: '已取消' }
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
    const res = await getVehiclePageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      status: searchForm.status || undefined,
      applicantName: searchForm.applicantName || undefined,
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
  searchForm.applicantName = ''
  searchForm.status = ''
  pagination.current = 1
  loadData()
}

function handleAdd() {
  form.vehicleType = 'car'
  form.passengerCount = 1
  form.startTime = ''
  form.endTime = ''
  form.purpose = ''
  form.destination = ''
  form.deptName = ''
  form.applicantName = userStore.userInfo?.nickName || ''
  addVisible.value = true
}

async function handleSubmit() {
  if (!form.startTime || !form.endTime) {
    Message.warning('请选择用车时间')
    return
  }
  if (!form.purpose) {
    Message.warning('请填写用车事由')
    return
  }
  submitting.value = true
  try {
    await submitVehicleApi({
      applicantName: form.applicantName,
      deptName: form.deptName,
      vehicleType: form.vehicleType,
      purpose: form.purpose,
      startTime: form.startTime,
      endTime: form.endTime,
      destination: form.destination,
      passengerCount: Number(form.passengerCount) || 1,
    })
    Message.success('提交成功')
    addVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function openApprove(record: OfficeVehicleRequest) {
  approveForm.id = record.id
  approveForm.action = 'pass'
  approveForm.remark = ''
  approveVisible.value = true
}

async function handleApprove() {
  approving.value = true
  try {
    await approveVehicleApi(approveForm.id, approveForm.action, approveForm.remark)
    Message.success('审批完成')
    approveVisible.value = false
    loadData()
  } finally {
    approving.value = false
  }
}

async function handleCancel(record: OfficeVehicleRequest) {
  await cancelVehicleApi(record.id)
  Message.success('已取消')
  loadData()
}

async function handleDelete(record: OfficeVehicleRequest) {
  await deleteVehicleApi(record.id)
  Message.success('删除成功')
  loadData()
}

function showRemark(record: OfficeVehicleRequest) {
  Message.info(`${record.approveRemark || '-'}`, 5000)
}

// ============ 流程进度 ============
const flowVisible = ref(false)
const flowNodes = ref<ApprovalFlowNode[]>([])
const currentStep = ref(0)

async function openFlow(record: OfficeVehicleRequest) {
  try {
    const res = await getFlowLogsApi('vehicle', record.id)
    flowNodes.value = res.data || []
    currentStep.value = Math.max(0, flowNodes.value.length - 1)
    flowVisible.value = true
  } catch (err) {
    console.error('vehicle flow error:', err)
  }
}

onMounted(loadData)
</script>
