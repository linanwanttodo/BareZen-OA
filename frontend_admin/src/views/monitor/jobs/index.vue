<template>
  <div class="page-container">
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

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('monitor.jobs.jobName')" data-index="jobName" :width="140" />
            <a-table-column :title="$t('monitor.jobs.jobGroup')" data-index="jobGroup" :width="100" />
            <a-table-column :title="$t('monitor.jobs.invokeTarget')" data-index="invokeTarget" />
            <a-table-column :title="$t('monitor.jobs.cronExpression')" :width="160">
              <template #cell="{ record }">
                <span class="font-mono text-xs">{{ record.cronExpression }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.status')" :width="80">
              <template #cell="{ record }">
                <t-switch
                  :model-value="record.status === '1'"
                  @change="(val: string | number | boolean) => handleToggle(record, Boolean(val))"
                />
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="260" align="right">
              <template #cell="{ record }">
                <t-space>
                  <t-button v-permission="PERM.monitor.jobEdit" size="small" @click="handleRun(record)">{{ $t('monitor.jobs.execute') }}</t-button>
                  <t-button v-permission="PERM.monitor.jobEdit" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</t-button>
                  <t-popconfirm v-permission="PERM.monitor.jobDelete" :content="$t('monitor.jobs.deleteConfirm')" @confirm="handleDelete(record)">
                    <t-button size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
            </a-table-column>
          </template>
        </a-table>

        <div v-if="pagination.total > pagination.pageSize" class="mt-4 flex justify-end">
          <t-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      </a-spin>
    </t-card>

    <t-dialog v-model="editVisible" :header="isAdd ? $t('monitor.jobs.addJob') : $t('monitor.jobs.editJob')" @confirm="handleSave" @close="editVisible = false">
      <t-form :model="editForm" auto-label-width>
        <t-form-item name="jobName" :label="$t('monitor.jobs.jobName')">
          <t-input v-model="editForm.jobName" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item name="invokeTarget" :label="$t('monitor.jobs.invokeTarget')">
          <t-input v-model="editForm.invokeTarget" placeholder="例: cleanTask.cleanOperLog()" />
        </t-form-item>
        <t-form-item name="cronExpression" :label="$t('monitor.jobs.cronExpression')">
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
import { ref, reactive, onMounted } from 'vue'
import {  MessagePlugin  } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import { getJobPageApi, addJobApi, updateJobApi, deleteJobApi, resumeJobApi, pauseJobApi, runJobApi } from '@/api/job'
import type { SysJob } from '@/api/job'

const { t } = useI18n()
const loading = ref(false)
const tableData = ref<SysJob[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const editVisible = ref(false)
const isAdd = ref(false)
const editForm = reactive({ jobName: '', invokeTarget: '', cronExpression: '', status: '1' as string, remark: '' })
let editingId: string | null = null

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getJobPageApi({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = res.data?.list || []
    pagination.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
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
</script>
