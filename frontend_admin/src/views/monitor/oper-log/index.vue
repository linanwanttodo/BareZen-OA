<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <Card class="mb-4">
      <CardContent class="pt-6">
        <div class="flex flex-wrap items-end gap-4">
          <div class="space-y-1.5">
            <Label>操作模块</Label>
            <Input v-model="searchForm.title" placeholder="请输入操作模块" class="w-44" />
          </div>
          <div class="space-y-1.5">
            <Label>操作人</Label>
            <Input v-model="searchForm.operUser" placeholder="请输入操作人" class="w-36" />
          </div>
          <div class="space-y-1.5">
            <Label>操作类型</Label>
            <Select
              :model-value="searchForm.businessType ?? ''"
              :options="bizOptions"
              placeholder="请选择"
              class="w-32"
              @change="(v) => (searchForm.businessType = v === '' ? undefined : (v as number))"
            />
          </div>
          <div class="space-y-1.5">
            <Label>状态</Label>
            <Select
              :model-value="searchForm.status ?? ''"
              :options="statusOptions"
              placeholder="请选择"
              class="w-32"
              @change="(v) => (searchForm.status = v === '' ? undefined : (v as number))"
            />
          </div>
          <div class="flex gap-2">
            <Button @click="handleSearch">搜索</Button>
            <Button variant="outline" @click="handleReset">重置</Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 表格区域 -->
    <Card>
      <CardHeader class="flex items-center justify-between">
        <CardTitle>操作日志</CardTitle>
        <Button variant="outline" @click="loadTable">
          <RefreshCw :size="16" class="mr-1.5" />
          刷新
        </Button>
      </CardHeader>
      <CardContent>
        <Empty v-if="loading" description="加载中" />
        <template v-else-if="tableData.length">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>操作模块</TableHead>
                <TableHead>类型</TableHead>
                <TableHead>请求方式</TableHead>
                <TableHead>操作人</TableHead>
                <TableHead>操作IP</TableHead>
                <TableHead>状态</TableHead>
                <TableHead>耗时</TableHead>
                <TableHead>操作时间</TableHead>
                <TableHead class="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="row in tableData" :key="row.id">
                <TableCell>{{ row.id }}</TableCell>
                <TableCell>{{ row.title }}</TableCell>
                <TableCell><Badge :variant="bizTypeVariant(row.businessType)">{{ bizTypeText(row.businessType) }}</Badge></TableCell>
                <TableCell><Badge :variant="methodVariant(row.requestMethod)">{{ row.requestMethod }}</Badge></TableCell>
                <TableCell>{{ row.operUser }}</TableCell>
                <TableCell><span class="mono-text">{{ row.operIp }}</span></TableCell>
                <TableCell><Badge :variant="row.status === 0 ? 'default' : 'destructive'">{{ row.status === 0 ? '成功' : '失败' }}</Badge></TableCell>
                <TableCell>
                  <span :class="row.costTime > 1000 ? 'text-orange-500' : 'text-green-600'">{{ row.costTime }}ms</span>
                </TableCell>
                <TableCell>{{ formatTime(row.operTime) }}</TableCell>
                <TableCell>
                  <div class="flex justify-end">
                    <Button variant="ghost" size="sm" @click="handleDetail(row)">详情</Button>
                  </div>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>

          <div class="mt-4 flex justify-end">
            <Pagination
              v-model="pagination.current"
              :total="pagination.total"
              :page-size="pagination.pageSize"
              @change="onPageChange"
            />
          </div>
        </template>
        <Empty v-else description="暂无日志" />
      </CardContent>
    </Card>

    <!-- 详情弹窗 -->
    <Dialog :open="detailVisible" title="操作日志详情" @update:open="detailVisible = $event">
      <div v-if="detailData" class="log-detail">
        <dl class="grid grid-cols-2 gap-x-8 gap-y-4">
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">操作模块</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ detailData.title }}</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">操作类型</dt>
            <dd class="mt-1"><Badge :variant="bizTypeVariant(detailData.businessType)">{{ bizTypeText(detailData.businessType) }}</Badge></dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">操作人</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ detailData.operUser }}</dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">操作IP</dt>
            <dd class="mt-1"><span class="mono-text">{{ detailData.operIp }}</span></dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">请求方式</dt>
            <dd class="mt-1"><Badge :variant="methodVariant(detailData.requestMethod)">{{ detailData.requestMethod }}</Badge></dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">耗时</dt>
            <dd class="mt-1">
              <span :class="detailData.costTime > 1000 ? 'text-orange-500' : 'text-green-600'">{{ detailData.costTime }}ms</span>
            </dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">操作状态</dt>
            <dd class="mt-1"><Badge :variant="detailData.status === 0 ? 'default' : 'destructive'">{{ detailData.status === 0 ? '成功' : '失败' }}</Badge></dd>
          </div>
          <div>
            <dt class="text-xs text-[var(--muted-foreground)]">操作时间</dt>
            <dd class="mt-1 text-sm text-[var(--foreground)]">{{ formatTime(detailData.operTime) }}</dd>
          </div>
        </dl>

        <div class="log-detail-section">
          <h4 class="log-detail-section__title">请求信息</h4>
          <dl class="grid grid-cols-1 gap-4">
            <div>
              <dt class="text-xs text-[var(--muted-foreground)]">请求URL</dt>
              <dd class="mt-1"><span class="mono-text">{{ detailData.operUrl }}</span></dd>
            </div>
            <div>
              <dt class="text-xs text-[var(--muted-foreground)]">类名方法</dt>
              <dd class="mt-1"><span class="mono-text">{{ detailData.className }}.{{ detailData.methodName }}</span></dd>
            </div>
          </dl>
        </div>

        <div v-if="detailData.operParam" class="log-detail-section">
          <h4 class="log-detail-section__title">请求参数</h4>
          <div class="log-detail-code">{{ formatJson(detailData.operParam) }}</div>
        </div>

        <div v-if="detailData.jsonResult" class="log-detail-section">
          <h4 class="log-detail-section__title">返回结果</h4>
          <div class="log-detail-code">{{ formatJson(detailData.jsonResult) }}</div>
        </div>

        <div v-if="detailData.errorMsg" class="log-detail-section">
          <h4 class="log-detail-section__title log-detail-section__title--error">错误信息</h4>
          <div class="log-detail-code log-detail-code--error">{{ detailData.errorMsg }}</div>
        </div>
      </div>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import {
  getOperLogPageApi,
  getOperLogApi,
  type SysOperLog,
  type OperLogListParams,
} from '@/api/operLog'
import Card from '@/components/ui/card/Card.vue'
import CardHeader from '@/components/ui/card/CardHeader.vue'
import CardTitle from '@/components/ui/card/CardTitle.vue'
import CardContent from '@/components/ui/card/CardContent.vue'
import Label from '@/components/ui/label/Label.vue'
import Input from '@/components/ui/input/Input.vue'
import Select from '@/components/ui/select/Select.vue'
import Button from '@/components/ui/button/Button.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import Table from '@/components/ui/table/Table.vue'
import TableHeader from '@/components/ui/table/TableHeader.vue'
import TableBody from '@/components/ui/table/TableBody.vue'
import TableRow from '@/components/ui/table/TableRow.vue'
import TableHead from '@/components/ui/table/TableHead.vue'
import TableCell from '@/components/ui/table/TableCell.vue'
import Pagination from '@/components/ui/pagination/Pagination.vue'
import Empty from '@/components/ui/empty/Empty.vue'
import Dialog from '@/components/ui/dialog/Dialog.vue'

