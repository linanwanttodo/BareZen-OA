<template>
  <div class="page-container">
    <!-- 工具栏 -->
    <!-- 列表容器：说明 + 工具条 + 表格共用一张白卡（对齐 TDesign Starter list/base） -->
    <t-card :bordered="false" class="bz-list-panel">
      <div class="bz-list-toolbar bz-list-toolbar--first">
        <span class="text-sm text-[var(--td-text-color-secondary)]">{{ $t('asset.categoryDesc') }}</span>
        <t-button v-permission="PERM.asset.categoryAdd" theme="primary" @click="handleAdd">
          <template #icon><icon-plus /></template>
          {{ $t('common.add') }}
        </t-button>
      </div>

      
          <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #status="{ row }">
                <t-tag :theme="row.status === 1 ? 'success' : 'default'" variant="light">
                  {{ row.status === 1 ? $t('common.enabled') : $t('common.disabled') }}
                </t-tag>
              </template>
    <template #col_3="{ row }">
                <t-space>
                  <t-link theme="primary" v-permission="PERM.asset.categoryEdit" @click="handleEdit(row)">{{ $t('common.edit') }}</t-link>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button v-permission="PERM.asset.categoryDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
  </t-table>
      
    </t-card>

    <!-- 新增/编辑弹窗 -->
    <t-dialog v-model:visible="editVisible" :header="isEdit ? $t('asset.editCategory') : $t('asset.addCategory')"
             width="480px" @confirm="handleSave" @close="editVisible = false" :ok-loading="saving">
      <t-form ref="formRef" :data="form" layout="vertical">
        <t-form-item name="name" :label="$t('asset.categoryName')" :rules="[{ required: true, message: t('asset.categoryNameRequired') }]">
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
import { ref, computed, reactive, onMounted } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import { getCategoryListApi, addCategoryApi, updateCategoryApi, deleteCategoryApi, type AssetCategory } from '@/api/asset'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const tableData = ref<AssetCategory[]>([])
const editVisible = ref(false)
const isEdit = ref(false)

const form = reactive<Partial<AssetCategory>>({})
const formRef = ref<FormInstanceFunctions>()

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
  // tdesign-vue-next 1.20.7 的 validate() 通过时返回 true；写成 if (await x.validate()) 会把
  // 「校验通过」当成失败而永远进不到保存分支，故一律用 !== true 判定「未通过」
  if ((await formRef.value?.validate()) !== true) return
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

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'name', title: t('asset.categoryName'), width: 200 },
    { colKey: 'sort', title: t('asset.categorySort'), width: 100, align: 'center' },
    { colKey: 'status', title: t('common.status'), width: 100, align: 'center' },
    { colKey: 'col_3', title: t('common.operate'), width: 160, align: 'right' },
  ])

</script>