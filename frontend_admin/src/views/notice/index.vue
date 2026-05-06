<template>
  <div class="page-container">
    <a-card>
      <a-card-content>
        <div class="flex flex-wrap items-end gap-3">
          <a-form-item :label="$t('notice.noticeTitle')">
            <a-input v-model="searchForm.noticeTitle" :placeholder="$t('notice.noticePlaceholder')" style="width: 200px" />
          </a-form-item>
          <a-form-item :label="$t('notice.noticeType')">
            <a-select v-model="searchForm.noticeType" :placeholder="$t('common.selectPlaceholder')" style="width: 120px" :options="noticeTypeOptions" />
          </a-form-item>
          <a-form-item :label="$t('common.status')">
            <a-select v-model="searchForm.status" :placeholder="$t('common.selectPlaceholder')" style="width: 120px" :options="noticeStatusOptions" />
          </a-form-item>
          <div class="flex gap-2">
            <a-button @click="handleSearch">{{ $t('common.search') }}</a-button>
            <a-button @click="handleReset">{{ $t('common.reset') }}</a-button>
          </div>
        </div>
      </a-card-content>
    </a-card>

    <a-card class="mt-4">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('notice.title') }}</span>
          <a-button v-permission="'system:notice:add'" type="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </a-button>
        </div>
      </template>
      <a-card-content>
        <div v-if="loading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--primary)]"></div>
          <span class="ml-2 text-[var(--muted-foreground)]">{{ $t('common.loading') }}</span>
        </div>
        <template v-else>
          <a-table :data="tableData" :pagination="false" row-key="id">
            <template #columns>
              <a-table-column :title="$t('common.operate')" data-index="id" :width="80" />
              <a-table-column :title="$t('notice.noticeTitle')">
                <template #cell="{ record }">
                  <span class="cursor-pointer text-[var(--primary)] hover:underline" @click="handleView(record)">{{ record.noticeTitle }}</span>
                </template>
              </a-table-column>
              <a-table-column :title="$t('notice.noticeType')" :width="100">
                <template #cell="{ record }">
                  <a-tag :color="record.noticeType === '1' ? 'arcoblue' : 'purple'">{{ record.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column :title="$t('notice.scope')" :width="120">
                <template #cell="{ record }">
                  <a-tag bordered>{{ record.scope === 'public' ? $t('notice.public') : record.scope === 'internal' ? $t('notice.internal') : $t('user.unknown') }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column :title="$t('notice.requireRead')" :width="100">
                <template #cell="{ record }">
                  <a-tag v-if="record.requireRead === 'Y'" color="red">{{ $t('notice.requireReadYes') }}</a-tag>
                  <a-tag v-else bordered>{{ $t('notice.requireReadNo') }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column :title="$t('notice.sendEmail')" :width="110">
                <template #cell="{ record }">
                  <a-tag v-if="record.sendEmail === 'Y'" color="arcoblue">{{ $t('notice.sendEmailYes') }}</a-tag>
                  <a-tag v-else bordered>{{ $t('notice.sendEmailNo') }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column :title="$t('common.status')" :width="100">
                <template #cell="{ record }">
                  <a-tag v-if="record.status === '1'" color="arcoblue">{{ noticeStatusText(record.status) }}</a-tag>
                  <a-tag v-else-if="record.status === '2'" color="red">{{ noticeStatusText(record.status) }}</a-tag>
                  <a-tag v-else bordered>{{ noticeStatusText(record.status) }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column :title="$t('common.createTime')" data-index="createTime" :width="160" />
              <a-table-column :title="$t('common.operate')" :width="280" align="right">
                <template #cell="{ record }">
                  <a-space>
                    <a-button type="text" size="small" @click="handleView(record)">{{ $t('common.detail') }}</a-button>
                    <a-button v-permission="'system:notice:edit'" type="text" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</a-button>
                    <a-button v-if="record.status === '0'" v-permission="'system:notice:edit'" type="text" size="small" @click="handlePublish(record)">{{ $t('notice.publish') }}</a-button>
                    <a-button v-if="record.status === '1'" v-permission="'system:notice:edit'" type="text" size="small" @click="handleRevoke(record)">{{ $t('notice.revoke') }}</a-button>
                    <a-button v-permission="'system:notice:list'" type="text" size="small" @click="handleReadReceipt(record)">{{ $t('notice.readReceipt') }}</a-button>
                    <a-button v-permission="'system:notice:delete'" type="text" size="small" @click="handleDelete(record)">{{ $t('common.delete') }}</a-button>
                  </a-space>
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
      </a-card-content>
    </a-card>

    <a-modal v-model:visible="formDialogVisible" :title="formType === 'add' ? $t('notice.addNotice') : $t('notice.editNotice')" @cancel="formDialogVisible = false">
      <div class="flex flex-col gap-4">
        <div v-if="formError" class="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">{{ formError }}</div>
        <div class="grid grid-cols-3 gap-4">
          <div class="col-span-2 flex flex-col gap-1.5">
            <span class="text-sm text-[var(--foreground)]">{{ $t('notice.noticeTitle') }} <span class="text-red-500">*</span></span>
            <a-input v-model="formData.noticeTitle" :placeholder="$t('notice.noticePlaceholder')" />
          </div>
          <div class="flex flex-col gap-1.5">
            <span class="text-sm text-[var(--foreground)]">{{ $t('notice.noticeType') }}</span>
            <a-select v-model="formData.noticeType" :options="[
              { label: $t('notice.typeNotice'), value: '1' },
              { label: $t('notice.typeAnnouncement'), value: '2' },
            ]" />
          </div>
        </div>
        <div class="flex flex-col gap-1.5">
          <span class="text-sm text-[var(--foreground)]">{{ $t('notice.noticeContent') }} <span class="text-red-500">*</span></span>
          <a-textarea v-model="formData.noticeContent" :placeholder="$t('notice.noticeContent')" style="min-height: 120px" />
        </div>
        <div class="flex flex-col gap-1.5">
          <span class="text-sm text-[var(--foreground)]">{{ $t('notice.scope') }}</span>
          <a-radio-group v-model="formData.scope" :options="[
            { label: $t('notice.scopeAll'), value: 'public' },
            { label: $t('notice.scopeInternal'), value: 'internal' },
          ]" />
        </div>
        <div class="flex flex-col gap-1.5">
          <span class="text-sm text-[var(--foreground)]">{{ $t('notice.requireRead') }}</span>
          <a-switch v-model="formData.requireRead" :checked-value="'Y'" :unchecked-value="'N'" />
          <span class="text-xs text-[var(--muted-foreground)]">{{ $t('notice.requireReadTip') }}</span>
        </div>
        <div class="flex flex-col gap-1.5">
          <span class="text-sm text-[var(--foreground)]">{{ $t('notice.sendEmail') }}</span>
          <a-switch v-model="formData.sendEmail" :checked-value="'Y'" :unchecked-value="'N'" />
          <span class="text-xs text-[var(--muted-foreground)]">{{ $t('notice.sendEmailTip') }}</span>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <a-button @click="formDialogVisible = false">{{ $t('common.cancel') }}</a-button>
          <a-button type="primary" @click="onFormConfirm">{{ $t('common.confirm') }}</a-button>
        </div>
      </template>
    </a-modal>

    <a-modal v-model:visible="detailVisible" :title="detailData?.noticeTitle || $t('notice.title')" @cancel="detailVisible = false">
      <div v-if="detailData">
        <div class="mb-4 flex flex-wrap items-center gap-2">
          <a-tag :color="detailData.noticeType === '1' ? 'arcoblue' : 'purple'">{{ detailData.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}</a-tag>
          <a-tag bordered>{{ detailData.scope === 'public' ? $t('notice.public') : detailData.scope === 'internal' ? $t('notice.internal') : $t('user.unknown') }}</a-tag>
          <a-tag v-if="detailData.status === '1'" color="arcoblue">{{ noticeStatusText(detailData.status) }}</a-tag>
          <a-tag v-else bordered>{{ noticeStatusText(detailData.status) }}</a-tag>
          <span class="ml-auto text-xs text-[var(--muted-foreground)]">{{ $t('notice.published') }}：{{ formatDateTime(detailData.createTime) }}</span>
        </div>
        <a-divider class="my-4" />
        <div class="notice-detail-content" v-html="sanitizeHtml(detailData.noticeContent)"></div>
      </div>
    </a-modal>

    <a-modal v-model:visible="receiptVisible" :title="receiptTitle" :width="560" @cancel="receiptVisible = false">
      <div v-if="receiptData">
        <div class="mb-4 flex items-center gap-4">
          <a-statistic :title="$t('notice.readCount')" :value="receiptData.readCount" />
          <a-tag v-if="receiptData.requireRead === 'Y'" color="red">{{ $t('notice.requireReadYes') }}</a-tag>
        </div>
        <a-divider class="my-3" />
        <div v-if="receiptData.reads.length" class="max-h-72 space-y-2 overflow-y-auto">
          <div v-for="(r, i) in receiptData.reads" :key="i" class="flex items-center justify-between rounded bg-[var(--muted)] px-3 py-2 text-sm">
            <span>{{ $t('notice.userIdLabel') }}：{{ r.userId }}</span>
            <span class="text-xs text-[var(--muted-foreground)]">{{ formatDateTime(r.readTime) }}</span>
          </div>
        </div>
        <a-empty v-else :description="$t('notice.noReadYet')" />
      </div>
    </a-modal>

    <a-modal v-model:visible="deleteConfirmVisible" :title="$t('common.confirmDelete')" @cancel="deleteConfirmVisible = false">
      <p class="text-sm text-[var(--foreground)]">{{ $t('notice.deleteConfirm', { title: deleteTarget?.noticeTitle }) }}</p>
      <template #footer>
        <div class="flex justify-end gap-2">
          <a-button @click="deleteConfirmVisible = false">{{ $t('common.cancel') }}</a-button>
          <a-button type="primary" status="danger" @click="confirmDelete">{{ $t('common.confirm') }}</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import { useI18n } from 'vue-i18n'
import { formatDateTime, noticeStatusText } from '@/utils/format'
import { Toast } from '@/utils/toast'
import {
  getNoticePageApi,
  addNoticeApi,
  updateNoticeApi,
  deleteNoticeApi,
  publishNoticeApi,
  revokeNoticeApi,
  getNoticeReadReceiptApi,
  type SysNotice,
  type NoticeListParams,
  type NoticeReadReceipt,
} from '@/api/notice'
import { sanitizeHtml } from '@/utils/dom'

const { t } = useI18n()

const noticeTypeOptions = [
  { label: t('notice.allTypes'), value: '' },
  { label: t('notice.typeNotice'), value: '1' },
  { label: t('notice.typeAnnouncement'), value: '2' },
]

const noticeStatusOptions = [
  { label: t('notice.allStatus'), value: '' },
  { label: t('notice.noticeStatusDraft'), value: '0' },
  { label: t('notice.noticeStatusPublished'), value: '1' },
  { label: t('notice.noticeStatusRevoked'), value: '2' },
]

const searchForm = reactive<NoticeListParams>({
  noticeTitle: '',
  noticeType: '',
  status: '',
})

function handleSearch() {
  pagination.current = 1
  loadTable()
}

function handleReset() {
  searchForm.noticeTitle = ''
  searchForm.noticeType = ''
  searchForm.status = ''
  pagination.current = 1
  loadTable()
}

const loading = ref(false)
const tableData = ref<SysNotice[]>([])

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

function onPageChange(page: number) {
  pagination.current = page
  loadTable()
}

async function loadTable() {
  loading.value = true
  try {
    const res = await getNoticePageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      noticeTitle: searchForm.noticeTitle || undefined,
      noticeType: searchForm.noticeType || undefined,
      status: searchForm.status || undefined,
    })
    tableData.value = res.data?.list || []
    pagination.total = res.data?.total || 0
  } catch (err) {
    console.error('notice load error:', err)
  } finally {
    loading.value = false
  }
}

async function handlePublish(row: SysNotice) {
  try {
    await publishNoticeApi(row.id)
    Toast.success(t('notice.publishSuccess'))
    loadTable()
  } catch (err) {
    console.error('notice handlePublish error:', err)
  }
}

async function handleRevoke(row: SysNotice) {
  try {
    await revokeNoticeApi(row.id)
    Toast.success(t('notice.revokeSuccess'))
    loadTable()
  } catch (err) {
    console.error('notice handleRevoke error:', err)
  }
}

function handleDelete(row: SysNotice) {
  deleteTarget.value = row
  deleteConfirmVisible.value = true
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  try {
    await deleteNoticeApi(deleteTarget.value.id)
    Toast.success(t('notice.deleteSuccess'))
    deleteConfirmVisible.value = false
    deleteTarget.value = null
    loadTable()
  } catch (err) {
    console.error('notice handleDelete error:', err)
  }
}

const detailVisible = ref(false)
const detailData = ref<SysNotice | null>(null)

function handleView(row: SysNotice) {
  detailData.value = row
  detailVisible.value = true
}

const formDialogVisible = ref(false)
const formType = ref<'add' | 'edit'>('add')
const formError = ref('')
const deleteConfirmVisible = ref(false)
const deleteTarget = ref<SysNotice | null>(null)

const defaultFormData = {
  id: undefined as string | undefined,
  noticeTitle: '',
  noticeType: '1',
  noticeContent: '',
  scope: 'public',
  requireRead: 'N',
  sendEmail: 'N',
  status: '0',
}

const formData = reactive({ ...defaultFormData })

function handleAdd() {
  formType.value = 'add'
  Object.assign(formData, { ...defaultFormData })
  formDialogVisible.value = true
}

function handleEdit(row: SysNotice) {
  formType.value = 'edit'
  Object.assign(formData, {
    id: row.id,
    noticeTitle: row.noticeTitle,
    noticeType: row.noticeType,
    noticeContent: row.noticeContent,
    scope: row.scope,
    requireRead: row.requireRead || 'N',
    sendEmail: row.sendEmail || 'N',
    status: row.status,
  })
  formDialogVisible.value = true
}

const receiptVisible = ref(false)
const receiptData = ref<NoticeReadReceipt | null>(null)
const receiptTitle = ref('')

async function handleReadReceipt(row: SysNotice) {
  try {
    const res = await getNoticeReadReceiptApi(row.id)
    receiptData.value = res.data
    receiptTitle.value = row.noticeTitle
    receiptVisible.value = true
  } catch (err) {
    console.error('notice read receipt error:', err)
  }
}

async function onFormConfirm() {
  formError.value = ''
  if (!formData.noticeTitle || !formData.noticeTitle.trim()) {
    formError.value = t('notice.titleRequired')
    return
  }
  if (!formData.noticeContent || !formData.noticeContent.trim()) {
    formError.value = t('notice.contentRequired')
    return
  }
  try {
    if (formType.value === 'add') {
      await addNoticeApi(formData)
      Toast.success(t('notice.addSuccess'))
    } else {
      await updateNoticeApi(formData)
      Toast.success(t('notice.editSuccess'))
    }
    formDialogVisible.value = false
    loadTable()
  } catch (err) {
    console.error('notice onFormConfirm error:', err)
  }
}

onMounted(() => {
  loadTable()
})
</script>

<style scoped>
.notice-detail-content {
  line-height: 1.8;
  color: var(--foreground);
  font-size: 14px;
  white-space: pre-wrap;
}
</style>
