<template>
  <div class="page-container">
    <!-- Tab：用品管理 / 申领记录 -->
    <t-tabs v-model:active-key="activeTab" class="mb-4">
      <t-tab-panel key="supply" :title="$t('supply.supplyList')" />
      <t-tab-panel key="request" :title="$t('supply.requestList')" />
    </t-tabs>

    <!-- 用品管理 -->
    <template v-if="activeTab === 'supply'">
      <t-card class="mb-4">
        <t-form :model="searchForm" layout="inline" @submit-success="handleSearch">
          <t-form-item field="supplyName" :label="$t('supply.supplyName')">
            <t-input v-model="searchForm.supplyName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 180px" />
          </t-form-item>
          <t-form-item field="category" :label="$t('supply.category')">
            <t-select v-model="searchForm.category" :placeholder="$t('common.all')" allow-clear style="width: 140px">
              <t-option value="stationery">{{ $t('supply.catStationery') }}</t-option>
              <t-option value="electronics">{{ $t('supply.catElectronics') }}</t-option>
              <t-option value="cleaning">{{ $t('supply.catCleaning') }}</t-option>
              <t-option value="other">{{ $t('supply.catOther') }}</t-option>
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

      <t-card>
        <template #title>
          <div class="flex items-center justify-between w-full">
            <span>{{ $t('supply.supplyList') }}</span>
            <t-button v-permission="PERM.office.supplyAdd" theme="primary" @click="openSupplyAdd">
              <template #icon><icon-plus /></template>
              {{ $t('supply.addSupply') }}
            </t-button>
          </div>
        </template>

        <t-loading :loading="loading">
            <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
  </t-table>

          <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
            <t-pagination v-model:current="pagination.current" :total="pagination.total" :page-size="pagination.pageSize" @change="loadSupplies" />
          </div>
        </t-loading>
      </t-card>
    </template>

    <!-- 申领记录 -->
    <template v-else>
      <t-card>
        <template #title>
          <div class="flex items-center justify-between w-full">
            <span>{{ $t('supply.requestList') }}</span>
            <t-select v-model="requestStatusFilter" :placeholder="$t('common.all')" allow-clear style="width: 140px" @change="loadRequests">
              <t-option value="0">{{ $t('supply.statusPending') }}</t-option>
              <t-option value="1">{{ $t('supply.statusApproved') }}</t-option>
              <t-option value="2">{{ $t('supply.statusRejected') }}</t-option>
              <t-option value="3">{{ $t('supply.statusCancelled') }}</t-option>
            </t-select>
          </div>
        </template>

        <t-loading :loading="requestLoading">
            <t-table row-key="id" :data="requestData" :columns="tableColumns2" :pagination="false">
  </t-table>

          <div v-if="requestPagination.total > 0" class="mt-4 flex justify-end">
            <t-pagination v-model:current="requestPagination.current" :total="requestPagination.total" :page-size="requestPagination.pageSize" @change="loadRequests" />
          </div>
        </t-loading>
      </t-card>
    </template>

    <!-- 用品新增/编辑弹窗 -->
    <t-dialog v-model="supplyDialogVisible" :title="isSupplyEdit ? $t('supply.editSupply') : $t('supply.addSupply')"
             :width="520" @confirm="handleSaveSupply" @cancel="supplyDialogVisible = false" :ok-loading="saving">
      <t-form :model="supplyForm" layout="vertical">
        <t-form-item field="supplyName" :label="$t('supply.supplyName')" required>
          <t-input v-model="supplyForm.supplyName" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item field="category" :label="$t('supply.category')">
              <t-select v-model="supplyForm.category">
                <t-option value="stationery">{{ $t('supply.catStationery') }}</t-option>
                <t-option value="electronics">{{ $t('supply.catElectronics') }}</t-option>
                <t-option value="cleaning">{{ $t('supply.catCleaning') }}</t-option>
                <t-option value="other">{{ $t('supply.catOther') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="spec" :label="$t('supply.spec')">
              <t-input v-model="supplyForm.spec" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="stock" :label="$t('supply.stock')" required>
              <t-input-number v-model="supplyForm.stock" :min="0" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="unit" :label="$t('supply.unit')">
              <t-input v-model="supplyForm.unit" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item field="remark" :label="$t('common.remark')">
          <t-textarea v-model="supplyForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 流程进度弹窗 -->
    <t-dialog v-model="flowVisible" :title="$t('supply.flow')" :width="520" footer-only>
      <t-steps v-if="flowNodes.length" direction="vertical" :current="currentStep">
        <t-step-item
          v-for="node in flowNodes"
          :key="node.id"
          :title="node.nodeName"
          :description="`${node.operatorName || '-'} · ${node.createTime}${node.remark ? ' · ' + node.remark : ''}`"
        />
      </t-steps>
      <t-empty v-else :description="$t('supply.flowEmpty')" />
    </t-dialog>

    <!-- 审批弹窗 -->
    <t-dialog v-model="approveVisible" :title="$t('supply.approve')" :width="480"
             @confirm="handleApprove" @cancel="approveVisible = false" :ok-loading="approving">
      <t-form :model="approveForm" layout="vertical">
        <t-form-item :label="$t('supply.approveAction')" required>
          <t-radio-group v-model="approveForm.action" variant="default-filled">
            <t-radio value="pass">{{ $t('supply.approvePass') }}</t-radio>
            <t-radio value="reject">{{ $t('supply.approveReject') }}</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item field="remark" :label="$t('supply.approveRemark')">
          <t-textarea v-model="approveForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted, onActivated } from 'vue'
import { useRoute } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import {
  getSupplyPageApi, addSupplyApi, updateSupplyApi, deleteSupplyApi,
  getSupplyRequestPageApi, approveSupplyRequestApi, deleteSupplyRequestApi,
  type OfficeSupply, type OfficeSupplyRequest,
} from '@/api/supply'
import { getFlowLogsApi, type ApprovalFlowNode } from '@/api/flowlog'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const route = useRoute()

// 申领管理菜单（office/supply-request）与用品管理菜单（office/supply）共用本页，按路由 path 定位初始 Tab
const activeTab = ref(route.path.includes('supply-request') ? 'request' : 'supply')
const loading = ref(false)
const requestLoading = ref(false)
const saving = ref(false)
const approving = ref(false)
const tableData = ref<OfficeSupply[]>([])
const requestData = ref<OfficeSupplyRequest[]>([])
const supplyDialogVisible = ref(false)
const approveVisible = ref(false)
const isSupplyEdit = ref(false)

const searchForm = reactive({ supplyName: '', category: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const requestPagination = reactive({ current: 1, pageSize: 10, total: 0 })
const requestStatusFilter = ref('')
const supplyForm = reactive<Partial<OfficeSupply>>({})
const approveForm = reactive<{ id: string; action: 'pass' | 'reject'; remark: string }>({ id: '', action: 'pass', remark: '' })

const categoryMap: Record<string, string> = { stationery: 'enums.supplyCategory.stationery', electronics: 'enums.supplyCategory.electronics', cleaning: 'enums.supplyCategory.cleaning', other: 'enums.supplyCategory.other' }
function categoryText(c: string | undefined): string { return c != null ? t(categoryMap[c] || '-') : '-' }

function statusText(status: string | undefined): string {
  const map: Record<string, string> = { 0: 'enums.status.pending', 1: 'enums.status.approved', 2: 'enums.status.rejected', 3: 'enums.status.cancelled' }
  return status != null ? t(map[status] || '-') : '-'
}
function statusColor(status: string | undefined): string {
  if (status === '0') return 'orange'
  if (status === '1') return 'green'
  if (status === '2') return 'red'
  return 'default'
}

async function loadSupplies() {
  loading.value = true
  try {
    const res = await getSupplyPageApi({
      pageNum: pagination.current, pageSize: pagination.pageSize,
      supplyName: searchForm.supplyName || undefined,
      category: searchForm.category || undefined,
    })
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } finally { loading.value = false }
}

async function loadRequests() {
  requestLoading.value = true
  try {
    const res = await getSupplyRequestPageApi(requestPagination.current, requestPagination.pageSize, requestStatusFilter.value || undefined)
    requestData.value = res.data.list || []
    requestPagination.total = res.data.total || 0
  } finally { requestLoading.value = false }
}

function handleSearch() { pagination.current = 1; loadSupplies() }
function handleReset() { searchForm.supplyName = ''; searchForm.category = ''; pagination.current = 1; loadSupplies() }

function openSupplyAdd() {
  isSupplyEdit.value = false
  Object.keys(supplyForm).forEach((k) => delete (supplyForm as Record<string, unknown>)[k])
  supplyForm.category = 'stationery'
  supplyForm.stock = 0
  supplyForm.unit = '个'
  supplyForm.status = '0'
  supplyDialogVisible.value = true
}

function openSupplyEdit(record: OfficeSupply) {
  isSupplyEdit.value = true
  Object.assign(supplyForm, record)
  supplyDialogVisible.value = true
}

async function handleSaveSupply() {
  if (!supplyForm.supplyName) { MessagePlugin.warning(t('common.msg.supplyNameRequired')); return }
  saving.value = true
  try {
    if (isSupplyEdit.value) await updateSupplyApi(supplyForm)
    else await addSupplyApi(supplyForm)
    MessagePlugin.success(t('common.saveSuccess'))
    supplyDialogVisible.value = false
    loadSupplies()
  } finally { saving.value = false }
}

async function handleDeleteSupply(record: OfficeSupply) {
  await deleteSupplyApi(record.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadSupplies()
}

function openApprove(record: OfficeSupplyRequest) {
  approveForm.id = record.id
  approveForm.action = 'pass'
  approveForm.remark = ''
  approveVisible.value = true
}

async function handleApprove() {
  approving.value = true
  try {
    await approveSupplyRequestApi(approveForm.id, approveForm.action, approveForm.remark)
    MessagePlugin.success(t('common.msg.approveDone'))
    approveVisible.value = false
    loadRequests()
    loadSupplies() // 库存可能变化
  } finally { approving.value = false }
}

async function handleDeleteRequest(record: OfficeSupplyRequest) {
  await deleteSupplyRequestApi(record.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadRequests()
}

function showRemark(record: OfficeSupplyRequest) {
  MessagePlugin.info({ content: record.approveRemark || '-', duration: 5000 })
}

// ============ 流程进度 ============
const flowVisible = ref(false)
const flowNodes = ref<ApprovalFlowNode[]>([])
const currentStep = ref(0)

async function openFlow(record: OfficeSupplyRequest) {
  try {
    const res = await getFlowLogsApi('supply', record.id)
    flowNodes.value = res.data || []
    currentStep.value = Math.max(0, flowNodes.value.length - 1)
    flowVisible.value = true
  } catch (err) {
    console.error('supply flow error:', err)
  }
}

onMounted(() => { loadSupplies(); loadRequests() })
onActivated(() => { loadSupplies(); loadRequests() })

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    ,
  ])

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns2 = computed(() => [
    ,
  ])

</script>
