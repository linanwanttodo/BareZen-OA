<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('todo.title') }}</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('todo.subtitle') }}</p>
      </div>
      <t-button theme="primary" @click="openAddDialog">
        <template #icon><icon-plus /></template>
        {{ $t('todo.add') }}
      </t-button>
    </div>

    <!-- 状态筛选 -->
    <div class="mb-6">
      <t-radio-group v-model="statusFilter" variant="default-filled" @change="onStatusFilter">
        <t-radio :value="''">{{ $t('todo.all') }}</t-radio>
        <t-radio :value="0">{{ $t('todo.statusTodo') }}</t-radio>
        <t-radio :value="1">{{ $t('todo.statusInProgress') }}</t-radio>
        <t-radio :value="2">{{ $t('todo.statusDone') }}</t-radio>
      </t-radio-group>
    </div>

    <!-- 待办列表 -->
    <t-loading :loading="loading">
      <div v-if="todos.length" class="space-y-3">
        <div
          v-for="todo in todos"
          :key="todo.id"
          class="flex items-start gap-4 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5"
          :class="{ 'opacity-60': todo.status === 2 || todo.status === 3 }"
        >
          <div class="mt-0.5 shrink-0">
            <t-checkbox :model-value="todo.status === 2" @change="(checked: any) => onToggleDone(todo, checked as boolean)" />
          </div>
          <div class="min-w-0 flex-1">
            <div class="mb-1 flex items-center gap-2">
              <h3 class="font-semibold text-[var(--foreground)]" :class="{ 'line-through': todo.status === 2 }">{{ todo.title }}</h3>
              <t-tag :color="priorityColor(todo.priority)" size="small">{{ priorityLabel(todo.priority) }}</t-tag>
              <t-tag :color="statusColor(todo.status)" size="small">{{ statusLabel(todo.status) }}</t-tag>
            </div>
            <p v-if="todo.description" class="mb-2 line-clamp-2 text-sm text-[var(--muted-foreground)]">{{ todo.description }}</p>
            <div class="flex items-center gap-4 text-xs text-[var(--muted-foreground)]">
              <span v-if="todo.dueDate">截止: {{ todo.dueDate }}</span>
              <span>创建: {{ formatDate(todo.createTime) }}</span>
            </div>
          </div>
          <div class="flex items-center gap-1">
            <t-button size="small" @click="openEditDialog(todo)">
              <template #icon><icon-edit /></template>
            </t-button>
            <t-popconfirm :content="$t('todo.deleteConfirm')" @confirm="onDelete(todo.id)">
              <t-button size="small" theme="danger">
                <template #icon><icon-delete /></template>
              </t-button>
            </t-popconfirm>
          </div>
        </div>
      </div>
      <t-empty v-else :description="$t('todo.noData')" />
    </t-loading>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="mt-6 flex justify-end">
      <t-pagination
        v-model:current="currentPage"
        :total="total"
        :page-size="pageSize"
        show-total
        @change="loadTodos"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <t-dialog
      v-model:visible="dialogVisible"
      :header="isEdit ? $t('todo.edit') : $t('todo.add')"
      :width="520"
      :mask-closable="false"
      @confirm="onSubmit"
      @cancel="dialogVisible = false"
    >
      <t-form ref="formRef" :data="formData" layout="vertical">
        <t-form-item name="title" :label="$t('todo.titleLabel')" :rules="[{ required: true, message: t('todo.titleRequired') }]">
          <t-input v-model="formData.title" :placeholder="$t('todo.titlePlaceholder')" />
        </t-form-item>
        <t-form-item name="description" :label="$t('todo.descriptionLabel')">
          <t-textarea v-model="formData.description" :placeholder="$t('todo.descriptionPlaceholder')" />
        </t-form-item>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item name="priority" :label="$t('todo.priorityLabel')">
              <t-select v-model="formData.priority">
                <t-option :value="0">{{ $t('todo.priorityLow') }}</t-option>
                <t-option :value="1">{{ $t('todo.priorityMedium') }}</t-option>
                <t-option :value="2">{{ $t('todo.priorityHigh') }}</t-option>
                <t-option :value="3">{{ $t('todo.priorityUrgent') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="status" :label="$t('todo.statusLabel')">
              <t-select v-model="formData.status">
                <t-option :value="0">{{ $t('todo.statusTodo') }}</t-option>
                <t-option :value="1">{{ $t('todo.statusInProgress') }}</t-option>
                <t-option :value="2">{{ $t('todo.statusDone') }}</t-option>
                <t-option :value="3">{{ $t('todo.statusCancelled') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item name="dueDate" :label="$t('todo.dueDateLabel')">
          
<t-date-picker v-model="formData.dueDate" value-type="YYYY-MM-DD" style="width: 100%" />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { MessagePlugin, DialogPlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { getTodoListApi, addTodoApi, updateTodoApi, deleteTodoApi } from '@/api/enterprise'
import type { TodoItem } from '@/types/api'
import { formatDate } from '@/utils/format'

const { t } = useI18n()

const loading = ref(false)
const todos = ref<TodoItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref<number | ''>('')

// ---- 弹窗 ----
// 表单实例：提交前调 validate() 触发各 t-form-item 的 rules
const formRef = ref<FormInstanceFunctions>()

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<string | null>(null)

const formData = reactive({
  title: '',
  description: '',
  priority: 1 as number,
  status: 0 as number,
  dueDate: '',
})

function priorityLabel(p: number) {
  return { 0: t('todo.priorityLow'), 1: t('todo.priorityMedium'), 2: t('todo.priorityHigh'), 3: t('todo.priorityUrgent') }[p] || t('todo.priorityMedium')
}
function priorityColor(p: number) {
  return ({ 0: 'gray', 1: 'blue', 2: 'orange', 3: 'red' } as Record<number, string>)[p] || 'blue'
}
function statusLabel(s: number) {
  return { 0: t('todo.statusTodo'), 1: t('todo.statusInProgress'), 2: t('todo.statusDone'), 3: t('todo.statusCancelled') }[s] || t('todo.statusTodo')
}
function statusColor(s: number) {
  return ({ 0: 'gray', 1: 'blue', 2: 'green', 3: 'red' } as Record<number, string>)[s] || 'gray'
}

function resetForm() {
  formData.title = ''
  formData.description = ''
  formData.priority = 1
  formData.status = 0
  formData.dueDate = ''
  editingId.value = null
  isEdit.value = false
}

function openAddDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(todo: TodoItem) {
  resetForm()
  isEdit.value = true
  editingId.value = todo.id
  formData.title = todo.title
  formData.description = todo.description || ''
  formData.priority = todo.priority
  formData.status = todo.status
  formData.dueDate = todo.dueDate || ''
  dialogVisible.value = true
}

async function loadTodos() {
  loading.value = true
  try {
    const res = await getTodoListApi({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: statusFilter.value !== '' ? statusFilter.value as number : undefined,
    })
    todos.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (err) {
    console.error('todo list load error:', err)
  } finally {
    loading.value = false
  }
}

function onStatusFilter() {
  currentPage.value = 1
  loadTodos()
}

async function onToggleDone(todo: TodoItem, checked: boolean) {
  const newStatus = checked ? 2 : 0
  try {
    await updateTodoApi({ id: todo.id, status: newStatus })
    loadTodos()
  } catch (err) {
    console.error('todo toggle error:', err)
  }
}

async function onSubmit() {
  // 校验不通过则保持对话框打开，字段错误就地显示（t-dialog 确认不会自行关闭）
  if (await formRef.value?.validate() !== true) return
  try {
    if (isEdit.value && editingId.value) {
      await updateTodoApi({
        id: editingId.value,
        title: formData.title,
        description: formData.description,
        priority: formData.priority,
        status: formData.status,
        dueDate: formData.dueDate,
      })
    } else {
      await addTodoApi({
        title: formData.title,
        description: formData.description,
        priority: formData.priority,
        dueDate: formData.dueDate,
      })
    }
    MessagePlugin.success(t('todo.saveSuccess'))
    dialogVisible.value = false
    resetForm()
    loadTodos()
  } catch {
    // 错误已在拦截器中处理
  }
}

async function onDelete(id: string) {
  try {
    await deleteTodoApi(id)
    MessagePlugin.success(t('todo.deleteSuccess'))
    loadTodos()
  } catch (err) {
    console.error('todo delete error:', err)
  }
}

onMounted(() => {
  loadTodos()
})
</script>
