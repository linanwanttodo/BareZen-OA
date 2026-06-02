<template>
  <div class="page-container">
    <!-- 工具栏 -->
    <t-card class="mb-4">
      <div class="flex items-center justify-between">
        <span class="text-sm text-[var(--wecom-text-secondary)]">{{ $t('asset.categoryDesc') }}</span>
        <t-button v-permission="PERM.asset.categoryAdd" theme="primary" @click="handleAdd">
          <template #icon><icon-plus /></template>
          {{ $t('common.add') }}
        </t-button>
      </div>
    </t-card>

    <!-- 分类列表 -->
    <t-card>
      <template #title>
        <span>{{ $t('asset.category') }}</span>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('asset.categoryName')" data-index="name" :width="200" />
            <a-table-column :title="$t('asset.categorySort')" data-index="sort" :width="100" align="center" />
            <a-table-column :title="$t('common.status')" :width="100" align="center">
              <template #cell="{ record }">
                <t-tag :color="record.status === 1 ? 'green' : 'default'">
                  {{ record.status === 1 ? $t('common.enabled') : $t('common.disabled') }}
                </t-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="160" align="right">
              <template #cell="{ record }">
                <t-space>
                  <t-button v-permission="PERM.asset.categoryEdit" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</t-button>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(record)">
                    <t-button v-permission="PERM.asset.categoryDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
            </a-table-column>
          </template>
        </a-table>
      </a-spin>
    </t-card>

    <!-- 新增/编辑弹窗 -->
    <t-dialog v-model="editVisible" :header="isEdit ? $t('asset.editCategory') : $t('asset.addCategory')"
             width="480px" @confirm="handleSave" @close="editVisible = false" :ok-loading="saving">
      <t-form :model="form" layout="vertical">
        <t-form-item name="name" :label="$t('asset.categoryName')" required>
          <t-input v-model="form.name" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item name="sort" :label="$t('asset.categorySort')">
          <t-input-number v-model="form.sort" :min="0" style="width: 100%" />
        </t-form-item>
        <t-form-item name="status" :label="$t('common.status')">
          <t-select v-model="form.status">
            <t-option :value="1">{{ $t('common.enabled') }}</t-option>
            <t-option :value="0">{{ $t('common.disabled') }}</t-option>
          </t-select>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, onMounted } from 'vue'
import {  MessagePlugin  } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import { getCategoryListApi, addCategoryApi, updateCategoryApi, deleteCategoryApi, type AssetCategory } from '@/api/asset'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const tableData = ref<AssetCategory[]>([])
const editVisible = ref(false)
const isEdit = ref(false)

const form = reactive<Partial<AssetCategory>>({})

async function loadData() {
  loading.value = true
  try {
    const res = await getCategoryListApi()
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  form.sort = 0
  form.status = 1
  editVisible.value = true
}

function handleEdit(record: AssetCategory) {
  isEdit.value = true
  Object.assign(form, record)
  editVisible.value = true
}

async function handleSave() {
  if (!form.name) {
    MessagePlugin.warning(t('asset.categoryNameRequired'))
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateCategoryApi(form)
    } else {
      await addCategoryApi(form)
    }
    MessagePlugin.success(t('common.saveSuccess'))
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: AssetCategory) {
  await deleteCategoryApi(record.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadData()
}

onMounted(loadData)
</script>