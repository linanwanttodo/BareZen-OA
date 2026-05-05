<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="month" :label="$t('attendanceStat.month')">
          <a-select v-model="searchForm.month" style="width: 140px" @change="handleSearch">
            <a-option v-for="m in monthOptions" :key="m.value" :value="m.value">{{ m.label }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="deptId" :label="$t('attendanceStat.dept')">
          <a-tree-select
            v-model="searchForm.deptId"
            :data="deptTree"
            :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
            :placeholder="$t('common.all')"
            allow-clear
            style="width: 200px"
          />
        </a-form-item>
        <a-form-item field="userName" :label="$t('attendanceStat.userName')">
          <a-input
            v-model="searchForm.userName"
            :placeholder="$t('attendanceStat.userNamePlaceholder')"
            allow-clear
            style="width: 180px"
            @press-enter="handleSearch"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">{{ $t('common.search') }}</a-button>
            <a-button @click="handleReset">{{ $t('common.reset') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 统计表格 -->
    <a-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('attendanceStat.title') }}</span>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="userId">
          <template #columns>
            <a-table-column :title="$t('attendanceStat.userName')" data-index="userName" :width="140" />
            <a-table-column :title="$t('attendanceStat.dept')" data-index="deptName" :width="160">
              <template #cell="{ record }">{{ record.deptName || '-' }}</template>
            </a-table-column>
            <a-table-column :title="$t('attendanceStat.workDays')" data-index="workDays" :width="100" align="center" />
            <a-table-column :title="$t('attendanceStat.lateCount')" data-index="lateCount" :width="100" align="center">
              <template #cell="{ record }">
                <span :class="record.lateCount > 0 ? 'text-[var(--color-warning-6)]' : ''">{{ record.lateCount }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('attendanceStat.earlyCount')" data-index="earlyCount" :width="100" align="center">
              <template #cell="{ record }">
                <span :class="record.earlyCount > 0 ? 'text-[var(--color-warning-6)]' : ''">{{ record.earlyCount }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('attendanceStat.absentCount')" data-index="absentCount" :width="100" align="center">
              <template #cell="{ record }">
                <span :class="record.absentCount > 0 ? 'text-[var(--color-danger-6)]' : ''">{{ record.absentCount }}</span>
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
        <a-empty v-if="!loading && !tableData.length" :description="$t('common.noData')" class="mt-4" />
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getClockStatPageApi, type AttClockMonthlyStat } from '@/api/attendance'
import { getDeptTreeApi, type SysDept } from '@/api/dept'

const loading = ref(false)
const tableData = ref<AttClockMonthlyStat[]>([])
const deptTree = ref<SysDept[]>([])

/** 近 12 个月选项（yyyy-MM），默认当月 */
const monthOptions = computed(() => {
  const now = new Date()
  const options: { value: string; label: string }[] = []
  for (let i = 0; i < 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    options.push({ value, label: `${d.getFullYear()}年${d.getMonth() + 1}月` })
  }
  return options
})

const searchForm = reactive({ month: monthOptions.value[0]?.value || '', deptId: undefined as number | undefined, userName: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

async function loadData() {
  loading.value = true
  try {
    const res = await getClockStatPageApi({
      month: searchForm.month,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      userName: searchForm.userName || undefined,
      deptId: searchForm.deptId,
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
  searchForm.deptId = undefined
  searchForm.userName = ''
  loadData()
}

async function loadDeptTree() {
  try {
    const res = await getDeptTreeApi()
    deptTree.value = res.data || []
  } catch (err) {
    console.error('load dept tree error:', err)
  }
}

onMounted(() => {
  loadDeptTree()
  loadData()
})
</script>