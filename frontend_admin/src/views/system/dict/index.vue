<template>
  <div class="page-container">
    <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <!-- 左侧：字典类型 -->
      <t-card :bordered="true" class="flex flex-col">
        <template #title>
          <div class="flex items-center justify-between w-full">
            <span>{{ $t('dict.title') }}</span>
            <t-button v-permission="PERM.system.dictAdd" theme="primary" size="small" @click="handleAddType">
              <template #icon><icon-plus /></template>
              {{ $t('common.add') }}
            </t-button>
          </div>
        </template>
        <div class="mb-3">
          <a-input-search v-model="typeSearch" :placeholder="$t('dict.searchPlaceholder')" allow-clear />
        </div>
        <div class="dict-type-list flex-1 overflow-y-auto" style="max-height: 500px">
          <div
            v-for="item in filteredDictTypes"
            :key="item.id"
            class="dict-type-item"
            :class="{ 'dict-type-item--active': selectedType?.id === item.id }"
            @click="handleSelectType(item)"
          >
            <div class="dict-type-item__info">
              <span class="dict-type-item__name">{{ item.dictName }}</span>
              <span class="dict-type-item__key">{{ item.dictType }}</span>
            </div>
            <t-tag v-if="item.status === '0'" color="red" size="small">{{ $t('common.disable') }}</t-tag>
            <a-dropdown>
              <icon-more class="cursor-pointer" style="color: var(--wecom-text-tertiary)" />
              <template #content>
                <t-dropdown-item v-permission="PERM.system.dictEdit" @click="handleEditType(item)">{{ $t('common.edit') }}</t-dropdown-item>
                <t-dropdown-item v-permission="PERM.system.dictDelete" @click="handleDeleteType(item)" status="danger">{{ $t('common.delete') }}</t-dropdown-item>
              </template>
            </a-dropdown>
          </div>
          <t-empty v-if="!filteredDictTypes.length" :description="$t('common.noData')" />
        </div>
        <div v-if="typePagination.total > typePagination.pageSize" class="mt-3 pt-3" style="border-top: 1px solid var(--wecom-border)">
          <t-pagination
            v-model:current="typePagination.current"
            :total="typePagination.total"
            :page-size="typePagination.pageSize"
            size="small"
            @change="onTypePageChange"
          />
        </div>
      </t-card>

      <!-- 右侧：字典数据 -->
      <t-card :bordered="true" class="flex flex-col">
        <template #title>
          <div class="flex items-center justify-between w-full">
            <div class="flex items-center gap-2">
              <span>{{ $t('dict.dataTitle') }}</span>
              <t-tag v-if="selectedType" color="blue">{{ selectedType.dictName }}</t-tag>
            </div>
            <t-button v-permission="PERM.system.dictAdd" theme="primary" size="small" :disabled="!selectedType" @click="handleAddData">
              <template #icon><icon-plus /></template>
              {{ $t('common.add') }}
            </t-button>
          </div>
        </template>
        <template v-if="selectedType">
          <div class="mb-3 flex items-center gap-2">
            <t-input v-model="dataSearchForm.dictLabel" :placeholder="$t('dict.dictLabel')" clearable style="width: 180px" />
            <t-button theme="primary" size="small" @click="loadData">{{ $t('common.search') }}</t-button>
          </div>

          <a-spin :loading="dataLoading">
            <a-table :data="dataTable" :pagination="false" :row-key="'id'">
              <template #columns>
                <a-table-column :title="$t('common.operate')" data-index="id" :width="60" />
                <a-table-column :title="$t('dict.dictLabel')" data-index="dictLabel" />
                <a-table-column :title="$t('dict.dictValue')" data-index="dictValue" />
                <a-table-column :title="$t('dept.orderNum')" data-index="dictSort" :width="60" />
                <a-table-column :title="$t('dict.isDefault')" :width="60">
                  <template #cell="{ record }">
                    <t-tag :color="record.isDefault === 'Y' ? 'blue' : 'gray'" size="small">
                      {{ record.isDefault === 'Y' ? $t('common.yes') : $t('common.no') }}
                    </t-tag>
                  </template>
                </a-table-column>
                <a-table-column :title="$t('common.status')" :width="80">
                  <template #cell="{ record }">
                    <t-tag :color="record.status === '1' ? 'green' : 'red'" size="small">
                      {{ record.status === '1' ? $t('common.normal') : $t('common.disable') }}
                    </t-tag>
                  </template>
                </a-table-column>
                <a-table-column :title="$t('common.operate')" :width="120" align="right">
                  <template #cell="{ record }">
                    <t-space>
                      <t-button v-permission="PERM.system.dictEdit" size="small" @click="handleEditData(record)">{{ $t('common.edit') }}</t-button>
                      <t-popconfirm v-permission="PERM.system.dictDelete" :content="$t('dict.deleteDataConfirm')" @confirm="handleDeleteData(record)">
                        <t-button size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                      </t-popconfirm>
                    </t-space>
                  </template>
                </a-table-column>
              </template>
            </a-table>

            <div v-if="dataPagination.total > dataPagination.pageSize" class="mt-3 flex justify-end">
              <t-pagination
                v-model:current="dataPagination.current"
                :total="dataPagination.total"
                :page-size="dataPagination.pageSize"
                size="small"
                @change="onDataPageChange"
              />
            </div>
          </a-spin>
        </template>
        <t-empty v-else :description="$t('dict.selectTypeHint')" />
      </t-card>
    </div>

    <!-- 字典类型 新增/编辑弹窗 -->
    <t-dialog
      v-model="typeFormVisible"
      :header="typeFormType === 'add' ? $t('dict.addType') : $t('dict.editType')"
      width="480px"
      :close-on-overlay-click="false"
      @confirm="onTypeFormConfirm"
      @close="typeFormVisible = false"
    >
      <t-form :model="typeFormData" layout="vertical">
        <t-form-item name="dictName" :label="$t('dict.dictName')" :rules="[{ required: true, message: $t('dict.dictName') }]">
          <t-input v-model="typeFormData.dictName" :placeholder="$t('dict.dictName')" />
        </t-form-item>
        <t-form-item name="dictType" :label="$t('dict.dictType')" :rules="[{ required: true, message: $t('dict.dictType') }]">
          <t-input v-model="typeFormData.dictType" :placeholder="$t('dict.dictType')" :disabled="typeFormType === 'edit'" />
        </t-form-item>
        <t-form-item name="status" :label="$t('common.status')">
          <t-radio-group v-model="typeFormData.status">
            <t-radio value="1">{{ $t('common.normal') }}</t-radio>
            <t-radio value="0">{{ $t('common.disable') }}</t-radio>
          </t-radio-group>
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 字典数据 新增/编辑弹窗 -->
    <t-dialog
      v-model="dataFormVisible"
      :header="dataFormType === 'add' ? $t('dict.addData') : $t('dict.editData')"
      width="560px"
      :close-on-overlay-click="false"
      @confirm="onDataFormConfirm"
      @close="dataFormVisible = false"
    >
      <t-form :model="dataFormData" layout="vertical">
        <t-form-item :label="$t('dict.dictType')">
          <t-input :model-value="selectedType?.dictType" disabled />
        </t-form-item>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item name="dictLabel" :label="$t('dict.dictLabel')" :rules="[{ required: true, message: $t('dict.dictLabel') }]">
              <t-input v-model="dataFormData.dictLabel" :placeholder="$t('dict.dictLabel')" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="dictValue" :label="$t('dict.dictValue')" :rules="[{ required: true, message: $t('dict.dictValue') }]">
              <t-input v-model="dataFormData.dictValue" :placeholder="$t('dict.dictValue')" />
            </t-form-item>
          </t-col>
        </t-row>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item name="dictSort" :label="$t('dept.orderNum')">
              <t-input-number v-model="dataFormData.dictSort" placeholder="0" :min="0" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="cssClass" :label="$t('dict.listClass')">
              <t-input v-model="dataFormData.cssClass" placeholder="css class" />
            </t-form-item>
          </t-col>
        </t-row>
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item name="listClass" :label="$t('dict.listClass')">
              <t-select v-model="dataFormData.listClass" :placeholder="$t('common.selectPlaceholder')" allow-clear>
                <t-option value="primary">主要（蓝色）</t-option>
                <t-option value="success">成功（绿色）</t-option>
                <t-option value="warning">警告（橙色）</t-option>
                <t-option value="danger">危险（红色）</t-option>
                <t-option value="default">默认（灰色）</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="isDefault" :label="$t('dict.isDefault')">
              <t-radio-group v-model="dataFormData.isDefault">
                <t-radio value="Y">{{ $t('common.yes') }}</t-radio>
                <t-radio value="N">{{ $t('common.no') }}</t-radio>
              </t-radio-group>
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item name="status" :label="$t('common.status')">
          <t-radio-group v-model="dataFormData.status">
            <t-radio value="1">{{ $t('common.normal') }}</t-radio>
            <t-radio value="0">{{ $t('common.disable') }}</t-radio>
          </t-radio-group>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, computed, onMounted } from 'vue'
