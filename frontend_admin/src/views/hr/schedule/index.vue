<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="userName" :label="$t('schedule.userName')">
          <a-input v-model="searchForm.userName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 160px" />
        </a-form-item>
        <a-form-item field="status" :label="$t('schedule.status')">
          <a-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <a-option value="0">{{ $t('schedule.statusTodo') }}</a-option>
            <a-option value="1">{{ $t('schedule.statusDoing') }}</a-option>
            <a-option value="2">{{ $t('schedule.statusDone') }}</a-option>
            <a-option value="3">{{ $t('schedule.statusCancelled') }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="dateRange" :label="$t('schedule.dateRange')">
          <a-date-picker v-model="searchForm.dateRange" type="daterange" style="width: 260px" />
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
        <span>{{ $t('schedule.title') }}</span>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('schedule.userName')" data-index="userName" :width="110" />
            <a-table-column :title="$t('schedule.title')" data-index="title" ellipsis :min-width="160" />
            <a-table-column :title="$t('schedule.startTime')" data-index="startTime" :width="150" />
            <a-table-column :title="$t('schedule.endTime')" data-index="endTime" :width="150" />
            <a-table-column :title="$t('schedule.status')" :width="90">
              <template #cell="{ record }">
                <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('schedule.location')" data-index="location" :width="120">
              <template #cell="{ record }">{{ record.location || '-' }}</template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="120" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-permission="'hr:schedule:delete'" size="small" status="danger" @click="handleDelete(record)">
                    {{ $t('common.delete') }}
                  </a-button>
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
            show-total
            @change="loadData"
          />
        </div>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useI18n } from 'vue-i18n'
import { getSchedulePageApi, deleteScheduleApi, type HrSchedule } from '@/api/schedule'

const { t } = useI18n()
const loading = ref(false)
const tableData = ref<HrSchedule[]>([])
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
</script>
