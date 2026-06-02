<template>
  <div class="page-container">
    <a-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <div class="flex items-center gap-2">
            <span>{{ $t('file.title') }}</span>
            <a-tag color="arcoblue">{{ pagination.total }}</a-tag>
          </div>
          <div class="flex gap-2">
            <input ref="fileInputRef" type="file" class="hidden" multiple @change="onFileSelected" />
            <a-button v-permission="PERM.system.fileUpload" type="primary" :loading="uploading" @click="triggerUpload">
              <template #icon><icon-upload /></template>
              {{ $t('file.upload') }}
            </a-button>
            <a-button v-permission="PERM.system.fileAdd" @click="openCreateFolder">
              <template #icon><icon-plus /></template>
              {{ $t('file.createFolder') }}
            </a-button>
            <a-button @click="loadList">
              <template #icon><icon-refresh /></template>
              {{ $t('file.refresh') }}
            </a-button>
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
          <div class="h-8 w-8 animate-spin rounded-full border-b-2 border-[var(--primary)]"></div>
          <span class="ml-2 text-[var(--muted-foreground)]">{{ $t('common.loading') }}</span>
        </div>
        <a-table v-else :data="tableData" :pagination="false" row-key="id" :bordered="false">
          <template #columns>
            <a-table-column :title="$t('file.fileName')">
              <template #cell="{ record }">
                <div class="flex items-center gap-2">
                  <icon-folder v-if="record.isFolder === 1" class="text-[var(--bz-warning)]" />
                  <icon-file v-else class="text-[var(--muted-foreground)]" />
                  <span
                    v-if="record.isFolder === 1"
                    class="cursor-pointer text-sm hover:text-[var(--primary)]"
                    @click="enterFolder(record)"
                  >{{ record.fileName }}</span>
                  <span v-else class="text-sm text-[var(--foreground)]">{{ record.originalName || record.fileName }}</span>
                </div>
              </template>
            </a-table-column>
            <a-table-column :title="$t('file.fileSize')" :width="120">
              <template #cell="{ record }">
                {{ record.isFolder === 1 ? '-' : formatSize(record.fileSize) }}
              </template>
            </a-table-column>
            <a-table-column :title="$t('file.fileType')" :width="100">
              <template #cell="{ record }">{{ getTypeText(record) }}</template>
            </a-table-column>
            <a-table-column :title="$t('file.scope')" :width="90">
              <template #cell="{ record }">
                <a-tag :color="record.scope === 'public' ? 'green' : 'gray'" size="small">
                  {{ record.scope === 'public' ? $t('file.scopePublic') : $t('file.scopeAdmin') }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.createTime')" :width="170">
              <template #cell="{ record }">{{ formatDateTime(record.createTime) }}</template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" align="right" :width="170">
              <template #cell="{ record }">
                <div class="flex justify-end gap-1">
                  <a-button v-if="record.isFolder !== 1" v-permission="PERM.system.fileDownload" type="text" size="small" @click="handleDownload(record)">
                    {{ $t('file.download') }}
                  </a-button>
                  <a-button v-permission="PERM.system.fileEdit" type="text" size="small" @click="openRename(record)">
                    {{ $t('file.rename') }}
                  </a-button>
                  <a-button v-permission="PERM.system.fileDelete" type="text" size="small" status="danger" @click="openDelete(record)">
                    {{ $t('common.delete') }}
                  </a-button>
                </div>
              </template>
            </a-table-column>
          </template>
          <template #empty>
            <a-empty :description="$t('file.emptyFolder')" />
          </template>
        </a-table>

        <div v-if="pagination.total > pagination.pageSize" class="mt-4 flex justify-end">
          <a-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadList"
          />
        </div>
    </a-card>

    <!-- 新建文件夹 -->
    <a-modal v-model:visible="createFolderVisible" :title="$t('file.createFolder')" @ok="handleCreateFolder" @cancel="createFolderVisible = false">
      <a-form :model="folderForm" layout="vertical">
        <a-form-item field="name" :label="$t('file.folderName')">
          <a-input v-model="folderForm.name" :placeholder="$t('file.folderNamePlaceholder')" @press-enter="handleCreateFolder" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 重命名 -->
    <a-modal v-model:visible="renameVisible" :title="$t('file.rename')" @ok="handleRename" @cancel="renameVisible = false">
      <a-form :model="renameForm" layout="vertical">
        <a-form-item field="name" :label="$t('file.fileName')">
          <a-input v-model="renameForm.name" :placeholder="$t('file.renamePlaceholder')" @press-enter="handleRename" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 删除确认 -->
    <a-modal v-model:visible="deleteVisible" :title="$t('common.confirm')" @ok="handleDelete" @cancel="deleteVisible = false">
      <p class="text-sm text-[var(--foreground)]">{{ deleteConfirmText }}</p>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconUpload, IconPlus, IconRefresh, IconFolder, IconFile } from '@arco-design/web-vue/es/icon'
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
        Message.success(`${t('file.uploadSuccess')}: ${file.name}`)
      } catch {
        Message.error(`${t('file.uploadFailed')}: ${file.name}`)
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
    Message.success(t('file.downloadStart'))
  } catch {
    Message.error(t('file.downloadFailed'))
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
    Message.warning(t('file.nameRequired'))
    return
  }
  try {
    await createFolderApi({ name, parentId: currentFolderId.value || null })
    Message.success(t('file.folderCreated'))
    createFolderVisible.value = false
    loadList()
  } catch {
    Message.error(t('file.folderCreatedFailed'))
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
    Message.warning(t('file.nameRequired'))
    return
  }
  try {
    await renameFileApi(renameTarget.value.id, name)
    Message.success(t('file.renamed'))
    renameVisible.value = false
    loadList()
  } catch {
    Message.error(t('file.renameFailed'))
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
    Message.success(t('file.deleted'))
    deleteVisible.value = false
    loadList()
  } catch {
    Message.error(t('file.deleteFailed'))
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
</script>
