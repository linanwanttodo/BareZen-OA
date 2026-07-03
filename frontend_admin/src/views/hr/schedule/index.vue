<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :data="searchForm" :label-width="80" @submit-success="handleSearch">
        <t-form-item name="userName" :label="$t('schedule.userName')">
          <t-input v-model="searchForm.userName" :placeholder="$t('common.inputPlaceholder')" allow-clear />
        </t-form-item>
        <t-form-item name="status" :label="$t('schedule.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear>
            <t-option value="0">{{ $t('schedule.statusTodo') }}</t-option>
            <t-option value="1">{{ $t('schedule.statusDoing') }}</t-option>
            <t-option value="2">{{ $t('schedule.statusDone') }}</t-option>
            <t-option value="3">{{ $t('schedule.statusCancelled') }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item name="dateRange" :label="$t('schedule.dateRange')">
          
<t-date-picker v-model="searchForm.dateRange" type="daterange"  value-type="YYYY-MM-DD" />
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
          <t-button v-permission="PERM.hr.scheduleAdd" class="ml-4" theme="primary" size="small" @click="openAdd">
          {{ $t('common.add') }}
        </t-button>
        </t-space>
      </div>

      
          <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #timeRange="{ row }">
                <span class="text-sm">{{ row.startTime }} ~ {{ row.endTime }}</span>
              </template>
    <template #status="{ row }">
                <t-tag :color="statusColor(row.status)" variant="light">{{ statusText(row.status) }}</t-tag>
              </template>
    <template #operate="{ row }">
                <t-space>
                  <t-link theme="primary" v-permission="PERM.hr.scheduleEdit" @click="openEdit(row)">{{ $t('common.edit') }}</t-link>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button v-permission="PERM.hr.scheduleDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
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
      
    </t-card>

    <!-- 新增/编辑弹窗 -->
    <t-dialog v-model:visible="dialogVisible" :title="isEdit ? $t('common.edit') : $t('common.add')" :width="560" :unmount-on-close="true" @before-ok="handleSave">
      <t-form ref="formRef" :data="form" layout="vertical">
        <t-form-item name="title" :label="$t('schedule.title')" :rules="[{ required: true, message: t('validation.required', { field: t('schedule.title') }) }]">
          <t-input v-model="form.title" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item name="userName" :label="$t('schedule.userName')">
          <t-input v-model="form.userName" :placeholder="$t('schedule.userNamePlaceholder')" />
        </t-form-item>
        <!-- name 必须指向 :data 里真实存在的字段。历史上用展示用的 name="timeRange"，
             而控件绑的是 form.startTime/form.endTime —— 该键恒为 undefined，
             挂 required 会永远报错并阻塞提交。正确做法是锚定真实字段。 -->
        <t-form-item name="startTime" :label="$t('schedule.timeRange')" :rules="[{ required: true, message: t('validation.required', { field: t('schedule.startTime') }) }]">
          <t-date-picker v-model="form.startTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: calc(50% - 12px)" :placeholder="$t('schedule.startTime')" />
          <span class="mx-2">~</span>
          <t-date-picker v-model="form.endTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: calc(50% - 12px)" :placeholder="$t('schedule.endTime')" />
        </t-form-item>
        <t-form-item name="location" :label="$t('schedule.location')">
          <t-input v-model="form.location" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item name="status" :label="$t('schedule.status')">
          <t-select v-model="form.status">
            <t-option value="0">{{ $t('schedule.statusTodo') }}</t-option>
            <t-option value="1">{{ $t('schedule.statusDoing') }}</t-option>
            <t-option value="2">{{ $t('schedule.statusDone') }}</t-option>
            <t-option value="3">{{ $t('schedule.statusCancelled') }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item name="description" :label="$t('schedule.description')">
          <t-textarea v-model="form.description" :placeholder="$t('common.inputPlaceholder')" :max-length="1000" show-word-limit />
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
import { getSchedulePageApi, addScheduleApi, updateScheduleApi, deleteScheduleApi, type HrSchedule } from '@/api/schedule'

const { t } = useI18n()
const loading = ref(false)
const tableData = ref<HrSchedule[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstanceFunctions>()
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
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」。
  // 本页走 t-dialog 的 @before-ok，需用 done(false) 阻止关闭，而不是直接 return。
  if ((await formRef.value?.validate()) !== true) {
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
    MessagePlugin.success(t('common.saveSuccess'))
    loadData()
    done(true)
  } catch (e) {
    MessagePlugin.error((e as Error)?.message || t('common.failed'))
    done(false)
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: HrSchedule) {
  try {
    await deleteScheduleApi(record.id)
    MessagePlugin.success(t('common.deleteSuccess'))
    loadData()
  } catch (e) {
    MessagePlugin.error((e as Error)?.message || t('common.failed'))
  }
}

onMounted(loadData)

/** 表格列定义（自定义单元格见 t-table 同名插槽） */
const tableColumns = computed(() => [
  { colKey: 'title', title: t('schedule.title'), width: 160, ellipsis: true },
  { colKey: 'userName', title: t('schedule.userName'), width: 100 },
  { colKey: 'timeRange', title: t('schedule.timeRange'), width: 250 },
  { colKey: 'location', title: t('schedule.location'), width: 120, ellipsis: true },
  { colKey: 'description', title: t('schedule.description'), ellipsis: true },
  { colKey: 'status', title: t('schedule.status'), width: 90 },
  { colKey: 'operate', title: t('common.operate'), width: 140, align: 'right' },
])

</script>
