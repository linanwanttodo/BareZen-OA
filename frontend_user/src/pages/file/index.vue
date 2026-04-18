<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <PageHeader title="文件中心" description="浏览和下载企业公开文件" />

    <div class="mb-4 flex flex-wrap items-center gap-2">
      <Button variant="ghost" size="sm" @click="goToFolder(null)">
        <Home :size="16" class="mr-1.5" />
        全部文件
      </Button>
      <template v-for="crumb in breadcrumbs" :key="crumb.id">
        <span class="text-[var(--muted-foreground)]">/</span>
        <Button variant="ghost" size="sm" @click="goToFolder(crumb.id)">{{ crumb.label }}</Button>
      </template>
      <div class="ml-auto w-56">
        <Input v-model="searchKeyword" placeholder="搜索文件..." @keyup.enter="handleSearch" />
      </div>
    </div>

    <Card>
      <CardContent class="pt-6">
        <div v-if="folderList.length > 0" class="mb-6">
          <div class="mb-3 text-sm font-medium text-[var(--muted-foreground)]">文件夹</div>
          <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <button
              v-for="item in folderList"
              :key="'f-' + item.id"
              type="button"
              class="flex flex-col items-center gap-2 rounded-lg border border-[var(--border)] p-4 text-[var(--foreground)]"
              @click="openFolder(item)"
            >
              <Folder :size="32" class="text-[var(--muted-foreground)]" />
              <div class="w-full truncate text-center text-sm">{{ item.fileName }}</div>
            </button>
          </div>
        </div>

        <div v-if="fileList.length > 0">
          <div class="mb-3 text-sm font-medium text-[var(--muted-foreground)]">文件</div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>文件名</TableHead>
                <TableHead>大小</TableHead>
                <TableHead>类型</TableHead>
                <TableHead>上传时间</TableHead>
                <TableHead class="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="file in fileList" :key="file.id">
                <TableCell>
                  <div class="flex items-center gap-2">
                    <component :is="getFileIcon(file.fileType)" :size="16" class="text-[var(--muted-foreground)]" />
                    {{ file.originalName }}
                  </div>
                </TableCell>
                <TableCell>{{ formatFileSize(file.fileSize) }}</TableCell>
                <TableCell>{{ file.fileType || '-' }}</TableCell>
                <TableCell>{{ file.createTime }}</TableCell>
                <TableCell class="text-right">
                  <Button variant="outline" size="sm" @click="downloadFile(file)">
                    <Download :size="14" class="mr-1" />
                    下载
                  </Button>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>

        <Empty v-if="!loading && folderList.length === 0 && fileList.length === 0" description="暂无文件" />
      </CardContent>
    </Card>

    <div v-if="total > pageSize" class="mt-6">
      <Pagination v-model="pageNum" :total="total" :page-size="pageSize" @change="loadFileList" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Home, Folder, Download, Image as ImageIcon, FileText, Video, Music, File } from 'lucide-vue-next'
import {
  getPublicFilePageApi,
  downloadFileUrl,
  downloadFileAsBlobApi,
  type SysFile,
  type FilePageParams,
} from '@/api/file'
import PageHeader from '@/components/common/PageHeader.vue'
import Card from '@/components/ui/card/Card.vue'
import CardContent from '@/components/ui/card/CardContent.vue'
import Input from '@/components/ui/input/Input.vue'
import Button from '@/components/ui/button/Button.vue'
import Table from '@/components/ui/table/Table.vue'
import TableHeader from '@/components/ui/table/TableHeader.vue'
import TableBody from '@/components/ui/table/TableBody.vue'
import TableRow from '@/components/ui/table/TableRow.vue'
import TableHead from '@/components/ui/table/TableHead.vue'
import TableCell from '@/components/ui/table/TableCell.vue'
import Pagination from '@/components/ui/pagination/Pagination.vue'
import Empty from '@/components/ui/empty/Empty.vue'

const loading = ref(false)
const searchKeyword = ref('')
const currentFolderId = ref<string | null>(null)
const breadcrumbs = ref<{ id: string | null; label: string }[]>([])

const folderList = ref<SysFile[]>([])
const fileList = ref<SysFile[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

onMounted(() => {
  loadFileList()
})

watch(currentFolderId, () => {
  pageNum.value = 1
  loadFileList()
})

async function loadFileList() {
  loading.value = true
  try {
    const params: FilePageParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      folderId: currentFolderId.value || undefined,
      originalName: searchKeyword.value || undefined,
    }
    const res = await getPublicFilePageApi(params)
    const records = res.data.records || []
    folderList.value = records.filter((r: SysFile) => r.isFolder === 1)
    fileList.value = records.filter((r: SysFile) => !r.isFolder)
    total.value = res.data.total || 0
  } catch (e) {
    console.error('加载文件列表失败', e)
  } finally {
    loading.value = false
  }
}

function openFolder(folder: SysFile) {
  breadcrumbs.value.push({ id: folder.id, label: folder.fileName || folder.originalName || '' })
  currentFolderId.value = folder.id
}

function goToFolder(folderId: string | null) {
  currentFolderId.value = folderId
  if (folderId === null) {
    breadcrumbs.value = []
  } else {
    const idx = breadcrumbs.value.findIndex((c) => c.id === folderId)
    if (idx >= 0) {
      breadcrumbs.value = breadcrumbs.value.slice(0, idx + 1)
    }
  }
}

function handleSearch() {
  pageNum.value = 1
  loadFileList()
}

function downloadFile(file: SysFile) {
  const fileName = file.originalName || file.fileName || 'download'
  downloadFileAsBlobApi(file.id)
    .then((res: any) => {
      const blob = res.data || res
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName
      a.click()
      URL.revokeObjectURL(url)
    })
    .catch(() => {
      window.open(downloadFileUrl(file.id), '_blank')
    })
}

function getFileIcon(type: string) {
  switch (type) {
    case 'image':
      return ImageIcon
    case 'document':
      return FileText
    case 'video':
      return Video
    case 'audio':
      return Music
    default:
      return File
  }
}

function formatFileSize(bytes: number): string {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(2) + ' ' + units[i]
}
</script>
