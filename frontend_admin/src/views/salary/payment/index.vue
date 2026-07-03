<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :data="searchForm" :label-width="80" :on-submit="handleSearch">
        <t-form-item name="paymentMonth" :label="$t('salary.paymentMonth')">
          
<t-date-picker v-model="searchForm.paymentMonth" mode="month" format="YYYY-MM" value-type="YYYY-MM" clearable />
        </t-form-item>
        <t-form-item name="paymentStatus" :label="$t('salary.paymentStatus')">
          <t-select v-model="searchForm.paymentStatus" :placeholder="$t('common.all')" allow-clear>
            <t-option value="0">{{ $t('salary.statusUnpaid') }}</t-option>
            <t-option value="1">{{ $t('salary.statusPaid') }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item name="keyword" :label="$t('salary.userName')">
          <t-input v-model="searchForm.keyword" :placeholder="$t('common.inputPlaceholder')" allow-clear />
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" type="submit">{{ $t('common.search') }}</t-button>
            <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-card>

    <!-- 表格 -->
    <t-card>
      <div class="bz-list-toolbar bz-list-toolbar--first">
        <t-space>
          <t-button v-permission="PERM.salary.paymentExecute" theme="primary" @click="openGenerate">
              <template #icon><icon-plus /></template>
              {{ $t('salary.generate') }}
            </t-button>
          <t-button v-permission="PERM.salary.paymentExecute" theme="success" :disabled="!selectedRowKeys.length" @click="handleExecute">
              {{ $t('salary.executePay') }}
            </t-button>
        </t-space>
      </div>

      
          <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false"
                   :selected-row-keys="selectedRowKeys" @select-change="onSelectChange" :hover="true" vertical-align="top">
    <template #paymentStatus="{ row }">
                <t-tag :theme="row.paymentStatus === '1' ? 'success' : 'warning'" variant="light">
                  {{ row.paymentStatus === '1' ? $t('salary.statusPaid') : $t('salary.statusUnpaid') }}
                </t-tag>
              </template>
    <template #actualSalary="{ row }">
                <span class="font-medium">¥{{ Number(row.actualSalary || 0).toFixed(2) }}</span>
              </template>
    <template #operate="{ row }">
                <t-space>
                  <t-popconfirm v-if="row.paymentStatus === '1'" :content="$t('salary.revokeConfirm')" @confirm="handleRevoke(row)">
                    <t-button v-permission="PERM.salary.paymentExecute" size="small" theme="warning">{{ $t('salary.revoke') }}</t-button>
                  </t-popconfirm>
                  <t-popconfirm v-else :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button v-permission="PERM.salary.paymentExecute" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
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
      
    </t-card>

    <!-- 生成草稿弹窗 -->
    <t-dialog v-model:visible="generateVisible" :title="$t('salary.generate')" :width="440"
             @confirm="handleGenerate" @cancel="generateVisible = false" :ok-loading="generating">
      <t-form ref="generateFormRef" :data="generateForm" layout="vertical">
        <t-form-item name="paymentMonth" :label="$t('salary.paymentMonth')" required>
          
<t-date-picker v-model="generateForm.paymentMonth" mode="month" format="YYYY-MM" value-type="YYYY-MM" style="width: 100%" :placeholder="$t('salary.monthPlaceholder')" />
        </t-form-item>
        <t-form-item :label="$t('salary.generateHint')">
          <span class="text-sm text-t-secondary">{{ $t('salary.generateTip') }}</span>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import {
  getSalaryPaymentPageApi, generateSalaryPaymentApi, executeSalaryPaymentApi,
  revokeSalaryPaymentApi, deleteSalaryPaymentApi,
  type SalaryPaymentRecord,
} from '@/api/salary'

const { t } = useI18n()
const loading = ref(false)
const generating = ref(false)
const tableData = ref<SalaryPaymentRecord[]>([])
const selectedRowKeys = ref<(string | number)[]>([])
const generateVisible = ref(false)

const searchForm = reactive({ paymentMonth: '', paymentStatus: '', keyword: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const generateFormRef = ref<FormInstanceFunctions>()
const generateForm = reactive({ paymentMonth: '' })

/** 仅未发放记录可勾选执行 */
const tableColumns = computed(() => [
  { colKey: 'row-select', type: 'multiple' as const, width: 46 },
  { colKey: 'userName', title: t('salary.userName'), width: 100 },
  { colKey: 'paymentMonth', title: t('salary.paymentMonth'), width: 100 },
  { colKey: 'totalIncome', title: t('salary.totalIncome'), width: 110, align: 'right' },
  { colKey: 'totalDeduction', title: t('salary.totalDeduction'), width: 110, align: 'right' },
  { colKey: 'actualSalary', title: t('salary.actualSalary'), width: 120, align: 'right' },
  { colKey: 'paymentStatus', title: t('salary.paymentStatus'), width: 90 },
  { colKey: 'paymentTime', title: t('salary.paymentTime'), width: 170 },
  { colKey: 'operate', title: t('common.operate'), width: 110, align: 'right' },
])

function onSelectChange(keys: (string | number)[]) {
  selectedRowKeys.value = keys
}

async function loadData() {
  loading.value = true
  try {
    const res = await getSalaryPaymentPageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      paymentMonth: searchForm.paymentMonth || undefined,
      paymentStatus: searchForm.paymentStatus || undefined,
      keyword: searchForm.keyword || undefined,
    })
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
    selectedRowKeys.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  searchForm.paymentMonth = ''
  searchForm.paymentStatus = ''
  searchForm.keyword = ''
  pagination.current = 1
  loadData()
}

function openGenerate() {
  generateForm.paymentMonth = searchForm.paymentMonth || ''
  generateVisible.value = true
}

async function handleGenerate() {
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await generateFormRef.value?.validate()) !== true) return
  if (!generateForm.paymentMonth) {
    MessagePlugin.warning(t('salary.monthRequired'))
    return
  }
  generating.value = true
  try {
    const res = await generateSalaryPaymentApi({ paymentMonth: generateForm.paymentMonth })
    MessagePlugin.success(t('salary.generateSuccess').replace('{n}', String(res.data ?? 0)))
    generateVisible.value = false
    loadData()
  } finally {
    generating.value = false
  }
}

async function handleExecute() {
  if (!selectedRowKeys.value.length) return
  // 只提交未发放记录（已发放行的选择无意义）
  const ids = tableData.value
    .filter((r) => r.paymentStatus === '0' && selectedRowKeys.value.includes(r.id))
    .map((r) => r.id)
  if (!ids.length) {
    MessagePlugin.warning(t('salary.noPayableSelected'))
    return
  }
  const res = await executeSalaryPaymentApi(ids)
  MessagePlugin.success(t('salary.executeSuccess').replace('{n}', String(res.data ?? 0)))
  loadData()
}

async function handleRevoke(record: SalaryPaymentRecord) {
  await revokeSalaryPaymentApi(record.id)
  MessagePlugin.success(t('salary.revoked'))
  loadData()
}

async function handleDelete(record: SalaryPaymentRecord) {
  await deleteSalaryPaymentApi(record.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadData()
}

onMounted(loadData)
</script>
