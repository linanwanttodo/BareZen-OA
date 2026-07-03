<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :data="searchForm" :label-width="80" :on-submit="handleSearch">
        <t-form-item name="bizType" :label="$t('flowTemplate.bizType')">
          <t-select v-model="searchForm.bizType" :placeholder="$t('common.inputPlaceholder')" allow-clear>
            <t-option v-for="item in bizTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</t-option>
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
      <div class="bz-list-toolbar bz-list-toolbar--first">
        <t-space>
          <t-button v-permission="PERM.system.flowTemplateAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </t-button>
        </t-space>
      </div>

      <t-alert class="mb-4" :title="$t('flowTemplate.tip')" type="info" />

      
          <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #bizType="{ row }">
                <t-tag theme="primary" variant="light">{{ bizTypeLabel(row.bizType) }}</t-tag>
              </template>
    <template #enabled="{ row }">
                <t-switch
                  v-permission="PERM.system.flowTemplateEdit"
                  :model-value="row.enabled === 1"
                  @change="(val: string | number | boolean) => handleEnableChange(row, Boolean(val))"
                />
              </template>
    <template #col_5="{ row }">
                <t-space>
                  <t-link theme="primary" v-permission="PERM.system.flowTemplateEdit" @click="handleEdit(row)">{{ $t('common.edit') }}</t-link>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button v-permission="PERM.system.flowTemplateDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
  </t-table>

        <div v-if="pagination.total > pagination.pageSize" class="mt-4 flex justify-end">
          <t-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      
    </t-card>

    <!-- 新增/编辑弹窗 -->
    <t-dialog v-model:visible="editVisible" :title="isEdit ? $t('flowTemplate.editNode') : $t('flowTemplate.addNode')"
             :width="480" @confirm="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <t-form ref="formRef" :data="form" layout="vertical">
        <t-form-item name="bizType" :label="$t('flowTemplate.bizType')" required>
          <t-select v-model="form.bizType" :disabled="isEdit" :placeholder="$t('flowTemplate.bizTypePlaceholder')">
            <t-option v-for="item in bizTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item name="nodeSeq" :label="$t('flowTemplate.nodeSeq')" required>
          <t-input-number v-model="form.nodeSeq" :min="1" :max="99" :disabled="isEdit" style="width: 100%" />
        </t-form-item>
        <t-form-item name="nodeName" :label="$t('flowTemplate.nodeName')" required>
          <t-input v-model="form.nodeName" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item name="enabled" :label="$t('common.status')">
          <t-radio-group v-model="form.enabled">
            <t-radio :value="1">{{ $t('common.enabled') }}</t-radio>
            <t-radio :value="0">{{ $t('common.disabled') }}</t-radio>
          </t-radio-group>
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
import { getFlowTemplatePageApi, addFlowTemplateApi, updateFlowTemplateApi, deleteFlowTemplateApi, type FlowTemplate } from '@/api/flow-template'

const { t } = useI18n()

/** 接入审批引擎的业务类型（与后端 ApprovalFlowTemplateController.SUPPORTED_BIZ_TYPES 一致） */
const bizTypeOptions = [
  { value: 'leave', label: t('flowTemplate.bizLeave') },
  { value: 'overtime', label: t('flowTemplate.bizOvertime') },
  { value: 'travel', label: t('flowTemplate.bizTravel') },
  { value: 'vehicle', label: t('flowTemplate.bizVehicle') },
]

function bizTypeLabel(type: string) {
  return bizTypeOptions.find((o) => o.value === type)?.label ?? type
}

const loading = ref(false)
const saving = ref(false)
const tableData = ref<FlowTemplate[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ bizType: '' })
const editVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstanceFunctions>()
const form = reactive<Partial<FlowTemplate>>({})

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getFlowTemplatePageApi({ ...searchForm, pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = res.data.list || []
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.bizType = ''; pagination.current = 1; loadData() }

function handleAdd() {
  isEdit.value = false
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  form.bizType = 'leave'
  form.nodeSeq = 1
  form.enabled = 1
  editVisible.value = true
}

function handleEdit(record: FlowTemplate) {
  isEdit.value = true
  Object.assign(form, record)
  editVisible.value = true
}

async function handleSave() {
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await formRef.value?.validate()) !== true) {
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateFlowTemplateApi(form)
    } else {
      await addFlowTemplateApi(form)
    }
    MessagePlugin.success(t('common.saveSuccess'))
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: FlowTemplate) {
  await deleteFlowTemplateApi(record.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadData()
}

async function handleEnableChange(record: FlowTemplate, val: boolean) {
  await updateFlowTemplateApi({ id: record.id, enabled: val ? 1 : 0 })
  MessagePlugin.success(t('common.saveSuccess'))
  record.enabled = val ? 1 : 0
}

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'bizType', title: t('flowTemplate.bizType'), width: 140 },
    { colKey: 'nodeSeq', title: t('flowTemplate.nodeSeq'), width: 100, align: 'center' },
    { colKey: 'nodeName', title: t('flowTemplate.nodeName') },
    { colKey: 'enabled', title: t('common.status'), width: 100, align: 'center' },
    { colKey: 'createTime', title: t('common.createTime'), width: 170 },
    { colKey: 'col_5', title: t('common.operate'), width: 140, align: 'right' },
  ])

</script>