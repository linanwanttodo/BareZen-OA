<template>
  <div class="page-container">
    <t-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <div class="flex items-center gap-2">
            <span>{{ $t('file.title') }}</span>
            <t-tag theme="primary">{{ pagination.total }}</t-tag>
          </div>
          <div class="flex gap-2">
            <input ref="fileInputRef" type="file" class="hidden" multiple @change="onFileSelected" />
            <t-button v-permission="PERM.system.fileUpload" theme="primary" :loading="uploading" @click="triggerUpload">
              <template #icon><icon-upload /></template>
              {{ $t('file.upload') }}
            </t-button>
            <t-button v-permission="PERM.system.fileAdd" @click="openCreateFolder">
              <template #icon><icon-plus /></template>
              {{ $t('file.createFolder') }}
            </t-button>
            <t-button @click="loadList">
              <template #icon><icon-refresh /></template>
              {{ $t('file.refresh') }}
            </t-button>
          </div>
        </div>
      </template>
        <!-- 面包屑导航 -->
        <div class="mb-4 flex flex-wrap items-center gap-1">
          <a class="cursor-pointer text-sm text-[var(--primary)] hover:underline" @click="goToLevel(-1)">{{ $t('file.root') }}</a>
          <template v-for="(crumb, idx) in navStack" :key="crumb.id">
            <span class="text-[var(--muted-foreground)]">/</span>
            <a
              class="cursor-pointer text-sm hover:underline"
              :class="idx === navStack.length - 1 ? 'font-medium text-[var(--foreground)]' : 'text-[var(--primary)]'"
              @click="goToLevel(idx)"
            >{{ crumb.name }}</a>
          </template>
        </div>

        <div v-if="loading" class="flex h-64 items-center justify-center">
          <t-loading size="28px" :text="$t('common.loading')" />
        </div>
          <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
    <template #fileName="{ row }">
                <div class="flex items-center gap-2">
                  <icon-folder v-if="row.isFolder === 1" class="text-[var(--bz-warning)]" />
                  <icon-file v-else class="text-[var(--muted-foreground)]" />
                  <span
                    v-if="row.isFolder === 1"
                    class="cursor-pointer text-sm hover:text-[var(--primary)]"
                    @click="enterFolder(row)"
                  >{{ row.fileName }}</span>
                  <span v-else class="text-sm text-[var(--foreground)]">{{ row.originalName || row.fileName }}</span>
                </div>
              </template>
    <template #fileSize="{ row }">
                {{ row.isFolder === 1 ? '-' : formatSize(row.fileSize) }}
              </template>
    <template #fileType="{ row }">{{ getTypeText(row) }}</template>
    <template #scope="{ row }">
                <t-tag :theme="row.scope === 'public' ? 'success' : 'default'" size="small">
                  {{ row.scope === 'public' ? $t('file.scopePublic') : $t('file.scopeAdmin') }}
                </t-tag>
              </template>
    <template #createTime="{ row }">{{ formatDateTime(row.createTime) }}</template>
    <template #operate="{ row }">
                <div class="flex justify-end gap-1">
                  <t-button v-if="row.isFolder !== 1" v-permission="PERM.system.fileDownload" variant="text" size="small" @click="handleDownload(row)">
                    {{ $t('file.download') }}
                  </t-button>
                  <t-button v-permission="PERM.system.fileEdit" variant="text" size="small" @click="openRename(row)">
                    {{ $t('file.rename') }}
                  </t-button>
                  <t-button v-permission="PERM.system.fileDelete" variant="text" size="small" theme="danger" @click="openDelete(row)">
                    {{ $t('common.delete') }}
                  </t-button>
                </div>
              </template>
  </t-table>

        <div v-if="pagination.total > pagination.pageSize" class="mt-4 flex justify-end">
          <t-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadList"
          />
        </div>
    </t-card>

    <!-- 新建文件夹 -->
    <t-dialog v-model="createFolderVisible" :title="$t('file.createFolder')" @confirm="handleCreateFolder" @cancel="createFolderVisible = false">
      <t-form :model="folderForm" layout="vertical">
        <t-form-item field="name" :label="$t('file.folderName')">
          <t-input v-model="folderForm.name" :placeholder="$t('file.folderNamePlaceholder')" @press-enter="handleCreateFolder" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 重命名 -->
    <t-dialog v-model="renameVisible" :title="$t('file.rename')" @confirm="handleRename" @cancel="renameVisible = false">
      <t-form :model="renameForm" layout="vertical">
        <t-form-item field="name" :label="$t('file.fileName')">
          <t-input v-model="renameForm.name" :placeholder="$t('file.renamePlaceholder')" @press-enter="handleRename" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 删除确认 -->
    <t-dialog v-model="deleteVisible" :title="$t('common.confirm')" @confirm="handleDelete" @cancel="deleteVisible = false">
      <p class="text-sm text-[var(--foreground)]">{{ deleteConfirmText }}</p>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { MessagePlugin } from 'tdesign-vue-next'
import { formatDateTime } from '@/utils/format'
import {
  uploadFileApi,
  downloadFileApi,
  deleteFileApi,
  createFolderApi,
  renameFileApi,
  pageByFolderApi,
  type SysFileItem,
} from '@/api/file'

const { t } = useI18n()

// ============ 导航（面包屑） ============
const navStack = ref<Array<{ id: number; name: string }>>([])
const currentFolderId = computed<number>(() =>
  navStack.value.length > 0 ? navStack.value[navStack.value.length - 1].id : 0,
)

