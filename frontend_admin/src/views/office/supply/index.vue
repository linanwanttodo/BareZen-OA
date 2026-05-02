<template>
  <div class="page-container">
    <!-- Tab：用品管理 / 申领记录 -->
    <a-tabs v-model:active-key="activeTab" class="mb-4">
      <a-tab-pane key="supply" :title="$t('supply.supplyList')" />
      <a-tab-pane key="request" :title="$t('supply.requestList')" />
    </a-tabs>

    <!-- 用品管理 -->
    <template v-if="activeTab === 'supply'">
      <a-card class="mb-4">
        <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
          <a-form-item field="supplyName" :label="$t('supply.supplyName')">
            <a-input v-model="searchForm.supplyName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item field="category" :label="$t('supply.category')">
            <a-select v-model="searchForm.category" :placeholder="$t('common.all')" allow-clear style="width: 140px">
              <a-option value="stationery">{{ $t('supply.catStationery') }}</a-option>
              <a-option value="electronics">{{ $t('supply.catElectronics') }}</a-option>
              <a-option value="cleaning">{{ $t('supply.catCleaning') }}</a-option>
              <a-option value="other">{{ $t('supply.catOther') }}</a-option>
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

      <a-card>
        <template #title>
          <div class="flex items-center justify-between w-full">
            <span>{{ $t('supply.supplyList') }}</span>
            <a-button v-permission="'office:supply:add'" type="primary" @click="openSupplyAdd">
              <template #icon><icon-plus /></template>
              {{ $t('supply.addSupply') }}
            </a-button>
          </div>
        </template>

        <a-spin :loading="loading">
          <a-table :data="tableData" :pagination="false" row-key="id">
            <template #columns>
              <a-table-column :title="$t('supply.supplyName')" data-index="supplyName" :width="140" />
              <a-table-column :title="$t('supply.category')" :width="100">
                <template #cell="{ record }">{{ categoryText(record.category) }}</template>
              </a-table-column>
              <a-table-column :title="$t('supply.spec')" data-index="spec" :width="120" />
              <a-table-column :title="$t('supply.stock')" :width="90">
                <template #cell="{ record }">
                  <a-tag :color="record.stock <= 5 ? 'red' : 'green'">{{ record.stock }} {{ record.unit }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column :title="$t('common.status')" :width="80">
                <template #cell="{ record }">
                  <a-tag :color="record.status === '0' ? 'green' : 'red'">
                    {{ record.status === '0' ? $t('common.enabled') : $t('common.disabled') }}
                  </a-tag>
                </template>
              </a-table-column>
              <a-table-column :title="$t('common.operate')" :width="150" align="right">
                <template #cell="{ record }">
                  <a-space>
                    <a-button v-permission="'office:supply:edit'" size="small" @click="openSupplyEdit(record)">{{ $t('common.edit') }}</a-button>
                    <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDeleteSupply(record)">
                      <a-button v-permission="'office:supply:delete'" size="small" status="danger">{{ $t('common.delete') }}</a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </a-table-column>
            </template>
          </a-table>

          <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
            <a-pagination v-model:current="pagination.current" :total="pagination.total" :page-size="pagination.pageSize" @change="loadSupplies" />
          </div>
        </a-spin>
      </a-card>
    </template>

    <!-- 申领记录 -->
    <template v-else>
      <a-card>
        <template #title>
          <div class="flex items-center justify-between w-full">
            <span>{{ $t('supply.requestList') }}</span>
            <a-select v-model="requestStatusFilter" :placeholder="$t('common.all')" allow-clear style="width: 140px" @change="loadRequests">
              <a-option value="0">{{ $t('supply.statusPending') }}</a-option>
              <a-option value="1">{{ $t('supply.statusApproved') }}</a-option>
              <a-option value="2">{{ $t('supply.statusRejected') }}</a-option>
              <a-option value="3">{{ $t('supply.statusCancelled') }}</a-option>
            </a-select>
          </div>
        </template>

        <a-spin :loading="requestLoading">
          <a-table :data="requestData" :pagination="false" row-key="id">
            <template #columns>
              <a-table-column :title="$t('supply.supplyName')" data-index="supplyName" :width="140" />
              <a-table-column :title="$t('supply.userName')" data-index="userName" :width="100" />
              <a-table-column :title="$t('supply.quantity')" data-index="quantity" :width="90" />
              <a-table-column :title="$t('supply.reason')" data-index="reason" ellipsis />
              <a-table-column :title="$t('common.status')" :width="90">
                <template #cell="{ record }">
                  <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column :title="$t('supply.approverName')" :width="100">
                <template #cell="{ record }">{{ record.approverName || '-' }}</template>
              </a-table-column>
              <a-table-column :title="$t('common.operate')" :width="170" align="right">
                <template #cell="{ record }">
                  <a-space>
                    <template v-if="record.status === '0'">
                      <a-button v-permission="'office:supply:approve'" size="small" type="primary" @click="openApprove(record)">
                        {{ $t('supply.approve') }}
                      </a-button>
                    </template>
                    <a-button v-if="record.approveRemark" size="small" @click="showRemark(record)">
                      {{ $t('supply.viewRemark') }}
                    </a-button>
                    <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDeleteRequest(record)">
                      <a-button v-permission="'office:supply:delete'" size="small" status="danger">{{ $t('common.delete') }}</a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </a-table-column>
            </template>
          </a-table>

          <div v-if="requestPagination.total > 0" class="mt-4 flex justify-end">
            <a-pagination v-model:current="requestPagination.current" :total="requestPagination.total" :page-size="requestPagination.pageSize" @change="loadRequests" />
          </div>
        </a-spin>
      </a-card>
    </template>

    <!-- 用品新增/编辑弹窗 -->
    <a-modal v-model:visible="supplyDialogVisible" :title="isSupplyEdit ? $t('supply.editSupply') : $t('supply.addSupply')"
             :width="520" @ok="handleSaveSupply" @cancel="supplyDialogVisible = false" :ok-loading="saving">
      <a-form :model="supplyForm" layout="vertical">
        <a-form-item field="supplyName" :label="$t('supply.supplyName')" required>
          <a-input v-model="supplyForm.supplyName" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="category" :label="$t('supply.category')">
              <a-select v-model="supplyForm.category">
                <a-option value="stationery">{{ $t('supply.catStationery') }}</a-option>
                <a-option value="electronics">{{ $t('supply.catElectronics') }}</a-option>
                <a-option value="cleaning">{{ $t('supply.catCleaning') }}</a-option>
                <a-option value="other">{{ $t('supply.catOther') }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="spec" :label="$t('supply.spec')">
              <a-input v-model="supplyForm.spec" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="stock" :label="$t('supply.stock')" required>
              <a-input-number v-model="supplyForm.stock" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="unit" :label="$t('supply.unit')">
              <a-input v-model="supplyForm.unit" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item field="remark" :label="$t('common.remark')">
          <a-textarea v-model="supplyForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 审批弹窗 -->
    <a-modal v-model:visible="approveVisible" :title="$t('supply.approve')" :width="480"
             @ok="handleApprove" @cancel="approveVisible = false" :ok-loading="approving">
      <a-form :model="approveForm" layout="vertical">
        <a-form-item :label="$t('supply.approveAction')" required>
          <a-radio-group v-model="approveForm.action" type="button">
            <a-radio value="pass">{{ $t('supply.approvePass') }}</a-radio>
            <a-radio value="reject">{{ $t('supply.approveReject') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="remark" :label="$t('supply.approveRemark')">
          <a-textarea v-model="approveForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  getSupplyPageApi, addSupplyApi, updateSupplyApi, deleteSupplyApi,
  getSupplyRequestPageApi, approveSupplyRequestApi, deleteSupplyRequestApi,
  type OfficeSupply, type OfficeSupplyRequest,
} from '@/api/supply'

const activeTab = ref('supply')
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

const categoryMap: Record<string, string> = { stationery: '文具', electronics: '电子耗材', cleaning: '清洁用品', other: '其他' }
function categoryText(c: string | undefined): string { return c != null ? categoryMap[c] || '-' : '-' }

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
  if (!supplyForm.supplyName) { Message.warning('用品名称必填'); return }
  saving.value = true
  try {
    if (isSupplyEdit.value) await updateSupplyApi(supplyForm)
    else await addSupplyApi(supplyForm)
    Message.success('保存成功')
    supplyDialogVisible.value = false
    loadSupplies()
  } finally { saving.value = false }
}

async function handleDeleteSupply(record: OfficeSupply) {
  await deleteSupplyApi(record.id)
  Message.success('删除成功')
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
    Message.success('审批完成')
    approveVisible.value = false
    loadRequests()
    loadSupplies() // 库存可能变化
  } finally { approving.value = false }
}

async function handleDeleteRequest(record: OfficeSupplyRequest) {
  await deleteSupplyRequestApi(record.id)
  Message.success('删除成功')
  loadRequests()
}

function showRemark(record: OfficeSupplyRequest) {
  Message.info(`${record.approveRemark || '-'}`, 5000)
}

onMounted(() => { loadSupplies(); loadRequests() })
</script>
