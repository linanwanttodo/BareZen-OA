<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="title" :label="$t('kb.title')">
          <a-input v-model="searchForm.title" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 200px" />
        </a-form-item>
        <a-form-item field="categoryId" :label="$t('kb.category')">
          <a-select v-model="searchForm.categoryId" :placeholder="$t('common.all')" allow-clear style="width: 150px">
            <a-option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="status" :label="$t('common.status')">
          <a-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <a-option value="0">{{ $t('kb.statusDraft') }}</a-option>
            <a-option value="1">{{ $t('kb.statusPublished') }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">{{ $t('common.search') }}</a-button>
            <a-button @click="handleReset">{{ $t('common.reset') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 表格 -->
    <a-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('kb.docList') }}</span>
          <a-button v-permission="PERM.kb.documentAdd" type="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </a-button>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('kb.title')" data-index="title" />
            <a-table-column :title="$t('kb.category')" :width="110">
              <template #cell="{ record }">
                <a-tag>{{ categoryName(record.categoryId) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('kb.deptName')" data-index="deptName" :width="110">
              <template #cell="{ record }">{{ record.deptName || $t('kb.allCompany') }}</template>
            </a-table-column>
            <a-table-column :title="$t('kb.authorName')" data-index="authorName" :width="100" />
            <a-table-column :title="$t('kb.viewCount')" data-index="viewCount" :width="80" align="right" />
            <a-table-column :title="$t('common.status')" :width="80">
              <template #cell="{ record }">
                <a-tag :color="record.status === '1' ? 'green' : 'default'">
                  {{ record.status === '1' ? $t('kb.statusPublished') : $t('kb.statusDraft') }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="280" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-permission="PERM.kb.documentList" size="small" @click="handleView(record)">{{ $t('kb.view') }}</a-button>
                  <a-button v-permission="PERM.kb.documentList" size="small" @click="handleVersions(record)">{{ $t('kb.versions') }}</a-button>
                  <a-button v-permission="PERM.kb.documentEdit" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</a-button>
                  <a-button v-if="record.status === '0'" v-permission="PERM.kb.documentEdit" size="small" type="primary" @click="handlePublish(record)">
                    {{ $t('kb.publish') }}
                  </a-button>
                  <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button v-permission="PERM.kb.documentDelete" size="small" status="danger">{{ $t('common.delete') }}</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>

        <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
          <a-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      </a-spin>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:visible="editVisible" :title="isEdit ? $t('kb.editDoc') : $t('kb.addDoc')"
             :width="720" @ok="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <a-form :model="form" layout="vertical">
        <a-form-item field="title" :label="$t('kb.title')" required>
          <a-input v-model="form.title" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="categoryId" :label="$t('kb.category')">
              <a-select v-model="form.categoryId" allow-clear>
                <a-option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="deptName" :label="$t('kb.deptName')">
              <a-input v-model="form.deptName" :placeholder="$t('kb.deptPlaceholder')" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item field="content" :label="$t('kb.content')">
          <a-textarea v-model="form.content" :placeholder="$t('common.inputPlaceholder')" :auto-size="{ minRows: 8, maxRows: 16 }" />
        </a-form-item>
        <a-form-item field="status" :label="$t('common.status')">
          <a-select v-model="form.status">
            <a-option value="0">{{ $t('kb.statusDraft') }}</a-option>
            <a-option value="1">{{ $t('kb.statusPublished') }}</a-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 文档详情弹窗 -->
    <a-modal v-model:visible="viewVisible" :title="currentDoc?.title" :width="720" footer-only>
      <div class="prose max-w-none">
        <div v-if="currentDoc?.content" class="whitespace-pre-wrap break-words" v-html="safeContent"></div>
        <a-empty v-else :description="$t('kb.noContent')" />
      </div>
    </a-modal>

    <!-- 版本历史弹窗 -->
    <a-modal v-model:visible="versionsVisible" :title="versionDoc?.title ? $t('kb.versions') + ' - ' + versionDoc.title : $t('kb.versions')" :width="680" footer-only>
      <a-spin :loading="versionsLoading">
        <a-table v-if="versions.length" :data="versions" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('kb.versionNo')" :width="80" align="center">
              <template #cell="{ record }">
                <a-tag color="arcoblue">v{{ record.versionNo }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('kb.title')" data-index="title" />
            <a-table-column :title="$t('kb.operatorName')" data-index="operatorName" :width="100" />
            <a-table-column :title="$t('kb.versionTime')" data-index="createTime" :width="170" />
          </template>
        </a-table>
        <a-empty v-else-if="!versionsLoading" :description="$t('common.noData')" />
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, onMounted, computed } from 'vue'
import { Message } from '@arco-design/web-vue'
import DOMPurify from 'dompurify'
import { getDocPageApi, getDocApi, addDocApi, updateDocApi, deleteDocApi, changeDocStatusApi, getDocVersionsApi, getKbCategoryListApi, type KbDocument, type KbDocumentVersion, type KbCategory } from '@/api/kb'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const tableData = ref<KbDocument[]>([])
const categories = ref<KbCategory[]>([])
const editVisible = ref(false)
const viewVisible = ref(false)
const versionsVisible = ref(false)
const versionsLoading = ref(false)
const versions = ref<KbDocumentVersion[]>([])
const versionDoc = ref<KbDocument | null>(null)
const isEdit = ref(false)
const currentDoc = ref<KbDocument | null>(null)

const searchForm = reactive({ title: '', categoryId: '' as string, status: '' })

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const form = reactive<Partial<KbDocument>>({})

const safeContent = computed(() => {
  return currentDoc.value?.content ? DOMPurify.sanitize(currentDoc.value.content) : ''
})

const categoryMap: Record<string, string> = {
  general: 'enums.kbCategory.general', manual: 'enums.kbCategory.manual', process: 'enums.kbCategory.process', tech: 'enums.kbCategory.tech', other: 'enums.kbCategory.other',
}

function categoryName(categoryId: string | undefined): string {
  if (!categoryId) return '-'
  const c = categories.value.find((x) => String(x.id) === String(categoryId))
  return c ? c.name : '-'
}

function categoryText(category: string | undefined): string {
  return category != null ? t(categoryMap[category] || '-') : '-'
}

async function loadCategories() {
  try {
    const res = await getKbCategoryListApi({ status: 1 })
    categories.value = res.data || []
  } catch {
    categories.value = []
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDocPageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      title: searchForm.title || undefined,
      categoryId: searchForm.categoryId || undefined,
      status: searchForm.status || undefined,
    })
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  searchForm.title = ''
  searchForm.categoryId = ''
  searchForm.status = ''
  pagination.current = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  form.status = '0'
  form.authorName = userStore.userInfo?.nickName || ''
  editVisible.value = true
}

function handleEdit(record: KbDocument) {
  isEdit.value = true
  Object.assign(form, record)
  editVisible.value = true
}

async function handleSave() {
  if (!form.title) {
    Message.warning(t('common.msg.docTitleRequired'))
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateDocApi(form)
    } else {
      await addDocApi(form)
    }
    Message.success(t('common.saveSuccess'))
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: KbDocument) {
  await deleteDocApi(record.id)
  Message.success(t('common.deleteSuccess'))
  loadData()
}

async function handlePublish(record: KbDocument) {
  await changeDocStatusApi(record.id, '1')
  Message.success(t('common.msg.published'))
  loadData()
}

async function handleView(record: KbDocument) {
  const res = await getDocApi(record.id)
  currentDoc.value = res.data
  viewVisible.value = true
}

async function handleVersions(record: KbDocument) {
  versionDoc.value = record
  versionsLoading.value = true
  versionsVisible.value = true
  try {
    const res = await getDocVersionsApi(record.id)
    versions.value = res.data || []
  } finally {
    versionsLoading.value = false
  }
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>
