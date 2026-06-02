<template>
  <div class="role-dept-layout">
    <!-- 左侧筛选侧边栏 -->
    <t-card class="filter-sidebar" :style="{ width: '280px', minWidth: '280px' }">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('roleDept.filterTitle') }}</span>
          <t-button type="text" size="small" @click="resetFilters">{{ $t('common.reset') }}</t-button>
        </div>
      </template>
        <!-- 角色筛选 -->
        <div class="filter-section">
          <div class="filter-title">
            <icon-user-group size="16px" />
            <span>{{ $t('roleDept.roleFilter') }}</span>
            <t-tag v-if="selectedRoleIds.length" color="arcoblue">{{ selectedRoleIds.length }}</t-tag>
          </div>
          <div class="filter-list">
            <label v-for="role in roleList" :key="role.id" class="filter-item flex items-center gap-2">
              <t-checkbox :model-value="selectedRoleIds.includes(String(role.id))" @change="() => toggleRole(String(role.id))" />
              <span class="filter-label">{{ role.roleName }}</span>
              <t-tag :color="role.status === '0' ? 'arcoblue' : 'red'">{{ role.status === '0' ? $t('common.enable') : $t('common.disable') }}</t-tag>
            </label>
            <div v-if="!roleLoading && roleList.length === 0" class="empty-tip">{{ $t('roleDept.noRoleData') }}</div>
          </div>
        </div>

        <t-divider class="my-4" />

        <!-- 部门筛选 -->
        <div class="filter-section">
          <div class="filter-title">
            <icon-branch size="16px" />
            <span>{{ $t('roleDept.deptFilter') }}</span>
            <t-tag v-if="selectedDeptIds.length" color="arcoblue">{{ selectedDeptIds.length }}</t-tag>
          </div>
          <div class="filter-list">
            <div v-for="item in deptFlat" :key="item.node.id" class="flex items-center gap-2 py-1" :style="{ paddingLeft: item.depth * 16 + 'px' }">
              <t-checkbox :model-value="selectedDeptIds.includes(item.node.id)" @change="() => toggleDept(item.node.id)" />
              <span class="text-sm text-[var(--foreground)]">{{ item.node.deptName }}</span>
            </div>
            <div v-if="!deptLoading && deptTree.length === 0" class="empty-tip">{{ $t('common.noDeptData') }}</div>
          </div>
        </div>
    </t-card>

    <!-- 右侧用户列表 -->
    <t-card class="user-content">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <div class="flex items-center gap-3">
            <span>{{ $t('user.title') }}</span>
            <span class="stat-badge">
              {{ $t('roleDept.totalUserCount', { count: total }) }}
              <template v-if="selectedRoleIds.length || selectedDeptIds.length">{{ $t('roleDept.filtered') }}</template>
            </span>
          </div>
          <t-input
            v-model="searchKeyword"
            :placeholder="$t('roleDept.searchPlaceholder')"
            style="width: 224px"
            @input="onSearchInput"
          />
        </div>
      </template>
        <t-empty v-if="tableLoading" :description="$t('common.loading')" />
        <template v-else-if="userList.length">
            <t-table row-key="id" :data="userList" :columns="tableColumns" :pagination="false">
    <template #loginDate="{ row }">
                  <span class="online-indicator" :class="isOnline(row.loginDate) ? 'online' : 'offline'">
                    <span class="dot"></span>
                    {{ isOnline(row.loginDate) ? $t('common.online') : $t('common.offline') }}
                  </span>
                </template>
    <template #col_5="{ row }">
                  <t-space>
                    <t-button type="text" size="small" @click="openRoleDialog(row)">{{ $t('roleDept.modifyRoleAction') }}</t-button>
                    <t-button type="text" size="small" @click="openDeptDialog(row)">{{ $t('roleDept.modifyDeptAction') }}</t-button>
                  </t-space>
                </template>
  </t-table>
        </template>
        <t-empty v-else :description="$t('roleDept.noUserData')" />

        <div v-if="total > 0" class="pagination-bar">
          <t-pagination v-model:current="pageNum" :total="total" :page-size="pageSize" @change="onPaginationChange" />
        </div>
    </t-card>

    <!-- 分配角色对话框 -->
    <t-dialog v-model="roleDialogVisible" :header="$t('roleDept.modifyRole')" @close="roleDialogVisible = false">
      <div class="dialog-body">
        <dl class="mb-3 grid grid-cols-2 gap-4">
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('user.userName') }}</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ currentUser?.userName }}</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('user.nickName') }}</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ currentUser?.nickName }}</dd>
          </div>
        </dl>
        <t-divider class="mb-3" />
        <div class="role-checkbox-area">
          <label v-for="role in roleList" :key="role.id" class="role-checkbox-item flex items-center gap-2">
            <t-checkbox :model-value="dialogRoleIds.includes(String(role.id))" @change="() => toggleDialogRole(String(role.id))" />
            <span>{{ role.roleName }}</span>
            <t-tag bordered class="role-key-tag">{{ role.roleKey }}</t-tag>
          </label>
        </div>
      </div>
      <template #footer>
        <t-button @click="roleDialogVisible = false">{{ $t('common.cancel') }}</t-button>
        <t-button theme="primary" :disabled="saving" @click="saveUserRoles">{{ $t('common.save') }}</t-button>
      </template>
    </t-dialog>

    <!-- 分配部门对话框 -->
    <t-dialog v-model="deptDialogVisible" :header="$t('roleDept.modifyDept')" @close="deptDialogVisible = false">
      <div class="dialog-body">
        <dl class="mb-3 grid grid-cols-2 gap-4">
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('user.userName') }}</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ currentUser?.userName }}</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">{{ $t('user.nickName') }}</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ currentUser?.nickName }}</dd>
          </div>
        </dl>
        <t-divider class="mb-3" />
        <t-alert theme="info" class="mb-3">{{ $t('roleDept.checkDeptAssign') }}</t-alert>
        <div class="dept-tree-area">
          <div v-for="item in deptFlat" :key="item.node.id" class="flex items-center gap-2 py-1" :style="{ paddingLeft: item.depth * 16 + 'px' }">
            <t-checkbox :model-value="dialogDeptIds.includes(item.node.id)" @change="() => toggleDialogDept(item.node.id)" />
            <span class="text-sm text-[var(--foreground)]">{{ item.node.deptName }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <t-button @click="deptDialogVisible = false">{{ $t('common.cancel') }}</t-button>
        <t-button theme="primary" :disabled="saving" @click="saveUserDepts">{{ $t('common.save') }}</t-button>
      </template>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { filterUsersApi, getUserRoleIdsApi, assignUserRolesApi } from '@/api/user'
import { getAllRolesListApi } from '@/api/role'
import { getDeptTreeApi, getUserDeptsApi, assignUserDeptsApi } from '@/api/dept'
import type { SysUser } from '@/api/user'
import type { SysRole } from '@/api/role'
import type { SysDept, SysUserDept } from '@/api/dept'
import { Toast } from '@/utils/toast'

const { t } = useI18n()

// ========== 筛选状态 ==========
const roleList = ref<SysRole[]>([])
const deptTree = ref<SysDept[]>([])
const selectedRoleIds = ref<string[]>([])
const selectedDeptIds = ref<string[]>([])
const searchKeyword = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

const roleLoading = ref(false)
const deptLoading = ref(false)

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

function toggleRole(id: string) {
  const idx = selectedRoleIds.value.indexOf(id)
  if (idx >= 0) selectedRoleIds.value.splice(idx, 1)
  else selectedRoleIds.value.push(id)
  onFilterChange()
}

function toggleDept(id: string) {
  const idx = selectedDeptIds.value.indexOf(id)
  if (idx >= 0) selectedDeptIds.value.splice(idx, 1)
  else selectedDeptIds.value.push(id)
  onFilterChange()
}

// ========== 用户列表状态 ==========
const userList = ref<SysUser[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const tableLoading = ref(false)

// ========== 在线/离线判断 ==========
function isOnline(loginDate: string | null | undefined): boolean {
  if (!loginDate) return false
  const now = Date.now()
  const lastLogin = new Date(loginDate).getTime()
  return !isNaN(lastLogin) && now - lastLogin < 30 * 60 * 1000
}

// ========== 对话框状态 ==========
const roleDialogVisible = ref(false)
const currentUser = ref<SysUser | null>(null)
const dialogRoleIds = ref<string[]>([])
const saving = ref(false)

const deptDialogVisible = ref(false)
const dialogDeptIds = ref<string[]>([])

// ========== 数据加载 ==========
async function loadRoles() {
  roleLoading.value = true
  try {
    const res: any = await getAllRolesListApi()
    roleList.value = Array.isArray(res.data) ? res.data : res.data?.list || []
  } catch {
    Toast.error(t('roleDept.loadRoleFailed'))
  } finally {
    roleLoading.value = false
  }
}

async function loadDepts() {
  deptLoading.value = true
  try {
    const res = await getDeptTreeApi()
    deptTree.value = res.data || []
  } catch {
    Toast.error(t('roleDept.loadDeptFailed'))
  } finally {
    deptLoading.value = false
  }
}

async function fetchUsers() {
  tableLoading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }
    if (selectedRoleIds.value.length) params.roleIds = selectedRoleIds.value.join(',')
    if (selectedDeptIds.value.length) params.deptIds = selectedDeptIds.value.join(',')
    if (searchKeyword.value) params.userName = searchKeyword.value
    const res = await filterUsersApi(params)
    userList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    Toast.error(t('roleDept.loadUserFailed'))
  } finally {
    tableLoading.value = false
  }
}

