<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold" style="color: #1D2129">待办事项</h1>
        <p class="mt-1 text-sm" style="color: #86909C">管理你的工作待办</p>
      </div>
      <a-button type="primary" @click="openAddDialog">
        <template #icon><icon-plus /></template>
        新增待办
      </a-button>
    </div>

    <!-- 状态筛选 -->
    <div class="mb-6">
      <a-radio-group v-model="statusFilter" type="button" @change="onStatusFilter">
        <a-radio :value="''">全部</a-radio>
        <a-radio :value="0">待办</a-radio>
        <a-radio :value="1">进行中</a-radio>
        <a-radio :value="2">已完成</a-radio>
      </a-radio-group>
    </div>

    <!-- 待办列表 -->
    <a-spin :loading="loading">
      <div v-if="todos.length" class="space-y-3">
        <div
          v-for="todo in todos"
          :key="todo.id"
          class="flex items-start gap-4 rounded-lg border p-5"
          style="border-color: #E5E6EB; background: #FFFFFF"
          :class="{ 'opacity-60': todo.status === 2 || todo.status === 3 }"
        >
          <div class="mt-0.5 shrink-0">
            <a-checkbox :model-value="todo.status === 2" @change="(v: boolean) => onToggleDone(todo, v)" />
          </div>
          <div class="min-w-0 flex-1">
            <div class="mb-1 flex items-center gap-2">
              <h3 class="font-semibold" style="color: #1D2129" :class="{ 'line-through': todo.status === 2 }">{{ todo.title }}</h3>
              <a-tag :color="priorityColor(todo.priority)" size="small">{{ priorityLabel(todo.priority) }}</a-tag>
              <a-tag :color="statusColor(todo.status)" size="small">{{ statusLabel(todo.status) }}</a-tag>
            </div>
            <p v-if="todo.description" class="mb-2 line-clamp-2 text-sm" style="color: #86909C">{{ todo.description }}</p>
            <div class="flex items-center gap-4 text-xs" style="color: #86909C">
              <span v-if="todo.dueDate">截止: {{ todo.dueDate }}</span>
              <span>创建: {{ formatDate(todo.createTime) }}</span>
            </div>
          </div>
          <div class="flex items-center gap-1">
            <a-button size="small" @click="openEditDialog(todo)">
              <template #icon><icon-edit /></template>
            </a-button>
            <a-popconfirm content="确认删除此待办？" @ok="onDelete(todo.id)">
              <a-button size="small" status="danger">
                <template #icon><icon-delete /></template>
              </a-button>
            </a-popconfirm>
          </div>
        </div>
      </div>
      <a-empty v-else description="暂无待办事项" />
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
      :title="isEdit ? '编辑待办' : '新增待办'"
      :width="520"
      :mask-closable="false"
      @ok="onSubmit"
      @cancel="dialogVisible = false"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item field="title" label="标题" :rules="[{ required: true, message: '请输入待办标题' }]">
          <a-input v-model="formData.title" placeholder="请输入待办标题" />
        </a-form-item>
        <a-form-item field="description" label="描述">
          <a-textarea v-model="formData.description" placeholder="请输入待办描述（可选）" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="priority" label="优先级">
              <a-select v-model="formData.priority">
                <a-option :value="0">低</a-option>
                <a-option :value="1">中</a-option>
                <a-option :value="2">高</a-option>
                <a-option :value="3">紧急</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="status" label="状态">
              <a-select v-model="formData.status">
                <a-option :value="0">待办</a-option>
                <a-option :value="1">进行中</a-option>
                <a-option :value="2">已完成</a-option>
                <a-option :value="3">已取消</a-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item field="dueDate" label="截止日期">
          <a-date-picker v-model="formData.dueDate" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { IconPlus, IconEdit, IconDelete } from '@arco-design/web-vue/es/icon'
import { getTodoListApi, addTodoApi, updateTodoApi, deleteTodoApi } from '@/api/enterprise'
import type { TodoItem } from '@/types/api'

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
  return { 0: '低', 1: '中', 2: '高', 3: '紧急' }[p] || '中'
}
function priorityColor(p: number) {
  return ({ 0: 'gray', 1: 'blue', 2: 'orange', 3: 'red' } as Record<number, string>)[p] || 'blue'
}
function statusLabel(s: number) {
  return { 0: '待办', 1: '进行中', 2: '已完成', 3: '已取消' }[s] || '待办'
}
function statusColor(s: number) {
  return ({ 0: 'gray', 1: 'blue', 2: 'green', 3: 'red' } as Record<number, string>)[s] || 'gray'
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  return dateStr.substring(0, 10)
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
    todos.value = (res.data?.records || []).map((t) => ({ ...t, done: t.status === 2 }))
    total.value = res.data?.total || 0
  } catch {
    // 静默
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
  } catch {
    // 静默
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
        status: formData.status,
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
  } catch {
    // 静默
  }
}

onMounted(() => {
  loadTodos()
})
</script>
