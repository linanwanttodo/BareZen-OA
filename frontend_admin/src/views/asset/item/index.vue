<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <t-form-item field="assetName" :label="$t('asset.assetName')">
          <t-input v-model="searchForm.assetName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 180px" />
        </t-form-item>
        <t-form-item field="assetNo" :label="$t('asset.assetNo')">
          <t-input v-model="searchForm.assetNo" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 160px" />
        </t-form-item>
        <t-form-item field="categoryId" :label="$t('asset.category')">
          <t-select v-model="searchForm.categoryId" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <t-option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item field="status" :label="$t('common.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <t-option value="0">{{ $t('asset.statusStock') }}</t-option>
            <t-option value="1">{{ $t('asset.statusInUse') }}</t-option>
            <t-option value="2">{{ $t('asset.statusRepair') }}</t-option>
            <t-option value="3">{{ $t('asset.statusScrapped') }}</t-option>
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
          <span>{{ $t('asset.title') }}</span>
          <t-button v-permission="PERM.asset.itemAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </t-button>
        </div>
      </template>

      <t-loading :loading="loading">
          <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
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

    <!-- 编辑弹窗 -->
    <t-dialog v-model="editVisible" :title="isEdit ? $t('asset.editItem') : $t('asset.addItem')"
             :width="640" @confirm="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <t-form :model="form" layout="vertical">
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item field="assetNo" :label="$t('asset.assetNo')" required>
              <t-input v-model="form.assetNo" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="assetName" :label="$t('asset.assetName')" required>
              <t-input v-model="form.assetName" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="categoryId" :label="$t('asset.category')">
              <t-select v-model="form.categoryId" allow-clear>
                <t-option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="status" :label="$t('common.status')">
              <t-select v-model="form.status">
                <t-option value="0">{{ $t('asset.statusStock') }}</t-option>
                <t-option value="1">{{ $t('asset.statusInUse') }}</t-option>
                <t-option value="2">{{ $t('asset.statusRepair') }}</t-option>
                <t-option value="3">{{ $t('asset.statusScrapped') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="brand" :label="$t('asset.brand')">
              <t-input v-model="form.brand" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="model" :label="$t('asset.model')">
              <t-input v-model="form.model" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="price" :label="$t('asset.price')">
              <!-- 接口 price 为 string，a-input-number 需要 number，双向桥接转换 -->
              <t-input-number
                :model-value="form.price != null && form.price !== '' ? Number(form.price) : undefined"
                :min="0"
                :precision="2"
                style="width: 100%"
                :placeholder="$t('common.inputPlaceholder')"
                @update:model-value="(val: string | number | undefined) => (form.price = val != null ? String(val) : '')"
              />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="purchaseDate" :label="$t('asset.purchaseDate')">
              
<!-- TODO(date-picker): 核对返回值格式（原 Arco 为字符串） --><t-date-picker v-model="form.purchaseDate" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="holderName" :label="$t('asset.holderName')">
              <t-input v-model="form.holderName" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item field="location" :label="$t('asset.location')">
              <t-input v-model="form.location" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="24">
            <t-form-item field="remark" :label="$t('common.remark')">
              <t-textarea v-model="form.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
    </t-dialog>
    <!-- 借用弹窗 -->
    <t-dialog v-model="borrowVisible" :title="$t('asset.borrow')" :width="480" @confirm="handleBorrowSave" :ok-loading="saving">
      <t-form :model="borrowForm" layout="vertical">
        <t-form-item field="borrowerName" :label="$t('asset.borrowerName')" required>
          <t-input v-model="borrowForm.borrowerName" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item field="expectReturnTime" :label="$t('asset.expectReturnTime')">
          
<!-- TODO(date-picker): 核对返回值格式（原 Arco 为字符串） --><t-date-picker v-model="borrowForm.expectReturnTime" style="width: 100%" />
        </t-form-item>
        <t-form-item field="remark" :label="$t('common.remark')">
          <t-textarea v-model="borrowForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 借用记录弹窗 -->
    <t-dialog v-model="recordsVisible" :title="$t('asset.records')" :width="640" footer-only>
        <t-table row-key="id" :data="records" :columns="tableColumns2" :pagination="false">
  </t-table>
      <t-empty v-if="!records.length" :description="$t('common.noData')" />
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
import { MessagePlugin as Message } from 'tdesign-vue-next'
import { getAssetPageApi, addAssetApi, updateAssetApi, deleteAssetApi, scrapAssetApi, borrowAssetApi, returnAssetApi, getBorrowRecordsApi, getCategoryListApi, type AssetItem, type AssetBorrowRecord, type AssetCategory } from '@/api/asset'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const tableData = ref<AssetItem[]>([])
const categories = ref<AssetCategory[]>([])
const editVisible = ref(false)
const isEdit = ref(false)
const borrowVisible = ref(false)
const recordsVisible = ref(false)
const records = ref<AssetBorrowRecord[]>([])
const currentItem = ref<AssetItem>()

const searchForm = reactive({
  assetName: '',
  assetNo: '',
  categoryId: '' as string,
  status: '',
})

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const form = reactive<Partial<AssetItem>>({})
const borrowForm = reactive<{ borrowerName: string; expectReturnTime?: string; remark?: string }>({ borrowerName: '' })

const categoryMap: Record<string, string> = {
  computer: 'enums.assetCategory.computer', equipment: 'enums.assetCategory.equipment', furniture: 'enums.assetCategory.furniture', vehicle: 'enums.assetCategory.vehicle', other: 'enums.assetCategory.other',
}

function categoryName(categoryId: string | undefined): string {
  if (!categoryId) return '-'
  const c = categories.value.find((x) => String(x.id) === String(categoryId))
  return c ? c.name : (categoryText(undefined) as string)
}

function categoryText(category: string | undefined): string {
  return category != null ? t(categoryMap[category] || '-') : '-'
}

function statusText(status: string | undefined): string {
  const map: Record<string, string> = { 0: 'enums.assetStatus.inStock', 1: 'enums.assetStatus.inUse', 2: 'enums.assetStatus.maintenance', 3: 'enums.assetStatus.scrapped' }
  return status != null ? t(map[status] || '-') : '-'
}

function statusColor(status: string | undefined): string {
  if (status === '0') return 'blue'
  if (status === '1') return 'green'
  if (status === '2') return 'orange'
  if (status === '3') return 'red'
  return 'default'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getAssetPageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      assetName: searchForm.assetName || undefined,
      assetNo: searchForm.assetNo || undefined,
      categoryId: searchForm.categoryId || undefined,
      status: searchForm.status || undefined,
    })
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await getCategoryListApi({ status: 1 })
    categories.value = res.data || []
  } catch {
    categories.value = []
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  searchForm.assetName = ''
  searchForm.assetNo = ''
  searchForm.categoryId = ''
  searchForm.status = ''
  pagination.current = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  form.assetNo = ''
  form.assetName = ''
  form.status = '0'
  editVisible.value = true
}

