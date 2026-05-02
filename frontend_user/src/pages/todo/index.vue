<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-[var(--foreground)]">{{ $t('todo.title') }}</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">管理你的工作待办</p>
      </div>
      <a-button type="primary" @click="openAddDialog">
        <template #icon><icon-plus /></template>
        {{ $t('todo.add') }}
      </a-button>
    </div>

    <!-- 状态筛选 -->
    <div class="mb-6">
      <a-radio-group v-model="statusFilter" type="button" @change="onStatusFilter">
        <a-radio :value="''">{{ $t('todo.all') }}</a-radio>
        <a-radio :value="0">{{ $t('todo.statusTodo') }}</a-radio>
        <a-radio :value="1">{{ $t('todo.statusInProgress') }}</a-radio>
        <a-radio :value="2">{{ $t('todo.statusDone') }}</a-radio>
      </a-radio-group>
    </div>

    <!-- 待办列表 -->
    <a-spin :loading="loading">
      <div v-if="todos.length" class="space-y-3">
        <div
          v-for="todo in todos"
          :key="todo.id"
          class="flex items-start gap-4 rounded-lg border border-[var(--border)] bg-[var(--card)] p-5"
          :class="{ 'opacity-60': todo.status === 2 || todo.status === 3 }"
        >
          <div class="mt-0.5 shrink-0">
            <a-checkbox :model-value="todo.status === 2" @change="(checked: any) => onToggleDone(todo, checked as boolean)" />
          </div>
          <div class="min-w-0 flex-1">
            <div class="mb-1 flex items-center gap-2">
              <h3 class="font-semibold text-[var(--foreground)]" :class="{ 'line-through': todo.status === 2 }">{{ todo.title }}</h3>
              <a-tag :color="priorityColor(todo.priority)" size="small">{{ priorityLabel(todo.priority) }}</a-tag>
              <a-tag :color="statusColor(todo.status)" size="small">{{ statusLabel(todo.status) }}</a-tag>
            </div>
            <p v-if="todo.description" class="mb-2 line-clamp-2 text-sm text-[var(--muted-foreground)]">{{ todo.description }}</p>
            <div class="flex items-center gap-4 text-xs text-[var(--muted-foreground)]">
              <span v-if="todo.dueDate">截止: {{ todo.dueDate }}</span>
              <span>创建: {{ formatDate(todo.createTime) }}</span>
            </div>
          </div>
          <div class="flex items-center gap-1">
            <a-button size="small" @click="openEditDialog(todo)">
              <template #icon><icon-edit /></template>
            </a-button>
            <a-popconfirm :content="$t('todo.deleteConfirm')" @ok="onDelete(todo.id)">
              <a-button size="small" status="danger">
                <template #icon><icon-delete /></template>
              </a-button>
            </a-popconfirm>
          </div>
        </div>
      </div>
      <a-empty v-else :description="$t('todo.noData')" />
    </a-spin>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="mt-6 flex justify-end">
      <a-pagination
        v-model:current="currentPage"
        :total="total"
        :page-size="pageSize"
        show-total
        @change="loadTodos"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:visible="dialogVisible"
      :title="isEdit ? $t('todo.edit') : $t('todo.add')"
      :width="520"
      :mask-closable="false"
      @ok="onSubmit"
      @cancel="dialogVisible = false"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item field="title" :label="$t('todo.titleLabel')" :rules="[{ required: true, message: '请输入待办标题' }]">
          <a-input v-model="formData.title" placeholder="请输入待办标题" />
        </a-form-item>
        <a-form-item field="description" :label="$t('todo.descriptionLabel')">
          <a-textarea v-model="formData.description" placeholder="请输入待办描述（可选）" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="priority" :label="$t('todo.priorityLabel')">
              <a-select v-model="formData.priority">
                <a-option :value="0">{{ $t('todo.priorityLow') }}</a-option>
                <a-option :value="1">{{ $t('todo.priorityMedium') }}</a-option>
                <a-option :value="2">{{ $t('todo.priorityHigh') }}</a-option>
                <a-option :value="3">紧急</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="status" :label="$t('todo.statusLabel')">
              <a-select v-model="formData.status">
                <a-option :value="0">{{ $t('todo.statusTodo') }}</a-option>
                <a-option :value="1">{{ $t('todo.statusInProgress') }}</a-option>
                <a-option :value="2">{{ $t('todo.statusDone') }}</a-option>
                <a-option :value="3">已取消</a-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item field="dueDate" :label="$t('todo.dueDateLabel')">
          <a-date-picker v-model="formData.dueDate" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import { IconPlus, IconEdit, IconDelete } from '@arco-design/web-vue/es/icon'
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
  return { 0: t('todo.priorityLow'), 1: t('todo.priorityMedium'), 2: t('todo.priorityHigh'), 3: '紧急' }[p] || t('todo.priorityMedium')
}
function priorityColor(p: number) {
  return ({ 0: 'gray', 1: 'blue', 2: 'orange', 3: 'red' } as Record<number, string>)[p] || 'blue'
}
function statusLabel(s: number) {
  return { 0: t('todo.statusTodo'), 1: t('todo.statusInProgress'), 2: t('todo.statusDone'), 3: '已取消' }[s] || t('todo.statusTodo')
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
    Message.success('操作成功')
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
    Message.success('删除成功')
    loadTodos()
  } catch (err) {
    console.error('todo delete error:', err)
  }
}

onMounted(() => {
  loadTodos()
})
</script>
