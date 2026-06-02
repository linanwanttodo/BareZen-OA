<template>
  <div class="page-container">
    <t-card>
        <div class="flex flex-wrap items-end gap-3">
          <t-form-item :label="$t('notice.noticeTitle')">
            <t-input v-model="searchForm.noticeTitle" :placeholder="$t('notice.noticePlaceholder')" style="width: 200px" />
          </t-form-item>
          <t-form-item :label="$t('notice.noticeType')">
            <t-select v-model="searchForm.noticeType" :placeholder="$t('common.selectPlaceholder')" style="width: 120px" :options="noticeTypeOptions" />
          </t-form-item>
          <t-form-item :label="$t('common.status')">
            <t-select v-model="searchForm.status" :placeholder="$t('common.selectPlaceholder')" style="width: 120px" :options="noticeStatusOptions" />
          </t-form-item>
          <div class="flex gap-2">
            <t-button @click="handleSearch">{{ $t('common.search') }}</t-button>
            <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
          </div>
        </div>
    </t-card>

    <t-card class="mt-4">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('notice.title') }}</span>
          <t-button v-permission="PERM.system.noticeAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </t-button>
        </div>
      </template>
        <div v-if="loading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--primary)]"></div>
          <span class="ml-2 text-[var(--muted-foreground)]">{{ $t('common.loading') }}</span>
        </div>
        <template v-else>
            <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
    <template #noticeTitle="{ row }">
                  <span class="cursor-pointer text-[var(--primary)] hover:underline" @click="handleView(row)">{{ row.noticeTitle }}</span>
                </template>
    <template #noticeType="{ row }">
                  <t-tag theme="primary">{{ row.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}</t-tag>
                </template>
    <template #scope="{ row }">
                  <t-tag theme="default">{{ row.scope === 'public' ? $t('notice.public') : row.scope === 'internal' ? $t('notice.internal') : $t('user.unknown') }}</t-tag>
                </template>
    <template #requireRead="{ row }">
                  <t-tag v-if="row.requireRead === 'Y'" theme="danger">{{ $t('notice.requireReadYes') }}</t-tag>
                  <t-tag v-else theme="default">{{ $t('notice.requireReadNo') }}</t-tag>
                </template>
    <template #sendEmail="{ row }">
                  <t-tag v-if="row.sendEmail === 'Y'" theme="primary">{{ $t('notice.sendEmailYes') }}</t-tag>
                  <t-tag v-else theme="default">{{ $t('notice.sendEmailNo') }}</t-tag>
                </template>
    <template #status="{ row }">
                  <t-tag v-if="row.status === '1'" theme="primary">{{ noticeStatusText(row.status) }}</t-tag>
                  <t-tag v-else-if="row.status === '2'" theme="danger">{{ noticeStatusText(row.status) }}</t-tag>
                  <t-tag v-else theme="default">{{ noticeStatusText(row.status) }}</t-tag>
                </template>
    <template #operate="{ row }">
                  <t-space>
                    <t-button variant="text" size="small" @click="handleView(row)">{{ $t('common.detail') }}</t-button>
                    <t-button v-permission="PERM.system.noticeEdit" variant="text" size="small" @click="handleEdit(row)">{{ $t('common.edit') }}</t-button>
                    <t-button v-if="row.status === '0'" v-permission="PERM.system.noticeEdit" variant="text" size="small" @click="handlePublish(row)">{{ $t('notice.publish') }}</t-button>
                    <t-button v-if="row.status === '1'" v-permission="PERM.system.noticeEdit" variant="text" size="small" @click="handleRevoke(row)">{{ $t('notice.revoke') }}</t-button>
                    <t-button v-permission="PERM.system.noticeList" variant="text" size="small" @click="handleReadReceipt(row)">{{ $t('notice.readReceipt') }}</t-button>
                    <t-button v-permission="PERM.system.noticeDelete" variant="text" size="small" @click="handleDelete(row)">{{ $t('common.delete') }}</t-button>
                  </t-space>
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

    <t-dialog v-model="formDialogVisible" :header="formType === 'add' ? $t('notice.addNotice') : $t('notice.editNotice')" @close="formDialogVisible = false">
      <div class="flex flex-col gap-4">
        <div v-if="formError" class="rounded border border-wecom-error-light bg-wecom-error-light px-3 py-2 text-sm text-wecom-error">{{ formError }}</div>
        <div class="grid grid-cols-3 gap-4">
          <div class="col-span-2 flex flex-col gap-1.5">
            <span class="text-sm text-[var(--foreground)]">{{ $t('notice.noticeTitle') }} <span class="text-wecom-error">*</span></span>
            <t-input v-model="formData.noticeTitle" :placeholder="$t('notice.noticePlaceholder')" />
          </div>
          <div class="flex flex-col gap-1.5">
            <span class="text-sm text-[var(--foreground)]">{{ $t('notice.noticeType') }}</span>
            <t-select v-model="formData.noticeType" :options="[
              { label: $t('notice.typeNotice'), value: '1' },
              { label: $t('notice.typeAnnouncement'), value: '2' },
            ]" />
          </div>
        </div>
        <div class="flex flex-col gap-1.5">
          <span class="text-sm text-[var(--foreground)]">{{ $t('notice.noticeContent') }} <span class="text-wecom-error">*</span></span>
          <t-textarea v-model="formData.noticeContent" :placeholder="$t('notice.noticeContent')" style="min-height: 120px" />
        </div>
        <div class="flex flex-col gap-1.5">
          <span class="text-sm text-[var(--foreground)]">{{ $t('notice.scope') }}</span>
          <t-radio-group v-model="formData.scope" :options="[
            { label: $t('notice.scopeAll'), value: 'public' },
            { label: $t('notice.scopeInternal'), value: 'internal' },
          ]" />
        </div>
        <div class="flex flex-col gap-1.5">
          <span class="text-sm text-[var(--foreground)]">{{ $t('notice.requireRead') }}</span>
          <t-switch v-model="formData.requireRead" :checked-value="'Y'" :unchecked-value="'N'" />
          <span class="text-xs text-[var(--muted-foreground)]">{{ $t('notice.requireReadTip') }}</span>
        </div>
        <div class="flex flex-col gap-1.5">
          <span class="text-sm text-[var(--foreground)]">{{ $t('notice.sendEmail') }}</span>
          <t-switch v-model="formData.sendEmail" :checked-value="'Y'" :unchecked-value="'N'" />
          <span class="text-xs text-[var(--muted-foreground)]">{{ $t('notice.sendEmailTip') }}</span>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <t-button @click="formDialogVisible = false">{{ $t('common.cancel') }}</t-button>
          <t-button theme="primary" @click="onFormConfirm">{{ $t('common.confirm') }}</t-button>
        </div>
      </template>
    </t-dialog>

    <t-dialog v-model="detailVisible" :header="detailData?.noticeTitle || $t('notice.title')" @close="detailVisible = false">
      <div v-if="detailData">
        <div class="mb-4 flex flex-wrap items-center gap-2">
          <t-tag theme="primary">{{ detailData.noticeType === '1' ? $t('notice.typeNotice') : $t('notice.typeAnnouncement') }}</t-tag>
          <t-tag theme="default">{{ detailData.scope === 'public' ? $t('notice.public') : detailData.scope === 'internal' ? $t('notice.internal') : $t('user.unknown') }}</t-tag>
          <t-tag v-if="detailData.status === '1'" theme="primary">{{ noticeStatusText(detailData.status) }}</t-tag>
          <t-tag v-else theme="default">{{ noticeStatusText(detailData.status) }}</t-tag>
          <span class="ml-auto text-xs text-[var(--muted-foreground)]">{{ $t('notice.published') }}：{{ formatDateTime(detailData.createTime) }}</span>
        </div>
        <t-divider class="my-4" />
        <div class="notice-detail-content" v-html="sanitizeHtml(detailData.noticeContent)"></div>
      </div>
    </t-dialog>

    <t-dialog v-model="receiptVisible" :header="receiptTitle" width="560px" @close="receiptVisible = false">
      <div v-if="receiptData">
        <div class="mb-4 flex items-center gap-4">
          <div class="flex flex-col">
            <span class="text-xs text-[var(--muted-foreground)]">{{ $t('notice.readCount') }}</span>
            <span class="text-2xl font-semibold text-[var(--foreground)]">{{ receiptData.readCount }}</span>
          </div>
          <t-tag v-if="receiptData.requireRead === 'Y'" theme="danger">{{ $t('notice.requireReadYes') }}</t-tag>
        </div>
        <t-divider class="my-3" />
        <div v-if="receiptData.reads.length" class="max-h-72 space-y-2 overflow-y-auto">
          <div v-for="(r, i) in receiptData.reads" :key="i" class="flex items-center justify-between rounded bg-[var(--muted)] px-3 py-2 text-sm">
            <span>{{ $t('notice.userIdLabel') }}：{{ r.userId }}</span>
            <span class="text-xs text-[var(--muted-foreground)]">{{ formatDateTime(r.readTime) }}</span>
          </div>
        </div>
        <t-empty v-else :description="$t('notice.noReadYet')" />
      </div>
    </t-dialog>

    <t-dialog v-model="deleteConfirmVisible" :header="$t('common.confirmDelete')" @close="deleteConfirmVisible = false">
      <p class="text-sm text-[var(--foreground)]">{{ $t('notice.deleteConfirm', { title: deleteTarget?.noticeTitle }) }}</p>
      <template #footer>
        <div class="flex justify-end gap-2">
          <t-button @click="deleteConfirmVisible = false">{{ $t('common.cancel') }}</t-button>
          <t-button theme="danger" @click="confirmDelete">{{ $t('common.confirm') }}</t-button>
        </div>
      </template>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
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

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'noticeTitle', title: t('notice.noticeTitle') },
    { colKey: 'noticeType', title: t('notice.noticeType'), width: 100 },
    { colKey: 'scope', title: t('notice.scope'), width: 120 },
    { colKey: 'requireRead', title: t('notice.requireRead'), width: 100 },
    { colKey: 'sendEmail', title: t('notice.sendEmail'), width: 110 },
    { colKey: 'status', title: t('common.status'), width: 100 },
    { colKey: 'createTime', title: t('common.createTime'), width: 160 },
    { colKey: 'operate', title: t('common.operate'), width: 280, align: 'right' },
  ])

</script>

<style scoped>
.notice-detail-content {
  line-height: 1.8;
  color: var(--foreground);
  font-size: 14px;
  white-space: pre-wrap;
}
</style>
