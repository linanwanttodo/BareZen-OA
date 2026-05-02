<template>
  <div class="page-container">
    <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <!-- 左侧：字典类型 -->
      <a-card :bordered="true" class="flex flex-col">
        <template #title>
          <div class="flex items-center justify-between w-full">
            <span>{{ $t('dict.title') }}</span>
            <a-button v-permission="'system:dict:add'" type="primary" size="small" @click="handleAddType">
              <template #icon><icon-plus /></template>
              {{ $t('common.add') }}
            </a-button>
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
            <a-tag v-if="item.status === '1'" color="red" size="small">{{ $t('common.disable') }}</a-tag>
            <a-dropdown>
              <icon-more class="cursor-pointer" style="color: #86909C" />
              <template #content>
                <a-doption v-permission="'system:dict:edit'" @click="handleEditType(item)">{{ $t('common.edit') }}</a-doption>
                <a-doption v-permission="'system:dict:delete'" @click="handleDeleteType(item)" status="danger">{{ $t('common.delete') }}</a-doption>
              </template>
            </a-dropdown>
          </div>
          <a-empty v-if="!filteredDictTypes.length" :description="$t('common.noData')" />
        </div>
        <div v-if="typePagination.total > typePagination.pageSize" class="mt-3 pt-3" style="border-top: 1px solid #E5E6EB">
          <a-pagination
            v-model:current="typePagination.current"
            :total="typePagination.total"
            :page-size="typePagination.pageSize"
            size="small"
            @change="onTypePageChange"
          />
        </div>
      </a-card>

      <!-- 右侧：字典数据 -->
      <a-card :bordered="true" class="flex flex-col">
        <template #title>
          <div class="flex items-center justify-between w-full">
            <div class="flex items-center gap-2">
              <span>{{ $t('dict.dataTitle') }}</span>
              <a-tag v-if="selectedType" color="blue">{{ selectedType.dictName }}</a-tag>
            </div>
            <a-button v-permission="'system:dict:add'" type="primary" size="small" :disabled="!selectedType" @click="handleAddData">
              <template #icon><icon-plus /></template>
              {{ $t('common.add') }}
            </a-button>
          </div>
        </template>
        <template v-if="selectedType">
          <div class="mb-3 flex items-center gap-2">
            <a-input v-model="dataSearchForm.dictLabel" :placeholder="$t('dict.dictLabel')" allow-clear style="width: 180px" />
            <a-button type="primary" size="small" @click="loadData">{{ $t('common.search') }}</a-button>
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
                    <a-tag :color="record.isDefault === 'Y' ? 'blue' : 'gray'" size="small">
                      {{ record.isDefault === 'Y' ? $t('common.yes') : $t('common.no') }}
                    </a-tag>
                  </template>
                </a-table-column>
                <a-table-column :title="$t('common.status')" :width="80">
                  <template #cell="{ record }">
                    <a-tag :color="record.status === '0' ? 'green' : 'red'" size="small">
                      {{ record.status === '0' ? $t('common.normal') : $t('common.disable') }}
                    </a-tag>
                  </template>
                </a-table-column>
                <a-table-column :title="$t('common.operate')" :width="120" align="right">
                  <template #cell="{ record }">
                    <a-space>
                      <a-button v-permission="'system:dict:edit'" size="small" @click="handleEditData(record)">{{ $t('common.edit') }}</a-button>
                      <a-popconfirm v-permission="'system:dict:delete'" :content="$t('dict.deleteDataConfirm')" @ok="handleDeleteData(record)">
                        <a-button size="small" status="danger">{{ $t('common.delete') }}</a-button>
                      </a-popconfirm>
                    </a-space>
                  </template>
                </a-table-column>
              </template>
            </a-table>

            <div v-if="dataPagination.total > dataPagination.pageSize" class="mt-3 flex justify-end">
              <a-pagination
                v-model:current="dataPagination.current"
                :total="dataPagination.total"
                :page-size="dataPagination.pageSize"
                size="small"
                @change="onDataPageChange"
              />
            </div>
          </a-spin>
        </template>
        <a-empty v-else :description="$t('dict.selectTypeHint')" />
      </a-card>
    </div>

    <!-- 字典类型 新增/编辑弹窗 -->
    <a-modal
      v-model:visible="typeFormVisible"
      :title="typeFormType === 'add' ? $t('dict.addType') : $t('dict.editType')"
      :width="480"
      :mask-closable="false"
      @ok="onTypeFormConfirm"
      @cancel="typeFormVisible = false"
    >
      <a-form :model="typeFormData" layout="vertical">
        <a-form-item field="dictName" :label="$t('dict.dictName')" :rules="[{ required: true, message: $t('dict.dictName') }]">
          <a-input v-model="typeFormData.dictName" :placeholder="$t('dict.dictName')" />
        </a-form-item>
        <a-form-item field="dictType" :label="$t('dict.dictType')" :rules="[{ required: true, message: $t('dict.dictType') }]">
          <a-input v-model="typeFormData.dictType" :placeholder="$t('dict.dictType')" :disabled="typeFormType === 'edit'" />
        </a-form-item>
        <a-form-item field="status" :label="$t('common.status')">
          <a-radio-group v-model="typeFormData.status">
            <a-radio value="0">{{ $t('common.normal') }}</a-radio>
            <a-radio value="1">{{ $t('common.disable') }}</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 字典数据 新增/编辑弹窗 -->
    <a-modal
      v-model:visible="dataFormVisible"
      :title="dataFormType === 'add' ? $t('dict.addData') : $t('dict.editData')"
      :width="560"
      :mask-closable="false"
      @ok="onDataFormConfirm"
      @cancel="dataFormVisible = false"
    >
      <a-form :model="dataFormData" layout="vertical">
        <a-form-item :label="$t('dict.dictType')">
          <a-input :model-value="selectedType?.dictType" disabled />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="dictLabel" :label="$t('dict.dictLabel')" :rules="[{ required: true, message: $t('dict.dictLabel') }]">
              <a-input v-model="dataFormData.dictLabel" :placeholder="$t('dict.dictLabel')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="dictValue" :label="$t('dict.dictValue')" :rules="[{ required: true, message: $t('dict.dictValue') }]">
              <a-input v-model="dataFormData.dictValue" :placeholder="$t('dict.dictValue')" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="dictSort" :label="$t('dept.orderNum')">
              <a-input-number v-model="dataFormData.dictSort" placeholder="0" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="cssClass" :label="$t('dict.listClass')">
              <a-input v-model="dataFormData.cssClass" placeholder="css class" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="listClass" :label="$t('dict.listClass')">
              <a-select v-model="dataFormData.listClass" :placeholder="$t('common.selectPlaceholder')" allow-clear>
                <a-option value="primary">主要（蓝色）</a-option>
                <a-option value="success">成功（绿色）</a-option>
                <a-option value="warning">警告（橙色）</a-option>
                <a-option value="danger">危险（红色）</a-option>
                <a-option value="default">默认（灰色）</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="isDefault" :label="$t('dict.isDefault')">
              <a-radio-group v-model="dataFormData.isDefault">
                <a-radio value="Y">{{ $t('common.yes') }}</a-radio>
                <a-radio value="N">{{ $t('common.no') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item field="status" :label="$t('common.status')">
          <a-radio-group v-model="dataFormData.status">
            <a-radio value="0">{{ $t('common.normal') }}</a-radio>
            <a-radio value="1">{{ $t('common.disable') }}</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { useI18n } from 'vue-i18n'
import { IconPlus, IconMore } from '@arco-design/web-vue/es/icon'
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

const defaultTypeFormData = { id: undefined as string | undefined, dictName: '', dictType: '', status: '0' }
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
      Message.success(t('dict.addSuccess'))
    } else {
      await updateDictTypeApi({ ...typeFormData })
      Message.success(t('dict.editSuccess'))
    }
    typeFormVisible.value = false
    loadTypes()
  } catch {
    // ignore
  }
}

async function handleDeleteType(item: SysDictType) {
  Modal.confirm({
    title: t('common.confirmDelete'),
    content: t('dict.deleteTypeConfirm'),
    onOk: async () => {
      try {
        await deleteDictTypeApi(item.id)
        Message.success(t('dict.deleteSuccess'))
        if (selectedType.value?.id === item.id) selectedType.value = null
        loadTypes()
      } catch {
        // ignore
      }
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
  status: '0',
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
      Message.success(t('dict.addSuccess'))
    } else {
      await updateDictDataApi(payload)
      Message.success(t('dict.editSuccess'))
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
    Message.success(t('dict.deleteSuccess'))
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
  background: #F2F3F5;
}
.dict-type-item--active {
  background: var(--color-primary-light-1);
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
  color: #1D2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dict-type-item__key {
  font-size: 12px;
  color: #86909C;
  font-family: monospace;
}
</style>
