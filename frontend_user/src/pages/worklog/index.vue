<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('worklog.title') }}</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('worklog.subtitle') }}</p>
      </div>
    </div>

    <!-- 提交日志表单 -->
    <a-card class="mb-6" :title="formTitle">
      <a-form :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item :label="$t('worklog.logType')" required>
              <a-select v-model="form.logType">
                <a-option value="daily">{{ $t('worklog.typeDaily') }}</a-option>
                <a-option value="weekly">{{ $t('worklog.typeWeekly') }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="$t('worklog.logDate')">
              <a-date-picker v-model="form.logDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="$t('worklog.author')">
              <a-input :model-value="userStore.userInfo?.nickName || '-'" disabled />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('worklog.content')" required>
          <a-textarea v-model="form.content" :placeholder="$t('worklog.contentPlaceholder')" :auto-size="{ minRows: 6, maxRows: 14 }" />
        </a-form-item>
        <a-space>
          <a-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ $t('worklog.submitBtn') }}
          </a-button>
          <a-button v-if="editingId" @click="cancelEdit">
            {{ $t('common.cancel') }}
          </a-button>
        </a-space>
      </a-form>
    </a-card>

    <!-- 我的日志列表 -->
    <a-card :title="$t('worklog.myLogs')">
      <a-tabs v-model:active-key="logTypeFilter" @change="loadLogs">
        <a-tab-pane key="" :title="$t('worklog.all')" />
        <a-tab-pane key="daily" :title="$t('worklog.typeDaily')" />
        <a-tab-pane key="weekly" :title="$t('worklog.typeWeekly')" />
      </a-tabs>
      <a-spin :loading="loading">
        <div v-if="logs.length" class="space-y-3">
          <div v-for="item in logs" :key="item.id" class="rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5">
            <div class="mb-2 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <a-tag :color="item.logType === 'weekly' ? 'arcoblue' : 'gray'" size="small">
                  {{ item.logType === 'weekly' ? $t('worklog.typeWeekly') : $t('worklog.typeDaily') }}
                </a-tag>
                <span class="text-sm text-[var(--muted-foreground)]">{{ item.logDate || '-' }}</span>
                <span v-if="item.reviewerName" class="text-xs text-[var(--bz-success)]">{{ $t('worklog.reviewed') }}</span>
              </div>
              <div class="flex items-center gap-2">
                <a-button size="mini" @click="openEdit(item)">{{ $t('common.edit') }}</a-button>
                <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDelete(item)">
                  <a-button size="mini" status="danger">{{ $t('common.delete') }}</a-button>
                </a-popconfirm>
              </div>
            </div>
            <p class="whitespace-pre-wrap text-sm leading-relaxed text-[var(--foreground)]">{{ item.content }}</p>
            <p v-if="item.reviewRemark" class="mt-2 text-xs text-[var(--muted-foreground)]">
              {{ $t('worklog.reviewRemark') }}：{{ item.reviewRemark }}
            </p>
          </div>
        </div>
        <a-empty v-else :description="$t('worklog.noData')" />
      </a-spin>

      <!-- 分页 -->
      <div v-if="pagination.total > 0" class="mt-4 flex justify-center">
        <a-pagination
          v-model:current="pagination.current"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          @change="loadLogs"
        />
      </div>
    </a-card>

    <!-- 编辑弹窗 -->
    <a-modal v-model:visible="editVisible" :title="$t('worklog.editTitle')" @ok="handleEditSubmit" @cancel="editVisible = false">
      <a-form :model="editForm" layout="vertical">
        <a-form-item :label="$t('worklog.content')" required>
          <a-textarea v-model="editForm.content" :auto-size="{ minRows: 6, maxRows: 14 }" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getMyWorkLogsApi, submitWorkLogApi, updateWorkLogApi, deleteWorkLogApi, type WorkLog } from '@/api/enterprise'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const logs = ref<WorkLog[]>([])
const logTypeFilter = ref('')
const editingId = ref<string | null>(null)
const editVisible = ref(false)
const editForm = reactive({ content: '' })

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const form = reactive<Partial<WorkLog>>({
  logType: 'daily',
  userName: userStore.userInfo?.nickName || '',
})

const formTitle = computed(() => editingId.value ? '编辑日志' : '提交日志')

async function loadLogs() {
  loading.value = true
  try {
    const res = await getMyWorkLogsApi({ page: pagination.current, pageSize: pagination.pageSize, logType: logTypeFilter.value || undefined })
    logs.value = res.data.list || []
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!form.content || !form.content.trim()) {
    Message.warning('请填写日志内容')
    return
  }
  submitting.value = true
  try {
    await submitWorkLogApi({
      userName: form.userName,
      logType: form.logType,
      logDate: form.logDate,
      content: form.content,
    })
    Message.success('提交成功')
    form.content = ''
    form.logDate = ''
    editingId.value = null
    loadLogs()
  } finally {
    submitting.value = false
  }
}

function openEdit(item: WorkLog) {
  editingId.value = item.id!
  editForm.content = item.content || ''
  editVisible.value = true
}

function cancelEdit() {
  editingId.value = null
  form.content = ''
  form.logDate = ''
  form.logType = 'daily'
}

async function handleEditSubmit() {
  if (!editForm.content || !editForm.content.trim()) {
    Message.warning('请填写日志内容')
    return
  }
  try {
    await updateWorkLogApi({ id: editingId.value!, content: editForm.content })
    Message.success('更新成功')
    editVisible.value = false
    editingId.value = null
    loadLogs()
  } catch {
    Message.error('更新失败')
  }
}

async function handleDelete(item: WorkLog) {
  try {
    await deleteWorkLogApi(item.id!)
    Message.success('删除成功')
    loadLogs()
  } catch {
    Message.error('删除失败')
  }
}

onMounted(loadLogs)
</script>
