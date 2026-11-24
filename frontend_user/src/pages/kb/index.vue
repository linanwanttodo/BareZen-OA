<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('kb.title') }}</h1>
      <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('kb.subtitle') }}</p>
    </div>

    <div class="flex flex-col gap-6 lg:flex-row">
      <!-- 左侧：知识分类 -->
      <aside class="w-full shrink-0 lg:w-48">
        <div class="flex gap-2 overflow-x-auto lg:flex-col lg:overflow-visible">
          <button
            v-for="cat in categoryOptions"
            :key="cat.id"
            class="whitespace-nowrap rounded-wecom-md border px-4 py-2 text-left text-sm transition-colors"
            :class="activeCategoryId === cat.id
              ? 'border-[var(--wecom-primary)] bg-[var(--wecom-primary-light)] font-medium text-[var(--wecom-primary)]'
              : 'border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] text-[var(--muted-foreground)] hover:text-[var(--foreground)]'"
            @click="selectCategory(cat.id)"
          >
            {{ cat.name }}
          </button>
        </div>
      </aside>

      <!-- 右侧：文档列表 + 搜索 -->
      <div class="min-w-0 flex-1">
        <t-input
          v-model="keyword"
          class="mb-4"
          clearable
          :placeholder="$t('kb.searchPlaceholder')"
          @enter="search"
          @clear="search"
        >
          <template #suffixIcon>
            <search-icon class="cursor-pointer" @click="search" />
          </template>
        </t-input>

        <t-loading :loading="loading">
          <div v-if="documents.length" class="space-y-3">
            <div
              v-for="doc in documents"
              :key="doc.id"
              class="cursor-pointer rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5"
              @click="openDetail(doc.id)"
            >
              <div class="mb-1 flex items-center gap-2">
                <t-tag v-if="categoryName(doc.categoryId)" theme="primary" variant="light">
                  {{ categoryName(doc.categoryId) }}
                </t-tag>
                <h3 class="truncate font-medium text-[var(--foreground)]">{{ doc.title }}</h3>
              </div>
              <p class="mb-2 line-clamp-2 text-sm text-[var(--muted-foreground)]">{{ excerpt(doc.content) }}</p>
              <div class="flex items-center gap-4 text-xs text-[var(--muted-foreground)]">
                <span v-if="doc.authorName">{{ $t('kb.author') }}：{{ doc.authorName }}</span>
                <span>{{ formatDate(doc.createTime) }}</span>
                <span>{{ doc.viewCount ?? 0 }} {{ $t('kb.viewCount') }}</span>
              </div>
            </div>
          </div>
          <t-empty v-else :description="$t('kb.noData')" />
        </t-loading>

        <div v-if="total > pageSize" class="mt-6">
          <t-pagination v-model:current="currentPage" :total="total" :page-size="pageSize" @change="loadDocuments" />
        </div>
      </div>
    </div>

    <!-- 文档详情弹窗 -->
    <t-dialog v-model:visible="detailVisible" :header="detailHeader" :width="720" :footer="false">
      <t-loading :loading="detailLoading">
        <div v-if="detail" class="space-y-4">
          <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-[var(--muted-foreground)]">
            <t-tag v-if="categoryName(detail.categoryId)" theme="primary" variant="light">
              {{ categoryName(detail.categoryId) }}
            </t-tag>
            <span v-if="detail.authorName">{{ $t('kb.author') }}：{{ detail.authorName }}</span>
            <span>{{ formatDate(detail.createTime) }}</span>
            <span>{{ detail.viewCount ?? 0 }} {{ $t('kb.viewCount') }}</span>
          </div>
          <div class="prose max-w-none" v-html="sanitizeHtml(detail.content)"></div>
          <p v-if="detail.remark" class="text-sm text-[var(--muted-foreground)]">
            {{ $t('salary.remark') }}：{{ detail.remark }}
          </p>
        </div>
      </t-loading>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { SearchIcon } from 'tdesign-icons-vue-next'
import { useI18n } from 'vue-i18n'
import { getKbCategoriesApi, getKbDocumentsApi, getKbDocumentDetailApi, type KbCategory, type KbDocument } from '@/api/kb'
import { formatDate } from '@/utils/format'
import { sanitizeHtml } from '@/utils/dom'

const { t } = useI18n()
const loading = ref(false)
const detailLoading = ref(false)
const categories = ref<KbCategory[]>([])
const documents = ref<KbDocument[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10
const activeCategoryId = ref<string>('')
const keyword = ref('')
const searchKeyword = ref('')
const detailVisible = ref(false)
const detail = ref<KbDocument | null>(null)

const categoryOptions = computed(() => [
  { id: '', name: t('kb.allCategories') },
  ...categories.value.map((c) => ({ id: c.id, name: c.name })),
])

const detailHeader = computed(() => (detail.value ? detail.value.title : t('kb.detail')))

function categoryName(categoryId: string | null): string {
  if (!categoryId) return ''
  return categories.value.find((c) => c.id === categoryId)?.name || ''
}

/** 列表摘要：正文为 HTML，去标签取纯文本前 120 字 */
function excerpt(html: string | null): string {
  if (!html) return ''
  const text = html.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim()
  return text.length > 120 ? `${text.slice(0, 120)}...` : text
}

function selectCategory(id: string) {
  activeCategoryId.value = id
  currentPage.value = 1
  loadDocuments()
}

function search() {
  searchKeyword.value = keyword.value.trim()
  currentPage.value = 1
  loadDocuments()
}

async function loadCategories() {
  try {
    const res = await getKbCategoriesApi()
    categories.value = res.data || []
  } catch {
    // 分类加载失败不阻塞文档列表
  }
}

async function loadDocuments() {
  loading.value = true
  try {
    const res = await getKbDocumentsApi({
      page: currentPage.value,
      pageSize,
      categoryId: activeCategoryId.value || undefined,
      title: searchKeyword.value || undefined,
    })
    documents.value = res.data.list || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function openDetail(id: string) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await getKbDocumentDetailApi(id)
    detail.value = res.data
  } catch (err) {
    detailVisible.value = false
    MessagePlugin.error((err as Error)?.message || t('kb.detailLoadFailed'))
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  loadCategories()
  loadDocuments()
})
</script>
