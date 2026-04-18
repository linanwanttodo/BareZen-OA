<template>
  <div class="role-dept-layout">
    <!-- 左侧筛选侧边栏 -->
    <Card class="filter-sidebar" :style="{ width: '280px', minWidth: '280px' }">
      <CardHeader class="flex items-center justify-between">
        <CardTitle>筛选条件</CardTitle>
        <Button variant="ghost" size="sm" @click="resetFilters">重置</Button>
      </CardHeader>
      <CardContent>
        <!-- 角色筛选 -->
        <div class="filter-section">
          <div class="filter-title">
            <Users :size="16" />
            <span>角色筛选</span>
            <Badge v-if="selectedRoleIds.length" variant="default">{{ selectedRoleIds.length }}</Badge>
          </div>
          <div class="filter-list">
            <label v-for="role in roleList" :key="role.id" class="filter-item flex items-center gap-2">
              <Checkbox :model-value="selectedRoleIds.includes(String(role.id))" @change="() => toggleRole(String(role.id))" />
              <span class="filter-label">{{ role.roleName }}</span>
              <Badge :variant="role.status === '0' ? 'default' : 'destructive'">{{ role.status === '0' ? '启用' : '停用' }}</Badge>
            </label>
            <div v-if="!roleLoading && roleList.length === 0" class="empty-tip">暂无角色数据</div>
          </div>
        </div>

        <Separator class="my-4" />

        <!-- 部门筛选 -->
        <div class="filter-section">
          <div class="filter-title">
            <Network :size="16" />
            <span>部门筛选</span>
            <Badge v-if="selectedDeptIds.length" variant="secondary">{{ selectedDeptIds.length }}</Badge>
          </div>
          <div class="filter-list">
            <div v-for="item in deptFlat" :key="item.node.id" class="flex items-center gap-2 py-1" :style="{ paddingLeft: item.depth * 16 + 'px' }">
              <Checkbox :model-value="selectedDeptIds.includes(item.node.id)" @change="() => toggleDept(item.node.id)" />
              <span class="text-sm text-[var(--foreground)]">{{ item.node.deptName }}</span>
            </div>
            <div v-if="!deptLoading && deptTree.length === 0" class="empty-tip">暂无部门数据</div>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 右侧用户列表 -->
    <Card class="user-content">
      <CardHeader class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <CardTitle>用户列表</CardTitle>
          <span class="stat-badge">
            共 <b>{{ total }}</b> 位用户
            <template v-if="selectedRoleIds.length || selectedDeptIds.length">（已筛选）</template>
          </span>
        </div>
        <Input
          :model-value="searchKeyword"
          placeholder="搜索用户名..."
          class="w-56"
          @update:model-value="(v) => { searchKeyword = v; onSearchInput() }"
        />
      </CardHeader>
      <CardContent>
        <Empty v-if="tableLoading" description="加载中" />
        <template v-else-if="userList.length">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>用户名</TableHead>
                <TableHead>昵称</TableHead>
                <TableHead>邮箱</TableHead>
                <TableHead>手机号</TableHead>
                <TableHead class="text-center">在线</TableHead>
                <TableHead class="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="row in userList" :key="row.id">
                <TableCell>{{ row.userName }}</TableCell>
                <TableCell>{{ row.nickName }}</TableCell>
                <TableCell>{{ row.email }}</TableCell>
                <TableCell>{{ row.phone }}</TableCell>
                <TableCell>
                  <span class="online-indicator" :class="isOnline(row.loginDate) ? 'online' : 'offline'">
                    <span class="dot"></span>
                    {{ isOnline(row.loginDate) ? '在线' : '离线' }}
                  </span>
                </TableCell>
                <TableCell>
                  <div class="flex justify-end gap-1">
                    <Button variant="ghost" size="sm" @click="openRoleDialog(row)">修改角色</Button>
                    <Button variant="ghost" size="sm" @click="openDeptDialog(row)">修改部门</Button>
                  </div>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </template>
        <Empty v-else description="暂无匹配的用户" />

        <div v-if="total > 0" class="pagination-bar">
          <Pagination v-model="pageNum" :total="total" :page-size="pageSize" @change="onPaginationChange" />
        </div>
      </CardContent>
    </Card>

    <!-- 分配角色对话框 -->
    <Dialog :open="roleDialogVisible" title="修改角色权限" @update:open="roleDialogVisible = $event">
      <div class="dialog-body">
        <dl class="mb-3 grid grid-cols-2 gap-4">
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">用户名</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ currentUser?.userName }}</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">昵称</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ currentUser?.nickName }}</dd>
          </div>
        </dl>
        <Separator class="mb-3" />
        <div class="role-checkbox-area">
          <label v-for="role in roleList" :key="role.id" class="role-checkbox-item flex items-center gap-2">
            <Checkbox :model-value="dialogRoleIds.includes(String(role.id))" @change="() => toggleDialogRole(String(role.id))" />
            <span>{{ role.roleName }}</span>
            <Badge variant="outline" class="role-key-tag">{{ role.roleKey }}</Badge>
          </label>
        </div>
      </div>
      <template #footer>
        <Button variant="outline" @click="roleDialogVisible = false">取消</Button>
        <Button :disabled="saving" @click="saveUserRoles">保存</Button>
      </template>
    </Dialog>

    <!-- 分配部门对话框 -->
    <Dialog :open="deptDialogVisible" title="修改部门分配" @update:open="deptDialogVisible = $event">
      <div class="dialog-body">
        <dl class="mb-3 grid grid-cols-2 gap-4">
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">用户名</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ currentUser?.userName }}</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">昵称</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ currentUser?.nickName }}</dd>
          </div>
        </dl>
        <Separator class="mb-3" />
        <Alert variant="info" class="mb-3">勾选部门即分配</Alert>
        <div class="dept-tree-area">
          <div v-for="item in deptFlat" :key="item.node.id" class="flex items-center gap-2 py-1" :style="{ paddingLeft: item.depth * 16 + 'px' }">
            <Checkbox :model-value="dialogDeptIds.includes(item.node.id)" @change="() => toggleDialogDept(item.node.id)" />
            <span class="text-sm text-[var(--foreground)]">{{ item.node.deptName }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <Button variant="outline" @click="deptDialogVisible = false">取消</Button>
        <Button :disabled="saving" @click="saveUserDepts">保存</Button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Users, Network } from 'lucide-vue-next'
