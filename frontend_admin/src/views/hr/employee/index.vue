<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :data="searchForm" :label-width="80" @submit-success="handleSearch">
        <t-form-item name="name" :label="$t('hr.name')">
          <t-input v-model="searchForm.name" :placeholder="$t('common.inputPlaceholder')" allow-clear />
        </t-form-item>
        <t-form-item name="empNo" :label="$t('hr.empNo')">
          <t-input v-model="searchForm.empNo" :placeholder="$t('common.inputPlaceholder')" allow-clear />
        </t-form-item>
        <t-form-item name="status" :label="$t('common.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear>
            <t-option value="0">{{ $t('hr.statusProbation') }}</t-option>
            <t-option value="1">{{ $t('hr.statusActive') }}</t-option>
            <t-option value="2">{{ $t('hr.statusLeft') }}</t-option>
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

    <!-- 表格 -->
    <t-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('hr.employeeList') }}</span>
          <t-button v-permission="PERM.hr.employeeAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </t-button>
        </div>
      </template>

      
          <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #gender="{ row }">
                {{ genderText(row.gender) }}
              </template>
    <template #employmentType="{ row }">
                {{ employmentText(row.employmentType) }}
              </template>
    <template #status="{ row }">
                <t-tag :color="statusColor(row.status)" variant="light">{{ statusText(row.status) }}</t-tag>
              </template>
    <template #col_9="{ row }">
                <t-space>
                  <t-link theme="primary" v-permission="PERM.hr.employeeEdit" @click="handleEdit(row)">{{ $t('common.edit') }}</t-link>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button v-permission="PERM.hr.employeeDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
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
    <t-dialog v-model:visible="editVisible" :title="isEdit ? $t('hr.editEmployee') : $t('hr.addEmployee')"
             :width="600" @confirm="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <t-form ref="formRef" :data="form" layout="vertical">
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item name="empNo" :label="$t('hr.empNo')" :rules="[{ required: true, message: t('validation.required', { field: t('hr.empNo') }) }]">
              <t-input v-model="form.empNo" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="name" :label="$t('hr.name')" :rules="[{ required: true, message: t('validation.required', { field: t('hr.name') }) }]">
              <t-input v-model="form.name" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="gender" :label="$t('hr.gender')">
              <t-select v-model="form.gender">
                <t-option value="0">{{ $t('hr.genderMale') }}</t-option>
                <t-option value="1">{{ $t('hr.genderFemale') }}</t-option>
                <t-option value="2">{{ $t('hr.genderUnknown') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="deptId" :label="$t('hr.deptName')">
              
<!-- TODO(tree-select): data 字段 key/title 需转为 value/label --><t-tree-select
                v-model="form.deptId"
                :data="deptTreeData"
                :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
                :placeholder="$t('common.selectPlaceholder')"
                allow-clear
                allow-search
                style="width: 100%"
              />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="position" :label="$t('hr.position')">
              <t-input v-model="form.position" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="phone" :label="$t('hr.phone')">
              <t-input v-model="form.phone" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="email" :label="$t('hr.email')">
              <t-input v-model="form.email" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="idCard" :label="$t('hr.idCard')">
              <t-input v-model="form.idCard" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="hireDate" :label="$t('hr.hireDate')">
              
<t-date-picker v-model="form.hireDate" style="width: 100%"  value-type="YYYY-MM-DD" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="leaveDate" :label="$t('hr.leaveDate')">
              
<t-date-picker v-model="form.leaveDate" style="width: 100%"  value-type="YYYY-MM-DD" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="employmentType" :label="$t('hr.employmentType')">
              <t-select v-model="form.employmentType">
                <t-option value="0">{{ $t('hr.empFullTime') }}</t-option>
                <t-option value="1">{{ $t('hr.empIntern') }}</t-option>
                <t-option value="2">{{ $t('hr.empPartTime') }}</t-option>
                <t-option value="3">{{ $t('hr.empLabor') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="education" :label="$t('hr.education')">
              <t-select v-model="form.education" allow-clear>
                <t-option value="高中">高中</t-option>
                <t-option value="大专">大专</t-option>
                <t-option value="本科">本科</t-option>
                <t-option value="硕士">硕士</t-option>
                <t-option value="博士">博士</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="status" :label="$t('common.status')">
              <t-select v-model="form.status">
                <t-option value="0">{{ $t('hr.statusProbation') }}</t-option>
                <t-option value="1">{{ $t('hr.statusActive') }}</t-option>
                <t-option value="2">{{ $t('hr.statusLeft') }}</t-option>
              </t-select>
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
import { getEmployeePageApi, addEmployeeApi, updateEmployeeApi, deleteEmployeeApi, type HrEmployee } from '@/api/hr'
import { getDeptTreeApi, type SysDept } from '@/api/dept'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const tableData = ref<HrEmployee[]>([])
let requestSeq = 0
const editVisible = ref(false)
const isEdit = ref(false)
const deptTreeData = ref<SysDept[]>([])

const searchForm = reactive({
  name: '',
  empNo: '',
  status: '',
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const formRef = ref<FormInstanceFunctions>()
const form = reactive<Partial<HrEmployee>>({})

function genderText(gender: string | undefined): string {
  if (gender === '0') return t('enums.gender.male')
  if (gender === '1') return t('enums.gender.female')
  return t('enums.gender.unknown')
}

function employmentText(type: string | undefined): string {
  const map: Record<string, string> = { 0: 'enums.empType.fullTime', 1: 'enums.empType.intern', 2: 'enums.empType.partTime', 3: 'enums.empType.labor' }
  return type != null ? t(map[type] || '-') : '-'
}

function statusText(status: string | undefined): string {
  const map: Record<string, string> = { 0: 'enums.empStatus.probation', 1: 'enums.empStatus.active', 2: 'enums.empStatus.left' }
  return status != null ? t(map[status] || '-') : '-'
}

function statusColor(status: string | undefined): string {
  if (status === '0') return 'orange'
  if (status === '1') return 'green'
  if (status === '2') return 'red'
  return 'default'
}

async function loadData() {
  const seq = ++requestSeq
  loading.value = true
  try {
    const res = await getEmployeePageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ...searchForm,
    })
    // 仅最新请求的结果写入状态，避免旧响应覆盖新结果（搜索/翻页竞态）
    if (seq !== requestSeq) return
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  searchForm.name = ''
  searchForm.empNo = ''
  searchForm.status = ''
  pagination.current = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  form.empNo = ''
  form.name = ''
  form.gender = '2'
  form.employmentType = '0'
  form.status = '0'
  editVisible.value = true
}

function handleEdit(record: HrEmployee) {
  isEdit.value = true
  Object.assign(form, record)
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
      await updateEmployeeApi(form)
    } else {
      await addEmployeeApi(form)
    }
    MessagePlugin.success(t('common.saveSuccess'))
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: HrEmployee) {
  await deleteEmployeeApi(record.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadData()
}

async function loadDeptTree() {
  try {
    const res = await getDeptTreeApi()
    deptTreeData.value = res.data || []
  } catch {
    deptTreeData.value = []
  }
}

onMounted(() => {
  loadDeptTree()
  loadData()
})

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'empNo', title: t('hr.empNo'), width: 110 },
    { colKey: 'name', title: t('hr.name'), width: 100 },
    { colKey: 'gender', title: t('hr.gender'), width: 70 },
    { colKey: 'deptName', title: t('hr.deptName'), width: 130 },
    { colKey: 'position', title: t('hr.position'), width: 130 },
    { colKey: 'phone', title: t('hr.phone'), width: 130 },
    { colKey: 'hireDate', title: t('hr.hireDate'), width: 110 },
    { colKey: 'employmentType', title: t('hr.employmentType'), width: 90 },
    { colKey: 'status', title: t('common.status'), width: 80 },
    { colKey: 'col_9', title: t('common.operate'), width: 150, align: 'right' },
  ])

</script>