import { MessagePlugin, DialogPlugin } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import {
  getDictTypePageApi,
  addDictTypeApi,
  updateDictTypeApi,
  deleteDictTypeApi,
  getDictDataPageApi,
  addDictDataApi,
  updateDictDataApi,
  deleteDictDataApi,
  type SysDictType,
  type SysDictData,
} from '@/api/dict'

const { t } = useI18n()

// ============ 字典类型 ============
const typeSearch = ref('')
const dictTypes = ref<SysDictType[]>([])
const selectedType = ref<SysDictType | null>(null)

const typePagination = reactive({ current: 1, pageSize: 20, total: 0 })

const filteredDictTypes = computed(() => {
  if (!typeSearch.value) return dictTypes.value
  const kw = typeSearch.value.toLowerCase()
  return dictTypes.value.filter(
    (t) => t.dictName.toLowerCase().includes(kw) || t.dictType.toLowerCase().includes(kw),
  )
})

async function loadTypes() {
  try {
    const res = await getDictTypePageApi({
      pageNum: typePagination.current,
      pageSize: typePagination.pageSize,
    })
    dictTypes.value = res.data?.list || []
    typePagination.total = res.data?.total || 0
  } catch {
    // ignore
  }
}

function onTypePageChange(page: number) {
  typePagination.current = page
  loadTypes()
}

