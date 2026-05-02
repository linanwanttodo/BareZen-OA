<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-[var(--foreground)]">{{ $t('schedule.title') }}</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('schedule.subtitle') }}</p>
      </div>
      <a-button type="primary" @click="openAdd">
        <template #icon><icon-plus /></template>
        {{ $t('schedule.add') }}
      </a-button>
    </div>

    <!-- 状态筛选 -->
    <div class="mb-6">
      <a-radio-group v-model="statusFilter" type="button" @change="loadSchedules">
        <a-radio :value="''">{{ $t('schedule.all') }}</a-radio>
        <a-radio value="0">{{ $t('schedule.statusTodo') }}</a-radio>
        <a-radio value="1">{{ $t('schedule.statusOngoing') }}</a-radio>
        <a-radio value="2">{{ $t('schedule.statusDone') }}</a-radio>
      </a-radio-group>
    </div>

    <!-- 日程列表 -->
    <a-spin :loading="loading">
      <div v-if="schedules.length" class="space-y-3">
        <div
          v-for="item in schedules"
          :key="item.id"
          class="flex items-start gap-4 rounded-lg border border-[var(--border)] bg-[var(--card)] p-5"
          :class="{ 'opacity-60': item.status === '2' || item.status === '3' }"
        >
          <div class="mt-0.5 w-40 shrink-0 text-sm">
            <div class="font-medium text-[var(--foreground)]">{{ item.startTime }}</div>
            <div class="text-[var(--muted-foreground)]">~ {{ item.endTime }}</div>
          </div>
          <div class="min-w-0 flex-1">
            <div class="mb-1 flex items-center gap-2">
              <h3 class="font-semibold text-[var(--foreground)]" :class="{ 'line-through': item.status === '2' }">{{ item.title }}</h3>
              <a-tag :color="statusColor(item.status)" size="small">{{ statusLabel(item.status) }}</a-tag>
              <a-tag v-if="item.location" size="small">{{ item.location }}</a-tag>
            </div>
            <p v-if="item.description" class="text-sm text-[var(--muted-foreground)]">{{ item.description }}</p>
            <p v-if="item.remindTime" class="mt-1 text-xs text-[var(--warning)]">{{ $t('schedule.remind') }}：{{ item.remindTime }}</p>
          </div>
          <div class="flex shrink-0 items-center gap-2">
            <a-button v-if="item.status === '0'" size="small" type="primary" @click="markDone(item)">
              {{ $t('schedule.done') }}
            </a-button>
            <a-button size="small" @click="openEdit(item)">{{ $t('schedule.edit') }}</a-button>
            <a-popconfirm :content="$t('schedule.deleteConfirm')" @ok="handleDelete(item)">
              <a-button size="small" status="danger">{{ $t('schedule.delete') }}</a-button>
            </a-popconfirm>
          </div>
        </div>
      </div>
      <a-empty v-else :description="$t('schedule.noData')" />
    </a-spin>

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:visible="dialogVisible" :title="isEdit ? $t('schedule.edit') : $t('schedule.add')"
             :width="560" @ok="handleSave" @cancel="dialogVisible = false" :ok-loading="saving">
      <a-form :model="form" layout="vertical">
        <a-form-item :label="$t('schedule.titleLabel')" required>
          <a-input v-model="form.title" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('schedule.timeRange')" required>
          <a-date-picker v-model="form.startTime" show-time style="width: 230px" :placeholder="$t('schedule.startTime')" />
          <span class="mx-2">~</span>
          <a-date-picker v-model="form.endTime" show-time style="width: 230px" :placeholder="$t('schedule.endTime')" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('schedule.remind')">
              <a-date-picker v-model="form.remindTime" show-time style="width: 100%" :placeholder="$t('schedule.remindPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('schedule.locationLabel')">
              <a-input v-model="form.location" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('schedule.descLabel')">
          <a-textarea v-model="form.description" :placeholder="$t('common.inputPlaceholder')" :max-length="1000" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getMySchedulesApi, addScheduleApi, updateScheduleApi, deleteScheduleApi, type Schedule } from '@/api/enterprise'

const loading = ref(false)
const saving = ref(false)
const schedules = ref<Schedule[]>([])
const statusFilter = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)

const form = reactive<Partial<Schedule>>({})

function statusLabel(status: string | undefined): string {
  const map: Record<string, string> = { 0: '待开始', 1: '进行中', 2: '已完成', 3: '已取消' }
  return status != null ? map[status] || '-' : '-'
}
function statusColor(status: string | undefined): string {
  if (status === '0') return 'orange'
  if (status === '1') return 'blue'
  if (status === '2') return 'green'
  return 'default'
}

async function loadSchedules() {
  loading.value = true
  try {
    const res = await getMySchedulesApi({ page: 1, pageSize: 50, status: statusFilter.value || undefined })
    schedules.value = res.data.list || []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  isEdit.value = false
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  dialogVisible.value = true
}

function openEdit(item: Schedule) {
  isEdit.value = true
  Object.assign(form, item)
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.title) {
    Message.warning('请填写日程标题')
    return
  }
  if (!form.startTime || !form.endTime) {
    Message.warning('请选择日程时间')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateScheduleApi(form)
    } else {
      await addScheduleApi(form)
    }
    Message.success('保存成功')
    dialogVisible.value = false
    loadSchedules()
  } finally {
    saving.value = false
  }
}

async function markDone(item: Schedule) {
  await updateScheduleApi({ id: item.id, status: '2' })
  Message.success('已标记完成')
  loadSchedules()
}

async function handleDelete(item: Schedule) {
  await deleteScheduleApi(item.id)
  Message.success('删除成功')
  loadSchedules()
}

onMounted(loadSchedules)
</script>
