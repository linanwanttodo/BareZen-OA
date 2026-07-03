<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4" :bordered="true">
      <t-form :data="searchForm" :label-width="80" @submit-success="handleSearch">
        <t-form-item name="menuName" :label="$t('menu.menuName')">
          <t-input v-model="searchForm.menuName" :placeholder="$t('menu.menuName')" allow-clear />
        </t-form-item>
        <t-form-item name="status" :label="$t('common.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.selectPlaceholder')" allow-clear>
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

    <!-- 树形表格区域 -->
    <t-card :bordered="true">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('menu.title') }}</span>
          <t-space>
            <t-button @click="toggleExpandAll">
              {{ isAllExpanded ? $t('common.collapseAll') : $t('common.expandAll') }}
            </t-button>
            <t-button v-permission="PERM.system.menuAdd" theme="primary" @click="handleAdd()">
              <template #icon><icon-plus /></template>
              {{ $t('common.add') }}
            </t-button>
          </t-space>
        </div>
      </template>

      
          <t-table :loading="loading" row-key="id" :data="flatRows" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #menuName="{ row }">
                <div class="flex items-center" :style="{ paddingLeft: row.depth * 16 + 'px' }">
                  <icon-right v-if="row.hasChildren && !expandedIds.includes(row.node.id)" class="cursor-pointer mr-1" @click="toggleExpand(row.node.id)" />
                  <icon-down v-if="row.hasChildren && expandedIds.includes(row.node.id)" class="cursor-pointer mr-1" @click="toggleExpand(row.node.id)" />
                  <span v-if="!row.hasChildren" class="inline-block w-5" />
                  <icon-folder v-if="row.node.menuType === 'M'" class="mr-1.5" style="color: var(--primary)" />
                  <icon-menu v-if="row.node.menuType === 'C'" class="mr-1.5" style="color: var(--primary)" />
                  <icon-command v-if="row.node.menuType === 'F'" class="mr-1.5" style="color: var(--primary)" />
                  <span>{{ row.node.menuName }}</span>
                </div>
              </template>
    <template #menuType="{ row }">
                <t-tag :theme="row.node.menuType === 'M' ? 'primary' : row.node.menuType === 'C' ? 'success' : 'warning'" variant="light">
                  {{ $t(menuTypeKey(row.node.menuType)) }}
                </t-tag>
              </template>
    <template #visible="{ row }">
                <t-tag :theme="row.node.visible === '0' ? 'success' : 'danger'" variant="light">
                  {{ row.node.visible === '0' ? $t('menu.show') : $t('menu.hide') }}
                </t-tag>
              </template>
    <template #status="{ row }">
                <t-tag :theme="row.node.status === '0' ? 'success' : 'danger'" variant="light">
                  {{ row.node.status === '0' ? $t('common.normal') : $t('common.disable') }}
                </t-tag>
              </template>
    <template #operate="{ row }">
                <t-space>
                  <t-link theme="primary" v-permission="PERM.system.menuAdd" v-if="row.node.menuType !== 'F'" @click="handleAdd(row.node)">{{ $t('common.add') }}</t-link>
                  <t-link theme="primary" v-permission="PERM.system.menuEdit" @click="handleEdit(row.node)">{{ $t('common.edit') }}</t-link>
                  <t-popconfirm v-permission="PERM.system.menuDelete" :content="$t('menu.deleteConfirm', { name: row.node.menuName })" @confirm="handleDelete(row.node)">
                    <t-button size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
  </t-table>
      
    </t-card>

    <!-- 新增/编辑弹窗 -->
    <t-dialog
      v-model="formDialogVisible"
      :title="formType === 'add' ? $t('menu.addMenu') : $t('menu.editMenu')"
      :width="640"
      :mask-closable="false"
      @confirm="onFormConfirm"
      @cancel="formDialogVisible = false"
    >
      <t-form :data="formData" layout="vertical">
        <t-form-item name="parentId" :label="$t('menu.parentMenu')">
          
