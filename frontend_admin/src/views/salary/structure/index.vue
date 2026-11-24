<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="bz-list-panel">
      <t-form :data="searchForm" :label-width="80" :on-submit="handleSearch">
        <t-row :gutter="[24, 24]">
        <t-col :span="3">
          <t-form-item name="keyword" :label="$t('salary.userName')">
            <t-input v-model="searchForm.keyword" :placeholder="$t('common.inputPlaceholder')" allow-clear />
          </t-form-item>
        </t-col>
        <t-col :span="3" class="bz-list-ops">
          <t-form-item>
            <t-space>
              <t-button theme="primary" type="submit">{{ $t('common.search') }}</t-button>
              <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
            </t-space>
          </t-form-item>
        </t-col>
        </t-row>
      </t-form>

      <div class="bz-list-toolbar bz-list-toolbar--first">
        <t-space>
          <t-button v-permission="PERM.salary.structureAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </t-button>
          <t-button v-permission="PERM.salary.structureCalculate" @click="openBatchTrial">
            <template #icon><icon-command /></template>
            {{ $t('salary.trialCalcBatch') }}
          </t-button>
        </t-space>
      </div>

      
          <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #baseSalary="{ row }">{{ money(num(row.baseSalary)) }}</template>
    <template #performanceSalary="{ row }">{{ money(num(row.performanceSalary)) }}</template>
    <template #totalIncome="{ row }">{{ money(totalOf(row)) }}</template>
    <template #incomeTax="{ row }">{{ money(num(row.incomeTax)) }}</template>
    <template #actualSalary="{ row }">
                <span class="font-medium">{{ money(actualOf(row)) }}</span>
              </template>
    <template #operate="{ row }">
                <t-space>
                  <t-link theme="primary" v-permission="PERM.salary.structureCalculate" @click="openTrial(row)">{{ $t('salary.trialCalc') }}</t-link>
                  <t-link theme="primary" v-permission="PERM.salary.structureEdit" @click="handleEdit(row)">{{ $t('common.edit') }}</t-link>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button v-permission="PERM.salary.structureDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
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

    <!-- 编辑弹窗 -->
    <t-dialog v-model:visible="editVisible" :title="isEdit ? $t('salary.editStructure') : $t('salary.addStructure')"
             :width="640" @confirm="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <t-form ref="formRef" :data="form" layout="vertical">
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item name="userId" :label="$t('salary.userId')" :rules="[{ required: true, message: t('validation.required', { field: t('salary.userId') }) }]">
              <t-select
                v-model="form.userId"
                filterable
                remote
                :loading="userSearching"
                :options="userOptions"
                :placeholder="$t('salary.userSearchPlaceholder')"
                style="width: 100%"
                :disabled="isEdit"
                @search="handleUserSearch"
                @change="handleUserChange"
              />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="userName" :label="$t('salary.userName')" :rules="[{ required: true, message: t('validation.required', { field: t('salary.userName') }) }]">
              <t-input v-model="form.userName" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="baseSalary" :label="$t('salary.baseSalary')">
              <t-input-number v-model="form.baseSalary" :min="0" :decimal-places="2" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="performanceSalary" :label="$t('salary.performanceSalary')">
              <t-input-number v-model="form.performanceSalary" :min="0" :decimal-places="2" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="positionAllowance" :label="$t('salary.positionAllowance')">
              <t-input-number v-model="form.positionAllowance" :min="0" :decimal-places="2" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="transportAllowance" :label="$t('salary.transportAllowance')">
              <t-input-number v-model="form.transportAllowance" :min="0" :decimal-places="2" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="mealAllowance" :label="$t('salary.mealAllowance')">
              <t-input-number v-model="form.mealAllowance" :min="0" :decimal-places="2" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="otherAllowance" :label="$t('salary.otherAllowance')">
              <t-input-number v-model="form.otherAllowance" :min="0" :decimal-places="2" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="socialInsurancePersonal" :label="$t('salary.socialInsurance')">
              <t-input-number v-model="form.socialInsurancePersonal" :min="0" :decimal-places="2" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="housingFundPersonal" :label="$t('salary.housingFund')">
              <t-input-number v-model="form.housingFundPersonal" :min="0" :decimal-places="2" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="incomeTax" :label="$t('salary.incomeTax')">
              <t-input-number v-model="form.incomeTax" :min="0" :decimal-places="2" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item :label="$t('salary.actualPreview')">
              <span class="font-medium leading-8">{{ money(actualOf(form)) }}</span>
            </t-form-item>
          </t-col>
          <t-col :span="24">
            <t-form-item name="remark" :label="$t('common.remark')">
              <t-textarea v-model="form.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
    </t-dialog>

    <!-- 试算弹窗：按员工调后端 calculate 接口 -->
    <t-dialog v-model:visible="trialVisible" :header="$t('salary.trialCalcTitle')" :width="560" :footer="false" @close="trialVisible = false">
      <t-form-item name="userId" :label="$t('salary.userId')">
        <t-select
          v-model="trialUserId"
          filterable
          remote
          :loading="trialUserSearching"
          :options="trialUserOptions"
          :placeholder="$t('salary.userSearchPlaceholder')"
          style="width: 100%"
          @search="handleTrialUserSearch"
          @change="onTrialUserChange"
        />
      </t-form-item>
      <template v-if="trialUserId">
        <t-loading v-if="trialLoading" size="28px" :text="$t('common.loading')" />
        <t-descriptions v-if="trialStructure" :column="2" :data="trialItems" layout="inline-vertical" />
        <t-empty v-else-if="!trialLoading" :description="$t('salary.trialNoStructure')" />
        <div v-if="trialResult !== null" class="mt-4 flex items-center justify-between px-1">
          <span class="text-sm" style="color: var(--wecom-text-tertiary)">{{ $t('salary.trialCalcResult') }}</span>
          <span class="text-lg font-medium">{{ money(num(trialResult)) }}</span>
        </div>
      </template>
    </t-dialog>

    <!-- 批量试算弹窗 -->
    <t-dialog
      v-model:visible="batchTrialVisible"
      :header="$t('salary.trialCalcBatchTitle')"
      :width="680"
      @confirm="handleBatchTrial"
      @cancel="batchTrialVisible = false"
      :ok-loading="batchTrialing"
    >
      <t-form-item name="userIds" :label="$t('salary.userId')">
        <t-select
          v-model="batchUserIds"
          multiple
          filterable
          remote
          :loading="batchUserSearching"
          :options="batchUserOptions"
          :placeholder="$t('salary.userSearchPlaceholder')"
          style="width: 100%"
          @search="handleBatchUserSearch"
        />
      </t-form-item>
      <div v-if="batchResults.length" class="mt-4">
        <t-table row-key="userId" :data="batchResults" :columns="batchTrialColumns" :pagination="false" size="small" :hover="true">
          <template #totalIncome="{ row }">{{ money(totalOf(row)) }}</template>
          <template #actualSalary="{ row }">
            <span class="font-medium">{{ money(actualOf(row)) }}</span>
          </template>
        </t-table>
        <p class="mt-2 text-sm" style="color: var(--wecom-text-tertiary)">{{ $t('salary.trialBatchCount', { n: batchResults.length }) }}</p>
      </div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import {
  getSalaryStructurePageApi, addSalaryStructureApi, updateSalaryStructureApi, deleteSalaryStructureApi,
  getSalaryStructureByUserApi, calculateSalaryApi, batchCalculateSalaryApi,
  type SalaryStructure,
} from '@/api/salary'
import { getUserListApi, type SysUser } from '@/api/user'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const tableData = ref<SalaryStructure[]>([])
const editVisible = ref(false)
const isEdit = ref(false)