function enterFolder(record: SysFileItem) {
  navStack.value.push({ id: record.id, name: record.fileName })
  pagination.current = 1
  loadList()
}

/** 跳转到指定层级：-1=根目录，>=0 对应面包屑索引 */
function goToLevel(idx: number) {
  if (idx < 0) {
    navStack.value = []
  } else {
    navStack.value = navStack.value.slice(0, idx + 1)
  }
  pagination.current = 1
  loadList()
}

// ============ 列表 ============
const loading = ref(false)
const tableData = ref<SysFileItem[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

async function loadList() {
  loading.value = true
  try {
    const res = await pageByFolderApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      folderId: currentFolderId.value || null,
    })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

// ============ 上传 ============
const fileInputRef = ref<HTMLInputElement | null>(null)
const uploading = ref(false)

function triggerUpload() {
  fileInputRef.value?.click()
}

async function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return
  uploading.value = true
  try {
    for (const file of Array.from(files)) {
      try {
        await uploadFileApi(file, currentFolderId.value || null)
        MessagePlugin.success(`${t('file.uploadSuccess')}: ${file.name}`)
      } catch {
        MessagePlugin.error(`${t('file.uploadFailed')}: ${file.name}`)
      }
    }
    loadList()
  } finally {
    uploading.value = false
    input.value = ''
  }
}

// ============ 下载 ============
async function handleDownload(record: SysFileItem) {
  try {
    const res = await downloadFileApi(record.id) as any
    const blob = new Blob([res.data], { type: 'application/octet-stream' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = getDownloadFilename(res.headers?.['content-disposition'], record.originalName || record.fileName)
    a.click()
    URL.revokeObjectURL(url)
    MessagePlugin.success(t('file.downloadStart'))
  } catch {
    MessagePlugin.error(t('file.downloadFailed'))
  }
}

/** 从 Content-Disposition 解析后端返回的原始文件名（UTF-8 编码） */
function getDownloadFilename(header: string | undefined, fallback: string): string {
  if (!header) return fallback
  const match = header.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i)
  if (match && match[1]) {
    try {
      return decodeURIComponent(match[1])
    } catch {
      return match[1]
    }
  }
  return fallback
}

// ============ 新建文件夹 ============
const createFolderVisible = ref(false)
const folderForm = reactive({ name: '' })

function openCreateFolder() {
  folderForm.name = ''
  createFolderVisible.value = true
}

async function handleCreateFolder() {
  const name = folderForm.name.trim()
  if (!name) {
    MessagePlugin.warning(t('file.nameRequired'))
    return
  }
  try {
    await createFolderApi({ name, parentId: currentFolderId.value || null })
    MessagePlugin.success(t('file.folderCreated'))
    createFolderVisible.value = false
    loadList()
  } catch {
    MessagePlugin.error(t('file.folderCreatedFailed'))
  }
}

// ============ 重命名 ============
const renameVisible = ref(false)
const renameForm = reactive({ name: '' })
const renameTarget = ref<SysFileItem | null>(null)

function openRename(record: SysFileItem) {
  renameTarget.value = record
  renameForm.name = record.fileName
  renameVisible.value = true
}

async function handleRename() {
  const name = renameForm.name.trim()
  if (!name || !renameTarget.value) {
    MessagePlugin.warning(t('file.nameRequired'))
    return
  }
  try {
    await renameFileApi(renameTarget.value.id, name)
    MessagePlugin.success(t('file.renamed'))
    renameVisible.value = false
    loadList()
  } catch {
    MessagePlugin.error(t('file.renameFailed'))
  }
}

// ============ 删除 ============
const deleteVisible = ref(false)
const deleteTarget = ref<SysFileItem | null>(null)

const deleteConfirmText = computed(() => {
  if (!deleteTarget.value) return ''
  const name = deleteTarget.value.fileName
  return deleteTarget.value.isFolder === 1
    ? t('file.deleteFolderConfirm', { name })
    : t('file.deleteFileConfirm', { name })
})

function openDelete(record: SysFileItem) {
  deleteTarget.value = record
  deleteVisible.value = true
}

async function handleDelete() {
  if (!deleteTarget.value) return
  try {
    await deleteFileApi(deleteTarget.value.id)
    MessagePlugin.success(t('file.deleted'))
    deleteVisible.value = false
    loadList()
  } catch {
    MessagePlugin.error(t('file.deleteFailed'))
  }
}

// ============ 展示辅助 ============
function formatSize(bytes: number): string {
  if (bytes == null || bytes === 0) return '-'
  if (bytes < 1024) return `${bytes} B`
  const units = ['KB', 'MB', 'GB', 'TB']
  let size = bytes / 1024
  let i = 0
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(1)} ${units[i]}`
}

function getTypeText(record: SysFileItem): string {
  if (record.isFolder === 1) return t('file.folder')
  const map: Record<string, string> = {
    image: t('file.typeImage'),
    document: t('file.typeDocument'),
    video: t('file.typeVideo'),
    audio: t('file.typeAudio'),
  }
  return map[record.fileType] || t('file.typeOther')
}

onMounted(loadList)

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'fileName', title: t('file.fileName') },
    { colKey: 'fileSize', title: t('file.fileSize'), width: 120 },
    { colKey: 'fileType', title: t('file.fileType'), width: 100 },
    { colKey: 'scope', title: t('file.scope'), width: 90 },
    { colKey: 'createTime', title: t('common.createTime'), width: 170 },
    { colKey: 'operate', title: t('common.operate'), width: 170, align: 'right' },
  ])

</script>