function handleEdit(record: AssetItem) {
  isEdit.value = true
  Object.assign(form, record)
  editVisible.value = true
}

async function handleSave() {
  if (!form.assetNo || !form.assetName) {
    Message.warning(t('common.msg.assetNoNameRequired'))
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateAssetApi(form)
    } else {
      await addAssetApi(form)
    }
    Message.success(t('common.saveSuccess'))
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: AssetItem) {
  await deleteAssetApi(record.id)
  Message.success(t('common.deleteSuccess'))
  loadData()
}

async function handleScrap(record: AssetItem) {
  await scrapAssetApi(record.id)
  Message.success(t('common.msg.scrapped'))
  loadData()
}

function handleBorrow(record: AssetItem) {
  currentItem.value = record
  borrowForm.borrowerName = ''
  borrowForm.expectReturnTime = undefined
  borrowForm.remark = ''
  borrowVisible.value = true
}

async function handleBorrowSave() {
  if (!borrowForm.borrowerName) {
    Message.warning(t('asset.borrowerNameRequired'))
    return
  }
  saving.value = true
  try {
    await borrowAssetApi(currentItem.value!.id, {
      borrowerName: borrowForm.borrowerName,
      expectReturnTime: borrowForm.expectReturnTime,
      remark: borrowForm.remark,
    })
    Message.success(t('common.saveSuccess'))
    borrowVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleReturn(record: AssetItem) {
  await returnAssetApi(record.id)
  Message.success(t('asset.returned'))
  loadData()
}

async function handleRecords(record: AssetItem) {
  const res = await getBorrowRecordsApi(record.id)
  records.value = res.data || []
  recordsVisible.value = true
}

onMounted(() => {
  loadCategories()
  loadData()
})

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    ,
  ])

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns2 = computed(() => [
    ,
  ])

</script>
