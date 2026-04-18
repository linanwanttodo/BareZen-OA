<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4" :bordered="true">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="userName" label="用户名">
          <a-input v-model="searchForm.userName" placeholder="请输入用户名" allow-clear style="width: 180px" />
        </a-form-item>
        <a-form-item field="phone" label="手机号">
          <a-input v-model="searchForm.phone" placeholder="请输入手机号" allow-clear style="width: 180px" />
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
          <span>用户管理</span>
          <a-space>
            <a-button type="primary" @click="handleAdd">
              <template #icon><icon-plus /></template>
              新增用户
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
          @selection-change="onSelectionChange"
        >
          <template #columns>
            <a-table-column title="ID" data-index="id" :width="80" />
            <a-table-column title="用户名" data-index="userName" />
            <a-table-column title="昵称" data-index="nickName" />
            <a-table-column title="手机号" data-index="phone" />
            <a-table-column title="邮箱" data-index="email" />
            <a-table-column title="性别" :width="80">
              <template #cell="{ record }">
                {{ record.sex === '0' ? '男' : record.sex === '1' ? '女' : '未知' }}
              </template>
            </a-table-column>
            <a-table-column title="状态" :width="100">
              <template #cell="{ record }">
                <a-switch
                  :model-value="record.status === '0'"
                  :loading="record._statusLoading"
                  @change="(val: boolean) => handleStatusChange(record, val)"
                />
              </template>
            </a-table-column>
            <a-table-column title="创建时间" :width="160">
              <template #cell="{ record }">
                {{ formatTime(record.createTime) }}
              </template>
            </a-table-column>
            <a-table-column title="操作" :width="280" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button size="small" @click="handleEdit(record)">编辑</a-button>
                  <a-button size="small" @click="handleAssignRole(record)">分配角色</a-button>
                  <a-button size="small" @click="handleAssignDept(record)">分配部门</a-button>
                  <a-button size="small" @click="handleResetPwd(record)">重置密码</a-button>
                  <a-popconfirm content="确认删除该用户？" @ok="handleDelete(record)">
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
      :title="formType === 'add' ? '新增用户' : '编辑用户'"
      :width="640"
      :mask-closable="false"
      @ok="onFormConfirm"
      @cancel="formDialogVisible = false"
    >
      <a-form :model="formData" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="userName" label="用户名" :rules="[{ required: true, message: '请输入用户名' }]">
              <a-input v-model="formData.userName" placeholder="请输入用户名" :disabled="formType === 'edit'" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="nickName" label="用户昵称" :rules="[{ required: true, message: '请输入用户昵称' }]">
              <a-input v-model="formData.nickName" placeholder="请输入用户昵称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="formType === 'add'" field="password" label="密码" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model="formData.password" placeholder="请输入密码" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="phone" label="手机号">
              <a-input v-model="formData.phone" placeholder="请输入手机号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="email" label="邮箱">
              <a-input v-model="formData.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="sex" label="性别">
              <a-radio-group v-model="formData.sex">
                <a-radio value="0">男</a-radio>
                <a-radio value="1">女</a-radio>
                <a-radio value="2">未知</a-radio>
              </a-radio-group>
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

    <!-- 分配角色弹窗 -->
    <a-modal
      v-model:visible="roleDialogVisible"
      title="分配角色"
      :width="480"
      :mask-closable="false"
      @ok="onRoleConfirm"
      @cancel="roleDialogVisible = false"
    >
      <div v-if="currentUser" class="mb-3">
        <a-space>
          <span style="color: #86909C">当前用户：</span>
          <span style="font-weight: 500">{{ currentUser.nickName }}（{{ currentUser.userName }}）</span>
        </a-space>
      </div>
      <a-checkbox-group v-model="selectedRoleIds" direction="vertical">
        <a-checkbox v-for="opt in roleOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </a-checkbox>
      </a-checkbox-group>
    </a-modal>

    <!-- 重置密码弹窗 -->
    <a-modal
      v-model:visible="resetPwdVisible"
      title="重置密码"
      :width="400"
      :mask-closable="false"
      @ok="onResetPwdConfirm"
      @cancel="resetPwdVisible = false"
    >
      <div v-if="currentUser" class="mb-3">
        <a-space>
          <span style="color: #86909C">当前用户：</span>
          <span style="font-weight: 500">{{ currentUser.nickName }}（{{ currentUser.userName }}）</span>
        </a-space>
      </div>
      <a-form :model="resetPwdData" layout="vertical">
        <a-form-item field="password" label="新密码" :rules="[{ required: true, message: '请输入新密码' }]">
          <a-input-password v-model="resetPwdData.password" placeholder="请输入新密码（至少6位）" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分配部门弹窗 -->
    <a-modal
      v-model:visible="deptDialogVisible"
      title="分配部门"
      :width="560"
      :mask-closable="false"
      @ok="onDeptConfirm"
      @cancel="deptDialogVisible = false"
    >
      <div v-if="currentUser" class="mb-3">
        <a-space>
          <span style="color: #86909C">当前用户：</span>
          <span style="font-weight: 500">{{ currentUser.nickName }}（{{ currentUser.userName }}）</span>
        </a-space>
      </div>
      <a-empty v-if="deptFlat.length === 0" description="暂无部门数据" />
      <div v-else class="max-h-[360px] overflow-y-auto border rounded p-2" style="border-color: #E5E6EB">
        <div v-for="item in deptFlat" :key="item.node.id" class="flex items-center gap-2 py-1.5" :style="{ paddingLeft: item.depth * 16 + 'px' }">
          <a-checkbox :model-value="checkedDeptKeys.includes(item.node.id)" @change="() => toggleDeptCheck(item.node.id)">
            {{ item.node.deptName }}
          </a-checkbox>
          <a-select
            v-if="checkedDeptKeys.includes(item.node.id)"
            :model-value="deptRoleMap[item.node.id] || '0'"
            size="small"
            style="width: 120px"
            @change="(v: string) => onDeptRoleChange(item.node.id, v)"
          >
            <a-option value="0">普通用户</a-option>
            <a-option value="1">管理员</a-option>
          </a-select>
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
} from '@arco-design/web-vue/es/icon'
import {
  getUserListApi,
  addUserApi,
  updateUserApi,
  deleteUserApi,
  batchDeleteUserApi,
  changeUserStatusApi,
  getUserRoleIdsApi,
  assignUserRolesApi,
  resetUserPasswordApi,
  type SysUser,
  type UserListParams,
} from '@/api/user'
import { getRoleListApi, type SysRole } from '@/api/role'
import { getDeptTreeApi, getUserDeptsApi, assignUserDeptsApi, type SysDept, type SysUserDept } from '@/api/dept'

