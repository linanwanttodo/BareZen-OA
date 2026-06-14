<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4" :bordered="true">
      <t-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <t-form-item field="roleName" :label="$t('role.roleName')">
          <t-input v-model="searchForm.roleName" :placeholder="$t('role.roleName')" allow-clear style="width: 200px" />
        </t-form-item>
        <t-form-item field="roleKey" :label="$t('role.roleKey')">
          <t-input v-model="searchForm.roleKey" :placeholder="$t('role.roleKey')" allow-clear style="width: 200px" />
        </t-form-item>
        <t-form-item field="status" :label="$t('common.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.selectPlaceholder')" allow-clear style="width: 120px">
            <t-option value="0">{{ $t('common.normal') }}</t-option>
            <t-option value="1">{{ $t('common.disable') }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" html-type="submit">{{ $t('common.search') }}</t-button>
            <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-card>

    <!-- 表格区域 -->
    <t-card :bordered="true">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('role.title') }}</span>
          <t-space>
            <t-button v-permission="PERM.system.roleAdd" theme="primary" @click="handleAdd">
              <template #icon><icon-plus /></template>
              {{ $t('common.add') }}
            </t-button>
            <t-button v-permission="PERM.system.roleDelete" theme="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">
              <template #icon><icon-delete /></template>
              {{ $t('common.batchDelete') }}
            </t-button>
          </t-space>
        </div>
      </template>

      <t-loading :loading="loading">
          <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
    <template #status="{ row }">
                <t-switch
                  :model-value="row.status === '0'"
                  :loading="row._statusLoading"
                  @change="(val: string | number | boolean) => handleStatusChange(row, Boolean(val))"
                />
              </template>
    <template #createTime="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
    <template #col_7="{ row }">
                <t-space>
                  <t-button v-permission="PERM.system.roleEdit" size="small" @click="handleEdit(row)">{{ $t('common.edit') }}</t-button>
                  <t-button v-permission="PERM.system.roleEdit" size="small" @click="handleAssignMenu(row)">{{ $t('role.assignMenu') }}</t-button>
                  <t-popconfirm v-permission="PERM.system.roleDelete" :content="$t('role.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
  </t-table>

        <div class="mt-4 flex justify-end">
          <t-pagination
            v-model:current="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            show-total
            show-jumper
            @change="onPageChange"
          />
        </div>
      </t-loading>
    </t-card>

    <!-- 新增/编辑弹窗 -->
    <t-dialog
      v-model="formDialogVisible"
      :title="formType === 'add' ? $t('role.addRole') : $t('role.editRole')"
      :width="520"
      :mask-closable="false"
      @confirm="onFormConfirm"
      @cancel="formDialogVisible = false"
    >
      <t-form :model="formData" layout="vertical">
        <t-form-item field="roleName" :label="$t('role.roleName')" :rules="[{ required: true, message: $t('role.roleName') }]">
          <t-input v-model="formData.roleName" :placeholder="$t('role.roleName')" />
        </t-form-item>
        <t-form-item field="roleKey" :label="$t('role.roleKey')" :rules="[{ required: true, message: $t('role.roleKey') }]">
          <t-input v-model="formData.roleKey" :placeholder="$t('role.roleKey') + ', ' + $t('common.inputPlaceholder')" />
        </t-form-item>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item field="roleSort" :label="$t('role.roleSort')">
              <t-input-number v-model="formData.roleSort" placeholder="0" :min="0" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="status" :label="$t('common.status')">
              <t-radio-group v-model="formData.status">
                <t-radio value="0">{{ $t('common.normal') }}</t-radio>
                <t-radio value="1">{{ $t('common.disable') }}</t-radio>
              </t-radio-group>
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item field="remark" :label="$t('common.remark')">
          <t-textarea v-model="formData.remark" :placeholder="$t('common.remark')" :max-length="200" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 分配菜单权限弹窗 -->
    <t-dialog
      v-model="menuDialogVisible"
      :title="$t('role.assignMenuTitle')"
      :width="560"
      :mask-closable="false"
      @confirm="onMenuConfirm"
      @cancel="menuDialogVisible = false"
    >
      <div v-if="currentRole" class="mb-3">
        <t-space>
          <span style="color: var(--wecom-text-tertiary)">{{ $t('common.currentRole') }}</span>
          <span style="font-weight: 500">{{ currentRole.roleName }}（{{ currentRole.roleKey }}）</span>
        </t-space>
      </div>
      <t-space class="mb-3">
        <t-button size="small" @click="handleExpandAll">{{ $t('role.expandCollapse') }}</t-button>
        <t-button size="small" @click="handleCheckAll">{{ $t('role.checkAll') }}</t-button>
      </t-space>
      <div class="max-h-[360px] overflow-y-auto border rounded p-2" style="border-color: var(--wecom-border)">
        <div v-for="item in menuFlatRows" :key="item.node.id" class="flex items-center py-1.5" :style="{ paddingLeft: item.depth * 16 + 'px' }">
          <icon-right v-if="item.hasChildren && !menuExpandedIds.includes(item.node.id)" class="cursor-pointer mr-1" @click="toggleMenuExpand(item.node.id)" />
          <icon-down v-if="item.hasChildren && menuExpandedIds.includes(item.node.id)" class="cursor-pointer mr-1" @click="toggleMenuExpand(item.node.id)" />
          <span v-if="!item.hasChildren" class="inline-block w-5" />
          <t-checkbox :model-value="checkedMenuIds.includes(item.node.id)" @change="() => toggleMenuCheck(item.node.id)">
            {{ item.node.menuName }}
          </t-checkbox>
          <t-tag size="small" class="ml-2" :theme="item.node.menuType === 'M' ? 'primary' : item.node.menuType === 'C' ? 'success' : 'warning'">
            {{ $t(menuTypeKey(item.node.menuType)) }}
          </t-tag>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, computed, onMounted } from 'vue'
import { MessagePlugin, DialogPlugin } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
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
    MessagePlugin.success(t('role.statusSuccess'))
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
    MessagePlugin.success(t('role.deleteSuccess'))
    loadTable()
  } catch {
    /* interceptor handles */
  }
}

async function handleBatchDelete() {
  DialogPlugin.confirm({
    header: t('common.confirmDelete'),
    body: t('role.batchDeleteConfirm', { count: selectedIds.value.length }),
    onConfirm: async () => {
      try {
        await batchDeleteRoleApi(selectedIds.value)
        MessagePlugin.success(t('role.batchDeleteSuccess'))
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
      MessagePlugin.success(t('role.addSuccess'))
    } else {
      await updateRoleApi(payload)
      MessagePlugin.success(t('role.editSuccess'))
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
    MessagePlugin.success(t('role.menuAssignSuccess'))
    menuDialogVisible.value = false
  } catch {
    /* interceptor handles */
  }
}

// ============ 初始化 ============
onMounted(() => {
  loadTable()
})

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'id', title: t('common.operate'), width: 80 },
    { colKey: 'roleName', title: t('role.roleName') },
    { colKey: 'roleKey', title: t('role.roleKey') },
    { colKey: 'roleSort', title: t('role.roleSort'), width: 80 },
    { colKey: 'status', title: t('common.status'), width: 100 },
    { colKey: 'remark', title: t('common.remark') },
    { colKey: 'createTime', title: t('common.createTime'), width: 160 },
    { colKey: 'col_7', title: t('common.operate'), width: 200, align: 'right' },
  ])

</script>
