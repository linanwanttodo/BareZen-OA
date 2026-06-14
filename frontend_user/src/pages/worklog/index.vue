<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('worklog.title') }}</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('worklog.subtitle') }}</p>
      </div>
    </div>

    <!-- 提交日志表单 -->
    <t-card class="mb-6" :title="formTitle">
      <t-form :model="form" layout="vertical">
        <t-row :gutter="16">
          <t-col :span="8">
            <t-form-item :label="$t('worklog.logType')" required>
              <t-select v-model="form.logType">
                <t-option value="daily">{{ $t('worklog.typeDaily') }}</t-option>
                <t-option value="weekly">{{ $t('worklog.typeWeekly') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="8">
            <t-form-item :label="$t('worklog.logDate')">
              
<t-date-picker v-model="form.logDate" value-type="YYYY-MM-DD" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="8">
            <t-form-item :label="$t('worklog.author')">
              <t-input :model-value="userStore.userInfo?.nickName || '-'" disabled />
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item :label="$t('worklog.content')" required>
          <t-textarea v-model="form.content" :placeholder="$t('worklog.contentPlaceholder')" :auto-size="{ minRows: 6, maxRows: 14 }" />
        </t-form-item>
        <t-space>
          <t-button theme="primary" :loading="submitting" @click="handleSubmit">
            {{ $t('worklog.submitBtn') }}
          </t-button>
          <t-button v-if="editingId" @click="cancelEdit">
            {{ $t('common.cancel') }}
          </t-button>
        </t-space>
      </t-form>
    </t-card>

    <!-- 我的日志列表 -->
    <t-card :title="$t('worklog.myLogs')">
      <t-tabs v-model:active-key="logTypeFilter" @change="loadLogs">
        <t-tab-panel key="" :title="$t('worklog.all')" />
        <t-tab-panel key="daily" :title="$t('worklog.typeDaily')" />
        <t-tab-panel key="weekly" :title="$t('worklog.typeWeekly')" />
      </t-tabs>
      <t-loading :loading="loading">
        <div v-if="logs.length" class="space-y-3">
          <div v-for="item in logs" :key="item.id" class="rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5">
            <div class="mb-2 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <t-tag :theme="item.logType === 'weekly' ? 'primary' : 'default'" size="small">
                  {{ item.logType === 'weekly' ? $t('worklog.typeWeekly') : $t('worklog.typeDaily') }}
                </t-tag>
                <span class="text-sm text-[var(--muted-foreground)]">{{ item.logDate || '-' }}</span>
                <span v-if="item.reviewerName" class="text-xs text-[var(--bz-success)]">{{ $t('worklog.reviewed') }}</span>
              </div>
              <div class="flex items-center gap-2">
                <t-button size="mini" @click="openEdit(item)">{{ $t('common.edit') }}</t-button>
                <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(item)">
                  <t-button size="mini" theme="danger">{{ $t('common.delete') }}</t-button>
                </t-popconfirm>
              </div>
            </div>
            <p class="whitespace-pre-wrap text-sm leading-relaxed text-[var(--foreground)]">{{ item.content }}</p>
            <p v-if="item.reviewRemark" class="mt-2 text-xs text-[var(--muted-foreground)]">
              {{ $t('worklog.reviewRemark') }}：{{ item.reviewRemark }}
            </p>
          </div>
        </div>
        <t-empty v-else :description="$t('worklog.noData')" />
      </t-loading>

      <!-- 分页 -->
      <div v-if="pagination.total > 0" class="mt-4 flex justify-center">
        <t-pagination
          v-model:current="pagination.current"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          @change="loadLogs"
        />
      </div>
    </t-card>

    <!-- 编辑弹窗 -->
    <t-dialog v-model="editVisible" :title="$t('worklog.editTitle')" @confirm="handleEditSubmit" @cancel="editVisible = false">
      <t-form :model="editForm" layout="vertical">
        <t-form-item :label="$t('worklog.content')" required>
          <t-textarea v-model="editForm.content" :auto-size="{ minRows: 6, maxRows: 14 }" />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
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
    MessagePlugin.warning('请填写日志内容')
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
    MessagePlugin.success('提交成功')
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
    MessagePlugin.warning('请填写日志内容')
    return
  }
  try {
    await updateWorkLogApi({ id: editingId.value!, content: editForm.content })
    MessagePlugin.success('更新成功')
    editVisible.value = false
    editingId.value = null
    loadLogs()
  } catch {
    MessagePlugin.error('更新失败')
  }
}

async function handleDelete(item: WorkLog) {
  try {
    await deleteWorkLogApi(item.id!)
    MessagePlugin.success('删除成功')
    loadLogs()
  } catch {
    MessagePlugin.error('删除失败')
  }
}

onMounted(loadLogs)
</script>
