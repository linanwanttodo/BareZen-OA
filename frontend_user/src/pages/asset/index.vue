<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('asset.title') }}</h1>
      <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('asset.subtitle') }}</p>
    </div>

    <!-- Tab：可借资产 / 我的借用 -->
    <t-tabs v-model:active-key="activeTab" class="mb-6" @change="onTabChange">
      <t-tab-panel value="available" :label="$t('asset.available')" />
      <t-tab-panel value="myBorrows" :label="$t('asset.myBorrows')" />
    </t-tabs>

    <!-- 可借资产 -->
    <div v-if="activeTab === 'available'">
      <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div class="flex flex-wrap gap-2">
          <button
            v-for="cat in categoryOptions"
            :key="cat.id"
            class="whitespace-nowrap rounded-wecom-md border px-3 py-1.5 text-sm transition-colors"
            :class="activeCategoryId === cat.id
              ? 'border-[var(--wecom-primary)] bg-[var(--wecom-primary-light)] font-medium text-[var(--wecom-primary)]'
              : 'border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] text-[var(--muted-foreground)] hover:text-[var(--foreground)]'"
            @click="selectCategory(cat.id)"
          >
            {{ cat.name }}
          </button>
        </div>
        <t-input
          v-model="keyword"
          class="sm:w-72"
          clearable
          :placeholder="$t('asset.searchPlaceholder')"
          @enter="search"
          @clear="search"
        >
          <template #suffixIcon>
            <search-icon class="cursor-pointer" @click="search" />
          </template>
        </t-input>
      </div>

      <t-loading :loading="itemLoading">
        <div v-if="items.length" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div
            v-for="item in items"
            :key="item.id"
            class="flex flex-col rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5"
          >
            <div class="flex items-start justify-between gap-2">
              <div class="min-w-0">
                <h3 class="truncate font-semibold text-[var(--foreground)]">{{ item.assetName }}</h3>
                <p class="mt-1 text-xs text-[var(--muted-foreground)]">{{ item.assetNo }}</p>
              </div>
              <t-tag theme="success" variant="light">{{ $t('asset.statusInStock') }}</t-tag>
            </div>
            <div class="mt-3 space-y-1 text-xs text-[var(--muted-foreground)]">
              <p v-if="brandModel(item)">{{ $t('asset.brandModel') }}：{{ brandModel(item) }}</p>
              <p v-if="item.location">{{ $t('asset.location') }}：{{ item.location }}</p>
              <p v-if="categoryName(item.categoryId)">{{ $t('asset.category') }}：{{ categoryName(item.categoryId) }}</p>
            </div>
            <div class="mt-4 flex items-center gap-2">
              <t-button theme="primary" size="small" @click="openBorrow(item)">
                {{ $t('asset.borrow') }}
              </t-button>
              <t-button size="small" variant="outline" @click="openDetail(item)">
                {{ $t('asset.detail') }}
              </t-button>
            </div>
          </div>
        </div>
        <t-empty v-else :description="$t('asset.noAvailable')" />
      </t-loading>

      <div v-if="total > pageSize" class="mt-6">
        <t-pagination v-model:current="currentPage" :total="total" :page-size="pageSize" @change="loadItems" />
      </div>
    </div>

    <!-- 我的借用 -->
    <div v-else>
      <t-loading :loading="borrowLoading">
        <div v-if="borrows.length" class="space-y-3">
          <div
            v-for="record in borrows"
            :key="record.id"
            class="flex flex-wrap items-start justify-between gap-4 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5"
          >
            <div class="min-w-0 flex-1">
              <div class="mb-1 flex flex-wrap items-center gap-2">
                <t-tag :theme="record.status === '1' ? 'success' : 'warning'" variant="light">
                  {{ record.status === '1' ? $t('asset.statusReturned') : $t('asset.statusBorrowing') }}
                </t-tag>
                <span class="font-medium text-[var(--foreground)]">{{ record.assetName || '-' }}</span>
                <span class="text-xs text-[var(--muted-foreground)]">{{ record.assetNo || '-' }}</span>
              </div>
              <div class="space-y-1 text-xs text-[var(--muted-foreground)]">
                <p>{{ $t('asset.borrowTime') }}：{{ record.borrowTime || '-' }}</p>
                <p>{{ $t('asset.expectReturnTime') }}：{{ record.expectReturnTime || '-' }}</p>
                <p v-if="record.status === '1'">{{ $t('asset.returnTime') }}：{{ record.returnTime || '-' }}</p>
                <p v-if="record.remark">{{ $t('asset.remark') }}：{{ record.remark }}</p>
              </div>
            </div>
            <t-button
              v-if="record.status === '0'"
              size="small"
              theme="primary"
              variant="outline"
              :loading="returningId === record.id"
              @click="handleReturn(record)"
            >
              {{ $t('asset.return') }}
            </t-button>
          </div>
        </div>
        <t-empty v-else :description="$t('asset.noBorrows')" />
      </t-loading>

      <div v-if="borrowTotal > borrowPageSize" class="mt-6">
        <t-pagination
          v-model:current="borrowPage"
          :total="borrowTotal"
          :page-size="borrowPageSize"
          @change="loadBorrows"
        />
      </div>
    </div>

    <!-- 资产详情弹窗 -->
    <t-dialog v-model:visible="detailVisible" :header="detail?.assetName || $t('asset.detail')" :width="560" :footer="false">
      <t-loading :loading="detailLoading">
        <div v-if="detail" class="space-y-3 text-sm">
          <div class="flex items-center justify-between">
            <span class="text-[var(--muted-foreground)]">{{ $t('asset.assetNo') }}</span>
            <span class="font-medium text-[var(--foreground)]">{{ detail.assetNo }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-[var(--muted-foreground)]">{{ $t('asset.category') }}</span>
            <span class="font-medium text-[var(--foreground)]">{{ categoryName(detail.categoryId) || '-' }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-[var(--muted-foreground)]">{{ $t('asset.brandModel') }}</span>
            <span class="font-medium text-[var(--foreground)]">{{ brandModel(detail) || '-' }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-[var(--muted-foreground)]">{{ $t('asset.location') }}</span>
            <span class="font-medium text-[var(--foreground)]">{{ detail.location || '-' }}</span>
          </div>
        </div>
      </t-loading>
    </t-dialog>

    <!-- 借用弹窗 -->
    <t-dialog
      v-model:visible="borrowVisible"
      :header="$t('asset.borrow')"
      :width="480"
      @confirm="handleBorrow"
      @cancel="borrowVisible = false"
      :ok-loading="submitting"
    >
      <t-form :data="borrowForm" layout="vertical">
        <t-form-item :label="$t('asset.assetName')">
          <t-input :model-value="borrowForm.assetName" disabled />
        </t-form-item>
        <t-form-item :label="$t('asset.expectReturnTime')">
          <t-date-picker
            v-model="borrowForm.expectReturnTime"
            style="width: 100%"
            :placeholder="$t('asset.expectReturnTimePlaceholder')"
            value-type="YYYY-MM-DD"
          />
        </t-form-item>
        <t-form-item :label="$t('asset.remark')">
          <t-textarea
            v-model="borrowForm.remark"
            :placeholder="$t('asset.remarkPlaceholder')"
            :max-length="500"
            show-word-limit
          />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { SearchIcon } from 'tdesign-icons-vue-next'
import { useI18n } from 'vue-i18n'
import {
  getAssetCategoriesApi,
  getAvailableAssetsApi,
  getAssetDetailApi,
  borrowAssetApi,
  getMyBorrowsApi,
  returnAssetApi,
  type AssetCategory,
  type AssetItem,
  type AssetBorrowRecord,
} from '@/api/asset'

const { t } = useI18n()

const activeTab = ref('available')
const itemLoading = ref(false)
const borrowLoading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const returningId = ref('')

const categories = ref<AssetCategory[]>([])
const items = ref<AssetItem[]>([])
const borrows = ref<AssetBorrowRecord[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10
const borrowTotal = ref(0)
const borrowPage = ref(1)
const borrowPageSize = 10

const activeCategoryId = ref<string>('')
const keyword = ref('')
const searchKeyword = ref('')

const detailVisible = ref(false)
const detail = ref<AssetItem | null>(null)

const borrowVisible = ref(false)
const borrowForm = reactive({ assetId: '', assetName: '', expectReturnTime: '', remark: '' })

const categoryOptions = computed(() => [
  { id: '', name: t('asset.allCategories') },
  ...categories.value.map((c) => ({ id: c.id, name: c.name })),
])

function categoryName(categoryId: string | null | undefined): string {
  if (!categoryId) return ''
  return categories.value.find((c) => c.id === categoryId)?.name || ''
}

function brandModel(item: AssetItem): string {
  return [item.brand, item.model].filter(Boolean).join(' ')
}

function selectCategory(id: string) {
  activeCategoryId.value = id
  currentPage.value = 1
  loadItems()
}

function search() {
  searchKeyword.value = keyword.value.trim()
  currentPage.value = 1
  loadItems()
}

async function loadCategories() {
  try {
    const res = await getAssetCategoriesApi()
    categories.value = res.data || []
  } catch {
    // 分类加载失败不阻塞资产列表
  }
}

async function loadItems() {
  itemLoading.value = true
  try {
    const res = await getAvailableAssetsApi({
      page: currentPage.value,
      pageSize,
      categoryId: activeCategoryId.value || undefined,
      assetName: searchKeyword.value || undefined,
    })
    items.value = res.data.list || []
    total.value = res.data.total || 0
  } finally {
    itemLoading.value = false
  }
}

async function loadBorrows() {
  borrowLoading.value = true
  try {
    const res = await getMyBorrowsApi({ page: borrowPage.value, pageSize: borrowPageSize })
    borrows.value = res.data.list || []
    borrowTotal.value = res.data.total || 0
  } finally {
    borrowLoading.value = false
  }
}

function onTabChange(key: string | number) {
  if (key === 'available') loadItems()
  else loadBorrows()
}

async function openDetail(item: AssetItem) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await getAssetDetailApi(item.id)
    detail.value = res.data
  } catch (err) {
    detailVisible.value = false
    MessagePlugin.error((err as Error)?.message || t('asset.detailLoadFailed'))
  } finally {
    detailLoading.value = false
  }
}

function openBorrow(item: AssetItem) {
  borrowForm.assetId = item.id
  borrowForm.assetName = item.assetName
  borrowForm.expectReturnTime = ''
  borrowForm.remark = ''
  borrowVisible.value = true
}

async function handleBorrow() {
  if (!borrowForm.assetId) return
  submitting.value = true
  try {
    await borrowAssetApi(borrowForm.assetId, {
      expectReturnTime: borrowForm.expectReturnTime || undefined,
      remark: borrowForm.remark || undefined,
    })
    MessagePlugin.success(t('asset.borrowSuccess'))
    borrowVisible.value = false
    loadItems()
  } catch (err) {
    MessagePlugin.error((err as Error)?.message || t('asset.borrowFailed'))
  } finally {
    submitting.value = false
  }
}

async function handleReturn(record: AssetBorrowRecord) {
  returningId.value = record.id
  try {
    await returnAssetApi(record.id)
    MessagePlugin.success(t('asset.returnSuccess'))
    loadBorrows()
    loadItems()
  } catch (err) {
    MessagePlugin.error((err as Error)?.message || t('asset.returnFailed'))
  } finally {
    returningId.value = ''
  }
}

onMounted(() => {
  loadCategories()
  loadItems()
})
</script>