// ============ 搜索 ============
const searchForm = reactive<UserListParams>({
  userName: '',
  phone: '',
  status: '',
})

function handleSearch() {
  pagination.current = 1
  loadTable()
}

function handleReset() {
  searchForm.userName = ''
  searchForm.phone = ''
  searchForm.status = ''
  pagination.current = 1
  loadTable()
}

// ============ 表格 ============
const loading = ref(false)
const tableData = ref<(SysUser & { _statusLoading?: boolean })[]>([])
const selectedIds = ref<string[]>([])

function onSelectionChange(keys: string[]) {
  selectedIds.value = keys
}

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

function onPageChange(page: number) {
  pagination.current = page
  loadTable()
}

async function loadTable() {
  loading.value = true
  try {
    const res = await getUserListApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      userName: searchForm.userName || undefined,
      phone: searchForm.phone || undefined,
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
async function handleStatusChange(row: SysUser & { _statusLoading?: boolean }, val: boolean) {
  const newStatus = val ? '0' : '1'
  row._statusLoading = true
  try {
    await changeUserStatusApi(row.id, newStatus)
    row.status = newStatus
    Message.success('状态修改成功')
  } catch {
    /* interceptor handles */
  } finally {
    row._statusLoading = false
  }
}

// ============ 删除 ============
async function handleDelete(row: SysUser) {
  try {
    await deleteUserApi(row.id)
    Message.success('删除成功')
    loadTable()
  } catch {
    /* interceptor handles */
  }
}

async function handleBatchDelete() {
  Modal.confirm({
    title: '确认删除',
    content: `确认删除选中的 ${selectedIds.value.length} 个用户吗？`,
    onOk: async () => {
      try {
        await batchDeleteUserApi(selectedIds.value)
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
  userName: '',
  nickName: '',
  password: '',
  phone: '',
  email: '',
  sex: '0',
  status: '0',
  remark: '',
}

const formData = reactive({ ...defaultFormData })

function handleAdd() {
  formType.value = 'add'
  Object.assign(formData, { ...defaultFormData })
  formDialogVisible.value = true
}

function handleEdit(row: SysUser) {
  formType.value = 'edit'
  Object.assign(formData, {
    id: row.id,
    userName: row.userName,
    nickName: row.nickName,
    password: '',
    phone: row.phone,
    email: row.email,
    sex: row.sex,
    status: row.status,
    remark: row.remark,
  })
  formDialogVisible.value = true
}

async function onFormConfirm() {
  try {
    if (formType.value === 'add') {
      await addUserApi({ ...formData })
      Message.success('新增成功')
    } else {
      const updateData = { ...formData }
      if (!updateData.password) delete (updateData as Record<string, unknown>).password
      await updateUserApi(updateData)
      Message.success('修改成功')
    }
    formDialogVisible.value = false
    loadTable()
  } catch {
    /* interceptor handles */
  }
}

// ============ 分配角色 ============
const roleDialogVisible = ref(false)
const currentUser = ref<SysUser | null>(null)
const roleOptions = ref<{ label: string; value: string }[]>([])
const selectedRoleIds = ref<string[]>([])

async function handleAssignRole(row: SysUser) {
  currentUser.value = row
  roleDialogVisible.value = true
  selectedRoleIds.value = []

  try {
    const [rolesRes, userRoleRes] = await Promise.allSettled([
      getRoleListApi({ pageNum: 1, pageSize: 999 }),
      getUserRoleIdsApi(row.id),
    ])

    if (rolesRes.status === 'fulfilled') {
      const allRoles: SysRole[] = rolesRes.value.data?.records || []
      roleOptions.value = allRoles.map((r) => ({ label: `${r.roleName}（${r.roleKey}）`, value: r.id }))
    }
    if (userRoleRes.status === 'fulfilled') {
      selectedRoleIds.value = userRoleRes.value.data || []
    }
  } catch {
    /* interceptor handles */
  }
}

async function onRoleConfirm() {
  if (!currentUser.value) return
  try {
    await assignUserRolesApi(currentUser.value.id, selectedRoleIds.value)
    Message.success('角色分配成功')
    roleDialogVisible.value = false
  } catch {
    /* interceptor handles */
  }
}

// ============ 重置密码 ============
const resetPwdVisible = ref(false)
const resetPwdData = reactive({ password: '' })

function handleResetPwd(row: SysUser) {
  currentUser.value = row
  resetPwdData.password = ''
  resetPwdVisible.value = true
}

async function onResetPwdConfirm() {
  if (!currentUser.value) return
  try {
    await resetUserPasswordApi(currentUser.value.id, resetPwdData.password)
    Message.success('密码重置成功')
    resetPwdVisible.value = false
  } catch {
    /* interceptor handles */
  }
}

// ============ 分配部门 ============
const deptDialogVisible = ref(false)
const deptTree = ref<SysDept[]>([])
const checkedDeptKeys = ref<string[]>([])
const deptRoleMap = ref<Record<string, string>>({})

interface DeptFlatRow {
  node: SysDept
  depth: number
}

const deptFlat = computed<DeptFlatRow[]>(() => {
  const result: DeptFlatRow[] = []
  const walk = (nodes: SysDept[], depth: number) => {
    for (const node of nodes) {
      result.push({ node, depth })
      if (node.children && node.children.length) walk(node.children, depth + 1)
    }
  }
  walk(deptTree.value, 0)
  return result
})

function toggleDeptCheck(id: string) {
  const idx = checkedDeptKeys.value.indexOf(id)
  if (idx >= 0) {
    checkedDeptKeys.value.splice(idx, 1)
    delete deptRoleMap.value[id]
  } else {
    checkedDeptKeys.value.push(id)
    deptRoleMap.value[id] = '0'
  }
}

function onDeptRoleChange(deptId: string, roleType: string) {
  deptRoleMap.value[deptId] = roleType
}

async function handleAssignDept(row: SysUser) {
  currentUser.value = row
  deptDialogVisible.value = true
  checkedDeptKeys.value = []
  deptRoleMap.value = {}

  try {
    const treeRes = await getDeptTreeApi()
    deptTree.value = treeRes.data || []

    const deptRes = await getUserDeptsApi(row.id)
    const userDepts: SysUserDept[] = deptRes.data || []
    checkedDeptKeys.value = userDepts.map((d) => d.deptId)
    userDepts.forEach((d) => {
      deptRoleMap.value[d.deptId] = d.roleType || '0'
    })
  } catch {
    /* interceptor handles */
  }
}

async function onDeptConfirm() {
  if (!currentUser.value) return
  try {
    const deptList: SysUserDept[] = checkedDeptKeys.value.map((deptId) => ({
      userId: currentUser.value!.id,
      deptId,
      roleType: deptRoleMap.value[deptId] || '0',
    }))
    await assignUserDeptsApi(currentUser.value.id, deptList)
    Message.success('部门分配成功')
    deptDialogVisible.value = false
  } catch {
    /* interceptor handles */
  }
}

// ============ 初始化 ============
onMounted(() => {
  loadTable()
})
</script>
