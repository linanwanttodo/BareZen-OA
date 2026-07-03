<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4" :bordered="true">
      <t-form :data="searchForm" :label-width="80" @submit-success="handleSearch">
        <t-form-item name="deptName" :label="$t('dept.deptName')">
          <t-input v-model="searchForm.deptName" :placeholder="$t('dept.deptName')" allow-clear />
        </t-form-item>
        <t-form-item name="status" :label="$t('common.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.selectPlaceholder')" allow-clear>
            <t-option value="0">{{ $t('dept.statusNormal') }}</t-option>
            <t-option value="1">{{ $t('dept.statusDisabled') }}</t-option>
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

    <!-- 树形表格区域 -->
    <t-card :bordered="true">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('dept.title') }}</span>
          <t-space>
            <t-button @click="toggleExpandAll">
              {{ isAllExpanded ? $t('common.collapseAll') : $t('common.expandAll') }}
            </t-button>
            <t-button v-permission="PERM.system.deptAdd" theme="primary" @click="handleAdd()">
              <template #icon><icon-plus /></template>
              {{ $t('common.add') }}
            </t-button>
          </t-space>
        </div>
      </template>

      
          <t-table :loading="loading" row-key="id" :data="flatRows" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #deptName="{ row }">
                <div class="flex items-center" :style="{ paddingLeft: row.depth * 16 + 'px' }">
                  <icon-right v-if="row.hasChildren && !isExpanded(row.node.id)" class="cursor-pointer mr-1" @click="toggleExpand(row.node.id)" />
                  <icon-down v-if="row.hasChildren && isExpanded(row.node.id)" class="cursor-pointer mr-1" @click="toggleExpand(row.node.id)" />
                  <span v-if="!row.hasChildren" class="inline-block w-5" />
                  <span>{{ row.node.deptName }}</span>
                </div>
              </template>
    <template #orderNum="{ row }">{{ row.node.sort }}</template>
    <template #leader="{ row }">{{ row.node.leader }}</template>
    <template #phone="{ row }">{{ row.node.phone }}</template>
    <template #email="{ row }">{{ row.node.email }}</template>
    <template #status="{ row }">
                <t-tag :theme="row.node.status === '0' ? 'success' : 'danger'" variant="light">
                  {{ row.node.status === '0' ? $t('dept.statusNormal') : $t('dept.statusDisabled') }}
                </t-tag>
              </template>
    <template #operate="{ row }">
                <t-space>
                  <t-button v-permission="PERM.system.deptAdd" size="small" @click="handleAdd(row.node)">{{ $t('common.add') }}</t-button>
                  <t-button v-permission="PERM.system.deptEdit" size="small" @click="handleEdit(row.node)">{{ $t('common.edit') }}</t-button>
                  <t-popconfirm v-permission="PERM.system.deptDelete" :content="$t('dept.deleteConfirm')" @confirm="handleDelete(row.node)">
                    <t-button size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
  </t-table>
      
    </t-card>

    <!-- 新增/编辑弹窗 -->
    <t-dialog
      v-model="formVisible"
      :title="formTitle"
      :width="520"
      :mask-closable="false"
      @confirm="onFormSubmit"
      @cancel="formVisible = false"
    >
      <t-form :data="formData" layout="vertical">
        <t-form-item name="parentId" :label="$t('dept.parentDept')">
          
<!-- TODO(tree-select): data 字段 key/title 需转为 value/label --><t-tree-select
            v-model="formData.parentId"
            :data="parentTreeData"
            :placeholder="$t('common.selectPlaceholder')"
            allow-clear
            :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
          />
        </t-form-item>
        <t-form-item name="deptName" :label="$t('dept.deptName')" :rules="[{ required: true, message: $t('dept.deptName') }]">
          <t-input v-model="formData.deptName" :placeholder="$t('dept.deptName')" />
        </t-form-item>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item name="sort" :label="$t('dept.orderNum')">
              <t-input-number v-model="formData.sort" placeholder="0" :min="0" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="status" :label="$t('common.status')">
              <t-radio-group v-model="formData.status">
                <t-radio value="0">{{ $t('dept.statusNormal') }}</t-radio>
                <t-radio value="1">{{ $t('dept.statusDisabled') }}</t-radio>
              </t-radio-group>
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item name="leader" :label="$t('dept.leader')">
          <t-input v-model="formData.leader" :placeholder="$t('dept.leader')" />
        </t-form-item>
        <t-form-item name="phone" :label="$t('user.phone')">
          <t-input v-model="formData.phone" :placeholder="$t('user.phone')" />
        </t-form-item>
        <t-form-item name="email" :label="$t('user.email')">
          <t-input v-model="formData.email" :placeholder="$t('user.email')" />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, onMounted, computed } from 'vue'
import { MessagePlugin, DialogPlugin } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import { getDeptTreeApi, addDeptApi, updateDeptApi, deleteDeptApi, getDeptApi } from '@/api/dept'
import type { SysDept } from '@/api/dept'

const { t } = useI18n()

// ============ 搜索 ============
const searchForm = ref({
  deptName: '',
  status: '',
})

const allData = ref<SysDept[]>([])

function handleSearch() {
  applyFilter()
}

function handleReset() {
  searchForm.value = { deptName: '', status: '' }
  applyFilter()
}

function applyFilter() {
  const { deptName, status } = searchForm.value
  let list = allData.value
  if (deptName) list = filterByName(list, deptName)
  if (status) list = filterByStatus(list, status)
  treeData.value = buildTree(list)
  expandedIds.value = getAllIds(treeData.value)
  isAllExpanded.value = true
}