import { filterUsersApi, getUserRoleIdsApi, assignUserRolesApi } from '@/api/user'
import { getAllRolesListApi } from '@/api/role'
import { getDeptTreeApi, getUserDeptsApi, assignUserDeptsApi } from '@/api/dept'
import type { SysUser } from '@/api/user'
import type { SysRole } from '@/api/role'
import type { SysDept, SysUserDept } from '@/api/dept'
import { Toast } from '@/utils/toast'
import Card from '@/components/ui/card/Card.vue'
import CardHeader from '@/components/ui/card/CardHeader.vue'
import CardTitle from '@/components/ui/card/CardTitle.vue'
import CardContent from '@/components/ui/card/CardContent.vue'
import Input from '@/components/ui/input/Input.vue'
import Button from '@/components/ui/button/Button.vue'
import Checkbox from '@/components/ui/checkbox/Checkbox.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import Table from '@/components/ui/table/Table.vue'
import TableHeader from '@/components/ui/table/TableHeader.vue'
import TableBody from '@/components/ui/table/TableBody.vue'
import TableRow from '@/components/ui/table/TableRow.vue'
import TableHead from '@/components/ui/table/TableHead.vue'
import TableCell from '@/components/ui/table/TableCell.vue'
import Pagination from '@/components/ui/pagination/Pagination.vue'
import Empty from '@/components/ui/empty/Empty.vue'
import Separator from '@/components/ui/separator/Separator.vue'
import Alert from '@/components/ui/alert/Alert.vue'
import Dialog from '@/components/ui/dialog/Dialog.vue'

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
    roleList.value = Array.isArray(res.data) ? res.data : res.data?.records || []
  } catch {
    Toast.error('加载角色列表失败')
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
    Toast.error('加载部门列表失败')
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
    userList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    Toast.error('加载用户列表失败')
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
    Toast.error('获取用户角色失败')
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
    Toast.success('角色分配成功')
    roleDialogVisible.value = false
    fetchUsers()
  } catch {
    Toast.error('角色分配失败')
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
    Toast.error('获取用户部门失败')
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
    Toast.success('部门分配成功')
    deptDialogVisible.value = false
    fetchUsers()
  } catch {
    Toast.error('部门分配失败')
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
  color: #00B42A;
}
.online-indicator.online .dot {
  background: #00B42A;
}
.online-indicator.offline {
  color: #86909C;
}
.online-indicator.offline .dot {
  background: #C9CDD4;
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
