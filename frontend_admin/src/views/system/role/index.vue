<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4" :bordered="true">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="roleName" label="角色名称">
          <a-input v-model="searchForm.roleName" placeholder="请输入角色名称" allow-clear style="width: 200px" />
        </a-form-item>
        <a-form-item field="roleKey" label="权限字符">
          <a-input v-model="searchForm.roleKey" placeholder="请输入权限字符" allow-clear style="width: 200px" />
        </a-form-item>
        <a-form-item field="status" label="状态">
          <a-select v-model="searchForm.status" placeholder="请选择" allow-clear style="width: 120px">
            <a-option value="0">正常</a-option>
            <a-option value="1">停用</a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 表格区域 -->
    <a-card :bordered="true">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>角色列表</span>
          <a-space>
            <a-button type="primary" @click="handleAdd">
              <template #icon><icon-plus /></template>
              新增
            </a-button>
            <a-button status="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">
              <template #icon><icon-delete /></template>
              批量删除
            </a-button>
          </a-space>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table
          :data="tableData"
          :pagination="false"
          :row-selection="{ selectedRowKeys: selectedIds, onSelect: (keys: string[]) => selectedIds = keys }"
          :row-key="'id'"
        >
          <template #columns>
            <a-table-column title="ID" data-index="id" :width="80" />
            <a-table-column title="角色名称" data-index="roleName" />
            <a-table-column title="权限字符" data-index="roleKey" />
            <a-table-column title="排序" data-index="roleSort" :width="80" />
            <a-table-column title="状态" :width="100">
              <template #cell="{ record }">
                <a-switch
                  :model-value="record.status === '0'"
                  :loading="record._statusLoading"
                  @change="(val: boolean) => handleStatusChange(record, val)"
                />
              </template>
            </a-table-column>
            <a-table-column title="备注" data-index="remark" />
            <a-table-column title="创建时间" :width="160">
              <template #cell="{ record }">
                {{ formatTime(record.createTime) }}
              </template>
            </a-table-column>
            <a-table-column title="操作" :width="200" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button size="small" @click="handleEdit(record)">编辑</a-button>
                  <a-button size="small" @click="handleAssignMenu(record)">分配权限</a-button>
                  <a-popconfirm content="确认删除该角色？" @ok="handleDelete(record)">
                    <a-button size="small" status="danger">删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>

        <div class="mt-4 flex justify-end">
          <a-pagination
            v-model:current="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            show-total
            show-jumper
            @change="onPageChange"
          />
        </div>
      </a-spin>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:visible="formDialogVisible"
      :title="formType === 'add' ? '新增角色' : '编辑角色'"
      :width="520"
      :mask-closable="false"
      @ok="onFormConfirm"
      @cancel="formDialogVisible = false"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item field="roleName" label="角色名称" :rules="[{ required: true, message: '请输入角色名称' }]">
          <a-input v-model="formData.roleName" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item field="roleKey" label="权限字符" :rules="[{ required: true, message: '请输入权限字符' }]">
          <a-input v-model="formData.roleKey" placeholder="请输入权限字符，如 admin" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="roleSort" label="角色排序">
              <a-input-number v-model="formData.roleSort" placeholder="0" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="status" label="状态">
              <a-radio-group v-model="formData.status">
                <a-radio value="0">正常</a-radio>
                <a-radio value="1">停用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item field="remark" label="备注">
          <a-textarea v-model="formData.remark" placeholder="请输入备注" :max-length="200" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分配菜单权限弹窗 -->
    <a-modal
      v-model:visible="menuDialogVisible"
      title="分配菜单权限"
      :width="560"
      :mask-closable="false"
      @ok="onMenuConfirm"
      @cancel="menuDialogVisible = false"
    >
      <div v-if="currentRole" class="mb-3">
        <a-space>
          <span style="color: #86909C">当前角色：</span>
          <span style="font-weight: 500">{{ currentRole.roleName }}（{{ currentRole.roleKey }}）</span>
        </a-space>
      </div>
      <a-space class="mb-3">
        <a-button size="small" @click="handleExpandAll">展开/折叠</a-button>
        <a-button size="small" @click="handleCheckAll">全选/取消</a-button>
      </a-space>
      <div class="max-h-[360px] overflow-y-auto border rounded p-2" style="border-color: #E5E6EB">
        <div v-for="item in menuFlatRows" :key="item.node.id" class="flex items-center py-1.5" :style="{ paddingLeft: item.depth * 16 + 'px' }">
          <icon-right v-if="item.hasChildren && !menuExpandedIds.includes(item.node.id)" class="cursor-pointer mr-1" @click="toggleMenuExpand(item.node.id)" />
          <icon-down v-if="item.hasChildren && menuExpandedIds.includes(item.node.id)" class="cursor-pointer mr-1" @click="toggleMenuExpand(item.node.id)" />
          <span v-if="!item.hasChildren" class="inline-block w-5" />
          <a-checkbox :model-value="checkedMenuIds.includes(item.node.id)" @change="() => toggleMenuCheck(item.node.id)">
            {{ item.node.menuName }}
          </a-checkbox>
          <a-tag size="small" class="ml-2" :color="item.node.menuType === 'M' ? 'blue' : item.node.menuType === 'C' ? 'green' : 'orange'">
            {{ menuTypeLabel(item.node.menuType) }}
          </a-tag>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconPlus,
  IconDelete,
  IconRight,
  IconDown,
} from '@arco-design/web-vue/es/icon'
import {
  getRoleListApi,
  addRoleApi,
  updateRoleApi,
  deleteRoleApi,
  batchDeleteRoleApi,
  changeRoleStatusApi,
  getRoleMenuIdsApi,
  assignRoleMenusApi,
  type SysRole,
  type RoleListParams,
} from '@/api/role'
import { getMenuTreeSelectApi } from '@/api/menu'
import type { SysMenu } from '@/types/menu'

