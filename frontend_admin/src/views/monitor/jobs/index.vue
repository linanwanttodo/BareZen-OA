<template>
  <div class="page-container">
    <a-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('monitor.jobs.title') }}</span>
          <a-space>
            <a-button v-permission="'monitor:job:add'" type="primary" size="small" @click="handleAdd">
              <template #icon><icon-plus /></template>
              {{ $t('common.add') }}
            </a-button>
          </a-space>
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
                <a-switch
                  :model-value="record.status === '1'"
                  @change="(val: string | number | boolean) => handleToggle(record, Boolean(val))"
                />
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="260" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-permission="'monitor:job:edit'" size="small" @click="handleRun(record)">{{ $t('monitor.jobs.execute') }}</a-button>
                  <a-button v-permission="'monitor:job:edit'" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</a-button>
                  <a-popconfirm v-permission="'monitor:job:delete'" :content="$t('monitor.jobs.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button size="small" status="danger">{{ $t('common.delete') }}</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>

        <div v-if="pagination.total > pagination.pageSize" class="mt-4 flex justify-end">
          <a-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      </a-spin>
    </a-card>

    <a-modal v-model:visible="editVisible" :title="isAdd ? $t('monitor.jobs.addJob') : $t('monitor.jobs.editJob')" @ok="handleSave" @cancel="editVisible = false">
      <a-form :model="editForm" auto-label-width>
        <a-form-item field="jobName" :label="$t('monitor.jobs.jobName')">
          <a-input v-model="editForm.jobName" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-form-item field="invokeTarget" :label="$t('monitor.jobs.invokeTarget')">
          <a-input v-model="editForm.invokeTarget" placeholder="例: cleanTask.cleanOperLog()" />
        </a-form-item>
        <a-form-item field="cronExpression" :label="$t('monitor.jobs.cronExpression')">
          <a-input v-model="editForm.cronExpression" placeholder="例: 0 0 2 * * ?" />
        </a-form-item>
        <a-form-item field="status" :label="$t('common.status')">
          <a-radio-group v-model="editForm.status">
            <a-radio value="1">{{ $t('common.normal') }}</a-radio>
            <a-radio value="0">{{ $t('common.disable') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="remark" :label="$t('common.remark')">
          <a-textarea v-model="editForm.remark" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
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
    Message.success(t('monitor.jobs.addSuccess'))
  } else {
    await updateJobApi({ id: editingId!, ...editForm })
    Message.success(t('monitor.jobs.updateSuccess'))
  }
  editVisible.value = false; loadData()
}

async function handleToggle(row: SysJob, val: boolean) {
  if (val) {
    await resumeJobApi(row.id)
  } else {
    await pauseJobApi(row.id)
  }
  Message.success(val ? t('monitor.jobs.resumeSuccess') : t('monitor.jobs.pauseSuccess')); loadData()
}

async function handleRun(row: SysJob) {
  await runJobApi(row.id)
  Message.success(t('monitor.jobs.runSuccess'))
}

async function handleDelete(row: SysJob) {
  await deleteJobApi(row.id)
  Message.success(t('monitor.jobs.deleteSuccess')); loadData()
}
</script>
