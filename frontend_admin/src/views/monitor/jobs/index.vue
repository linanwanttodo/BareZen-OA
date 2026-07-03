<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :data="searchForm" :label-width="80" @submit-success="handleSearch">
        <t-form-item name="jobName" :label="$t('monitor.jobs.jobName')" :rules="[{ required: true, message: t('validation.required', { field: t('monitor.jobs.jobName') }) }]">
          <t-input v-model="searchForm.jobName" :placeholder="$t('common.inputPlaceholder')" allow-clear />
        </t-form-item>
        <t-form-item name="status" :label="$t('common.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.selectPlaceholder')" clearable>
            <t-option value="1" :label="$t('common.normal')" />
            <t-option value="0" :label="$t('common.disable')" />
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

    <t-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('monitor.jobs.title') }}</span>
          <t-space>
            <t-button v-permission="PERM.monitor.jobAdd" theme="primary" size="small" @click="handleAdd">
              <template #icon><icon-plus /></template>
              {{ $t('common.add') }}
            </t-button>
          </t-space>
        </div>
      </template>

      
          <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #cronExpression="{ row }">
                <span class="font-mono text-xs">{{ row.cronExpression }}</span>
              </template>
    <template #status="{ row }">
                <t-switch
                  :model-value="row.status === '1'"
                  @change="(val: string | number | boolean) => handleToggle(row, Boolean(val))"
                />
              </template>
    <template #col_5="{ row }">
                <t-space>
                  <t-link theme="primary" v-permission="PERM.monitor.jobEdit" @click="handleRun(row)">{{ $t('monitor.jobs.execute') }}</t-link>
                  <t-link theme="primary" v-permission="PERM.monitor.jobEdit" @click="handleEdit(row)">{{ $t('common.edit') }}</t-link>
                  <t-popconfirm v-permission="PERM.monitor.jobDelete" :content="$t('monitor.jobs.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
  </t-table>

        <div v-if="pagination.total > pagination.pageSize" class="mt-4 flex justify-end">
          <t-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      
    </t-card>

    <t-dialog v-model:visible="editVisible" :header="isAdd ? $t('monitor.jobs.addJob') : $t('monitor.jobs.editJob')" @confirm="handleSave" @close="editVisible = false">
      <t-form ref="editFormRef" :data="editForm" auto-label-width>
        <t-form-item name="jobName" :label="$t('monitor.jobs.jobName')">
          <t-input v-model="editForm.jobName" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item name="invokeTarget" :label="$t('monitor.jobs.invokeTarget')" :rules="[{ required: true, message: t('validation.required', { field: t('monitor.jobs.invokeTarget') }) }]">
          <t-input v-model="editForm.invokeTarget" placeholder="例: cleanTask.cleanOperLog()" />
        </t-form-item>
        <t-form-item name="cronExpression" :label="$t('monitor.jobs.cronExpression')" :rules="[{ required: true, message: t('validation.required', { field: t('monitor.jobs.cronExpression') }) }]">
          <t-input v-model="editForm.cronExpression" placeholder="例: 0 0 2 * * ?" />
        </t-form-item>
        <t-form-item name="status" :label="$t('common.status')">
          <t-radio-group v-model="editForm.status">
            <t-radio value="1">{{ $t('common.normal') }}</t-radio>
            <t-radio value="0">{{ $t('common.disable') }}</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item name="remark" :label="$t('common.remark')">
          <t-textarea v-model="editForm.remark" />
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
import { getJobPageApi, addJobApi, updateJobApi, deleteJobApi, resumeJobApi, pauseJobApi, runJobApi } from '@/api/job'
import type { SysJob } from '@/api/job'

const { t } = useI18n()
const loading = ref(false)
const tableData = ref<SysJob[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const searchForm = reactive<{ jobName: string; status: string | undefined }>({ jobName: '', status: undefined })
const editVisible = ref(false)
const isAdd = ref(false)
const editFormRef = ref<FormInstanceFunctions>()
const editForm = reactive({ jobName: '', invokeTarget: '', cronExpression: '', status: '1' as string, remark: '' })
let editingId: string | null = null

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getJobPageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      jobName: searchForm.jobName || undefined,
      status: searchForm.status || undefined,
    })
    tableData.value = res.data?.list || []
    pagination.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  searchForm.jobName = ''
  searchForm.status = undefined
  pagination.current = 1
  loadData()
}

function handleAdd() {
  isAdd.value = true; editingId = null
  editForm.jobName = ''; editForm.invokeTarget = ''; editForm.cronExpression = ''; editForm.status = '1'; editForm.remark = ''
  editVisible.value = true
}

function handleEdit(row: SysJob) {
  isAdd.value = false; editingId = row.id
  editForm.jobName = row.jobName; editForm.invokeTarget = row.invokeTarget
  editForm.cronExpression = row.cronExpression; editForm.status = row.status; editForm.remark = row.remark
  editVisible.value = true
}

async function handleSave() {
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await editFormRef.value?.validate()) !== true) return
  if (isAdd.value) {
    await addJobApi(editForm)
    MessagePlugin.success(t('monitor.jobs.addSuccess'))
  } else {
    await updateJobApi({ id: editingId!, ...editForm })
    MessagePlugin.success(t('monitor.jobs.updateSuccess'))
  }
  editVisible.value = false; loadData()
}

async function handleToggle(row: SysJob, val: boolean) {
  if (val) {
    await resumeJobApi(row.id)
  } else {
    await pauseJobApi(row.id)
  }
  MessagePlugin.success(val ? t('monitor.jobs.resumeSuccess') : t('monitor.jobs.pauseSuccess')); loadData()
}

async function handleRun(row: SysJob) {
  await runJobApi(row.id)
  MessagePlugin.success(t('monitor.jobs.runSuccess'))
}

async function handleDelete(row: SysJob) {
  await deleteJobApi(row.id)
  MessagePlugin.success(t('monitor.jobs.deleteSuccess')); loadData()
}

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'jobName', title: t('monitor.jobs.jobName'), width: 140 },
    { colKey: 'jobGroup', title: t('monitor.jobs.jobGroup'), width: 100 },
    { colKey: 'invokeTarget', title: t('monitor.jobs.invokeTarget') },
    { colKey: 'cronExpression', title: t('monitor.jobs.cronExpression'), width: 160 },
    { colKey: 'status', title: t('common.status'), width: 80 },
    { colKey: 'col_5', title: t('common.operate'), width: 260, align: 'right' },
  ])

</script>