function handleSelectType(item: SysDictType) {
  selectedType.value = item
  dataSearchForm.dictLabel = ''
  dataPagination.current = 1
  loadData()
}

// ---- 类型 CRUD ----
const typeFormVisible = ref(false)
const typeFormType = ref<'add' | 'edit'>('add')

const defaultTypeFormData = { id: undefined as string | undefined, dictName: '', dictType: '', status: '1' }
const typeFormData = reactive({ ...defaultTypeFormData })

function handleAddType() {
  typeFormType.value = 'add'
  Object.assign(typeFormData, { ...defaultTypeFormData })
  typeFormVisible.value = true
}

function handleEditType(item: SysDictType) {
  typeFormType.value = 'edit'
  Object.assign(typeFormData, { id: item.id, dictName: item.dictName, dictType: item.dictType, status: item.status })
  typeFormVisible.value = true
}

async function onTypeFormConfirm() {
  try {
    if (typeFormType.value === 'add') {
      await addDictTypeApi({ ...typeFormData })
      MessagePlugin.success(t('dict.addSuccess'))
    } else {
      await updateDictTypeApi({ ...typeFormData })
      MessagePlugin.success(t('dict.editSuccess'))
    }
    typeFormVisible.value = false
    loadTypes()
  } catch {
    // ignore
  }
}

