<template>
  <div class="cloud-drive">
    <div class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <template v-if="sidebarCollapsed">
          <Button variant="ghost" size="icon" @click="sidebarCollapsed = false" title="展开">
            <PanelLeft :size="16" />
          </Button>
        </template>
        <template v-else>
          <span class="sidebar-title">文件管理</span>
          <Button variant="ghost" size="icon" @click="sidebarCollapsed = true" title="折叠">
            <PanelLeftClose :size="16" />
          </Button>
        </template>
      </div>
      <div v-show="!sidebarCollapsed" class="sidebar-body">
        <div
          v-for="item in folderFlat"
          :key="item.node.id"
          class="tree-node"
          :class="{ active: currentFolderId === item.node.id }"
          :style="{ paddingLeft: 8 + item.depth * 16 + 'px' }"
          @click="onFolderSelect(item.node.id)"
        >
          <Folder :size="16" class="folder-icon" />
          <span class="tree-node-label">{{ item.node.fileName }}</span>
        </div>
        <div v-if="!folderFlat.length" class="empty-tip">暂无文件夹</div>
      </div>
    </div>

    <div class="main-content">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="flex flex-wrap items-center gap-1 text-sm">
            <button class="breadcrumb-item" @click="goToFolder(null)"><Home :size="14" /> 全部文件</button>
            <template v-for="crumb in breadcrumbs" :key="crumb.id">
              <span class="text-[var(--muted-foreground)]">/</span>
              <button class="breadcrumb-item" @click="goToFolder(crumb.id)"><Folder :size="14" /> {{ crumb.label }}</button>
            </template>
          </div>
        </div>

        <div class="toolbar-right">
          <Input
            :model-value="searchKeyword"
            placeholder="搜索文件/文件夹"
            class="w-64"
            @update:model-value="(v) => { searchKeyword = v; handleSearch() }"
          />
          <RadioGroup v-model="viewMode" :options="viewModeOptions" />
          <Button variant="default" @click="openCreateFolder">
            <FolderPlus :size="16" />
            新建
          </Button>
          <Button @click="openUpload">
            <Upload :size="16" />
            上传
          </Button>
        </div>
      </div>

      <div class="content-body">
        <div v-if="loading" class="py-16"><Empty description="加载中" /></div>
        <template v-else-if="viewMode === 'grid'">
          <div v-if="folderList.length === 0 && fileList.length === 0" class="py-16"><Empty description="暂无文件" /></div>
          <div class="grid-view">
            <div
              v-for="item in folderList"
              :key="'f-' + item.id"
              class="grid-item grid-item-folder"
              @dblclick="openFolder(item)"
            >
              <div class="grid-item-icon"><Folder :size="48" /></div>
              <div class="grid-item-name">{{ item.fileName }}</div>
              <div class="grid-item-meta">
                <Badge variant="outline" size="sm">{{ item.scope === 'public' ? '公开' : '管理端' }}</Badge>
              </div>
              <div class="grid-item-actions">
                <Button variant="ghost" size="icon" title="重命名" @click.stop="openRename(item)"><Pencil :size="15" /></Button>
                <Button variant="ghost" size="icon" title="删除" @click.stop="confirmDelete(item)"><Trash2 :size="15" /></Button>
              </div>
            </div>

            <div
              v-for="item in fileList"
              :key="'x-' + item.id"
              class="grid-item"
              :class="{ selected: selectedFile?.id === item.id }"
              @click="selectedFile = selectedFile?.id === item.id ? null : item"
            >
              <div class="grid-item-icon"><component :is="fileIconComp(item.fileType)" :size="48" /></div>
              <div class="grid-item-name">{{ item.originalName }}</div>
              <div class="grid-item-meta">
                {{ formatFileSize(item.fileSize) }}
                <Badge variant="outline" size="sm" class="ml-1">{{ item.scope === 'public' ? '公开' : '管理端' }}</Badge>
              </div>
              <div class="grid-item-actions">
                <Button variant="ghost" size="icon" title="下载" @click.stop="downloadFile(item)"><Download :size="15" /></Button>
                <Button variant="ghost" size="icon" title="重命名" @click.stop="openRename(item)"><Pencil :size="15" /></Button>
                <Button variant="ghost" size="icon" title="删除" @click.stop="confirmDelete(item)"><Trash2 :size="15" /></Button>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div v-if="!allItems.length" class="py-16"><Empty description="暂无文件" /></div>
          <Table v-else>
            <TableHeader>
              <TableRow>
                <TableHead>名称</TableHead>
                <TableHead>大小</TableHead>
                <TableHead>可见范围</TableHead>
                <TableHead>类型</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead class="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="row in allItems" :key="row.id">
                <TableCell>
                  <div class="row-name" :class="{ 'cursor-pointer': row.isFolder }" @click="row.isFolder ? openFolder(row) : null">
                    <Folder v-if="row.isFolder" :size="16" class="mr-1" />
                    <component v-else :is="fileIconComp(row.fileType)" :size="16" class="mr-1" />
                    {{ row.isFolder ? row.fileName : row.originalName }}
                  </div>
                </TableCell>
                <TableCell>{{ row.isFolder ? '-' : formatFileSize(row.fileSize) }}</TableCell>
                <TableCell>
                  <Badge variant="outline">{{ row.scope === 'public' ? '公开' : '管理端' }}</Badge>
                </TableCell>
                <TableCell>{{ row.fileType }}</TableCell>
                <TableCell>{{ row.createTime }}</TableCell>
                <TableCell>
                  <div class="flex justify-end gap-1">
                    <Button v-if="!row.isFolder" variant="ghost" size="icon" title="下载" @click="downloadFile(row)"><Download :size="15" /></Button>
                    <Button variant="ghost" size="icon" title="重命名" @click="openRename(row)"><Pencil :size="15" /></Button>
                    <Button variant="ghost" size="icon" title="删除" @click="confirmDelete(row)"><Trash2 :size="15" /></Button>
                  </div>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>

          <div v-if="total > 0" class="mt-4 flex justify-end">
            <Pagination v-model="pageNum" :total="total" :page-size="pageSize" @change="onPageChange" />
          </div>
        </template>
      </div>
    </div>

    <!-- 创建文件夹 -->
    <Dialog :open="createFolderVisible" title="新建文件夹" @update:open="createFolderVisible = $event">
      <div class="space-y-4">
        <div class="space-y-1.5">
          <Label>文件夹名称</Label>
          <Input v-model="newFolderName" placeholder="请输入文件夹名称" />
        </div>
        <div class="space-y-1.5">
          <Label>可见范围</Label>
          <RadioGroup v-model="newFolderScope" :options="scopeOptions" />
        </div>
      </div>
      <template #footer>
        <Button variant="outline" @click="createFolderVisible = false">取消</Button>
        <Button @click="handleCreateFolder">确定</Button>
      </template>
    </Dialog>

    <!-- 上传文件 -->
    <Dialog :open="uploadVisible" title="上传文件" @update:open="uploadVisible = $event">
      <div class="space-y-4">
        <label class="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-md border border-dashed border-[var(--border)] bg-[var(--muted)] p-8 text-center">
          <Upload :size="28" class="text-[var(--muted-foreground)]" />
          <span class="text-sm text-[var(--muted-foreground)]">点击选择文件（可多选）</span>
          <input ref="fileInputRef" type="file" multiple class="hidden" @change="onFileChange" />
        </label>
        <ul v-if="selectedFiles.length" class="max-h-32 space-y-1 overflow-y-auto text-sm text-[var(--foreground)]">
          <li v-for="(f, i) in selectedFiles" :key="i" class="truncate">{{ f.name }}（{{ formatFileSize(f.size) }}）</li>
        </ul>
        <div class="space-y-1.5">
          <Label>可见范围</Label>
          <RadioGroup v-model="uploadScope" :options="scopeOptions" />
        </div>
      </div>
      <template #footer>
        <Button variant="outline" @click="uploadVisible = false">取消</Button>
        <Button :disabled="uploading" @click="handleUploadConfirm">开始上传</Button>
      </template>
    </Dialog>

    <!-- 重命名 -->
    <Dialog :open="renameVisible" title="重命名" @update:open="renameVisible = $event">
      <div class="space-y-1.5">
        <Label>名称</Label>
        <Input v-model="renameName" placeholder="请输入新名称" />
      </div>
      <template #footer>
        <Button variant="outline" @click="renameVisible = false">取消</Button>
        <Button @click="handleRename">确定</Button>
      </template>
    </Dialog>

    <!-- 删除确认 -->
    <Dialog :open="deleteVisible" title="确认删除" @update:open="deleteVisible = $event">
      <p>确定要删除「{{ deleteTarget?.isFolder ? deleteTarget?.fileName : deleteTarget?.originalName }}」吗？</p>
      <p v-if="deleteTarget?.isFolder" class="text-warning">文件夹内的所有文件将被一并删除，且不可恢复。</p>
      <template #footer>
        <Button variant="outline" @click="deleteVisible = false">取消</Button>
        <Button variant="destructive" @click="handleDelete">删除</Button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import {
  Folder,
  Home,
  PanelLeft,
  PanelLeftClose,
  FolderPlus,
  Upload,
  Download,
  Pencil,
  Trash2,
  Image,
  FileText,
  Video,
  Music,
  File,
  LayoutGrid,
  List,
} from 'lucide-vue-next'
import {
  getFilePageApi,
  getFolderTreeApi,
  createFolderApi,
  renameFileApi,
  deleteFileApi,
  uploadFileApi,
  downloadFileUrl,
  downloadFileAsBlobApi,
  type SysFile,
  type FilePageParams,
} from '@/api/file'
import { Toast } from '@/utils/toast'
import Label from '@/components/ui/label/Label.vue'
import Input from '@/components/ui/input/Input.vue'
import Button from '@/components/ui/button/Button.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import RadioGroup from '@/components/ui/radio-group/RadioGroup.vue'
import Table from '@/components/ui/table/Table.vue'
import TableHeader from '@/components/ui/table/TableHeader.vue'
import TableBody from '@/components/ui/table/TableBody.vue'
import TableRow from '@/components/ui/table/TableRow.vue'
import TableHead from '@/components/ui/table/TableHead.vue'
import TableCell from '@/components/ui/table/TableCell.vue'
import Pagination from '@/components/ui/pagination/Pagination.vue'
import Empty from '@/components/ui/empty/Empty.vue'
import Dialog from '@/components/ui/dialog/Dialog.vue'