<!-- TODO(tree-select): data 字段 key/title 需转为 value/label --><t-tree-select
            v-model="formData.parentId"
            :data="parentTreeData"
            :placeholder="$t('menu.parentMenuPlaceholder')"
            allow-clear
            :field-names="{ key: 'id', title: 'menuName', children: 'children' }"
          />
        </t-form-item>
        <t-form-item name="menuType" :label="$t('menu.menuType')">
          <t-radio-group v-model="formData.menuType" @change="onMenuTypeChange">
            <t-radio value="M">{{ $t('menu.directory') }}</t-radio>
            <t-radio value="C">{{ $t('menu.menu') }}</t-radio>
            <t-radio value="F">{{ $t('menu.button') }}</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-row :gutter="16">
          <t-col :span="8">
            <t-form-item name="menuName" :label="$t('menu.menuName')" :rules="[{ required: true, message: $t('menu.menuName') }]">
              <t-input v-model="formData.menuName" :placeholder="$t('menu.menuName')" />
            </t-form-item>
          </t-col>
          <t-col :span="8">
            <t-form-item name="orderNum" :label="$t('menu.orderNum')" :rules="[{ required: true, message: $t('menu.orderNum') }]">
              <t-input-number v-model="formData.orderNum" placeholder="0" :min="0" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col v-if="formData.menuType !== 'F'" :span="8">
            <t-form-item name="icon" :label="$t('menu.icon')">
              <t-input v-model="formData.icon" :placeholder="$t('menu.icon')" />
            </t-form-item>
          </t-col>
        </t-row>
        <template v-if="formData.menuType !== 'F'">
          <t-row :gutter="16">
            <t-col :span="12">
              <t-form-item name="path" :label="$t('menu.path')">
                <t-input v-model="formData.path" :placeholder="$t('menu.path')" />
              </t-form-item>
            </t-col>
            <t-col v-if="formData.menuType === 'C'" :span="12">
              <t-form-item name="component" :label="$t('menu.component')">
                <t-input v-model="formData.component" :placeholder="$t('menu.component')" />
              </t-form-item>
            </t-col>
          </t-row>
          <t-form-item name="visible" :label="$t('menu.visible')">
            <t-radio-group v-model="formData.visible">
              <t-radio value="0">{{ $t('menu.show') }}</t-radio>
              <t-radio value="1">{{ $t('menu.hide') }}</t-radio>
            </t-radio-group>
          </t-form-item>
        </template>
        <t-row :gutter="16">
          <t-col v-if="formData.menuType === 'C' || formData.menuType === 'F'" :span="12">
            <t-form-item name="perms" :label="$t('menu.perms')">
              <t-input v-model="formData.perms" :placeholder="$t('menu.perms')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="status" :label="$t('menu.status')">
              <t-radio-group v-model="formData.status">
                <t-radio value="0">{{ $t('common.normal') }}</t-radio>
                <t-radio value="1">{{ $t('common.disable') }}</t-radio>
              </t-radio-group>
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, computed, onMounted } from 'vue'
import { MessagePlugin, DialogPlugin } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import {
  getMenuListApi,
  getMenuTreeSelectApi,
  addMenuApi,
  updateMenuApi,
  deleteMenuApi,
} from '@/api/menu'
import type { SysMenu } from '@/types/menu'

const { t } = useI18n()

function menuTypeKey(type: string): string {
  const map: Record<string, string> = { M: 'menu.directory', C: 'menu.menu', F: 'menu.button' }
  return map[type] || type
}

// ============ 搜索 ============
const searchForm = reactive({
  menuName: '',
  status: '',
})

function handleSearch() {
  loadTable()
}

function handleReset() {
  searchForm.menuName = ''
  searchForm.status = ''
  loadTable()
}

// ============ 树形表格 ============
const loading = ref(false)
const rawMenuList = ref<SysMenu[]>([])
const expandedIds = ref<string[]>([])
const isAllExpanded = ref(false)

interface FlatRow {
  node: SysMenu
  depth: number
  hasChildren: boolean
}

const flatRows = computed<FlatRow[]>(() => {
  let list = rawMenuList.value
  if (searchForm.menuName || searchForm.status) {
    list = filterMenuTree(list, searchForm.menuName, searchForm.status)
  }
  const result: FlatRow[] = []
  const walk = (nodes: SysMenu[], depth: number) => {
    for (const node of nodes) {
      const children = node.children || []
      result.push({ node, depth, hasChildren: children.length > 0 })
      if (children.length > 0 && expandedIds.value.includes(node.id)) {
        walk(children, depth + 1)
      }
    }
  }
  walk(list, 0)
  return result
})

