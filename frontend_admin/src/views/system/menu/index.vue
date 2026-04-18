<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4" :bordered="true">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="menuName" label="菜单名称">
          <a-input v-model="searchForm.menuName" placeholder="请输入菜单名称" allow-clear style="width: 200px" />
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

    <!-- 树形表格区域 -->
    <a-card :bordered="true">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>菜单列表</span>
          <a-space>
            <a-button @click="toggleExpandAll">
              {{ isAllExpanded ? '全部折叠' : '全部展开' }}
            </a-button>
            <a-button type="primary" @click="handleAdd()">
              <template #icon><icon-plus /></template>
              新增
            </a-button>
          </a-space>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="flatRows" :pagination="false" :row-key="'id'">
          <template #columns>
            <a-table-column title="ID" :width="80">
              <template #cell="{ record }">{{ record.node.id }}</template>
            </a-table-column>
            <a-table-column title="菜单名称">
              <template #cell="{ record }">
                <div class="flex items-center" :style="{ paddingLeft: record.depth * 16 + 'px' }">
                  <icon-right v-if="record.hasChildren && !expandedIds.includes(record.node.id)" class="cursor-pointer mr-1" @click="toggleExpand(record.node.id)" />
                  <icon-down v-if="record.hasChildren && expandedIds.includes(record.node.id)" class="cursor-pointer mr-1" @click="toggleExpand(record.node.id)" />
                  <span v-if="!record.hasChildren" class="inline-block w-5" />
                  <icon-folder v-if="record.node.menuType === 'M'" class="mr-1.5" style="color: #3370FF" />
                  <icon-menu v-if="record.node.menuType === 'C'" class="mr-1.5" style="color: #3370FF" />
                  <icon-command v-if="record.node.menuType === 'F'" class="mr-1.5" style="color: #3370FF" />
                  <span>{{ record.node.menuName }}</span>
                </div>
              </template>
            </a-table-column>
            <a-table-column title="类型" :width="100">
              <template #cell="{ record }">
                <a-tag :color="record.node.menuType === 'M' ? 'blue' : record.node.menuType === 'C' ? 'green' : 'orange'">
                  {{ menuTypeLabel(record.node.menuType) }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column title="图标" data-index="icon" :width="100" />
            <a-table-column title="排序" data-index="orderNum" :width="80" />
            <a-table-column title="权限标识" data-index="perms" />
            <a-table-column title="路由地址" data-index="path" />
            <a-table-column title="组件路径" data-index="component" />
            <a-table-column title="可见" :width="80">
              <template #cell="{ record }">
                <a-tag :color="record.node.visible === '0' ? 'green' : 'red'">
                  {{ record.node.visible === '0' ? '显示' : '隐藏' }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column title="状态" :width="80">
              <template #cell="{ record }">
                <a-tag :color="record.node.status === '0' ? 'green' : 'red'">
                  {{ record.node.status === '0' ? '正常' : '停用' }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column title="操作" :width="180" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-if="record.node.menuType !== 'F'" size="small" @click="handleAdd(record.node)">新增</a-button>
                  <a-button size="small" @click="handleEdit(record.node)">编辑</a-button>
                  <a-popconfirm :content="`确认删除菜单【${record.node.menuName}】吗？`" @ok="handleDelete(record.node)">
                    <a-button size="small" status="danger">删除</a-button>
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
      v-model:visible="formDialogVisible"
      :title="formType === 'add' ? '新增菜单' : '编辑菜单'"
      :width="640"
      :mask-closable="false"
      @ok="onFormConfirm"
      @cancel="formDialogVisible = false"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item field="parentId" label="上级菜单">
          <a-tree-select
            v-model="formData.parentId"
            :data="parentTreeData"
            placeholder="选择上级菜单（留空为顶级）"
            allow-clear
            :field-names="{ value: 'id', title: 'menuName', children: 'children' }"
          />
        </a-form-item>
        <a-form-item field="menuType" label="菜单类型">
          <a-radio-group v-model="formData.menuType" @change="onMenuTypeChange">
            <a-radio value="M">目录</a-radio>
            <a-radio value="C">菜单</a-radio>
            <a-radio value="F">按钮</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item field="menuName" label="菜单名称" :rules="[{ required: true, message: '请输入菜单名称' }]">
              <a-input v-model="formData.menuName" placeholder="请输入菜单名称" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item field="orderNum" label="显示排序" :rules="[{ required: true, message: '请输入排序' }]">
              <a-input-number v-model="formData.orderNum" placeholder="0" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col v-if="formData.menuType !== 'F'" :span="8">
            <a-form-item field="icon" label="图标">
              <a-input v-model="formData.icon" placeholder="图标名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <template v-if="formData.menuType !== 'F'">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item field="path" label="路由地址">
                <a-input v-model="formData.path" placeholder="请输入路由地址" />
              </a-form-item>
            </a-col>
            <a-col v-if="formData.menuType === 'C'" :span="12">
              <a-form-item field="component" label="组件路径">
                <a-input v-model="formData.component" placeholder="如 system/user/index" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item field="visible" label="是否显示">
            <a-radio-group v-model="formData.visible">
              <a-radio value="0">显示</a-radio>
              <a-radio value="1">隐藏</a-radio>
            </a-radio-group>
          </a-form-item>
        </template>
        <a-row :gutter="16">
          <a-col v-if="formData.menuType === 'C' || formData.menuType === 'F'" :span="12">
            <a-form-item field="perms" label="权限标识">
              <a-input v-model="formData.perms" placeholder="如 system:user:list" />
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
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconPlus,
  IconRight,
  IconDown,
  IconFolder,
  IconMenu,
  IconCommand,
} from '@arco-design/web-vue/es/icon'
import {
  getMenuListApi,
  getMenuTreeSelectApi,
  addMenuApi,
  updateMenuApi,
  deleteMenuApi,
} from '@/api/menu'
import type { SysMenu } from '@/types/menu'

function menuTypeLabel(type: string) {
  const map: Record<string, string> = { M: '目录', C: '菜单', F: '按钮' }
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
    Message.success('删除成功')
    loadTable()
  } catch {
    /* interceptor handles */
  }
}

// ============ 新增/编辑 ============
const formDialogVisible = ref(false)
const formType = ref<'add' | 'edit'>('add')

const parentTreeData = computed(() => {
  return [{ id: '0', menuName: '顶级菜单', children: rawMenuList.value }]
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
  orderNum: 0 as number | string,
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
      Message.success('新增成功')
    } else {
      await updateMenuApi(payload)
      Message.success('修改成功')
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
</script>