const viewModeOptions = [
  { label: '', value: 'grid', icon: LayoutGrid },
  { label: '', value: 'table', icon: List },
]
const scopeOptions = [
  { label: '管理端可见', value: 'admin' },
  { label: '所有用户可见', value: 'public' },
]

const loading = ref(false)
const sidebarCollapsed = ref(false)
const viewMode = ref<'grid' | 'table'>('grid')
const searchKeyword = ref('')

const currentFolderId = ref<string | null>(null)
const folderTree = ref<any[]>([])
const breadcrumbs = ref<{ id: string | null; label: string }[]>([])

interface FolderFlatRow {
  node: SysFile
  depth: number
}
const folderFlat = computed<FolderFlatRow[]>(() => {
  const result: FolderFlatRow[] = []
  const walk = (nodes: any[], depth: number) => {
    for (const node of nodes) {
      result.push({ node, depth })
      if (node.children && node.children.length) walk(node.children, depth + 1)
    }
  }
  walk(folderTree.value, 0)
  return result
})

const folderList = ref<SysFile[]>([])
const fileList = ref<SysFile[]>([])
const allItems = computed(() => [...folderList.value, ...fileList.value])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

const selectedFile = ref<SysFile | null>(null)

const createFolderVisible = ref(false)
const newFolderName = ref('')
const newFolderScope = ref('admin')

