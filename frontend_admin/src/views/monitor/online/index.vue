<template>
  <div class="page-container">
    <a-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <div class="flex items-center gap-2">
            <span>{{ $t('monitor.online.title') }}</span>
            <a-tag color="arcoblue">{{ $t('monitor.online.onlineCount', { count: onlineCount }) }}</a-tag>
          </div>
          <div class="flex gap-2">
            <a-button @click="loadTable">
              <template #icon><icon-refresh /></template>
              {{ $t('monitor.online.refresh') }}
            </a-button>
            <a-button v-permission="PERM.monitor.onlineDelete" status="danger" :disabled="!selectedTokens.length" @click="handleBatchForceLogout">
              <template #icon><icon-poweroff /></template>
              {{ $t('monitor.online.batchLogout') }}
            </a-button>
          </div>
        </div>
      </template>
        <div v-if="loading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--primary)]"></div>
          <span class="ml-2 text-[var(--muted-foreground)]">{{ $t('common.loading') }}</span>
        </div>
        <template v-else>
          <a-table :data="tableData" :pagination="false" :row-key="(record: OnlineUser) => record.tokenDigest || String(record.userId)">
            <template #columns>
              <a-table-column :title="$t('common.operate')" :width="50">
                <template #header>
                  <a-checkbox :model-value="allSelected" @change="toggleSelectAll" />
                </template>
                <template #cell="{ record }">
                  <a-checkbox :model-value="selectedTokens.includes(rowKey(record))" @change="() => toggleRow(rowKey(record))" />
                </template>
              </a-table-column>
              <a-table-column title="ID" data-index="userId" />
              <a-table-column :title="$t('user.nickName')">
                <template #cell="{ record }">
                  <div class="flex items-center gap-2">
                    <a-avatar :size="24" :style="{ backgroundColor: 'var(--primary)' }">{{ record.nickName?.charAt(0) || '?' }}</a-avatar>
                    <div class="flex flex-col">
                      <span class="text-sm font-medium text-[var(--foreground)]">{{ record.nickName }}</span>
                      <span class="text-xs text-[var(--muted-foreground)]">{{ record.userName }}</span>
                    </div>
                  </div>
                </template>
              </a-table-column>
              <a-table-column :title="$t('monitor.online.ipAddr')" data-index="loginIp" />
              <a-table-column title="登录地点" data-index="loginLocation" />
              <a-table-column title="浏览器" data-index="browser" />
              <a-table-column title="操作系统" data-index="os" />
              <a-table-column :title="$t('monitor.online.loginTime')">
                <template #cell="{ record }">
                  {{ formatDateTime(record.loginTime) }}
                </template>
              </a-table-column>
              <a-table-column :title="$t('common.operate')" align="right">
                <template #cell="{ record }">
                  <a-button v-permission="PERM.monitor.onlineDelete" type="text" size="small" @click="handleForceLogout(record)">{{ $t('monitor.online.forceLogout') }}</a-button>
                </template>
              </a-table-column>
            </template>
          </a-table>

          <div v-if="pagination.total > pagination.pageSize" class="mt-4 flex justify-end">
            <a-pagination
              v-model:current="pagination.current"
              :total="pagination.total"
              :page-size="pagination.pageSize"
              @change="onPageChange"
            />
          </div>
        </template>
    </a-card>

    <a-modal v-model:visible="confirmDialogVisible" :title="$t('common.confirm')" @cancel="confirmDialogVisible = false">
      <p class="text-sm text-[var(--foreground)]">{{ confirmDialogTitle }}</p>
      <template #footer>
        <div class="flex justify-end gap-2">
          <a-button @click="confirmDialogVisible = false">{{ $t('common.cancel') }}</a-button>
          <a-button type="primary" @click="onConfirm">{{ $t('common.confirm') }}</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconRefresh, IconPoweroff } from '@arco-design/web-vue/es/icon'
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
</script>

<style scoped>
.mono-text {
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
}
</style>