// ========== 筛选操作 ==========
function onFilterChange() {
  pageNum.value = 1
  fetchUsers()
}

function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    pageNum.value = 1
    fetchUsers()
  }, 300)
}

function resetFilters() {
  selectedRoleIds.value = []
  selectedDeptIds.value = []
  searchKeyword.value = ''
  pageNum.value = 1
  fetchUsers()
}

// ========== 分页 ==========
function onPaginationChange() {
  fetchUsers()
}

// ========== 角色分配 ==========
async function openRoleDialog(user: SysUser) {
  currentUser.value = user
  dialogRoleIds.value = []
  roleDialogVisible.value = true
  try {
    const res = await getUserRoleIdsApi(user.id)
    dialogRoleIds.value = (res.data || []).map(String)
  } catch {
    Toast.error(t('roleDept.getUserRoleFailed'))
  }
}

function toggleDialogRole(id: string) {
  const idx = dialogRoleIds.value.indexOf(id)
  if (idx >= 0) dialogRoleIds.value.splice(idx, 1)
  else dialogRoleIds.value.push(id)
}

async function saveUserRoles() {
  if (!currentUser.value) return
  saving.value = true
  try {
    await assignUserRolesApi(currentUser.value.id, dialogRoleIds.value)
    Toast.success(t('roleDept.assignRoleSuccess'))
    roleDialogVisible.value = false
    fetchUsers()
  } catch {
    Toast.error(t('roleDept.assignRoleFailed'))
  } finally {
    saving.value = false
  }
}