function filterByName(list: SysDept[], name: string): SysDept[] {
  return list.filter((item) => {
    const match = item.deptName.includes(name)
    const childMatch = item.children ? filterByName(item.children, name).length > 0 : false
    return match || childMatch
  })
}

function filterByStatus(list: SysDept[], status: string): SysDept[] {
  return list.filter((item) => {
    const match = item.status === status
    const childMatch = item.children ? filterByStatus(item.children, status).length > 0 : false
    return match || childMatch
  })
}

// ============ 树形数据 ============
const treeData = ref<SysDept[]>([])
const loading = ref(false)
const expandedIds = ref<string[]>([])
const isAllExpanded = ref(false)

interface FlatRow {
  node: SysDept
  depth: number
  hasChildren: boolean
}

const flatRows = computed<FlatRow[]>(() => {
  const result: FlatRow[] = []
  const walk = (nodes: SysDept[], depth: number) => {
    for (const node of nodes) {
      const children = node.children || []
      result.push({ node, depth, hasChildren: children.length > 0 })
      if (children.length > 0 && expandedIds.value.includes(node.id)) {
        walk(children, depth + 1)
      }
    }
  }
  walk(treeData.value, 0)
  return result
})

function buildTree(flatList: SysDept[]): SysDept[] {
  const map = new Map<string, SysDept>()
  const tree: SysDept[] = []

  flatList.forEach((item) => {
    map.set(item.id, { ...item, children: [] })
  })

  flatList.forEach((item) => {
    const node = map.get(item.id)!
    if (item.parentId && item.parentId !== '0' && map.has(String(item.parentId))) {
      const parent = map.get(String(item.parentId))!
      if (!parent.children) parent.children = []
      parent.children.push(node)
    } else {
      tree.push(node)
    }
  })

  return tree
}

function isExpanded(id: string) {
  return expandedIds.value.includes(id)
}

function toggleExpand(id: string) {
  const idx = expandedIds.value.indexOf(id)
  if (idx >= 0) expandedIds.value.splice(idx, 1)
  else expandedIds.value.push(id)
}

function toggleExpandAll() {
  if (isAllExpanded.value) {
    expandedIds.value = []
    isAllExpanded.value = false
  } else {
    expandedIds.value = getAllIds(allData.value)
    isAllExpanded.value = true
  }
}

function getAllIds(list: SysDept[]): string[] {
  const ids: string[] = []
  list.forEach((item) => {
    ids.push(item.id)
    if (item.children) ids.push(...getAllIds(item.children))
  })
  return ids
}

// ============ 加载数据 ============
async function loadTable() {
  loading.value = true
  try {
    const res = await getDeptTreeApi()
    allData.value = res.data || []
    treeData.value = buildTree(allData.value)
    expandedIds.value = getAllIds(allData.value)
    isAllExpanded.value = true
  } catch {
    /* interceptor handles */
  } finally {
    loading.value = false
  }
}

// ============ 新增/编辑 ============
const formVisible = ref(false)
const formTitle = ref('')
const editingId = ref<string | null>(null)

const parentTreeData = computed(() => {
  return [{ id: '', deptName: t('dept.topLevel'), children: allData.value }]
})

const formData = ref({
  parentId: '' as string,
  deptName: '',
  sort: 0 as number,
  leader: '',
  phone: '',
  email: '',
  status: '0',
})

function handleAdd(parent?: SysDept) {
  editingId.value = null
  formTitle.value = t('dept.addDept')
  formData.value = {
    parentId: parent ? String(parent.id) : '',
    deptName: '',
    sort: parent ? (parent.sort ?? 0) + 1 : 0,
    leader: '',
    phone: '',
    email: '',
    status: '0',
  }
  formVisible.value = true
}

async function handleEdit(row: SysDept) {
  editingId.value = row.id
  formTitle.value = t('dept.editDept')
  try {
    const res = await getDeptApi(row.id)
    const data = res.data
    formData.value = {
      parentId: data.parentId ? String(data.parentId) : '',
      deptName: data.deptName,
      sort: data.sort ?? 0,
      leader: data.leader ?? '',
      phone: data.phone ?? '',
      email: data.email ?? '',
      status: data.status ?? '0',
    }
    formVisible.value = true
  } catch {
    /* interceptor handles */
  }
}

async function onFormSubmit() {
  try {
    const payload = {
      ...formData.value,
      sort: Number(formData.value.sort) || 0,
    }
    if (editingId.value) {
      await updateDeptApi({ id: editingId.value, ...payload })
      MessagePlugin.success(t('dept.editSuccess'))
    } else {
      await addDeptApi(payload)
      MessagePlugin.success(t('dept.addSuccess'))
    }
    formVisible.value = false
    loadTable()
  } catch {
    /* interceptor handles */
  }
}

// ============ 删除 ============
async function handleDelete(row: SysDept) {
  try {
    await deleteDeptApi(row.id)
    MessagePlugin.success(t('dept.deleteSuccess'))
    loadTable()
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
    { colKey: 'deptName', title: t('dept.deptName') },
    { colKey: 'orderNum', title: t('dept.orderNum'), width: 80 },
    { colKey: 'leader', title: t('dept.leader') },
    { colKey: 'phone', title: t('user.phone') },
    { colKey: 'email', title: t('user.email') },
    { colKey: 'status', title: t('common.status'), width: 100 },
    { colKey: 'operate', title: t('common.operate'), width: 180, align: 'right' },
  ])

</script>