const searchForm = reactive({ keyword: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
// userId 后端 Long 序列化为 string，选择器直接以 string id 作为选项值
type StructureForm = Omit<Partial<SalaryStructure>, 'userId'> & { userId?: string | number }
const formRef = ref<FormInstanceFunctions>()
const form = reactive<StructureForm>({})

/** 员工远程搜索选择器（登录名/昵称模糊匹配） */
const userOptions = ref<Array<{ value: string; label: string }>>([])
const userSearching = ref(false)
let userSearchTimer: ReturnType<typeof setTimeout> | null = null

function toOption(u: SysUser) {
  return { value: String(u.id), label: `${u.nickName || u.userName}（${u.userName}）` }
}

async function fetchUsers(kw: string) {
  userSearching.value = true
  try {
    const res = await getUserListApi({ pageNum: 1, pageSize: 20, userName: kw || undefined, status: '0' })
    userOptions.value = (res.data.list || []).map(toOption)
  } finally {
    userSearching.value = false
  }
}

function handleUserSearch(kw: string) {
  if (userSearchTimer) clearTimeout(userSearchTimer)
  userSearchTimer = setTimeout(() => fetchUsers(kw.trim()), 300)
}

function handleUserChange(val: unknown) {
  const hit = userOptions.value.find((o) => o.value === String(val))
  if (hit) {
    // 选中后自动回填姓名（取昵称部分）
    form.userName = hit.label.replace(/（.*）$/, '')
  }
}

function num(v: unknown): number {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

/** 收入合计（六项工资构成） */
function totalOf(r: Partial<SalaryStructure> | StructureForm): number {
  return num(r.baseSalary) + num(r.performanceSalary) + num(r.positionAllowance)
    + num(r.transportAllowance) + num(r.mealAllowance) + num(r.otherAllowance)
}

/** 实发 = 收入合计 - 社保 - 公积金 - 个税（与后端计算口径一致） */
function actualOf(r: Partial<SalaryStructure> | StructureForm): number {
  return totalOf(r) - num(r.socialInsurancePersonal) - num(r.housingFundPersonal) - num(r.incomeTax)
}

function money(v: number): string {
  return `¥${v.toFixed(2)}`
}

async function loadData() {
  loading.value = true
  try {
    const res = await getSalaryStructurePageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
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
  searchForm.keyword = ''
  pagination.current = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  form.baseSalary = 0
  form.performanceSalary = 0
  userOptions.value = []
  fetchUsers('')
  editVisible.value = true
}

function handleEdit(record: SalaryStructure) {
  isEdit.value = true
  Object.assign(form, record)
  // 编辑时锁定员工，选项回填当前值保证选择器有展示文案
  userOptions.value = [{ value: String(record.userId), label: record.userName || String(record.userId) }]
  editVisible.value = true
}

async function handleSave() {
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await formRef.value?.validate()) !== true) {
    
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateSalaryStructureApi(form)
    } else {
      await addSalaryStructureApi(form)
    }
    MessagePlugin.success(t('common.saveSuccess'))
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: SalaryStructure) {
  await deleteSalaryStructureApi(record.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadData()
}

// ---- 单人试算 ----
const trialVisible = ref(false)
const trialLoading = ref(false)
const trialUserId = ref<string | undefined>(undefined)
const trialStructure = ref<SalaryStructure | null>(null)
const trialResult = ref<string | null>(null)

const trialUserOptions = ref<Array<{ value: string; label: string }>>([])
const trialUserSearching = ref(false)
let trialUserTimer: ReturnType<typeof setTimeout> | null = null

const trialItems = computed(() => {
  const s = trialStructure.value
  if (!s) return []
  return [
    { label: t('salary.userName'), value: s.userName || '-' },
    { label: t('salary.baseSalary'), value: money(num(s.baseSalary)) },
    { label: t('salary.performanceSalary'), value: money(num(s.performanceSalary)) },
    { label: t('salary.positionAllowance'), value: money(num(s.positionAllowance)) },
    { label: t('salary.transportAllowance'), value: money(num(s.transportAllowance)) },
    { label: t('salary.mealAllowance'), value: money(num(s.mealAllowance)) },
    { label: t('salary.otherAllowance'), value: money(num(s.otherAllowance)) },
    { label: t('salary.socialInsurance'), value: money(num(s.socialInsurancePersonal)) },
    { label: t('salary.housingFund'), value: money(num(s.housingFundPersonal)) },
    { label: t('salary.incomeTax'), value: money(num(s.incomeTax)) },
  ]
})

function handleTrialUserSearch(kw: string) {
  if (trialUserTimer) clearTimeout(trialUserTimer)
  trialUserTimer = setTimeout(async () => {
    trialUserSearching.value = true
    try {
      const res = await getUserListApi({ pageNum: 1, pageSize: 20, userName: kw.trim() || undefined, status: '0' })
      trialUserOptions.value = (res.data.list || []).map(toOption)
    } finally {
      trialUserSearching.value = false
    }
  }, 300)
}

async function onTrialUserChange(val: unknown) {
  if (!val) return
  await loadTrialData(String(val))
}

async function loadTrialData(userId: string) {
  trialLoading.value = true
  trialStructure.value = null
  trialResult.value = null
  try {
    const [structRes, calcRes] = await Promise.allSettled([
      getSalaryStructureByUserApi(userId),
      calculateSalaryApi(userId),
    ])
    if (structRes.status === 'fulfilled') {
      trialStructure.value = structRes.value.data || null
    }
    if (calcRes.status === 'fulfilled') {
      trialResult.value = calcRes.value.data ?? null
    }
  } finally {
    trialLoading.value = false
  }
}

function openTrial(record?: SalaryStructure) {
  trialVisible.value = true
  trialStructure.value = null
  trialResult.value = null
  if (record) {
    trialUserId.value = String(record.userId)
    trialUserOptions.value = [{ value: String(record.userId), label: record.userName || String(record.userId) }]
    loadTrialData(String(record.userId))
  } else {
    trialUserId.value = undefined
    trialUserOptions.value = []
  }
}

// ---- 批量试算 ----
const batchTrialVisible = ref(false)
const batchTrialing = ref(false)
const batchUserIds = ref<string[]>([])
const batchResults = ref<SalaryStructure[]>([])

const batchUserOptions = ref<Array<{ value: string; label: string }>>([])
const batchUserSearching = ref(false)
let batchUserTimer: ReturnType<typeof setTimeout> | null = null

const batchTrialColumns = computed(() => [
  { colKey: 'userName', title: t('salary.userName'), width: 120 },
  { colKey: 'totalIncome', title: t('salary.totalIncome'), width: 120, align: 'right' },
  { colKey: 'incomeTax', title: t('salary.incomeTax'), width: 110, align: 'right' },
  { colKey: 'actualSalary', title: t('salary.actualSalary'), width: 130, align: 'right' },
])

function handleBatchUserSearch(kw: string) {
  if (batchUserTimer) clearTimeout(batchUserTimer)
  batchUserTimer = setTimeout(async () => {
    batchUserSearching.value = true
    try {
      const res = await getUserListApi({ pageNum: 1, pageSize: 20, userName: kw.trim() || undefined, status: '0' })
      batchUserOptions.value = (res.data.list || []).map(toOption)
    } finally {
      batchUserSearching.value = false
    }
  }, 300)
}

function openBatchTrial() {
  batchTrialVisible.value = true
  batchUserIds.value = []
  batchResults.value = []
  batchUserOptions.value = []
}

async function handleBatchTrial() {
  if (!batchUserIds.value.length) {
    MessagePlugin.warning(t('salary.trialUserRequired'))
    return
  }
  batchTrialing.value = true
  try {
    const res = await batchCalculateSalaryApi(batchUserIds.value)
    batchResults.value = res.data || []
    MessagePlugin.success(t('salary.trialBatchCount', { n: batchResults.value.length }))
  } finally {
    batchTrialing.value = false
  }
}

onMounted(loadData)

/** 表格列定义（自定义单元格见 t-table 同名插槽） */
const tableColumns = computed(() => [
  { colKey: 'userId', title: t('salary.userId'), width: 90 },
  { colKey: 'userName', title: t('salary.userName'), width: 100 },
  { colKey: 'baseSalary', title: t('salary.baseSalary'), width: 110, align: 'right' },
  { colKey: 'performanceSalary', title: t('salary.performanceSalary'), width: 110, align: 'right' },
  { colKey: 'totalIncome', title: t('salary.totalIncome'), width: 110, align: 'right' },
  { colKey: 'incomeTax', title: t('salary.incomeTax'), width: 100, align: 'right' },
  { colKey: 'actualSalary', title: t('salary.actualSalary'), width: 120, align: 'right' },
  { colKey: 'remark', title: t('common.remark'), ellipsis: true },
  { colKey: 'operate', title: t('common.operate'), width: 180, align: 'right' },
])
</script>
