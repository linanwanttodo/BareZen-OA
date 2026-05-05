<template>
  <div class="page-container">
    <!-- 工具栏 -->
    <a-card class="mb-4">
      <div class="flex items-center justify-between">
        <span class="text-sm text-[var(--color-text-2)]">{{ $t('kb.categoryDesc') }}</span>
        <a-button v-permission="'kb:category:add'" type="primary" @click="handleAdd">
          <template #icon><icon-plus /></template>
          {{ $t('common.add') }}
        </a-button>
      </div>
    </a-card>

    <!-- 分类列表 -->
    <a-card>
      <template #title>
        <span>{{ $t('kb.category') }}</span>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('kb.categoryName')" data-index="name" :width="200" />
            <a-table-column :title="$t('kb.categorySort')" data-index="sort" :width="100" align="center" />
            <a-table-column :title="$t('common.status')" :width="100" align="center">
              <template #cell="{ record }">
                <a-tag :color="record.status === 1 ? 'green' : 'default'">
                  {{ record.status === 1 ? $t('common.enabled') : $t('common.disabled') }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="160" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-permission="'kb:category:edit'" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</a-button>
                  <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button v-permission="'kb:category:delete'" size="small" status="danger">{{ $t('common.delete') }}</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>
      </a-spin>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:visible="editVisible" :title="isEdit ? $t('kb.editCategory') : $t('kb.addCategory')"
             :width="480" @ok="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <a-form :model="form" layout="vertical">
        <a-form-item field="name" :label="$t('kb.categoryName')" required>
          <a-input v-model="form.name" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-form-item field="sort" :label="$t('kb.categorySort')">
          <a-input-number v-model="form.sort" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item field="status" :label="$t('common.status')">
          <a-select v-model="form.status">
            <a-option :value="1">{{ $t('common.enabled') }}</a-option>
            <a-option :value="0">{{ $t('common.disabled') }}</a-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useI18n } from 'vue-i18n'
import { getKbCategoryListApi, addKbCategoryApi, updateKbCategoryApi, deleteKbCategoryApi, type KbCategory } from '@/api/kb'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const tableData = ref<KbCategory[]>([])
const editVisible = ref(false)
const isEdit = ref(false)

const form = reactive<Partial<KbCategory>>({})

async function loadData() {
  loading.value = true
  try {
    const res = await getKbCategoryListApi()
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

function handleEdit(record: KbCategory) {
  isEdit.value = true
  Object.assign(form, record)
  editVisible.value = true
}

async function handleSave() {
  if (!form.name) {
    Message.warning(t('kb.categoryNameRequired'))
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateKbCategoryApi(form)
    } else {
      await addKbCategoryApi(form)
    }
    Message.success(t('common.saveSuccess'))
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: KbCategory) {
  await deleteKbCategoryApi(record.id)
  Message.success(t('common.deleteSuccess'))
  loadData()
}

onMounted(loadData)
</script>