// ========== 部门分配 ==========
async function openDeptDialog(user: SysUser) {
  currentUser.value = user
  dialogDeptIds.value = []
  deptDialogVisible.value = true
  try {
    const res = await getUserDeptsApi(user.id)
    dialogDeptIds.value = (res.data || []).map((d: any) => String(d.deptId))
  } catch {
    Toast.error(t('roleDept.getUserDeptFailed'))
  }
}

function toggleDialogDept(id: string) {
  const idx = dialogDeptIds.value.indexOf(id)
  if (idx >= 0) dialogDeptIds.value.splice(idx, 1)
  else dialogDeptIds.value.push(id)
}

async function saveUserDepts() {
  if (!currentUser.value) return
  saving.value = true
  try {
    const deptList: SysUserDept[] = dialogDeptIds.value.map((deptId) => ({
      userId: currentUser.value!.id,
      deptId,
      roleType: '0',
    }))
    await assignUserDeptsApi(currentUser.value.id, deptList)
    Toast.success(t('roleDept.assignDeptSuccess'))
    deptDialogVisible.value = false
    fetchUsers()
  } catch {
    Toast.error(t('roleDept.assignDeptFailed'))
  } finally {
    saving.value = false
  }
}

// ========== 初始化 ==========
onMounted(() => {
  loadRoles()
  loadDepts()
  fetchUsers()
})

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'userName', title: t('user.userName') },
    { colKey: 'nickName', title: t('user.nickName') },
    { colKey: 'email', title: t('user.email') },
    { colKey: 'phone', title: t('user.phone') },
    { colKey: 'loginDate', title: t('common.status'), align: 'center' },
    { colKey: 'col_5', title: t('common.operate'), align: 'right' },
  ])

</script>

<style scoped>
.role-dept-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  height: 100%;
  min-height: calc(100vh - 120px);
}

.filter-sidebar {
  align-self: stretch;
}

.filter-section {
  padding-top: 4px;
}

.filter-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--muted-foreground);
  margin-bottom: 10px;
}

.filter-list {
  max-height: 280px;
  overflow-y: auto;
}

.filter-item {
  padding: 4px 2px;
  border-radius: 4px;
}

.filter-label {
  margin-right: 6px;
}

.empty-tip {
  color: var(--muted-foreground);
  font-size: 13px;
  text-align: center;
  padding: 20px 0;
}

.user-content {
  flex: 1;
  overflow: hidden;
}

.stat-badge {
  font-size: 13px;
  color: var(--muted-foreground);
}
.stat-badge b {
  color: var(--primary);
  font-size: 15px;
}

.online-indicator {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
}
.online-indicator .dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.online-indicator.online {
  color: var(--wecom-success);
}
.online-indicator.online .dot {
  background: var(--wecom-success);
}
.online-indicator.offline {
  color: var(--wecom-text-tertiary);
}
.online-indicator.offline .dot {
  background: var(--wecom-text-disabled);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 0;
  border-top: 1px solid var(--border);
  margin-top: 16px;
}

.dialog-body {
  max-height: 520px;
  overflow-y: auto;
}

.role-checkbox-item {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  margin-bottom: 8px;
}
.role-checkbox-item:last-child {
  margin-bottom: 0;
}
.role-key-tag {
  margin-left: 8px;
}

.dept-tree-area {
  display: block;
  max-height: 340px;
  overflow-y: auto;
}
</style>
