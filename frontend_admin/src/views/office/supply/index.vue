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
        <t-form :data="searchForm" :label-width="80" :on-submit="handleSearch">
          <t-form-item name="supplyName" :label="$t('supply.supplyName')">
            <t-input v-model="searchForm.supplyName" :placeholder="$t('common.inputPlaceholder')" allow-clear />
          </t-form-item>
          <t-form-item name="category" :label="$t('supply.category')">
            <t-select v-model="searchForm.category" :placeholder="$t('common.all')" allow-clear>
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
        <div class="bz-list-toolbar bz-list-toolbar--first">
          <t-space>
            <t-button v-permission="PERM.office.supplyAdd" theme="primary" @click="openSupplyAdd">
              <template #icon><icon-plus /></template>
              {{ $t('supply.addSupply') }}
            </t-button>
          </t-space>
        </div>

        
            <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #category="{ row }">{{ categoryText(row.category) }}</template>
    <template #operate="{ row }">
                <t-space>
                  <t-link theme="primary" v-permission="PERM.office.supplyEdit" @click="openSupplyEdit(row)">{{ $t('common.edit') }}</t-link>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDeleteSupply(row)">
                    <t-button v-permission="PERM.office.supplyDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
  </t-table>

          <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
            <t-pagination v-model:current="pagination.current" :total="pagination.total" :page-size="pagination.pageSize" @change="loadSupplies" />
          </div>
        
      </t-card>
    </template>

    <!-- 申领记录 -->
    <template v-else>
      <t-card>
        <template #title>
          <div class="flex items-center justify-between w-full">
            <span>{{ $t('supply.requestList') }}</span>
            <t-select v-model="requestStatusFilter" :placeholder="$t('common.all')" allow-clear @change="loadRequests">
              <t-option value="0">{{ $t('supply.statusPending') }}</t-option>
              <t-option value="1">{{ $t('supply.statusApproved') }}</t-option>
              <t-option value="2">{{ $t('supply.statusRejected') }}</t-option>
              <t-option value="3">{{ $t('supply.statusCancelled') }}</t-option>
            </t-select>
          </div>
        </template>

        
            <t-table :loading="requestLoading" row-key="id" :data="requestData" :columns="tableColumns2" :pagination="false" :hover="true" vertical-align="top">
    <template #status="{ row }">
                <t-tag :color="statusColor(row.status)" variant="light">{{ statusText(row.status) }}</t-tag>
              </template>
    <template #approverName="{ row }">{{ row.approverName || '-' }}</template>
    <template #operate="{ row }">
                <t-space>
                  <t-link theme="primary" v-if="row.status === '0'" v-permission="PERM.office.supplyApprove" @click="openApprove(row)">
                    {{ $t('supply.approve') }}
                  </t-link>
                  <t-link theme="primary" v-if="row.approveRemark" @click="showRemark(row)">{{ $t('supply.viewRemark') }}</t-link>
                  <t-link theme="primary" @click="openFlow(row)">{{ $t('supply.flow') }}</t-link>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDeleteRequest(row)">
                    <t-button v-permission="PERM.office.supplyDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
  </t-table>

          <div v-if="requestPagination.total > 0" class="mt-4 flex justify-end">
            <t-pagination v-model:current="requestPagination.current" :total="requestPagination.total" :page-size="requestPagination.pageSize" @change="loadRequests" />
          </div>
        
      </t-card>
    </template>

    <!-- 用品新增/编辑弹窗 -->
    <t-dialog v-model:visible="supplyDialogVisible" :title="isSupplyEdit ? $t('supply.editSupply') : $t('supply.addSupply')"
             :width="520" @confirm="handleSaveSupply" @cancel="supplyDialogVisible = false" :ok-loading="saving">
      <t-form ref="supplyFormRef" :data="supplyForm" layout="vertical">
        <t-form-item name="supplyName" :label="$t('supply.supplyName')" required>
          <t-input v-model="supplyForm.supplyName" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item name="category" :label="$t('supply.category')">
              <t-select v-model="supplyForm.category">
                <t-option value="stationery">{{ $t('supply.catStationery') }}</t-option>
                <t-option value="electronics">{{ $t('supply.catElectronics') }}</t-option>
                <t-option value="cleaning">{{ $t('supply.catCleaning') }}</t-option>
                <t-option value="other">{{ $t('supply.catOther') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="spec" :label="$t('supply.spec')">
              <t-input v-model="supplyForm.spec" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="stock" :label="$t('supply.stock')" required>
              <t-input-number v-model="supplyForm.stock" :min="0" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="unit" :label="$t('supply.unit')">
              <t-input v-model="supplyForm.unit" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item name="remark" :label="$t('common.remark')">
          <t-textarea v-model="supplyForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 流程进度弹窗 -->
    <t-dialog v-model:visible="flowVisible" :title="$t('supply.flow')" :width="520" footer-only>
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
    <t-dialog v-model:visible="approveVisible" :title="$t('supply.approve')" :width="480"
             @confirm="handleApprove" @cancel="approveVisible = false" :ok-loading="approving">
      <t-form ref="approveFormRef" :data="approveForm" layout="vertical">
        <t-form-item :label="$t('supply.approveAction')" required>
          <t-radio-group v-model="approveForm.action" variant="default-filled">
            <t-radio value="pass">{{ $t('supply.approvePass') }}</t-radio>
            <t-radio value="reject">{{ $t('supply.approveReject') }}</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item name="remark" :label="$t('supply.approveRemark')">
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
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
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
const supplyFormRef = ref<FormInstanceFunctions>()
const supplyForm = reactive<Partial<OfficeSupply>>({})
const approveFormRef = ref<FormInstanceFunctions>()
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
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await supplyFormRef.value?.validate()) !== true) return
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
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await approveFormRef.value?.validate()) !== true) return
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

/** 表格列定义（自定义单元格见 t-table 同名插槽） */
const tableColumns = computed(() => [
  { colKey: 'supplyName', title: t('supply.supplyName'), width: 160, ellipsis: true },
  { colKey: 'category', title: t('supply.category'), width: 110 },
  { colKey: 'spec', title: t('supply.spec'), width: 120, ellipsis: true },
  { colKey: 'stock', title: t('supply.stock'), width: 90, align: 'center' },
  { colKey: 'unit', title: t('supply.unit'), width: 70 },
  { colKey: 'remark', title: t('common.remark'), ellipsis: true },
  { colKey: 'createTime', title: t('common.createTime'), width: 170 },
  { colKey: 'operate', title: t('common.operate'), width: 140, align: 'right' },
])

/** 申领记录表格列定义 */
const tableColumns2 = computed(() => [
  { colKey: 'supplyName', title: t('supply.supplyName'), width: 150, ellipsis: true },
  { colKey: 'userName', title: t('supply.userName'), width: 100 },
  { colKey: 'deptName', title: t('office.deptName'), width: 120 },
  { colKey: 'quantity', title: t('supply.quantity'), width: 80, align: 'center' },
  { colKey: 'reason', title: t('supply.reason'), ellipsis: true },
  { colKey: 'status', title: t('common.status'), width: 90 },
  { colKey: 'approverName', title: t('supply.approverName'), width: 100 },
  { colKey: 'createTime', title: t('common.createTime'), width: 170 },
  { colKey: 'operate', title: t('common.operate'), width: 230, align: 'right' },
])

</script>