function filterMenuTree(nodes: SysMenu[], name: string, status: string): SysMenu[] {
  const result: SysMenu[] = []
  for (const node of nodes) {
    const nameMatch = !name || node.menuName.includes(name)
    const statusMatch = !status || node.status === status
    const selfMatch = nameMatch && statusMatch
    const filteredChildren = node.children ? filterMenuTree(node.children, name, status) : []
    if (selfMatch || filteredChildren.length) {
      result.push({ ...node, children: filteredChildren.length ? filteredChildren : selfMatch ? node.children : undefined })
    }
  }
  return result
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
    expandedIds.value = collectAllExpandableIds(rawMenuList.value)
    isAllExpanded.value = true
  }
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

async function loadTable() {
  loading.value = true
  try {
    const res = await getMenuListApi()
    rawMenuList.value = res.data || []
    expandedIds.value = collectAllExpandableIds(rawMenuList.value)
    isAllExpanded.value = true
  } catch {
    /* interceptor handles */
  } finally {
    loading.value = false
  }
}

// ============ 删除 ============
async function handleDelete(row: SysMenu) {
  try {
    await deleteMenuApi(row.id)
    MessagePlugin.success(t('menu.deleteSuccess'))
    loadTable()
  } catch {
    /* interceptor handles */
  }
}

// ============ 新增/编辑 ============
const formDialogVisible = ref(false)
const formType = ref<'add' | 'edit'>('add')

const parentTreeData = computed<any[]>(() => {
  return [{ id: '0', menuName: t('menu.topLevelMenu'), children: rawMenuList.value }]
})

const defaultFormData = {
  id: undefined as string | undefined,
  parentId: '' as string,
  menuName: '',
  menuType: 'M' as 'M' | 'C' | 'F',
  path: '',
  component: '',
  perms: '',
  icon: '',
  orderNum: 0 as number,
  visible: '0',
  status: '0',
  remark: '',
}

const formData = reactive({ ...defaultFormData })

function onMenuTypeChange() {
  if (formData.menuType === 'F') {
    formData.path = ''
    formData.component = ''
    formData.icon = ''
    formData.visible = '0'
  }
  if (formData.menuType === 'M') {
    formData.perms = ''
    formData.component = ''
  }
}

function handleAdd(row?: SysMenu) {
  formType.value = 'add'
  Object.assign(formData, {
    ...defaultFormData,
    parentId: row?.id || '',
    menuType: row ? (row.menuType === 'M' ? 'C' : 'F') : 'M',
  })
  formDialogVisible.value = true
}

function handleEdit(row: SysMenu) {
  formType.value = 'edit'
  Object.assign(formData, {
    id: row.id,
    parentId: row.parentId,
    menuName: row.menuName,
    menuType: row.menuType,
    path: row.path,
    component: row.component,
    perms: row.perms,
    icon: row.icon,
    orderNum: row.orderNum,
    visible: row.visible,
    status: row.status,
    remark: row.remark,
  })
  formDialogVisible.value = true
}

async function onFormConfirm() {
  try {
    const payload = { ...formData, orderNum: Number(formData.orderNum) || 0 }
    if (formType.value === 'add') {
      await addMenuApi(payload)
      MessagePlugin.success(t('menu.addSuccess'))
    } else {
      await updateMenuApi(payload)
      MessagePlugin.success(t('menu.editSuccess'))
    }
    formDialogVisible.value = false
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
    { colKey: 'menuName', title: t('menu.menuName') },
    { colKey: 'menuType', title: t('menu.menuType'), width: 100 },
    { colKey: 'icon', title: t('menu.icon'), width: 100 },
    { colKey: 'orderNum', title: t('menu.orderNum'), width: 80 },
    { colKey: 'perms', title: t('menu.perms') },
    { colKey: 'path', title: t('menu.path') },
    { colKey: 'component', title: t('menu.component') },
    { colKey: 'visible', title: t('menu.visible'), width: 80 },
    { colKey: 'status', title: t('menu.status'), width: 80 },
    { colKey: 'operate', title: t('common.operate'), width: 180, align: 'right' },
  ])

</script>
