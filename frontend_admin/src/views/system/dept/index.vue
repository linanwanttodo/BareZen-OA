<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4" :bordered="true">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="deptName" :label="$t('dept.deptName')">
          <a-input v-model="searchForm.deptName" :placeholder="$t('dept.deptName')" allow-clear style="width: 200px" />
        </a-form-item>
        <a-form-item field="status" :label="$t('common.status')">
          <a-select v-model="searchForm.status" :placeholder="$t('common.selectPlaceholder')" allow-clear style="width: 120px">
            <a-option value="0">{{ $t('dept.statusNormal') }}</a-option>
            <a-option value="1">{{ $t('dept.statusDisabled') }}</a-option>
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

    <!-- 树形表格区域 -->
    <a-card :bordered="true">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('dept.title') }}</span>
          <a-space>
            <a-button @click="toggleExpandAll">
              {{ isAllExpanded ? $t('common.collapseAll') : $t('common.expandAll') }}
            </a-button>
            <a-button v-permission="'system:dept:add'" type="primary" @click="handleAdd()">
              <template #icon><icon-plus /></template>
              {{ $t('common.add') }}
            </a-button>
          </a-space>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="flatRows" :pagination="false" :row-key="'id'">
          <template #columns>
            <a-table-column :title="$t('common.operate')" :width="80">
              <template #cell="{ record }">{{ record.node.id }}</template>
            </a-table-column>
            <a-table-column :title="$t('dept.deptName')">
              <template #cell="{ record }">
                <div class="flex items-center" :style="{ paddingLeft: record.depth * 16 + 'px' }">
                  <icon-right v-if="record.hasChildren && !isExpanded(record.node.id)" class="cursor-pointer mr-1" @click="toggleExpand(record.node.id)" />
                  <icon-down v-if="record.hasChildren && isExpanded(record.node.id)" class="cursor-pointer mr-1" @click="toggleExpand(record.node.id)" />
                  <span v-if="!record.hasChildren" class="inline-block w-5" />
                  <span>{{ record.node.deptName }}</span>
                </div>
              </template>
            </a-table-column>
            <a-table-column :title="$t('dept.orderNum')" :width="80">
              <template #cell="{ record }">{{ record.node.sort }}</template>
            </a-table-column>
            <a-table-column :title="$t('dept.leader')">
              <template #cell="{ record }">{{ record.node.leader }}</template>
            </a-table-column>
            <a-table-column :title="$t('user.phone')">
              <template #cell="{ record }">{{ record.node.phone }}</template>
            </a-table-column>
            <a-table-column :title="$t('user.email')">
              <template #cell="{ record }">{{ record.node.email }}</template>
            </a-table-column>
            <a-table-column :title="$t('common.status')" :width="100">
              <template #cell="{ record }">
                <a-tag :color="record.node.status === '0' ? 'green' : 'red'">
                  {{ record.node.status === '0' ? $t('dept.statusNormal') : $t('dept.statusDisabled') }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="180" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-permission="'system:dept:add'" size="small" @click="handleAdd(record.node)">{{ $t('common.add') }}</a-button>
                  <a-button v-permission="'system:dept:edit'" size="small" @click="handleEdit(record.node)">{{ $t('common.edit') }}</a-button>
                  <a-popconfirm v-permission="'system:dept:delete'" :content="$t('dept.deleteConfirm')" @ok="handleDelete(record.node)">
                    <a-button size="small" status="danger">{{ $t('common.delete') }}</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>
      </a-spin>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:visible="formVisible"
      :title="formTitle"
      :width="520"
      :mask-closable="false"
      @ok="onFormSubmit"
      @cancel="formVisible = false"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item field="parentId" :label="$t('dept.parentDept')">
          <a-tree-select
            v-model="formData.parentId"
            :data="parentTreeData"
            :placeholder="$t('common.selectPlaceholder')"
            allow-clear
            :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
          />
        </a-form-item>
        <a-form-item field="deptName" :label="$t('dept.deptName')" :rules="[{ required: true, message: $t('dept.deptName') }]">
          <a-input v-model="formData.deptName" :placeholder="$t('dept.deptName')" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="sort" :label="$t('dept.orderNum')">
              <a-input-number v-model="formData.sort" placeholder="0" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="status" :label="$t('common.status')">
              <a-radio-group v-model="formData.status">
                <a-radio value="0">{{ $t('dept.statusNormal') }}</a-radio>
                <a-radio value="1">{{ $t('dept.statusDisabled') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item field="leader" :label="$t('dept.leader')">
          <a-input v-model="formData.leader" :placeholder="$t('dept.leader')" />
        </a-form-item>
        <a-form-item field="phone" :label="$t('user.phone')">
          <a-input v-model="formData.phone" :placeholder="$t('user.phone')" />
        </a-form-item>
        <a-form-item field="email" :label="$t('user.email')">
          <a-input v-model="formData.email" :placeholder="$t('user.email')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { useI18n } from 'vue-i18n'
import {
  IconPlus,
  IconRight,
  IconDown,
} from '@arco-design/web-vue/es/icon'
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
      Message.success(t('dept.editSuccess'))
    } else {
      await addDeptApi(payload)
      Message.success(t('dept.addSuccess'))
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
    Message.success(t('dept.deleteSuccess'))
    loadTable()
  } catch {
    /* interceptor handles */
  }
}

// ============ 初始化 ============
onMounted(() => {
  loadTable()
})
</script>
