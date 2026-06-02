<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4" :bordered="true">
      <t-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <t-form-item field="userName" :label="$t('user.userName')">
          <t-input v-model="searchForm.userName" :placeholder="$t('user.userName')" allow-clear style="width: 180px" />
        </t-form-item>
        <t-form-item field="phone" :label="$t('user.phone')">
          <t-input v-model="searchForm.phone" :placeholder="$t('user.phone')" allow-clear style="width: 180px" />
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
          <span>{{ $t('user.userManagement') }}</span>
          <t-space>
            <t-button v-permission="PERM.system.userAdd" theme="primary" @click="handleAdd">
              <template #icon><icon-plus /></template>
              {{ $t('user.addUser') }}
            </t-button>
            <t-button v-permission="PERM.system.userAdd" @click="batchVisible = true">
              <template #icon><icon-user-group /></template>
              {{ $t('user.batchAdd') }}
            </t-button>
            <t-button v-permission="PERM.system.userDelete" theme="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">
              <template #icon><icon-delete /></template>
              {{ $t('user.batchDelete') }}
            </t-button>
          </t-space>
        </div>
      </template>

      <t-loading :loading="loading">
          <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
    <template #sex="{ row }">
                {{ row.sex === '0' ? $t('user.male') : row.sex === '1' ? $t('user.female') : $t('user.unknown') }}
              </template>
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
    <template #col_8="{ row }">
                <t-space>
                  <t-button v-permission="PERM.system.userEdit" size="small" @click="handleEdit(row)">{{ $t('common.edit') }}</t-button>
                  <t-button v-permission="PERM.system.userEdit" size="small" @click="handleAssignRole(row)">{{ $t('user.assignRole') }}</t-button>
                  <t-button size="small" @click="handleAssignDept(row)">{{ $t('user.assignDept') }}</t-button>
                  <t-button size="small" @click="handleResetPwd(row)">{{ $t('user.resetPwd') }}</t-button>
                  <t-popconfirm v-permission="PERM.system.userDelete" :content="$t('user.deleteConfirm')" @confirm="handleDelete(row)">
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
      :title="formType === 'add' ? $t('user.addUser') : $t('user.editUser')"
      :width="640"
      :mask-closable="false"
      @confirm="onFormConfirm"
      @cancel="formDialogVisible = false"
    >
      <t-form :model="formData" layout="vertical">
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item field="userName" :label="$t('user.userName')" :rules="[{ required: true, message: $t('user.userName') + $t('validation.required', { field: '' }) }]">
              <t-input v-model="formData.userName" :placeholder="$t('user.userName')" :disabled="formType === 'edit'" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="nickName" :label="$t('user.nickName')" :rules="[{ required: true, message: $t('user.nickName') }]">
              <t-input v-model="formData.nickName" :placeholder="$t('user.nickName')" />
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item v-if="formType === 'add'" field="password" :label="$t('login.password')" :rules="[{ required: true, message: $t('login.passwordRequired') }]">
          <t-input v-model="formData.password" :placeholder="$t('login.passwordRequired')" />
        </t-form-item>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item field="phone" :label="$t('user.phone')">
              <t-input v-model="formData.phone" :placeholder="$t('user.phone')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="email" :label="$t('user.email')">
              <t-input v-model="formData.email" :placeholder="$t('user.email')" />
            </t-form-item>
          </t-col>
        </t-row>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item field="sex" :label="$t('user.sex')">
              <t-radio-group v-model="formData.sex">
                <t-radio value="0">{{ $t('user.male') }}</t-radio>
                <t-radio value="1">{{ $t('user.female') }}</t-radio>
                <t-radio value="2">{{ $t('user.unknown') }}</t-radio>
              </t-radio-group>
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

    <!-- 分配角色弹窗 -->
    <t-dialog
      v-model="roleDialogVisible"
      :title="$t('user.assignRole')"
      :width="480"
      :mask-closable="false"
      @confirm="onRoleConfirm"
      @cancel="roleDialogVisible = false"
    >
      <div v-if="currentUser" class="mb-3">
        <t-space>
          <span style="color: var(--wecom-text-tertiary)">{{ $t('common.currentUser') }}</span>
          <span style="font-weight: 500">{{ currentUser.nickName }}（{{ currentUser.userName }}）</span>
        </t-space>
      </div>
      <t-checkbox-group v-model="selectedRoleIds" direction="vertical">
        <t-checkbox v-for="opt in roleOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </t-checkbox>
      </t-checkbox-group>
    </t-dialog>

    <!-- 重置密码弹窗 -->
    <t-dialog
      v-model="resetPwdVisible"
      :title="$t('user.resetPwd')"
      :width="400"
      :mask-closable="false"
      @confirm="onResetPwdConfirm"
      @cancel="resetPwdVisible = false"
    >
      <div v-if="currentUser" class="mb-3">
        <t-space>
          <span style="color: var(--wecom-text-tertiary)">{{ $t('common.currentUser') }}</span>
          <span style="font-weight: 500">{{ currentUser.nickName }}（{{ currentUser.userName }}）</span>
        </t-space>
      </div>
      <t-form :model="resetPwdData" layout="vertical">
        <t-form-item field="password" :label="$t('user.newPassword')" :rules="[{ required: true, message: $t('user.newPassword') + $t('validation.required', { field: '' }) }]">
          <t-input v-model="resetPwdData.password" :placeholder="$t('user.newPassword')" />
        </t-form-item>
        <div v-if="resetPwdData.password" class="mb-4 rounded-wecom-md bg-[var(--wecom-bg-container)] p-3 text-xs">
          <p class="mb-1 font-medium">{{ $t('user.pwdPolicyTitle') }}</p>
          <p v-for="rule in passwordRules" :key="rule.label" class="flex items-center gap-1.5" :class="rule.valid ? 'text-[var(--wecom-success)]' : 'text-[var(--wecom-text-tertiary)]'">
            <icon-check v-if="rule.valid" size="12px" />
            <icon-minus v-else size="12px" />
            <span>{{ rule.label }}</span>
          </p>
        </div>
      </t-form>
    </t-dialog>

    <!-- 批量创建账号弹窗 -->
    <t-dialog
      v-model="batchVisible"
      :title="$t('user.batchAdd')"
      :width="640"
      @confirm="handleBatchAdd"
      @cancel="batchVisible = false"
      :ok-loading="batchLoading"
    >
      <div class="mb-3 text-sm text-[var(--muted-foreground)]">{{ $t('user.batchTip') }}</div>
      <t-textarea
        v-model="batchText"
        :placeholder="$t('user.batchPlaceholder')"
        :auto-size="{ minRows: 8, maxRows: 16 }"
      />
      <div v-if="batchError" class="mt-3 rounded border border-wecom-error-light bg-wecom-error-light px-3 py-2 text-sm text-wecom-error">{{ batchError }}</div>
      <div class="mt-3 rounded-wecom-md bg-[var(--wecom-bg-container)] p-3 text-xs">
        <p class="mb-1 font-medium">{{ $t('user.pwdPolicyTitle') }}</p>
        <p v-for="rule in batchPasswordRules" :key="rule.label" class="flex items-center gap-1.5" :class="rule.valid ? 'text-[var(--wecom-success)]' : 'text-[var(--wecom-text-tertiary)]'">
          <icon-check v-if="rule.valid" size="12px" />
          <icon-minus v-else size="12px" />
          <span>{{ rule.label }}</span>
        </p>
      </div>
    </t-dialog>

    <!-- 分配部门弹窗 -->
    <t-dialog
      v-model="deptDialogVisible"
      :title="$t('user.assignDept')"
      :width="560"
      :mask-closable="false"
      @confirm="onDeptConfirm"
      @cancel="deptDialogVisible = false"
    >
      <div v-if="currentUser" class="mb-3">
        <t-space>
          <span style="color: var(--wecom-text-tertiary)">{{ $t('common.currentUser') }}</span>
          <span style="font-weight: 500">{{ currentUser.nickName }}（{{ currentUser.userName }}）</span>
        </t-space>
      </div>
      <t-empty v-if="deptFlat.length === 0" :description="$t('common.noDeptData')" />
      <div v-else class="max-h-[360px] overflow-y-auto border rounded p-2" style="border-color: var(--wecom-border)">
        <div v-for="item in deptFlat" :key="item.node.id" class="flex items-center gap-2 py-1.5" :style="{ paddingLeft: item.depth * 16 + 'px' }">
          <t-checkbox :model-value="checkedDeptKeys.includes(item.node.id)" @change="() => toggleDeptCheck(item.node.id)">
            {{ item.node.deptName }}
          </t-checkbox>
          <t-select
            v-if="checkedDeptKeys.includes(item.node.id)"
            :model-value="deptRoleMap[item.node.id] || '0'"
            size="small"
            style="width: 120px"
            @change="(v: string | number | boolean | Record<string, any> | (string | number | boolean | Record<string, any>)[]) => onDeptRoleChange(item.node.id, String(v))"
          >
            <t-option value="0">{{ $t('user.normalUser') }}</t-option>
            <t-option value="1">{{ $t('user.adminUser') }}</t-option>
          </t-select>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, computed, onMounted } from 'vue'
import { MessagePlugin as Message, DialogPlugin as Modal } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
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
    header: t('common.confirmDelete'),
    body: t('user.batchDeleteConfirm', { count: selectedIds.value.length }),
    onConfirm: async () => {
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
  if (!passwordRules.value.every((r) => r.valid)) {
    Message.warning(t('user.batchPwdError', { name: currentUser.value.userName }))
    return
  }
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

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'id', title: t('common.operate'), width: 80 },
    { colKey: 'userName', title: t('user.userName') },
    { colKey: 'nickName', title: t('user.nickName') },
    { colKey: 'phone', title: t('user.phone') },
    { colKey: 'email', title: t('user.email') },
    { colKey: 'sex', title: t('user.sex'), width: 80 },
    { colKey: 'status', title: t('common.status'), width: 100 },
    { colKey: 'createTime', title: t('common.createTime'), width: 160 },
    { colKey: 'col_8', title: t('common.operate'), width: 280, align: 'right' },
  ])

</script>