// ============ 搜索 ============
const searchForm = reactive<RoleListParams>({
  roleName: '',
  roleKey: '',
  status: '',
})

function handleSearch() {
  pagination.current = 1
  loadTable()
}

function handleReset() {
  searchForm.roleName = ''
  searchForm.roleKey = ''
  searchForm.status = ''
  pagination.current = 1
  loadTable()
}

// ============ 表格 ============
const loading = ref(false)
const tableData = ref<(SysRole & { _statusLoading?: boolean })[]>([])
const selectedIds = ref<string[]>([])

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

function onPageChange(page: number) {
  pagination.current = page
  loadTable()
}

async function loadTable() {
  loading.value = true
  try {
    const res = await getRoleListApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      roleName: searchForm.roleName || undefined,
      roleKey: searchForm.roleKey || undefined,
      status: searchForm.status || undefined,
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    /* interceptor handles */
  } finally {
    loading.value = false
  }
}

function formatTime(time: string) {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

// ============ 状态切换 ============
async function handleStatusChange(row: SysRole & { _statusLoading?: boolean }, val: boolean) {
  const newStatus = val ? '0' : '1'
  row._statusLoading = true
  try {
    await changeRoleStatusApi(row.id, newStatus)
    row.status = newStatus
    Message.success('状态修改成功')
  } catch {
    /* interceptor handles */
  } finally {
    row._statusLoading = false
  }
}

// ============ 删除 ============
async function handleDelete(row: SysRole) {
  try {
    await deleteRoleApi(row.id)
    Message.success('删除成功')
    loadTable()
  } catch {
    /* interceptor handles */
  }
}

async function handleBatchDelete() {
  Modal.confirm({
    title: '确认删除',
    content: `确认删除选中的 ${selectedIds.value.length} 个角色吗？`,
    onOk: async () => {
      try {
        await batchDeleteRoleApi(selectedIds.value)
        Message.success('批量删除成功')
        selectedIds.value = []
        loadTable()
      } catch {
        /* interceptor handles */
      }
    },
  })
}

// ============ 新增/编辑 ============
const formDialogVisible = ref(false)
const formType = ref<'add' | 'edit'>('add')

const defaultFormData = {
  id: undefined as string | undefined,
  roleName: '',
  roleKey: '',
  roleSort: 0 as number | string,
  status: '0',
  remark: '',
}

const formData = reactive({ ...defaultFormData })

function handleAdd() {
  formType.value = 'add'
  Object.assign(formData, { ...defaultFormData })
  formDialogVisible.value = true
}

function handleEdit(row: SysRole) {
  formType.value = 'edit'
  Object.assign(formData, {
    id: row.id,
    roleName: row.roleName,
    roleKey: row.roleKey,
    roleSort: row.roleSort,
    status: row.status,
    remark: row.remark,
  })
  formDialogVisible.value = true
}

async function onFormConfirm() {
  try {
    const payload = { ...formData, roleSort: Number(formData.roleSort) || 0 }
    if (formType.value === 'add') {
      await addRoleApi(payload)
      Message.success('新增成功')
    } else {
      await updateRoleApi(payload)
      Message.success('修改成功')
    }
    formDialogVisible.value = false
    loadTable()
  } catch {
    /* interceptor handles */
  }
}

// ============ 分配菜单权限 ============
const menuDialogVisible = ref(false)
const currentRole = ref<SysRole | null>(null)
const menuTreeData = ref<SysMenu[]>([])
const checkedMenuIds = ref<string[]>([])
const menuExpandedIds = ref<string[]>([])

interface MenuFlatRow {
  node: SysMenu
  depth: number
  hasChildren: boolean
}

const menuFlatRows = computed<MenuFlatRow[]>(() => {
  const result: MenuFlatRow[] = []
  const walk = (nodes: SysMenu[], depth: number) => {
    for (const node of nodes) {
      const children = node.children || []
      result.push({ node, depth, hasChildren: children.length > 0 })
      if (children.length > 0 && menuExpandedIds.value.includes(node.id)) {
        walk(children, depth + 1)
      }
    }
  }
  walk(menuTreeData.value, 0)
  return result
})

function menuTypeLabel(type: string) {
  const map: Record<string, string> = { M: '目录', C: '菜单', F: '按钮' }
  return map[type] || type
}

function toggleMenuExpand(id: string) {
  const idx = menuExpandedIds.value.indexOf(id)
  if (idx >= 0) menuExpandedIds.value.splice(idx, 1)
  else menuExpandedIds.value.push(id)
}

function toggleMenuCheck(id: string) {
  const idx = checkedMenuIds.value.indexOf(id)
  if (idx >= 0) checkedMenuIds.value.splice(idx, 1)
  else checkedMenuIds.value.push(id)
}

function collectAllExpandableIds(nodes: SysMenu[]): string[] {
  const ids: string[] = []
  for (const node of nodes) {
    if (node.children && node.children.length) {
      ids.push(node.id)
      ids.push(...collectAllExpandableIds(node.children))
    }
  }
  return ids
}

function collectAllCheckableIds(nodes: SysMenu[]): string[] {
  const ids: string[] = []
  for (const node of nodes) {
    ids.push(node.id)
    if (node.children) ids.push(...collectAllCheckableIds(node.children))
  }
  return ids
}

async function handleAssignMenu(row: SysRole) {
  currentRole.value = row
  menuDialogVisible.value = true
  checkedMenuIds.value = []
  menuExpandedIds.value = []

  try {
    const [treeRes, idsRes] = await Promise.allSettled([getMenuTreeSelectApi(), getRoleMenuIdsApi(row.id)])
    if (treeRes.status === 'fulfilled') {
      menuTreeData.value = treeRes.value.data || []
      menuExpandedIds.value = menuTreeData.value.map((m) => m.id)
    }
    if (idsRes.status === 'fulfilled') {
      checkedMenuIds.value = idsRes.value.data || []
    }
  } catch {
    /* interceptor handles */
  }
}

function handleExpandAll() {
  if (menuExpandedIds.value.length > 0) menuExpandedIds.value = []
  else menuExpandedIds.value = collectAllExpandableIds(menuTreeData.value)
}

function handleCheckAll() {
  if (checkedMenuIds.value.length > 0) checkedMenuIds.value = []
  else checkedMenuIds.value = collectAllCheckableIds(menuTreeData.value)
}

async function onMenuConfirm() {
  if (!currentRole.value) return
  try {
    await assignRoleMenusApi(currentRole.value.id, checkedMenuIds.value)
    Message.success('权限分配成功')
    menuDialogVisible.value = false
  } catch {
    /* interceptor handles */
  }
}

// ============ 初始化 ============
onMounted(() => {
  loadTable()
})
</script>