type BadgeVariant = 'default' | 'secondary' | 'destructive' | 'outline'

const bizOptions = [
  { label: '其它', value: 0 },
  { label: '新增', value: 1 },
  { label: '修改', value: 2 },
  { label: '删除', value: 3 },
]
const statusOptions = [
  { label: '成功', value: 0 },
  { label: '失败', value: 1 },
]

// ============ 搜索 ============
const searchForm = reactive<OperLogListParams>({
  title: '',
  operUser: '',
  businessType: undefined,
  status: undefined,
})

function handleSearch() {
  pagination.current = 1
  loadTable()
}

function handleReset() {
  searchForm.title = ''
  searchForm.operUser = ''
  searchForm.businessType = undefined
  searchForm.status = undefined
  pagination.current = 1
  loadTable()
}

// ============ 表格 ============
const loading = ref(false)
const tableData = ref<SysOperLog[]>([])

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

function onPageChange(page: number) {
  pagination.current = page
  loadTable()
}

function bizTypeText(type: number) {
  const map: Record<number, string> = { 0: '其它', 1: '新增', 2: '修改', 3: '删除' }
  return map[type] ?? '其它'
}

function bizTypeVariant(type: number): BadgeVariant {
  const map: Record<number, BadgeVariant> = { 0: 'outline', 1: 'default', 2: 'secondary', 3: 'destructive' }
  return map[type] ?? 'outline'
}

function methodVariant(method: string): BadgeVariant {
  const map: Record<string, BadgeVariant> = {
    GET: 'outline',
    POST: 'default',
    PUT: 'secondary',
    DELETE: 'destructive',
    PATCH: 'outline',
  }
  return map[method?.toUpperCase()] ?? 'outline'
}

function formatTime(time: string) {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

function formatJson(str: string) {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

async function loadTable() {
  loading.value = true
  try {
    const res = await getOperLogPageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      title: searchForm.title || undefined,
      operUser: searchForm.operUser || undefined,
      businessType: searchForm.businessType,
      status: searchForm.status,
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

// ============ 详情 ============
const detailVisible = ref(false)
const detailData = ref<SysOperLog | null>(null)

async function handleDetail(row: SysOperLog) {
  try {
    const res = await getOperLogApi(row.id)
    detailData.value = res.data
  } catch {
    detailData.value = row
  }
  detailVisible.value = true
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

.log-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.log-detail-section {
  margin-top: 4px;
}
.log-detail-section__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--foreground);
  margin-bottom: 8px;
}
.log-detail-code {
  background: var(--muted);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 12px 16px;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--foreground);
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 240px;
  overflow-y: auto;
}
.log-detail-code--error {
  color: var(--destructive);
}
</style>
