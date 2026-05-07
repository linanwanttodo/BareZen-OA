<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="assetName" :label="$t('asset.assetName')">
          <a-input v-model="searchForm.assetName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 180px" />
        </a-form-item>
        <a-form-item field="assetNo" :label="$t('asset.assetNo')">
          <a-input v-model="searchForm.assetNo" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 160px" />
        </a-form-item>
        <a-form-item field="categoryId" :label="$t('asset.category')">
          <a-select v-model="searchForm.categoryId" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <a-option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="status" :label="$t('common.status')">
          <a-select v-model="searchForm.status" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <a-option value="0">{{ $t('asset.statusStock') }}</a-option>
            <a-option value="1">{{ $t('asset.statusInUse') }}</a-option>
            <a-option value="2">{{ $t('asset.statusRepair') }}</a-option>
            <a-option value="3">{{ $t('asset.statusScrapped') }}</a-option>
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
          <span>{{ $t('asset.title') }}</span>
          <a-button v-permission="PERM.asset.itemAdd" type="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </a-button>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('asset.assetNo')" data-index="assetNo" :width="130" />
            <a-table-column :title="$t('asset.assetName')" data-index="assetName" :width="140" />
            <a-table-column :title="$t('asset.category')" :width="90">
              <template #cell="{ record }">
                <a-tag>{{ categoryName(record.categoryId) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('asset.brandModel')" :width="130">
              <template #cell="{ record }">
                <span class="text-sm">{{ record.brand || '-' }} {{ record.model || '' }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('asset.price')" data-index="price" :width="100" align="right" />
            <a-table-column :title="$t('asset.holderName')" data-index="holderName" :width="90" />
            <a-table-column :title="$t('asset.location')" data-index="location" :width="100" />
            <a-table-column :title="$t('common.status')" :width="80">
              <template #cell="{ record }">
                <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="260" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-permission="PERM.asset.itemBorrow" v-if="record.status === '0'" size="small" type="primary" @click="handleBorrow(record)">
                    {{ $t('asset.borrow') }}
                  </a-button>
                  <a-button v-permission="PERM.asset.itemBorrow" v-else-if="record.status === '1'" size="small" status="success" @click="handleReturn(record)">
                    {{ $t('asset.return') }}
                  </a-button>
                  <a-button size="small" @click="handleRecords(record)">{{ $t('asset.records') }}</a-button>
                  <a-button v-permission="PERM.asset.itemEdit" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</a-button>
                  <a-button v-if="record.status !== '3'" v-permission="PERM.asset.itemEdit" size="small" status="warning" @click="handleScrap(record)">
                    {{ $t('asset.scrap') }}
                  </a-button>
                  <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button v-permission="PERM.asset.itemDelete" size="small" status="danger">{{ $t('common.delete') }}</a-button>
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

    <!-- 编辑弹窗 -->
    <a-modal v-model:visible="editVisible" :title="isEdit ? $t('asset.editItem') : $t('asset.addItem')"
             :width="640" @ok="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <a-form :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="assetNo" :label="$t('asset.assetNo')" required>
              <a-input v-model="form.assetNo" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="assetName" :label="$t('asset.assetName')" required>
              <a-input v-model="form.assetName" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="categoryId" :label="$t('asset.category')">
              <a-select v-model="form.categoryId" allow-clear>
                <a-option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="status" :label="$t('common.status')">
              <a-select v-model="form.status">
                <a-option value="0">{{ $t('asset.statusStock') }}</a-option>
                <a-option value="1">{{ $t('asset.statusInUse') }}</a-option>
                <a-option value="2">{{ $t('asset.statusRepair') }}</a-option>
                <a-option value="3">{{ $t('asset.statusScrapped') }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="brand" :label="$t('asset.brand')">
              <a-input v-model="form.brand" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="model" :label="$t('asset.model')">
              <a-input v-model="form.model" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="price" :label="$t('asset.price')">
              <!-- 接口 price 为 string，a-input-number 需要 number，双向桥接转换 -->
              <a-input-number
                :model-value="form.price != null && form.price !== '' ? Number(form.price) : undefined"
                :min="0"
                :precision="2"
                style="width: 100%"
                :placeholder="$t('common.inputPlaceholder')"
                @update:model-value="(val) => (form.price = val != null ? String(val) : '')"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="purchaseDate" :label="$t('asset.purchaseDate')">
              <a-date-picker v-model="form.purchaseDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="holderName" :label="$t('asset.holderName')">
              <a-input v-model="form.holderName" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="location" :label="$t('asset.location')">
              <a-input v-model="form.location" :placeholder="$t('common.inputPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item field="remark" :label="$t('common.remark')">
              <a-textarea v-model="form.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
    <!-- 借用弹窗 -->
    <a-modal v-model:visible="borrowVisible" :title="$t('asset.borrow')" :width="480" @ok="handleBorrowSave" :ok-loading="saving">
      <a-form :model="borrowForm" layout="vertical">
        <a-form-item field="borrowerName" :label="$t('asset.borrowerName')" required>
          <a-input v-model="borrowForm.borrowerName" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-form-item field="expectReturnTime" :label="$t('asset.expectReturnTime')">
          <a-date-picker v-model="borrowForm.expectReturnTime" style="width: 100%" />
        </a-form-item>
        <a-form-item field="remark" :label="$t('common.remark')">
          <a-textarea v-model="borrowForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 借用记录弹窗 -->
    <a-modal v-model:visible="recordsVisible" :title="$t('asset.records')" :width="640" footer-only>
      <a-table :data="records" :pagination="false" row-key="id">
        <template #columns>
          <a-table-column :title="$t('asset.borrowerName')" data-index="borrowerName" :width="100" />
          <a-table-column :title="$t('asset.borrowTime')" data-index="borrowTime" :width="170" />
          <a-table-column :title="$t('asset.expectReturnTime')" data-index="expectReturnTime" :width="130" />
          <a-table-column :title="$t('asset.returnTime')" data-index="returnTime" :width="170" />
          <a-table-column :title="$t('common.status')" :width="90">
            <template #cell="{ record }">
              <a-tag :color="record.status === '0' ? 'orange' : 'green'">
                {{ record.status === '0' ? $t('asset.borrowing') : $t('asset.returned') }}
              </a-tag>
            </template>
          </a-table-column>
        </template>
      </a-table>
      <a-empty v-if="!records.length" :description="$t('common.noData')" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
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
</script>