const uploadVisible = ref(false)
const uploadScope = ref('admin')
const selectedFiles = ref<File[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)
const uploading = ref(false)

const renameVisible = ref(false)
const renameTarget = ref<SysFile | null>(null)
const renameName = ref('')

const deleteVisible = ref(false)
const deleteTarget = ref<SysFile | null>(null)

onMounted(() => {
  loadFolderTree()
  loadFileList()
})

watch(currentFolderId, () => {
  pageNum.value = 1
  loadFileList()
})

async function loadFolderTree() {
  try {
    const res = await getFolderTreeApi(undefined)
    folderTree.value = buildTreeFromFlatList(res.data || [])
  } catch (e: any) {
    Toast.error('加载文件夹树失败: ' + (e?.message || '请检查权限'))
  }
}

function buildTreeFromFlatList(folders: SysFile[]): any[] {
  const map = new Map<string, any>()
  folders.forEach((f) => map.set(f.id, { ...f, children: [] }))
  const roots: any[] = []
  folders.forEach((f) => {
    const node = map.get(f.id)!
    const pid = f.folderId?.toString() || '0'
    if (pid === '0' || !map.has(pid)) roots.push(node)
    else {
      const parent = map.get(pid)
      if (parent) parent.children.push(node)
    }
  })
  return roots
}

async function loadFileList() {
  loading.value = true
  try {
    const params: FilePageParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      folderId: currentFolderId.value ?? undefined,
      originalName: searchKeyword.value || undefined,
    }
    const res = await getFilePageApi(params)
    const records = res.data.records || []
    folderList.value = records.filter((r: SysFile) => r.isFolder === 1)
    fileList.value = records.filter((r: SysFile) => !r.isFolder)
    total.value = res.data.total || 0
  } catch (e: any) {
    Toast.error('加载文件列表失败: ' + (e?.message || '请检查权限'))
  } finally {
    loading.value = false
  }
}

async function onFolderSelect(val: string | null) {
  currentFolderId.value = val
}

