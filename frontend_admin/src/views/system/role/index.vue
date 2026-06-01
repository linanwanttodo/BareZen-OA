<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4" :bordered="true">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="roleName" :label="$t('role.roleName')">
          <a-input v-model="searchForm.roleName" :placeholder="$t('role.roleName')" allow-clear style="width: 200px" />
        </a-form-item>
        <a-form-item field="roleKey" :label="$t('role.roleKey')">
          <a-input v-model="searchForm.roleKey" :placeholder="$t('role.roleKey')" allow-clear style="width: 200px" />
        </a-form-item>
        <a-form-item field="status" :label="$t('common.status')">
          <a-select v-model="searchForm.status" :placeholder="$t('common.selectPlaceholder')" allow-clear style="width: 120px">
            <a-option value="0">{{ $t('common.normal') }}</a-option>
            <a-option value="1">{{ $t('common.disable') }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">{{ $t('common.search') }}</a-button>
            <a-button @click="handleReset">{{ $t('common.reset') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 表格区域 -->
    <a-card :bordered="true">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('role.title') }}</span>
          <a-space>
            <a-button v-permission="PERM.system.roleAdd" type="primary" @click="handleAdd">
              <template #icon><icon-plus /></template>
              {{ $t('common.add') }}
            </a-button>
            <a-button v-permission="PERM.system.roleDelete" status="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">
              <template #icon><icon-delete /></template>
              {{ $t('common.batchDelete') }}
            </a-button>
          </a-space>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table
          :data="tableData"
          :pagination="false"
          :row-selection="{ type: 'checkbox', selectedRowKeys: selectedIds }"
          :row-key="'id'"
          @selection-change="onSelectionChange"
        >
          <template #columns>
            <a-table-column :title="$t('common.operate')" data-index="id" :width="80" />
            <a-table-column :title="$t('role.roleName')" data-index="roleName" />
            <a-table-column :title="$t('role.roleKey')" data-index="roleKey" />
            <a-table-column :title="$t('role.roleSort')" data-index="roleSort" :width="80" />
            <a-table-column :title="$t('common.status')" :width="100">
              <template #cell="{ record }">
                <a-switch
                  :model-value="record.status === '0'"
                  :loading="record._statusLoading"
                  @change="(val: string | number | boolean) => handleStatusChange(record, Boolean(val))"
                />
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.remark')" data-index="remark" />
            <a-table-column :title="$t('common.createTime')" :width="160">
              <template #cell="{ record }">
                {{ formatTime(record.createTime) }}
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="200" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-permission="PERM.system.roleEdit" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</a-button>
                  <a-button v-permission="PERM.system.roleEdit" size="small" @click="handleAssignMenu(record)">{{ $t('role.assignMenu') }}</a-button>
                  <a-popconfirm v-permission="PERM.system.roleDelete" :content="$t('role.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button size="small" status="danger">{{ $t('common.delete') }}</a-button>
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
      :title="formType === 'add' ? $t('role.addRole') : $t('role.editRole')"
      :width="520"
      :mask-closable="false"
      @ok="onFormConfirm"
      @cancel="formDialogVisible = false"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item field="roleName" :label="$t('role.roleName')" :rules="[{ required: true, message: $t('role.roleName') }]">
          <a-input v-model="formData.roleName" :placeholder="$t('role.roleName')" />
        </a-form-item>
        <a-form-item field="roleKey" :label="$t('role.roleKey')" :rules="[{ required: true, message: $t('role.roleKey') }]">
          <a-input v-model="formData.roleKey" :placeholder="$t('role.roleKey') + ', ' + $t('common.inputPlaceholder')" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="roleSort" :label="$t('role.roleSort')">
              <a-input-number v-model="formData.roleSort" placeholder="0" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="status" :label="$t('common.status')">
              <a-radio-group v-model="formData.status">
                <a-radio value="0">{{ $t('common.normal') }}</a-radio>
                <a-radio value="1">{{ $t('common.disable') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item field="remark" :label="$t('common.remark')">
          <a-textarea v-model="formData.remark" :placeholder="$t('common.remark')" :max-length="200" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分配菜单权限弹窗 -->
    <a-modal
      v-model:visible="menuDialogVisible"
      :title="$t('role.assignMenuTitle')"
      :width="560"
      :mask-closable="false"
      @ok="onMenuConfirm"
      @cancel="menuDialogVisible = false"
    >
      <div v-if="currentRole" class="mb-3">
        <a-space>
          <span style="color: var(--wecom-text-tertiary)">{{ $t('common.currentRole') }}</span>
          <span style="font-weight: 500">{{ currentRole.roleName }}（{{ currentRole.roleKey }}）</span>
        </a-space>
      </div>
      <a-space class="mb-3">
        <a-button size="small" @click="handleExpandAll">{{ $t('role.expandCollapse') }}</a-button>
        <a-button size="small" @click="handleCheckAll">{{ $t('role.checkAll') }}</a-button>
      </a-space>
      <div class="max-h-[360px] overflow-y-auto border rounded p-2" style="border-color: var(--wecom-border)">
        <div v-for="item in menuFlatRows" :key="item.node.id" class="flex items-center py-1.5" :style="{ paddingLeft: item.depth * 16 + 'px' }">
          <icon-right v-if="item.hasChildren && !menuExpandedIds.includes(item.node.id)" class="cursor-pointer mr-1" @click="toggleMenuExpand(item.node.id)" />
          <icon-down v-if="item.hasChildren && menuExpandedIds.includes(item.node.id)" class="cursor-pointer mr-1" @click="toggleMenuExpand(item.node.id)" />
          <span v-if="!item.hasChildren" class="inline-block w-5" />
          <a-checkbox :model-value="checkedMenuIds.includes(item.node.id)" @change="() => toggleMenuCheck(item.node.id)">
            {{ item.node.menuName }}
          </a-checkbox>
          <a-tag size="small" class="ml-2" :color="item.node.menuType === 'M' ? 'blue' : item.node.menuType === 'C' ? 'green' : 'orange'">
            {{ $t(menuTypeKey(item.node.menuType)) }}
          </a-tag>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, computed, onMounted } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()

function menuTypeKey(type: string): string {
  const map: Record<string, string> = { M: 'role.directory', C: 'role.menu', F: 'role.button' }
  return map[type] || type
}

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

function onSelectionChange(keys: (string | number)[]) {
  selectedIds.value = keys.map(String)
}

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
    tableData.value = res.data?.list || []
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
    Message.success(t('role.statusSuccess'))
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
    Message.success(t('role.deleteSuccess'))
    loadTable()
  } catch {
    /* interceptor handles */
  }
}

async function handleBatchDelete() {
  Modal.confirm({
    title: t('common.confirmDelete'),
    content: t('role.batchDeleteConfirm', { count: selectedIds.value.length }),
    onOk: async () => {
      try {
        await batchDeleteRoleApi(selectedIds.value)
        Message.success(t('role.batchDeleteSuccess'))
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
  roleSort: 0 as number,
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
      Message.success(t('role.addSuccess'))
    } else {
      await updateRoleApi(payload)
      Message.success(t('role.editSuccess'))
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
    Message.success(t('role.menuAssignSuccess'))
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
