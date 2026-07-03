<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('schedule.title') }}</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('schedule.subtitle') }}</p>
      </div>
      <t-button theme="primary" @click="openAdd">
        <template #icon><icon-plus /></template>
        {{ $t('schedule.add') }}
      </t-button>
    </div>

    <!-- 状态筛选 -->
    <div class="mb-6">
      <t-radio-group v-model="statusFilter" variant="default-filled" @change="loadSchedules">
        <t-radio :value="''">{{ $t('schedule.all') }}</t-radio>
        <t-radio value="0">{{ $t('schedule.statusTodo') }}</t-radio>
        <t-radio value="1">{{ $t('schedule.statusOngoing') }}</t-radio>
        <t-radio value="2">{{ $t('schedule.statusDone') }}</t-radio>
      </t-radio-group>
    </div>

    <!-- 日程列表 -->
    <t-loading :loading="loading">
      <div v-if="schedules.length" class="space-y-3">
        <div
          v-for="item in schedules"
          :key="item.id"
          class="flex items-start gap-4 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5"
          :class="{ 'opacity-60': item.status === '2' || item.status === '3' }"
        >
          <div class="mt-0.5 w-40 shrink-0 text-sm">
            <div class="font-medium text-[var(--foreground)]">{{ item.startTime }}</div>
            <div class="text-[var(--muted-foreground)]">~ {{ item.endTime }}</div>
          </div>
          <div class="min-w-0 flex-1">
            <div class="mb-1 flex items-center gap-2">
              <h3 class="font-semibold text-[var(--foreground)]" :class="{ 'line-through': item.status === '2' }">{{ item.title }}</h3>
              <t-tag :color="statusColor(item.status)" size="small">{{ statusLabel(item.status) }}</t-tag>
              <t-tag v-if="item.location" size="small">{{ $t('schedule.locationLabel') }}：{{ item.location }}</t-tag>
            </div>
            <p v-if="item.description" class="text-sm text-[var(--muted-foreground)]">{{ item.description }}</p>
            <p v-if="item.remindTime" class="mt-1 text-xs text-[var(--bz-warning)]">{{ $t('schedule.remind') }}：{{ item.remindTime }}</p>
          </div>
          <div class="flex shrink-0 items-center gap-2">
            <t-button v-if="item.status === '0'" size="small" theme="primary" @click="markDone(item)">
              {{ $t('schedule.done') }}
            </t-button>
            <t-button size="small" @click="openEdit(item)">{{ $t('schedule.edit') }}</t-button>
            <t-popconfirm :content="$t('schedule.deleteConfirm')" @confirm="handleDelete(item)">
              <t-button size="small" theme="danger">{{ $t('schedule.delete') }}</t-button>
            </t-popconfirm>
          </div>
        </div>
      </div>
      <t-empty v-else :description="$t('schedule.noData')" />
    </t-loading>

    <!-- 新增/编辑弹窗 -->
    <t-dialog v-model:visible="dialogVisible" :header="isEdit ? $t('schedule.edit') : $t('schedule.add')"
             :width="560" @confirm="handleSave" @cancel="dialogVisible = false" :ok-loading="saving">
      <t-form ref="formRef" :data="form" layout="vertical">
        <t-form-item :label="$t('schedule.titleLabel')" required name="title" :rules="[{ required: true, message: $t('validation.required', { field: $t('schedule.titleLabel') }) }]">
          <t-input v-model="form.title" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item :label="$t('schedule.timeRange')" required name="startTime" :rules="[{ required: true, message: $t('validation.required', { field: $t('schedule.timeRange') }) }]">
          
<t-date-picker v-model="form.startTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('schedule.startTime')" />
          <span class="mx-2">~</span>
          
<t-date-picker v-model="form.endTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('schedule.endTime')" />
        </t-form-item>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item :label="$t('schedule.remind')">
              
<t-date-picker v-model="form.remindTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('schedule.remindPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item :label="$t('schedule.locationLabel')">
              <t-input v-model="form.location" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item :label="$t('schedule.descLabel')">
          <t-textarea v-model="form.description" :placeholder="$t('common.inputPlaceholder')" :max-length="1000" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { getMySchedulesApi, addScheduleApi, updateScheduleApi, deleteScheduleApi, type Schedule } from '@/api/enterprise'

const loading = ref(false)
const saving = ref(false)
const schedules = ref<Schedule[]>([])
const statusFilter = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)

const formRef = ref<FormInstanceFunctions>()

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
  // 校验不通过则中止提交，字段错误就地显示
  if (await formRef.value?.validate() !== true) return
  if (!form.title) {
    MessagePlugin.warning('请填写日程标题')
    return
  }
  if (!form.startTime || !form.endTime) {
    MessagePlugin.warning('请选择日程时间')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateScheduleApi(form)
    } else {
      await addScheduleApi(form)
    }
    MessagePlugin.success('保存成功')
    dialogVisible.value = false
    loadSchedules()
  } finally {
    saving.value = false
  }
}

async function markDone(item: Schedule) {
  await updateScheduleApi({ id: item.id, status: '2' })
  MessagePlugin.success('已标记完成')
  loadSchedules()
}

async function handleDelete(item: Schedule) {
  await deleteScheduleApi(item.id)
  MessagePlugin.success('删除成功')
  loadSchedules()
}

onMounted(loadSchedules)
</script>
