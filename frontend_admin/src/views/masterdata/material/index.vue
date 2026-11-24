<template>
  <div class="page-container">
    <t-card class="bz-list-panel">
      <!-- 搜索区域 -->
      <t-form :data="searchForm" :label-width="80" :on-submit="handleSearch">
        <t-row :gutter="[24, 24]">
          <t-col :span="3">
            <t-form-item name="keyword" :label="$t('masterdata.keyword')">
              <t-input v-model="searchForm.keyword" :placeholder="$t('common.inputPlaceholder')" allow-clear />
            </t-form-item>
          </t-col>
          <t-col :span="3">
            <t-form-item name="status" :label="$t('common.status')">
              <t-select v-model="searchForm.status" :placeholder="$t('common.selectPlaceholder')" allow-clear>
                <t-option value="0">{{ $t('common.enabled') }}</t-option>
                <t-option value="1">{{ $t('common.disabled') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="3" class="bz-list-ops">
            <t-form-item>
              <t-space>
                <t-button theme="primary" type="submit">{{ $t('common.search') }}</t-button>
                <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
              </t-space>
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>

      <div class="bz-list-toolbar bz-list-toolbar--first">
        <t-space>
          <t-button v-permission="PERM.masterdata.materialAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </t-button>
        </t-space>
      </div>

      <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
        <template #status="{ row }">
          <t-tag :color="row.status === '0' ? 'green' : 'gray'" variant="light">
            {{ row.status === '0' ? $t('common.enabled') : $t('common.disabled') }}
          </t-tag>
        </template>
        <template #operate="{ row }">
          <t-space>
            <t-link theme="primary" v-permission="PERM.masterdata.materialEdit" @click="handleEdit(row)">{{ $t('common.edit') }}</t-link>
            <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
              <t-button v-permission="PERM.masterdata.materialDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
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
    </t-card>

    <!-- 编辑弹窗 -->
    <t-dialog v-model:visible="editVisible" :header="isEdit ? $t('masterdata.editMaterial') : $t('masterdata.addMaterial')"
             :width="560" @confirm="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <t-form ref="formRef" :data="form" layout="vertical">
        <t-row :gutter="16">
          <t-col :span="6">
            <t-form-item name="materialCode" :label="$t('masterdata.materialCode')" :rules="[{ required: true, message: t('validation.required', { field: t('masterdata.materialCode') }) }]">
              <t-input v-model="form.materialCode" :placeholder="$t('common.inputPlaceholder')" :disabled="isEdit" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item name="materialName" :label="$t('masterdata.materialName')" :rules="[{ required: true, message: t('validation.required', { field: t('masterdata.materialName') }) }]">
              <t-input v-model="form.materialName" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item name="contactPerson" :label="$t('masterdata.contactPerson')">
              <t-input v-model="form.contactPerson" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item name="contactPhone" :label="$t('masterdata.contactPhone')">
              <t-input v-model="form.contactPhone" :placeholder="$t('common.inputPlaceholder')" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item name="status" :label="$t('common.status')">
              <t-select v-model="form.status">
                <t-option value="0">{{ $t('common.enabled') }}</t-option>
                <t-option value="1">{{ $t('common.disabled') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="remark" :label="$t('common.remark')">
              <t-textarea v-model="form.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import {
  getMaterialPageApi, addMaterialApi, updateMaterialApi, deleteMaterialApi,
  type MaterialInfo,
} from '@/api/masterdata'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const tableData = ref<MaterialInfo[]>([])
const editVisible = ref(false)
const isEdit = ref(false)

const searchForm = reactive({ keyword: '', status: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const formRef = ref<FormInstanceFunctions>()
const form = reactive<Partial<MaterialInfo>>({})

async function loadData() {
  loading.value = true
  try {
    const res = await getMaterialPageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
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
  searchForm.keyword = ''
  searchForm.status = ''
  pagination.current = 1
  loadData()
}

function resetForm() {
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  form.status = '0'
}

function handleAdd() {
  isEdit.value = false
  resetForm()
  editVisible.value = true
}

function handleEdit(record: MaterialInfo) {
  isEdit.value = true
  resetForm()
  Object.assign(form, record)
  editVisible.value = true
}

async function handleSave() {
  if ((await formRef.value?.validate()) !== true) {
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateMaterialApi(form)
    } else {
      await addMaterialApi(form)
    }
    MessagePlugin.success(t('common.saveSuccess'))
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: MaterialInfo) {
  await deleteMaterialApi(record.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadData()
}

onMounted(loadData)

/** 表格列定义（自定义单元格见 t-table 同名插槽） */
const tableColumns = computed(() => [
  { colKey: 'materialCode', title: t('masterdata.materialCode'), width: 150 },
  { colKey: 'materialName', title: t('masterdata.materialName'), width: 180, ellipsis: true },
  { colKey: 'contactPerson', title: t('masterdata.contactPerson'), width: 110 },
  { colKey: 'contactPhone', title: t('masterdata.contactPhone'), width: 140 },
  { colKey: 'status', title: t('common.status'), width: 90 },
  { colKey: 'remark', title: t('common.remark'), ellipsis: true },
  { colKey: 'updateTime', title: t('common.updateTime'), width: 170 },
  { colKey: 'operate', title: t('common.operate'), width: 130, align: 'right' },
])
</script>
