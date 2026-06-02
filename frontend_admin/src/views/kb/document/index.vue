<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <t-form-item field="title" :label="$t('kb.title')">
          <t-input v-model="searchForm.title" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 200px" />
        </t-form-item>
        <t-form-item field="categoryId" :label="$t('kb.category')">
          <t-select v-model="searchForm.categoryId" :placeholder="$t('common.all')" allow-clear style="width: 150px">
            <t-option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item field="status" :label="$t('common.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <t-option value="0">{{ $t('kb.statusDraft') }}</t-option>
            <t-option value="1">{{ $t('kb.statusPublished') }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" html-type="submit">{{ $t('common.search') }}</t-button>
            <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-card>

    <!-- 表格 -->
    <t-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('kb.docList') }}</span>
          <t-button v-permission="PERM.kb.documentAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </t-button>
        </div>
      </template>

      <t-loading :loading="loading">
          <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
    <template #categoryId="{ row }">
                <t-tag>{{ categoryName(row.categoryId) }}</t-tag>
              </template>
    <template #deptName="{ row }">{{ row.deptName || $t('kb.allCompany') }}</template>
    <template #status="{ row }">
                <t-tag :color="row.status === '1' ? 'green' : 'default'">
                  {{ row.status === '1' ? $t('kb.statusPublished') : $t('kb.statusDraft') }}
                </t-tag>
              </template>
    <template #operate="{ row }">
                <t-space>
                  <t-button v-permission="PERM.kb.documentList" size="small" @click="handleView(row)">{{ $t('kb.view') }}</t-button>
                  <t-button v-permission="PERM.kb.documentList" size="small" @click="handleVersions(row)">{{ $t('kb.versions') }}</t-button>
                  <t-button v-permission="PERM.kb.documentEdit" size="small" @click="handleEdit(row)">{{ $t('common.edit') }}</t-button>
                  <t-button v-if="row.status === '0'" v-permission="PERM.kb.documentEdit" size="small" theme="primary" @click="handlePublish(row)">
                    {{ $t('kb.publish') }}
                  </t-button>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button v-permission="PERM.kb.documentDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
  </t-table>

        <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
          <t-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      </t-loading>
    </t-card>

    <!-- 新增/编辑弹窗 -->
    <t-dialog v-model="editVisible" :title="isEdit ? $t('kb.editDoc') : $t('kb.addDoc')"
             :width="720" @confirm="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <t-form :model="form" layout="vertical">
        <t-form-item field="title" :label="$t('kb.title')" required>
          <t-input v-model="form.title" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item field="categoryId" :label="$t('kb.category')">
              <t-select v-model="form.categoryId" allow-clear>
                <t-option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="deptName" :label="$t('kb.deptName')">
              <t-input v-model="form.deptName" :placeholder="$t('kb.deptPlaceholder')" />
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item field="content" :label="$t('kb.content')">
          <t-textarea v-model="form.content" :placeholder="$t('common.inputPlaceholder')" :auto-size="{ minRows: 8, maxRows: 16 }" />
        </t-form-item>
        <t-form-item field="status" :label="$t('common.status')">
          <t-select v-model="form.status">
            <t-option value="0">{{ $t('kb.statusDraft') }}</t-option>
            <t-option value="1">{{ $t('kb.statusPublished') }}</t-option>
          </t-select>
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 文档详情弹窗 -->
    <t-dialog v-model="viewVisible" :title="currentDoc?.title" :width="720" footer-only>
      <div class="prose max-w-none">
        <div v-if="currentDoc?.content" class="whitespace-pre-wrap break-words" v-html="safeContent"></div>
        <t-empty v-else :description="$t('kb.noContent')" />
      </div>
    </t-dialog>

    <!-- 版本历史弹窗 -->
    <t-dialog v-model="versionsVisible" :title="versionDoc?.title ? $t('kb.versions') + ' - ' + versionDoc.title : $t('kb.versions')" :width="680" footer-only>
      <t-loading :loading="versionsLoading">
          <t-table row-key="id" :data="versions" :columns="tableColumns2" :pagination="false">
    <template #versionNo="{ row }">
                <t-tag theme="primary">v{{ row.versionNo }}</t-tag>
              </template>
  </t-table>
      </t-loading>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, onMounted, computed } from 'vue'
import { MessagePlugin as Message } from 'tdesign-vue-next'
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

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'title', title: t('kb.title') },
    { colKey: 'categoryId', title: t('kb.category'), width: 110 },
    { colKey: 'deptName', title: t('kb.deptName'), width: 110 },
    { colKey: 'authorName', title: t('kb.authorName'), width: 100 },
    { colKey: 'viewCount', title: t('kb.viewCount'), width: 80, align: 'right' },
    { colKey: 'status', title: t('common.status'), width: 80 },
    { colKey: 'operate', title: t('common.operate'), width: 280, align: 'right' },
  ])

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns2 = computed(() => [
    { colKey: 'versionNo', title: t('kb.versionNo'), width: 80, align: 'center' },
    { colKey: 'title_2', title: t('kb.title') },
    { colKey: 'operatorName', title: t('kb.operatorName'), width: 100 },
    { colKey: 'createTime', title: t('kb.versionTime'), width: 170 },
  ])

</script>
