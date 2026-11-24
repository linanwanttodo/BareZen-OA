<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('files.title') }}</h1>
      <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('files.subtitle') }}</p>
    </div>

    <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <t-upload
        theme="custom"
        :auto-upload="true"
        :multiple="true"
        :allow-upload-duplicate-file="true"
        :size-limit="{ size: MAX_SIZE_MB, unit: 'MB' }"
        :request-method="uploadRequest"
      >
        <t-button theme="primary" :loading="uploading">
          <template #icon><upload-icon /></template>
          {{ $t('files.upload') }}
        </t-button>
      </t-upload>
      <t-input
        v-model="keyword"
        class="sm:w-72"
        clearable
        :placeholder="$t('files.searchPlaceholder')"
        @enter="search"
        @clear="search"
      >
        <template #suffixIcon>
          <search-icon class="cursor-pointer" @click="search" />
        </template>
      </t-input>
    </div>

    <t-loading :loading="loading">
      <div v-if="files.length" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div
          v-for="file in files"
          :key="file.id"
          class="flex flex-col rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5"
        >
          <div class="flex items-start gap-3">
            <component
              :is="fileIcon(file.fileType)"
              class="mt-0.5 shrink-0 text-[var(--wecom-primary)]"
              size="22px"
            />
            <div class="min-w-0 flex-1">
              <p class="truncate font-medium text-[var(--foreground)]" :title="file.fileName">
                {{ file.fileName }}
              </p>
              <p class="mt-1 text-xs text-[var(--muted-foreground)]">
                {{ formatSize(file.fileSize) }} · {{ formatDateTime(file.createTime) }}
              </p>
            </div>
          </div>
          <div class="mt-4 flex items-center gap-2">
            <t-button
              size="small"
              theme="primary"
              variant="outline"
              :loading="downloadingId === file.id"
              @click="handleDownload(file)"
            >
              {{ $t('files.download') }}
            </t-button>
            <t-popconfirm :content="$t('files.deleteConfirm')" @confirm="handleDelete(file)">
              <t-button
                size="small"
                theme="danger"
                variant="outline"
                :loading="deletingId === file.id"
              >
                {{ $t('files.delete') }}
              </t-button>
            </t-popconfirm>
          </div>
        </div>
      </div>
      <t-empty v-else :description="$t('files.noData')" />
    </t-loading>

    <div v-if="total > pageSize" class="mt-6">
      <t-pagination v-model:current="currentPage" :total="total" :page-size="pageSize" @change="loadFiles" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import type { UploadFile, RequestMethodResponse } from 'tdesign-vue-next'
import {
  DeleteIcon,
  DownloadIcon,
  FileIcon,
  ImageIcon,
  MusicIcon,
  SearchIcon,
  UploadIcon,
  VideoIcon,
} from 'tdesign-icons-vue-next'
import { useI18n } from 'vue-i18n'
import {
  deleteFileApi,
  downloadFileApi,
  getMyFilesApi,
  uploadFileApi,
  type UserFile,
} from '@/api/files'
import { formatDateTime } from '@/utils/format'

const { t } = useI18n()

/** 与服务端 SysFileServiceImpl.MAX_FILE_SIZE 一致：100MB */
const MAX_SIZE_MB = 100

const loading = ref(false)
const downloadingId = ref('')
const deletingId = ref('')
const files = ref<UserFile[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10
const keyword = ref('')
const searchKeyword = ref('')

/** 并发上传计数（多文件选择时 tdesign 会并发触发 requestMethod） */
const activeUploadCount = ref(0)
const uploading = computed(() => activeUploadCount.value > 0)

/** 并发上传期间是否需要补刷列表（最后一个上传结束时消费） */
const pendingRefresh = ref(false)

function fileIcon(fileType: string | undefined) {
  switch (fileType) {
    case 'image':
      return ImageIcon
    case 'video':
      return VideoIcon
    case 'audio':
      return MusicIcon
    default:
      return FileIcon
  }
}

function formatSize(bytes: number | null | undefined): string {
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

function search() {
  searchKeyword.value = keyword.value.trim()
  currentPage.value = 1
  loadFiles()
}

async function loadFiles() {
  loading.value = true
  try {
    const res = await getMyFilesApi({
      page: currentPage.value,
      pageSize,
      fileName: searchKeyword.value || undefined,
    })
    files.value = res.data.list || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function uploadRequest(file: UploadFile | UploadFile[]): Promise<RequestMethodResponse> {
  const target = Array.isArray(file) ? file[0] : file
  const raw = target?.raw
  if (!raw) {
    return { status: 'fail', error: t('files.uploadFailed'), response: {} }
  }
  activeUploadCount.value++
  try {
    await uploadFileApi(raw)
    MessagePlugin.success(t('files.uploadSuccess'))
    // 新文件按上传时间倒序排在最前，回到第一页才能看到刚上传的文件
    currentPage.value = 1
    // 并发上传时只由最后一个完成的请求刷新列表，避免多次请求互相覆盖
    if (activeUploadCount.value === 1) {
      await loadFiles()
    } else {
      pendingRefresh.value = true
    }
    return { status: 'success', response: { url: raw.name } }
  } catch (err) {
    // 失败提示由 request 拦截器统一弹出（服务端友好错误信息）
    return { status: 'fail', error: (err as Error)?.message || t('files.uploadFailed'), response: {} }
  } finally {
    activeUploadCount.value--
    // 最后一个并发上传结束时补刷一次，确保全部文件都已入库
    if (activeUploadCount.value === 0 && pendingRefresh.value) {
      pendingRefresh.value = false
      loadFiles()
    }
  }
}

/** 从 Content-Disposition 解析后端返回的原始文件名（UTF-8 编码，空格为 + 时还原） */
function getDownloadFilename(header: string | undefined, fallback: string): string {
  if (!header) return fallback
  const utf8 = header.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)
  if (utf8 && utf8[1]) {
    try {
      return decodeURIComponent(utf8[1].trim())
    } catch {
      return utf8[1].trim()
    }
  }
  const plain = header.match(/filename\s*=\s*"?([^";]+)"?/i)
  if (plain && plain[1]) {
    try {
      return decodeURIComponent(plain[1].trim().replace(/\+/g, '%20'))
    } catch {
      return plain[1].trim()
    }
  }
  return fallback
}

async function handleDownload(file: UserFile) {
  downloadingId.value = file.id
  try {
    const res = await downloadFileApi(file.id)
    const blob = new Blob([res.data], { type: 'application/octet-stream' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = getDownloadFilename(res.headers?.['content-disposition'], file.fileName)
    a.click()
    URL.revokeObjectURL(url)
    MessagePlugin.success(t('files.downloadStarted'))
  } catch (err) {
    // 401/403 已由 request 拦截器统一提示
    const status = (err as { response?: { status?: number } })?.response?.status
    if (status !== 401 && status !== 403) {
      MessagePlugin.error(t('files.downloadFailed'))
    }
  } finally {
    downloadingId.value = ''
  }
}

async function handleDelete(file: UserFile) {
  deletingId.value = file.id
  try {
    await deleteFileApi(file.id)
    MessagePlugin.success(t('files.deleteSuccess'))
    // 当前页仅剩一条被删除时回退一页，避免停留在空页
    if (files.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    loadFiles()
  } catch (err) {
    MessagePlugin.error((err as Error)?.message || t('files.deleteFailed'))
  } finally {
    deletingId.value = ''
  }
}

onMounted(loadFiles)
</script>