function openFolder(folder: SysFile) {
  breadcrumbs.value.push({ id: folder.id, label: folder.fileName || folder.originalName })
  currentFolderId.value = folder.id
}

function goToFolder(folderId: string | null) {
  currentFolderId.value = folderId
  if (folderId === null) {
    breadcrumbs.value = []
  } else {
    const idx = breadcrumbs.value.findIndex((c) => c.id === folderId)
    if (idx >= 0) breadcrumbs.value = breadcrumbs.value.slice(0, idx + 1)
  }
}

function openCreateFolder() {
  newFolderName.value = ''
  newFolderScope.value = 'admin'
  createFolderVisible.value = true
}

async function handleCreateFolder() {
  if (!newFolderName.value.trim()) {
    Toast.warning('请输入文件夹名称')
    return
  }
  try {
    await createFolderApi(newFolderName.value.trim(), currentFolderId.value || undefined, newFolderScope.value)
    Toast.success('创建成功')
    createFolderVisible.value = false
    await loadFolderTree()
    await loadFileList()
  } catch (e: any) {
    Toast.error(e?.message || '创建失败')
  }
}

function openUpload() {
  selectedFiles.value = []
  uploadScope.value = 'admin'
  if (fileInputRef.value) fileInputRef.value.value = ''
  uploadVisible.value = true
}

function onFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files) selectedFiles.value = Array.from(target.files)
}

async function handleUploadConfirm() {
  if (!selectedFiles.value.length) {
    Toast.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    for (const file of selectedFiles.value) {
      await uploadFileApi(file, currentFolderId.value || undefined, uploadScope.value)
    }
    Toast.success('上传完成')
    uploadVisible.value = false
    await loadFileList()
    await loadFolderTree()
  } catch (e: any) {
    Toast.error(e?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

function openRename(item: SysFile) {
  renameTarget.value = item
  renameName.value = item.fileName || item.originalName
  renameVisible.value = true
}

async function handleRename() {
  if (!renameName.value.trim()) {
    Toast.warning('请输入名称')
    return
  }
  if (!renameTarget.value) return
  try {
    await renameFileApi(renameTarget.value.id, renameName.value.trim())
    Toast.success('重命名成功')
    renameVisible.value = false
    await loadFileList()
    await loadFolderTree()
  } catch (e: any) {
    Toast.error(e?.message || '重命名失败')
  }
}

function confirmDelete(item: SysFile) {
  deleteTarget.value = item
  deleteVisible.value = true
}

async function handleDelete() {
  if (!deleteTarget.value) return
  try {
    await deleteFileApi(deleteTarget.value.id)
    Toast.success('删除成功')
    deleteVisible.value = false
    deleteTarget.value = null
    selectedFile.value = null
    await loadFileList()
    await loadFolderTree()
  } catch (e: any) {
    Toast.error(e?.message || '删除失败')
  }
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

function handleSearch() {
  pageNum.value = 1
  loadFileList()
}

function onPageChange() {
  loadFileList()
}

function fileIconComp(type: string) {
  switch (type) {
    case 'image':
      return Image
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

<style scoped>
.cloud-drive {
  display: flex;
  height: calc(100vh - 100px);
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}
.sidebar {
  width: 260px;
  min-width: 260px;
  background: var(--card);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar.collapsed {
  width: 48px;
  min-width: 48px;
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}
.sidebar-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--foreground);
}
.sidebar-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
}
.tree-node.active {
  background: var(--muted);
}
.folder-icon {
  color: var(--primary);
  flex-shrink: 0;
}
.tree-node-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  padding: 14px 20px;
  background: var(--card);
  border-bottom: 1px solid var(--border);
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.breadcrumb-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--foreground);
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
}
.content-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
.grid-view {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(152px, 1fr));
  gap: 16px;
}
.grid-item {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 14px 12px 12px;
  text-align: center;
  cursor: pointer;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.grid-item.selected {
  border-color: var(--primary);
  background: var(--muted);
}
.grid-item-folder {
  background: var(--muted);
}
.grid-item-icon {
  margin-bottom: 10px;
  color: var(--muted-foreground);
}
.grid-item-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--foreground);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 100%;
  margin-bottom: 6px;
}
.grid-item-meta {
  font-size: 11px;
  color: var(--muted-foreground);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.grid-item-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border);
  width: 100%;
}
.row-name {
  display: flex;
  align-items: center;
  gap: 4px;
}
.empty-tip {
  color: var(--muted-foreground);
  font-size: 13px;
  text-align: center;
  padding: 20px 0;
}
.text-warning {
  color: var(--destructive);
  font-size: 13px;
  margin-top: 8px;
}
</style>
