<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :data="searchForm" :label-width="80" :on-submit="handleSearch">
        <t-form-item name="month" :label="$t('attendanceStat.month')">
          <t-select v-model="searchForm.month" @change="handleSearch">
            <t-option v-for="m in monthOptions" :key="m.value" :value="m.value">{{ m.label }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item name="deptId" :label="$t('attendanceStat.dept')">
          
<!-- TODO(tree-select): data 字段 key/title 需转为 value/label --><t-tree-select
            v-model="searchForm.deptId"
            :data="deptTree"
            :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
            :placeholder="$t('common.all')"
            allow-clear
            style="width: 200px"
          />
        </t-form-item>
        <t-form-item name="userName" :label="$t('attendanceStat.userName')">
          <t-input
            v-model="searchForm.userName"
            :placeholder="$t('attendanceStat.userNamePlaceholder')"
            allow-clear
           
            @press-enter="handleSearch"
          />
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" type="submit">{{ $t('common.search') }}</t-button>
            <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-card>

    <!-- 统计表格 -->
    <t-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('attendanceStat.title') }}</span>
        </div>
      </template>

      
          <t-table :loading="loading" row-key="userId" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #deptName="{ row }">{{ row.deptName || '-' }}</template>
    <template #lateCount="{ row }">
                <span :class="row.lateCount > 0 ? 'text-[var(--wecom-warning)]' : ''">{{ row.lateCount }}</span>
              </template>
    <template #earlyCount="{ row }">
                <span :class="row.earlyCount > 0 ? 'text-[var(--wecom-warning)]' : ''">{{ row.earlyCount }}</span>
              </template>
    <template #absentCount="{ row }">
                <span :class="row.absentCount > 0 ? 'text-[var(--wecom-error)]' : ''">{{ row.absentCount }}</span>
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
        <t-empty v-if="!loading && !tableData.length" :description="$t('common.noData')" class="mt-4" />
      
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getClockStatPageApi, type AttClockMonthlyStat } from '@/api/attendance'
import { getDeptTreeApi, type SysDept } from '@/api/dept'

const { t } = useI18n()

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

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'userName', title: t('attendanceStat.userName'), width: 140 },
    { colKey: 'deptName', title: t('attendanceStat.dept'), width: 160 },
    { colKey: 'workDays', title: t('attendanceStat.workDays'), width: 100, align: 'center' },
    { colKey: 'lateCount', title: t('attendanceStat.lateCount'), width: 100, align: 'center' },
    { colKey: 'earlyCount', title: t('attendanceStat.earlyCount'), width: 100, align: 'center' },
    { colKey: 'absentCount', title: t('attendanceStat.absentCount'), width: 100, align: 'center' },
  ])

</script>