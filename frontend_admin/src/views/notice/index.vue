<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4" :bordered="true">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="noticeTitle" label="公告标题">
          <a-input v-model="searchForm.noticeTitle" placeholder="请输入公告标题" allow-clear style="width: 200px" />
        </a-form-item>
        <a-form-item field="noticeType" label="公告类型">
          <a-select v-model="searchForm.noticeType" placeholder="请选择" allow-clear style="width: 120px">
            <a-option value="1">通知</a-option>
            <a-option value="2">公告</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="status" label="状态">
          <a-select v-model="searchForm.status" placeholder="请选择" allow-clear style="width: 120px">
            <a-option value="0">草稿</a-option>
            <a-option value="1">已发布</a-option>
            <a-option value="2">已下架</a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 表格区域 -->
    <a-card :bordered="true">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>通知公告</span>
          <a-button type="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            新增
          </a-button>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" :row-key="'id'">
          <template #columns>
            <a-table-column title="ID" data-index="id" :width="80" />
            <a-table-column title="公告标题" :width="200">
              <template #cell="{ record }">
                <a-link @click="handleView(record)">{{ record.noticeTitle }}</a-link>
              </template>
            </a-table-column>
            <a-table-column title="类型" :width="100">
              <template #cell="{ record }">
                <a-tag :color="record.noticeType === '1' ? 'blue' : 'green'">
                  {{ record.noticeType === '1' ? '通知' : '公告' }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column title="可见范围" :width="120">
              <template #cell="{ record }">
                <a-tag color="gray">{{ record.scope === 'public' ? '全部用户' : '仅站内用户' }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column title="状态" :width="100">
              <template #cell="{ record }">
                <a-tag :color="record.status === '1' ? 'green' : record.status === '2' ? 'red' : 'gray'">
                  {{ statusText(record.status) }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column title="创建时间" :width="160">
              <template #cell="{ record }">{{ formatTime(record.createTime) }}</template>
            </a-table-column>
            <a-table-column title="操作" :width="240" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button size="small" @click="handleView(record)">查看</a-button>
                  <a-button size="small" @click="handleEdit(record)">编辑</a-button>
                  <a-button v-if="record.status === '0'" size="small" status="success" @click="handlePublish(record)">发布</a-button>
                  <a-button v-if="record.status === '1'" size="small" status="warning" @click="handleRevoke(record)">下架</a-button>
                  <a-popconfirm content="确认删除该公告？" @ok="handleDelete(record)">
                    <a-button size="small" status="danger">删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>

        <div v-if="tableData.length" class="mt-4 flex justify-end">
          <a-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            show-total
            @change="onPageChange"
          />
        </div>
      </a-spin>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:visible="formDialogVisible"
      :title="formType === 'add' ? '新增公告' : '编辑公告'"
      :width="640"
      :mask-closable="false"
      @ok="onFormConfirm"
      @cancel="formDialogVisible = false"
    >
      <a-form :model="formData" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="16">
            <a-form-item field="noticeTitle" label="公告标题" :rules="[{ required: true, message: '请输入公告标题' }]">
              <a-input v-model="formData.noticeTitle" placeholder="请输入公告标题" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item field="noticeType" label="公告类型">
              <a-select v-model="formData.noticeType">
                <a-option value="1">通知</a-option>
                <a-option value="2">公告</a-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item field="noticeContent" label="公告内容" :rules="[{ required: true, message: '请输入公告内容' }]">
          <a-textarea v-model="formData.noticeContent" placeholder="请输入公告内容" :max-length="5000" show-word-limit />
        </a-form-item>
        <a-form-item field="scope" label="可见范围">
          <a-radio-group v-model="formData.scope">
            <a-radio value="public">全部用户（包括游客）</a-radio>
            <a-radio value="internal">仅站内用户</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 查看详情弹窗 -->
    <a-modal
      v-model:visible="detailVisible"
      :title="detailData?.noticeTitle || '公告详情'"
      :width="640"
      :footer="false"
    >
      <div v-if="detailData">
        <div class="mb-4 flex flex-wrap items-center gap-2">
          <a-tag :color="detailData.noticeType === '1' ? 'blue' : 'green'">
            {{ detailData.noticeType === '1' ? '通知' : '公告' }}
          </a-tag>
          <a-tag color="gray">{{ detailData.scope === 'public' ? '全部用户' : '仅站内用户' }}</a-tag>
          <a-tag :color="detailData.status === '1' ? 'green' : 'gray'">
            {{ statusText(detailData.status) }}
          </a-tag>
          <span class="ml-auto text-xs" style="color: #86909C">发布时间：{{ formatTime(detailData.createTime) }}</span>
        </div>
        <a-divider />
        <div class="notice-detail-content" v-html="detailData.noticeContent"></div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import {
  getNoticePageApi,
  addNoticeApi,
  updateNoticeApi,
  deleteNoticeApi,
  publishNoticeApi,
  revokeNoticeApi,
  type SysNotice,
  type NoticeListParams,
} from '@/api/notice'

// ============ 搜索 ============
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

// ============ 表格 ============
const loading = ref(false)
const tableData = ref<SysNotice[]>([])

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

function onPageChange(page: number) {
  pagination.current = page
  loadTable()
}

function statusText(status: string) {
  const map: Record<string, string> = { '0': '草稿', '1': '已发布', '2': '已下架' }
  return map[status] || '未知'
}

function formatTime(time: string) {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
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
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

// ============ 发布/下架 ============
async function handlePublish(row: SysNotice) {
  try {
    await publishNoticeApi(row.id)
    Message.success('发布成功')
    loadTable()
  } catch {
    // ignore
  }
}

async function handleRevoke(row: SysNotice) {
  try {
    await revokeNoticeApi(row.id)
    Message.success('下架成功')
    loadTable()
  } catch {
    // ignore
  }
}

// ============ 删除 ============
async function handleDelete(row: SysNotice) {
  try {
    await deleteNoticeApi(row.id)
    Message.success('删除成功')
    loadTable()
  } catch {
    // ignore
  }
}

// ============ 查看详情 ============
const detailVisible = ref(false)
const detailData = ref<SysNotice | null>(null)

function handleView(row: SysNotice) {
  detailData.value = row
  detailVisible.value = true
}

// ============ 新增/编辑 ============
const formDialogVisible = ref(false)
const formType = ref<'add' | 'edit'>('add')

const defaultFormData = {
  id: undefined as string | undefined,
  noticeTitle: '',
  noticeType: '1',
  noticeContent: '',
  scope: 'public',
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
    status: row.status,
  })
  formDialogVisible.value = true
}

async function onFormConfirm() {
  try {
    if (formType.value === 'add') {
      await addNoticeApi(formData)
      Message.success('新增成功')
    } else {
      await updateNoticeApi(formData)
      Message.success('修改成功')
    }
    formDialogVisible.value = false
    loadTable()
  } catch {
    // ignore
  }
}

// ============ 初始化 ============
onMounted(() => {
  loadTable()
})
</script>

<style scoped>
.notice-detail-content {
  line-height: 1.8;
  color: #1D2129;
  font-size: 14px;
  white-space: pre-wrap;
}
</style>
