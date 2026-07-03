<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :data="searchForm" :label-width="80" :on-submit="handleSearch">
        <t-form-item name="keyword" :label="$t('salary.userName')">
          <t-input v-model="searchForm.keyword" :placeholder="$t('common.inputPlaceholder')" allow-clear />
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" html-type="submit">{{ $t('common.search') }}</t-button>
            <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-card>

    <!-- 表格 -->
    <t-card>
      <div class="bz-list-toolbar bz-list-toolbar--first">
        <t-space>
          <t-button v-permission="PERM.salary.structureAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
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
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import {
  getSalaryStructurePageApi, addSalaryStructureApi, updateSalaryStructureApi, deleteSalaryStructureApi,
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
  { colKey: 'operate', title: t('common.operate'), width: 140, align: 'right' },
])
</script>