async function handleDeleteType(item: SysDictType) {
  const dialog = DialogPlugin.confirm({
    header: t('common.confirmDelete'),
    body: t('dict.deleteTypeConfirm'),
    confirmBtn: { content: t('common.confirm'), theme: 'danger' },
    onConfirm: async () => {
      try {
        await deleteDictTypeApi(item.id)
        MessagePlugin.success(t('dict.deleteSuccess'))
        if (selectedType.value?.id === item.id) selectedType.value = null
        loadTypes()
      } catch {
        // ignore
      }
      dialog.destroy()
    },
    onClose: () => {
      dialog.destroy()
    },
  })
}

// ============ 字典数据 ============
const dataTable = ref<SysDictData[]>([])
const dataLoading = ref(false)
const dataSearchForm = reactive({ dictLabel: '' })
const dataPagination = reactive({ current: 1, pageSize: 10, total: 0 })

function onDataPageChange(page: number) {
  dataPagination.current = page
  loadData()
}

async function loadData() {
  if (!selectedType.value) return
  dataLoading.value = true
  try {
    const res = await getDictDataPageApi({
      pageNum: dataPagination.current,
      pageSize: dataPagination.pageSize,
      dictType: selectedType.value.dictType,
      dictLabel: dataSearchForm.dictLabel || undefined,
    })
    dataTable.value = res.data?.list || []
    dataPagination.total = res.data?.total || 0
  } catch {
    // ignore
  } finally {
    dataLoading.value = false
  }
}

// ---- 数据 CRUD ----
const dataFormVisible = ref(false)
const dataFormType = ref<'add' | 'edit'>('add')

const defaultDataFormData = {
  id: undefined as string | undefined,
  dictType: '',
  dictLabel: '',
  dictValue: '',
  dictSort: 0 as number,
  cssClass: '',
  listClass: '' as string,
  isDefault: 'N',
  status: '1',
}
const dataFormData = reactive({ ...defaultDataFormData })

function handleAddData() {
  dataFormType.value = 'add'
  Object.assign(dataFormData, { ...defaultDataFormData, dictType: selectedType.value?.dictType })
  dataFormVisible.value = true
}

function handleEditData(row: SysDictData) {
  dataFormType.value = 'edit'
  Object.assign(dataFormData, {
    id: row.id,
    dictType: row.dictType,
    dictLabel: row.dictLabel,
    dictValue: row.dictValue,
    dictSort: row.dictSort,
    cssClass: row.cssClass,
    listClass: row.listClass,
    isDefault: row.isDefault,
    status: row.status,
  })
  dataFormVisible.value = true
}

async function onDataFormConfirm() {
  try {
    const payload = { ...dataFormData, dictSort: Number(dataFormData.dictSort) || 0 }
    if (dataFormType.value === 'add') {
      await addDictDataApi(payload)
      MessagePlugin.success(t('dict.addSuccess'))
    } else {
      await updateDictDataApi(payload)
      MessagePlugin.success(t('dict.editSuccess'))
    }
    dataFormVisible.value = false
    loadData()
  } catch {
    // ignore
  }
}

async function handleDeleteData(row: SysDictData) {
  try {
    await deleteDictDataApi(row.id)
    MessagePlugin.success(t('dict.deleteSuccess'))
    loadData()
  } catch {
    // ignore
  }
}

// ============ 初始化 ============
onMounted(() => {
  loadTypes()
})
</script>

<style scoped>
.dict-type-list {
  margin: 0 -8px;
  padding: 0 8px;
}
.dict-type-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
}
.dict-type-item:hover {
  background: var(--wecom-bg-container);
}
.dict-type-item--active {
  background: var(--wecom-primary-light);
}
.dict-type-item__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.dict-type-item__name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wecom-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dict-type-item__key {
  font-size: 12px;
  color: var(--wecom-text-tertiary);
  font-family: monospace;
}
</style>
