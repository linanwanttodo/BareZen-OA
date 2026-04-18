<template>
  <div class="page-container">
    <Card>
      <CardHeader class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <CardTitle>在线用户</CardTitle>
          <Badge variant="default">{{ onlineCount }} 人在线</Badge>
        </div>
        <div class="flex gap-2">
          <Button variant="outline" @click="loadTable">
            <RefreshCw :size="16" class="mr-1.5" />
            刷新
          </Button>
          <Button variant="destructive" :disabled="!selectedTokens.length" @click="handleBatchForceLogout">
            <Power :size="16" class="mr-1.5" />
            批量下线
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <Empty v-if="loading" description="加载中" />
        <template v-else>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead class="w-[50px]">
                  <Checkbox :model-value="allSelected" @change="toggleSelectAll" />
                </TableHead>
                <TableHead>用户ID</TableHead>
                <TableHead>用户</TableHead>
                <TableHead>登录IP</TableHead>
                <TableHead>登录地点</TableHead>
                <TableHead>浏览器</TableHead>
                <TableHead>操作系统</TableHead>
                <TableHead>登录时间</TableHead>
                <TableHead class="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="row in tableData" :key="row.token">
                <TableCell>
                  <Checkbox :model-value="selectedTokens.includes(row.token)" @change="() => toggleRow(row.token)" />
                </TableCell>
                <TableCell>{{ row.userId }}</TableCell>
                <TableCell>
                  <div class="flex items-center gap-2">
                    <Avatar :fallback="row.nickName?.charAt(0) || '?'" size="sm" />
                    <div class="flex flex-col">
                      <span class="text-sm font-medium text-[var(--foreground)]">{{ row.nickName }}</span>
                      <span class="text-xs text-[var(--muted-foreground)]">{{ row.userName }}</span>
                    </div>
                  </div>
                </TableCell>
                <TableCell><span class="mono-text">{{ row.loginIp }}</span></TableCell>
                <TableCell>{{ row.loginLocation }}</TableCell>
                <TableCell>{{ row.browser }}</TableCell>
                <TableCell>{{ row.os }}</TableCell>
                <TableCell>{{ formatTime(row.loginTime) }}</TableCell>
                <TableCell>
                  <div class="flex justify-end">
                    <Button variant="ghost" size="sm" @click="handleForceLogout(row)">强制下线</Button>
                  </div>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>

          <div v-if="pagination.total > pagination.pageSize" class="mt-4 flex justify-end">
            <Pagination
              v-model="pagination.current"
              :total="pagination.total"
              :page-size="pagination.pageSize"
              @change="onPageChange"
            />
          </div>
        </template>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { RefreshCw, Power } from 'lucide-vue-next'
import {
  getOnlineUserListApi,
  getOnlineUserCountApi,
  forceLogoutApi,
  type OnlineUser,
} from '@/api/online'
import { Toast } from '@/utils/toast'
import Card from '@/components/ui/card/Card.vue'
import CardHeader from '@/components/ui/card/CardHeader.vue'
import CardTitle from '@/components/ui/card/CardTitle.vue'
import CardContent from '@/components/ui/card/CardContent.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import Button from '@/components/ui/button/Button.vue'
import Checkbox from '@/components/ui/checkbox/Checkbox.vue'
import Avatar from '@/components/ui/avatar/Avatar.vue'
import Table from '@/components/ui/table/Table.vue'
import TableHeader from '@/components/ui/table/TableHeader.vue'
import TableBody from '@/components/ui/table/TableBody.vue'
import TableRow from '@/components/ui/table/TableRow.vue'
import TableHead from '@/components/ui/table/TableHead.vue'
import TableCell from '@/components/ui/table/TableCell.vue'
import Pagination from '@/components/ui/pagination/Pagination.vue'
import Empty from '@/components/ui/empty/Empty.vue'

// ============ 表格 ============
const loading = ref(false)
const tableData = ref<OnlineUser[]>([])
const selectedTokens = ref<string[]>([])
const onlineCount = ref(0)

const allSelected = computed(
  () => tableData.value.length > 0 && tableData.value.every((r) => selectedTokens.value.includes(r.token)),
)

function toggleRow(token: string) {
  const idx = selectedTokens.value.indexOf(token)
  if (idx >= 0) selectedTokens.value.splice(idx, 1)
  else selectedTokens.value.push(token)
}

function toggleSelectAll() {
  if (allSelected.value) selectedTokens.value = []
  else selectedTokens.value = tableData.value.map((r) => r.token)
}

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

function onPageChange(page: number) {
  pagination.current = page
  loadTable()
}

function formatTime(time: string) {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

async function loadTable() {
  loading.value = true
  try {
    const [listRes, countRes] = await Promise.allSettled([
      getOnlineUserListApi({ pageNum: pagination.current, pageSize: pagination.pageSize }),
      getOnlineUserCountApi(),
    ])

    if (listRes.status === 'fulfilled') {
      tableData.value = listRes.value.data?.records || []
      pagination.total = listRes.value.data?.total || 0
    }
    if (countRes.status === 'fulfilled') {
      onlineCount.value = countRes.value.data ?? 0
    }
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

// ============ 强制下线 ============
async function handleForceLogout(row: OnlineUser) {
  if (!confirm(`确认强制下线用户【${row.nickName}】吗？`)) return
  try {
    await forceLogoutApi(row.token)
    Toast.success(`已强制下线用户【${row.nickName}】`)
    loadTable()
  } catch {
    // ignore
  }
}

async function handleBatchForceLogout() {
  if (!confirm(`确认强制下线选中的 ${selectedTokens.value.length} 个用户吗？`)) return
  try {
    await Promise.all(selectedTokens.value.map((token) => forceLogoutApi(token)))
    Toast.success(`已强制下线 ${selectedTokens.value.length} 个用户`)
    selectedTokens.value = []
    loadTable()
  } catch {
    // ignore
  }
}

// ============ 初始化 ============
onMounted(() => {
  loadTable()
})
</script>

<style scoped>
.mono-text {
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
}
</style>
