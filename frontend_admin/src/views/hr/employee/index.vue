<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="name" :label="$t('hr.name')">
          <a-input v-model="searchForm.name" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 180px" />
        </a-form-item>
        <a-form-item field="empNo" :label="$t('hr.empNo')">
          <a-input v-model="searchForm.empNo" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 180px" />
        </a-form-item>
        <a-form-item field="status" :label="$t('common.status')">
          <a-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <a-option value="0">{{ $t('hr.statusProbation') }}</a-option>
            <a-option value="1">{{ $t('hr.statusActive') }}</a-option>
            <a-option value="2">{{ $t('hr.statusLeft') }}</a-option>
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
          <span>{{ $t('hr.employeeList') }}</span>
          <a-button v-permission="PERM.hr.employeeAdd" type="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </a-button>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('hr.empNo')" data-index="empNo" :width="110" />
            <a-table-column :title="$t('hr.name')" data-index="name" :width="100" />
            <a-table-column :title="$t('hr.gender')" :width="70">
              <template #cell="{ record }">
                {{ genderText(record.gender) }}
              </template>
            </a-table-column>
            <a-table-column :title="$t('hr.deptName')" data-index="deptName" :width="130" />
            <a-table-column :title="$t('hr.position')" data-index="position" :width="130" />
            <a-table-column :title="$t('hr.phone')" data-index="phone" :width="130" />
            <a-table-column :title="$t('hr.hireDate')" data-index="hireDate" :width="110" />
            <a-table-column :title="$t('hr.employmentType')" :width="90">
              <template #cell="{ record }">
                {{ employmentText(record.employmentType) }}
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.status')" :width="80">
              <template #cell="{ record }">
                <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="150" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-permission="PERM.hr.employeeEdit" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</a-button>
                  <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button v-permission="PERM.hr.employeeDelete" size="small" status="danger">{{ $t('common.delete') }}</a-button>
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

    <!-- 编辑弹窗 -->
    <a-modal v-model:visible="editVisible" :title="isEdit ? $t('hr.editEmployee') : $t('hr.addEmployee')"
             :width="600" @ok="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <a-form :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="empNo" :label="$t('hr.empNo')" required>
              <a-input v-model="form.empNo" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="name" :label="$t('hr.name')" required>
              <a-input v-model="form.name" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="gender" :label="$t('hr.gender')">
              <a-select v-model="form.gender">
                <a-option value="0">{{ $t('hr.genderMale') }}</a-option>
                <a-option value="1">{{ $t('hr.genderFemale') }}</a-option>
                <a-option value="2">{{ $t('hr.genderUnknown') }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="deptName" :label="$t('hr.deptName')">
              <a-input v-model="form.deptName" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="position" :label="$t('hr.position')">
              <a-input v-model="form.position" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="phone" :label="$t('hr.phone')">
              <a-input v-model="form.phone" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="email" :label="$t('hr.email')">
              <a-input v-model="form.email" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="idCard" :label="$t('hr.idCard')">
              <a-input v-model="form.idCard" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="hireDate" :label="$t('hr.hireDate')">
              <a-date-picker v-model="form.hireDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="leaveDate" :label="$t('hr.leaveDate')">
              <a-date-picker v-model="form.leaveDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="employmentType" :label="$t('hr.employmentType')">
              <a-select v-model="form.employmentType">
                <a-option value="0">{{ $t('hr.empFullTime') }}</a-option>
                <a-option value="1">{{ $t('hr.empIntern') }}</a-option>
                <a-option value="2">{{ $t('hr.empPartTime') }}</a-option>
                <a-option value="3">{{ $t('hr.empLabor') }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="education" :label="$t('hr.education')">
              <a-select v-model="form.education" allow-clear>
                <a-option value="高中">高中</a-option>
                <a-option value="大专">大专</a-option>
                <a-option value="本科">本科</a-option>
                <a-option value="硕士">硕士</a-option>
                <a-option value="博士">博士</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="status" :label="$t('common.status')">
              <a-select v-model="form.status">
                <a-option value="0">{{ $t('hr.statusProbation') }}</a-option>
                <a-option value="1">{{ $t('hr.statusActive') }}</a-option>
                <a-option value="2">{{ $t('hr.statusLeft') }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item field="remark" :label="$t('common.remark')">
              <a-textarea v-model="form.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getEmployeePageApi, addEmployeeApi, updateEmployeeApi, deleteEmployeeApi, type HrEmployee } from '@/api/hr'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const tableData = ref<HrEmployee[]>([])
const editVisible = ref(false)
const isEdit = ref(false)

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
  loading.value = true
  try {
    const res = await getEmployeePageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ...searchForm,
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
  if (!form.empNo || !form.name) {
    Message.warning(t('common.msg.empNoNameRequired'))
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateEmployeeApi(form)
    } else {
      await addEmployeeApi(form)
    }
    Message.success(t('common.saveSuccess'))
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: HrEmployee) {
  await deleteEmployeeApi(record.id)
  Message.success(t('common.deleteSuccess'))
  loadData()
}

onMounted(loadData)
</script>
