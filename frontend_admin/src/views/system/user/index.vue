<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4" :bordered="true">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="userName" :label="$t('user.userName')">
          <a-input v-model="searchForm.userName" :placeholder="$t('user.userName')" allow-clear style="width: 180px" />
        </a-form-item>
        <a-form-item field="phone" :label="$t('user.phone')">
          <a-input v-model="searchForm.phone" :placeholder="$t('user.phone')" allow-clear style="width: 180px" />
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
          <span>{{ $t('user.userManagement') }}</span>
          <a-space>
            <a-button v-permission="PERM.system.userAdd" type="primary" @click="handleAdd">
              <template #icon><icon-plus /></template>
              {{ $t('user.addUser') }}
            </a-button>
            <a-button v-permission="PERM.system.userAdd" @click="batchVisible = true">
              <template #icon><icon-user-group /></template>
              {{ $t('user.batchAdd') }}
            </a-button>
            <a-button v-permission="PERM.system.userDelete" status="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">
              <template #icon><icon-delete /></template>
              {{ $t('user.batchDelete') }}
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
            <a-table-column :title="$t('user.userName')" data-index="userName" />
            <a-table-column :title="$t('user.nickName')" data-index="nickName" />
            <a-table-column :title="$t('user.phone')" data-index="phone" />
            <a-table-column :title="$t('user.email')" data-index="email" />
            <a-table-column :title="$t('user.sex')" :width="80">
              <template #cell="{ record }">
                {{ record.sex === '0' ? $t('user.male') : record.sex === '1' ? $t('user.female') : $t('user.unknown') }}
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.status')" :width="100">
              <template #cell="{ record }">
                <a-switch
                  :model-value="record.status === '0'"
                  :loading="record._statusLoading"
                  @change="(val: string | number | boolean) => handleStatusChange(record, Boolean(val))"
                />
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.createTime')" :width="160">
              <template #cell="{ record }">
                {{ formatTime(record.createTime) }}
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="280" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-permission="PERM.system.userEdit" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</a-button>
                  <a-button v-permission="PERM.system.userEdit" size="small" @click="handleAssignRole(record)">{{ $t('user.assignRole') }}</a-button>
                  <a-button size="small" @click="handleAssignDept(record)">{{ $t('user.assignDept') }}</a-button>
                  <a-button size="small" @click="handleResetPwd(record)">{{ $t('user.resetPwd') }}</a-button>
                  <a-popconfirm v-permission="PERM.system.userDelete" :content="$t('user.deleteConfirm')" @ok="handleDelete(record)">
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
      :title="formType === 'add' ? $t('user.addUser') : $t('user.editUser')"
      :width="640"
      :mask-closable="false"
      @ok="onFormConfirm"
      @cancel="formDialogVisible = false"
    >
      <a-form :model="formData" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="userName" :label="$t('user.userName')" :rules="[{ required: true, message: $t('user.userName') + $t('validation.required', { field: '' }) }]">
              <a-input v-model="formData.userName" :placeholder="$t('user.userName')" :disabled="formType === 'edit'" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="nickName" :label="$t('user.nickName')" :rules="[{ required: true, message: $t('user.nickName') }]">
              <a-input v-model="formData.nickName" :placeholder="$t('user.nickName')" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="formType === 'add'" field="password" :label="$t('login.password')" :rules="[{ required: true, message: $t('login.passwordRequired') }]">
          <a-input-password v-model="formData.password" :placeholder="$t('login.passwordRequired')" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="phone" :label="$t('user.phone')">
              <a-input v-model="formData.phone" :placeholder="$t('user.phone')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="email" :label="$t('user.email')">
              <a-input v-model="formData.email" :placeholder="$t('user.email')" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="sex" :label="$t('user.sex')">
              <a-radio-group v-model="formData.sex">
                <a-radio value="0">{{ $t('user.male') }}</a-radio>
                <a-radio value="1">{{ $t('user.female') }}</a-radio>
                <a-radio value="2">{{ $t('user.unknown') }}</a-radio>
              </a-radio-group>
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

    <!-- 分配角色弹窗 -->
    <a-modal
      v-model:visible="roleDialogVisible"
      :title="$t('user.assignRole')"
      :width="480"
      :mask-closable="false"
      @ok="onRoleConfirm"
      @cancel="roleDialogVisible = false"
    >
      <div v-if="currentUser" class="mb-3">
        <a-space>
          <span style="color: #86909C">{{ $t('common.currentUser') }}</span>
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
      :title="$t('user.resetPwd')"
      :width="400"
      :mask-closable="false"
      @ok="onResetPwdConfirm"
      @cancel="resetPwdVisible = false"
    >
      <div v-if="currentUser" class="mb-3">
        <a-space>
          <span style="color: #86909C">{{ $t('common.currentUser') }}</span>
          <span style="font-weight: 500">{{ currentUser.nickName }}（{{ currentUser.userName }}）</span>
        </a-space>
      </div>
      <a-form :model="resetPwdData" layout="vertical">
        <a-form-item field="password" :label="$t('user.newPassword')" :rules="[{ required: true, message: $t('user.newPassword') + $t('validation.required', { field: '' }) }]">
          <a-input-password v-model="resetPwdData.password" :placeholder="$t('user.newPassword')" />
        </a-form-item>
        <div v-if="resetPwdData.password" class="mb-4 rounded-md bg-[var(--muted)] p-3 text-xs text-[var(--muted-foreground)]">
          <p class="mb-1 font-medium">{{ $t('user.pwdPolicyTitle') }}</p>
          <p v-for="rule in passwordRules" :key="rule.label">
            <span :class="rule.valid ? 'text-[var(--bz-success)]' : 'text-[var(--muted-foreground)]'">
              {{ rule.valid ? '已满足' : '未满足' }} {{ rule.label }}
            </span>
          </p>
        </div>
      </a-form>
    </a-modal>

    <!-- 批量创建账号弹窗 -->
    <a-modal
      v-model:visible="batchVisible"
      :title="$t('user.batchAdd')"
      :width="640"
      @ok="handleBatchAdd"
      @cancel="batchVisible = false"
      :ok-loading="batchLoading"
    >
      <div class="mb-3 text-sm text-[var(--muted-foreground)]">{{ $t('user.batchTip') }}</div>
      <a-textarea
        v-model="batchText"
        :placeholder="$t('user.batchPlaceholder')"
        :auto-size="{ minRows: 8, maxRows: 16 }"
      />
      <div v-if="batchError" class="mt-3 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">{{ batchError }}</div>
      <div class="mt-3 rounded-md bg-[var(--muted)] p-3 text-xs text-[var(--muted-foreground)]">
        <p class="mb-1 font-medium">{{ $t('user.pwdPolicyTitle') }}</p>
        <p v-for="rule in batchPasswordRules" :key="rule.label" :class="rule.valid ? 'text-[var(--bz-success)]' : 'text-[var(--muted-foreground)]'">
          {{ rule.valid ? '已满足' : '未满足' }} {{ rule.label }}
        </p>
      </div>
    </a-modal>

    <!-- 分配部门弹窗 -->
    <a-modal
      v-model:visible="deptDialogVisible"
      :title="$t('user.assignDept')"
      :width="560"
      :mask-closable="false"
      @ok="onDeptConfirm"
      @cancel="deptDialogVisible = false"
    >
      <div v-if="currentUser" class="mb-3">
        <a-space>
          <span style="color: #86909C">{{ $t('common.currentUser') }}</span>
          <span style="font-weight: 500">{{ currentUser.nickName }}（{{ currentUser.userName }}）</span>
        </a-space>
      </div>
      <a-empty v-if="deptFlat.length === 0" :description="$t('common.noDeptData')" />
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
            @change="(v: string | number | boolean | Record<string, any> | (string | number | boolean | Record<string, any>)[]) => onDeptRoleChange(item.node.id, String(v))"
          >
            <a-option value="0">{{ $t('user.normalUser') }}</a-option>
            <a-option value="1">{{ $t('user.adminUser') }}</a-option>
          </a-select>
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
} from '@arco-design/web-vue/es/icon'
import {
  getUserListApi,
  addUserApi,
  updateUserApi,
  deleteUserApi,
  batchAddUsersApi,
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

const { t } = useI18n()

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
    const res = await getUserListApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      userName: searchForm.userName || undefined,
      phone: searchForm.phone || undefined,
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
async function handleStatusChange(row: SysUser & { _statusLoading?: boolean }, val: boolean) {
  const newStatus = val ? '0' : '1'
  row._statusLoading = true
  try {
    await changeUserStatusApi(row.id, newStatus)
    row.status = newStatus
    Message.success(t('user.statusSuccess'))
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
    Message.success(t('user.deleteSuccess'))
    loadTable()
  } catch {
    /* interceptor handles */
  }
}

async function handleBatchDelete() {
  Modal.confirm({
    title: t('common.confirmDelete'),
    content: t('user.batchDeleteConfirm', { count: selectedIds.value.length }),
    onOk: async () => {
      try {
        await batchDeleteUserApi(selectedIds.value)
        Message.success(t('user.batchDeleteSuccess'))
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
      Message.success(t('user.addSuccess'))
    } else {
      const updateData = { ...formData }
      if (!updateData.password) delete (updateData as Record<string, unknown>).password
      await updateUserApi(updateData)
      Message.success(t('user.editSuccess'))
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
      const allRoles: SysRole[] = rolesRes.value.data?.list || []
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
    Message.success(t('user.roleAssignSuccess'))
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
    Message.success(t('user.resetPwdSuccess'))
    resetPwdVisible.value = false
  } catch {
    /* interceptor handles */
  }
}

// ============ 密码强度规则（实时校验，仿大型网站） ============
interface PwdRule {
  label: string
  valid: boolean
}

function computePasswordRules(pwd: string): PwdRule[] {
  return [
    { label: t('user.pwdRuleLength'), valid: pwd.length >= 8 },
    { label: t('user.pwdRuleUpper'), valid: /[A-Z]/.test(pwd) },
    { label: t('user.pwdRuleLower'), valid: /[a-z]/.test(pwd) },
    { label: t('user.pwdRuleDigit'), valid: /\d/.test(pwd) },
    { label: t('user.pwdRuleSpecial'), valid: /[^A-Za-z0-9]/.test(pwd) },
  ]
}

const passwordRules = computed(() => computePasswordRules(resetPwdData.password))

// ============ 批量创建账号 ============
const batchVisible = ref(false)
const batchLoading = ref(false)
const batchText = ref('')
const batchError = ref('')

/** 批量密码规则：默认取第一行解析出的密码（若输入为空则全不满足） */
const batchPasswordRules = computed(() => {
  const first = parseBatchLines()[0]?.password || ''
  return computePasswordRules(first)
})

interface BatchLine {
  userName: string
  password: string
}

/** 解析批量输入：每行 "用户名,密码"（支持中英文逗号、空格/制表符分隔） */
function parseBatchLines(): BatchLine[] {
  const lines: BatchLine[] = []
  for (const raw of batchText.value.split('\n')) {
    const line = raw.trim()
    if (!line) continue
    const parts = line.split(/[,，\s\t]+/).filter(Boolean)
    if (parts.length >= 2) {
      lines.push({ userName: parts[0].trim(), password: parts[1].trim() })
    }
  }
  return lines
}

async function handleBatchAdd() {
  batchError.value = ''
  const lines = parseBatchLines()
  if (!lines.length) {
    batchError.value = t('user.batchEmptyError')
    return
  }
  // 前端先做一次强密码策略校验，给出友好提示
  const invalid = lines.find((l) => computePasswordRules(l.password).some((r) => !r.valid))
  if (invalid) {
    batchError.value = t('user.batchPwdError', { name: invalid.userName })
    return
  }
  batchLoading.value = true
  try {
    const created = await batchAddUsersApi(lines.map((l) => ({ userName: l.userName, password: l.password })))
    Message.success(t('user.batchAddSuccess', { count: String(created ?? lines.length) }))
    batchVisible.value = false
    batchText.value = ''
    loadTable()
  } catch {
    /* interceptor handles */
  } finally {
    batchLoading.value = false
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
    Message.success(t('user.deptAssignSuccess'))
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
