<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-[var(--foreground)]">{{ $t('worklog.title') }}</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('worklog.subtitle') }}</p>
      </div>
    </div>

    <!-- 提交日志表单 -->
    <a-card class="mb-6" :title="$t('worklog.submit')">
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
        <a-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ $t('worklog.submitBtn') }}
        </a-button>
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
          <div v-for="item in logs" :key="item.id" class="rounded-lg border border-[var(--border)] bg-[var(--card)] p-5">
            <div class="mb-2 flex items-center gap-2">
              <a-tag :color="item.logType === 'weekly' ? 'purple' : 'blue'" size="small">
                {{ item.logType === 'weekly' ? $t('worklog.typeWeekly') : $t('worklog.typeDaily') }}
              </a-tag>
              <span class="text-sm text-[var(--muted-foreground)]">{{ item.logDate || '-' }}</span>
              <span v-if="item.reviewerName" class="text-xs text-[var(--success)]">{{ $t('worklog.reviewed') }}</span>
            </div>
            <p class="whitespace-pre-wrap text-sm leading-relaxed text-[var(--foreground)]">{{ item.content }}</p>
            <p v-if="item.reviewRemark" class="mt-2 text-xs text-[var(--muted-foreground)]">
              {{ $t('worklog.reviewRemark') }}：{{ item.reviewRemark }}
            </p>
          </div>
        </div>
        <a-empty v-else :description="$t('worklog.noData')" />
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getMyWorkLogsApi, submitWorkLogApi, type WorkLog } from '@/api/enterprise'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const logs = ref<WorkLog[]>([])
const logTypeFilter = ref('')

const form = reactive<Partial<WorkLog>>({
  logType: 'daily',
  userName: userStore.userInfo?.nickName || '',
})

async function loadLogs() {
  loading.value = true
  try {
    const res = await getMyWorkLogsApi({ page: 1, pageSize: 20, logType: logTypeFilter.value || undefined })
    logs.value = res.data.list || []
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
    loadLogs()
  } finally {
    submitting.value = false
  }
}

onMounted(loadLogs)
</script>
