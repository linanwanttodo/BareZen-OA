<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <t-form-item field="userName" :label="$t('schedule.userName')">
          <t-input v-model="searchForm.userName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 160px" />
        </t-form-item>
        <t-form-item field="status" :label="$t('schedule.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <t-option value="0">{{ $t('schedule.statusTodo') }}</t-option>
            <t-option value="1">{{ $t('schedule.statusDoing') }}</t-option>
            <t-option value="2">{{ $t('schedule.statusDone') }}</t-option>
            <t-option value="3">{{ $t('schedule.statusCancelled') }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item field="dateRange" :label="$t('schedule.dateRange')">
          
<!-- TODO(date-picker): 核对返回值格式（原 Arco 为字符串） --><t-date-picker v-model="searchForm.dateRange" type="daterange" style="width: 260px" />
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
        <span>{{ $t('schedule.title') }}</span>
        <t-button v-permission="PERM.hr.scheduleAdd" class="ml-4" theme="primary" size="small" @click="openAdd">
          {{ $t('common.add') }}
        </t-button>
      </template>

      <t-loading :loading="loading">
          <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
  </t-table>

        <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
          <t-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            show-total
            @change="loadData"
          />
        </div>
      </t-loading>
    </t-card>

    <!-- 新增/编辑弹窗 -->
    <t-dialog v-model="dialogVisible" :title="isEdit ? $t('common.edit') : $t('common.add')" :width="560" :unmount-on-close="true" @before-ok="handleSave">
      <t-form :model="form" layout="vertical">
        <t-form-item field="title" :label="$t('schedule.title')" required>
          <t-input v-model="form.title" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item field="userName" :label="$t('schedule.userName')">
          <t-input v-model="form.userName" :placeholder="$t('schedule.userNamePlaceholder')" />
        </t-form-item>
        <t-form-item field="timeRange" :label="$t('schedule.timeRange')" required>
          
<!-- TODO(date-picker): 核对返回值格式（原 Arco 为字符串） --><t-date-picker v-model="form.startTime" show-time style="width: calc(50% - 12px)" :placeholder="$t('schedule.startTime')" />
          <span class="mx-2">~</span>
          
<!-- TODO(date-picker): 核对返回值格式（原 Arco 为字符串） --><t-date-picker v-model="form.endTime" show-time style="width: calc(50% - 12px)" :placeholder="$t('schedule.endTime')" />
        </t-form-item>
        <t-form-item field="location" :label="$t('schedule.location')">
          <t-input v-model="form.location" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item field="status" :label="$t('schedule.status')">
          <t-select v-model="form.status">
            <t-option value="0">{{ $t('schedule.statusTodo') }}</t-option>
            <t-option value="1">{{ $t('schedule.statusDoing') }}</t-option>
            <t-option value="2">{{ $t('schedule.statusDone') }}</t-option>
            <t-option value="3">{{ $t('schedule.statusCancelled') }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item field="description" :label="$t('schedule.description')">
          <t-textarea v-model="form.description" :placeholder="$t('common.inputPlaceholder')" :max-length="1000" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
import { MessagePlugin as Message } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import { getSchedulePageApi, addScheduleApi, updateScheduleApi, deleteScheduleApi, type HrSchedule } from '@/api/schedule'

const { t } = useI18n()
const loading = ref(false)
const tableData = ref<HrSchedule[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive<Partial<HrSchedule>>({})
const saving = ref(false)
// Arco DatePicker 类型声明未建模 daterange（update:modelValue 声明为单值），
// 实际运行时返回数组，故字段声明为单值并用 Array.isArray 做运行时防护
const searchForm = reactive<{
  userName: string
  status: string
  dateRange: string | number | Date | undefined
}>({ userName: '', status: '', dateRange: undefined })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

function statusText(status: string | undefined): string {
  const map: Record<string, string> = {
    '0': t('schedule.statusTodo'),
    '1': t('schedule.statusDoing'),
    '2': t('schedule.statusDone'),
    '3': t('schedule.statusCancelled'),
  }
  return status != null ? map[status] || '-' : '-'
}
function statusColor(status: string | undefined): string {
  if (status === '0') return 'blue'
  if (status === '1') return 'orange'
  if (status === '2') return 'green'
  return 'default'
}

async function loadData() {
  loading.value = true
  try {
    const range = searchForm.dateRange
    const res = await getSchedulePageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      userName: searchForm.userName || undefined,
      status: searchForm.status || undefined,
      startDate: Array.isArray(range) ? String(range[0] ?? '') || undefined : undefined,
      endDate: Array.isArray(range) ? String(range[1] ?? '') || undefined : undefined,
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
  searchForm.userName = ''
  searchForm.status = ''
  searchForm.dateRange = undefined
  pagination.current = 1
  loadData()
}

function openAdd() {
  isEdit.value = false
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  form.status = '0'
  dialogVisible.value = true
}

function openEdit(record: HrSchedule) {
  isEdit.value = true
  Object.assign(form, record)
  dialogVisible.value = true
}

async function handleSave(done: (closed: boolean) => void) {
  if (!form.title) {
    Message.warning(t('schedule.titleRequired'))
    done(false)
    return
  }
  if (!form.startTime || !form.endTime) {
    Message.warning(t('schedule.timeRequired'))
    done(false)
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateScheduleApi(form)
    } else {
      await addScheduleApi(form)
    }
    Message.success(t('common.saveSuccess'))
    loadData()
    done(true)
  } catch (e) {
    Message.error((e as Error)?.message || t('common.failed'))
    done(false)
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: HrSchedule) {
  try {
    await deleteScheduleApi(record.id)
    Message.success(t('common.deleteSuccess'))
    loadData()
  } catch (e) {
    Message.error((e as Error)?.message || t('common.failed'))
  }
}

onMounted(loadData)

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    ,
  ])

</script>
