<template>
  <div class="page-container">
    <t-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <div class="flex items-center gap-2">
            <span>{{ $t('monitor.online.title') }}</span>
            <t-tag theme="primary">{{ $t('monitor.online.onlineCount', { count: onlineCount }) }}</t-tag>
          </div>
          <div class="flex gap-2">
            <t-button @click="loadTable">
              <template #icon><icon-refresh /></template>
              {{ $t('monitor.online.refresh') }}
            </t-button>
            <t-button v-permission="PERM.monitor.onlineDelete" theme="danger" :disabled="!selectedTokens.length" @click="handleBatchForceLogout">
              <template #icon><icon-poweroff /></template>
              {{ $t('monitor.online.batchLogout') }}
            </t-button>
          </div>
        </div>
      </template>
        <div v-if="loading" class="flex items-center justify-center h-64">
          <t-loading size="28px" :text="$t('common.loading')" />
        </div>
        <template v-else>
            <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
    <template #col_0="{ row }">
                  <t-checkbox :model-value="selectedTokens.includes(rowKey(row))" @change="() => toggleRow(rowKey(row))" />
                </template>
    <template #nickName="{ row }">
                  <div class="flex items-center gap-2">
                    <t-avatar size="24px" :style="{ backgroundColor: 'var(--primary)' }">{{ row.nickName?.charAt(0) || '?' }}</t-avatar>
                    <div class="flex flex-col">
                      <span class="text-sm font-medium text-[var(--foreground)]">{{ row.nickName }}</span>
                      <span class="text-xs text-[var(--muted-foreground)]">{{ row.userName }}</span>
                    </div>
                  </div>
                </template>
    <template #loginTime="{ row }">
                  {{ formatDateTime(row.loginTime) }}
                </template>
    <template #col_8="{ row }">
                  <t-button v-permission="PERM.monitor.onlineDelete" type="text" size="small" @click="handleForceLogout(row)">{{ $t('monitor.online.forceLogout') }}</t-button>
                </template>
  </t-table>

          <div v-if="pagination.total > pagination.pageSize" class="mt-4 flex justify-end">
            <t-pagination
              v-model:current="pagination.current"
              :total="pagination.total"
              :page-size="pagination.pageSize"
              @change="onPageChange"
            />
          </div>
        </template>
    </t-card>

    <t-dialog v-model="confirmDialogVisible" :header="$t('common.confirm')" @close="confirmDialogVisible = false">
      <p class="text-sm text-[var(--foreground)]">{{ confirmDialogTitle }}</p>
      <template #footer>
        <div class="flex justify-end gap-2">
          <t-button @click="confirmDialogVisible = false">{{ $t('common.cancel') }}</t-button>
          <t-button theme="primary" @click="onConfirm">{{ $t('common.confirm') }}</t-button>
        </div>
      </template>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatDateTime } from '@/utils/format'
import {
  getOnlineUserListApi,
  getOnlineUserCountApi,
  forceLogoutApi,
  type OnlineUser,
} from '@/api/online'
import { Toast } from '@/utils/toast'

const { t } = useI18n()

// ============ 表格 ============
const loading = ref(false)
const tableData = ref<OnlineUser[]>([])
const selectedTokens = ref<string[]>([])
const onlineCount = ref(0)

function rowKey(row: OnlineUser): string {
  return (row.tokenDigest && row.tokenDigest.length > 0) ? row.tokenDigest : String(row.userId)
}

const allSelected = computed(
  () => tableData.value.length > 0 && tableData.value.every((r) => selectedTokens.value.includes(rowKey(r))),
)

function toggleRow(token: string) {
  const idx = selectedTokens.value.indexOf(token)
  if (idx >= 0) selectedTokens.value.splice(idx, 1)
  else selectedTokens.value.push(token)
}

function toggleSelectAll() {
  if (allSelected.value) selectedTokens.value = []
  else selectedTokens.value = tableData.value.map((r) => rowKey(r))
}

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

function onPageChange(page: number) {
  pagination.current = page
  loadTable()
}

async function loadTable() {
  loading.value = true
  try {
    const [listRes, countRes] = await Promise.allSettled([
      getOnlineUserListApi({ pageNum: pagination.current, pageSize: pagination.pageSize }),
      getOnlineUserCountApi(),
    ])

    if (listRes.status === 'fulfilled') {
      tableData.value = listRes.value.data?.list || []
      pagination.total = listRes.value.data?.total || 0
    }
    if (countRes.status === 'fulfilled') {
      onlineCount.value = countRes.value.data ?? 0
    }
  } catch (err) {
    console.error('online load error:', err)
  } finally {
    loading.value = false
  }
}

// ============ 确认弹窗 ============
const confirmDialogVisible = ref(false)
const confirmDialogTitle = ref('')
let confirmCallback: (() => Promise<void>) | null = null

function showConfirm(title: string, callback: () => Promise<void>) {
  confirmDialogTitle.value = title
  confirmCallback = callback
  confirmDialogVisible.value = true
}

async function onConfirm() {
  if (!confirmCallback) return
  try {
    await confirmCallback()
  } finally {
    confirmDialogVisible.value = false
    confirmCallback = null
  }
}

// ============ 强制下线 ============
function handleForceLogout(row: OnlineUser) {
  showConfirm(t('monitor.online.forceLogoutUserConfirm', { name: row.nickName }), async () => {
    await forceLogoutApi(rowKey(row))
    Toast.success(t('monitor.online.logoutSuccess'))
    loadTable()
  })
}

function handleBatchForceLogout() {
  showConfirm(t('monitor.online.forceLogoutConfirm'), async () => {
    const results = await Promise.allSettled(selectedTokens.value.map((digest) => forceLogoutApi(digest)))
    const failed = results.filter((r) => r.status === 'rejected')
    if (failed.length === 0) {
      Toast.success(t('monitor.online.batchLogoutSuccess', { count: selectedTokens.value.length }))
    } else {
      Toast.warning(t('monitor.online.batchLogoutPartial', { success: selectedTokens.value.length - failed.length, failed: failed.length }))
    }
    selectedTokens.value = []
    loadTable()
  })
}

// ============ 初始化 ============
onMounted(() => {
  loadTable()
})

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'col_0', title: t('common.operate'), width: 50 },
    { colKey: 'userId', title: '' },
    { colKey: 'nickName', title: t('user.nickName') },
    { colKey: 'loginIp', title: t('monitor.online.ipAddr') },
    { colKey: 'loginLocation', title: '' },
    { colKey: 'browser', title: '' },
    { colKey: 'os', title: '' },
    { colKey: 'loginTime', title: t('monitor.online.loginTime') },
    { colKey: 'col_8', title: t('common.operate'), align: 'right' },
  ])

</script>

<style scoped>
.mono-text {
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
}
</style>
