<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="bizType" :label="$t('flowTemplate.bizType')">
          <a-select v-model="searchForm.bizType" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 200px">
            <a-option v-for="item in bizTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</a-option>
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
          <span>{{ $t('flowTemplate.title') }}</span>
          <a-button v-permission="'system:flow:template:add'" type="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </a-button>
        </div>
      </template>

      <a-alert class="mb-4" :title="$t('flowTemplate.tip')" type="info" />

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('flowTemplate.bizType')" :width="140">
              <template #cell="{ record }">
                <a-tag color="arcoblue">{{ bizTypeLabel(record.bizType) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('flowTemplate.nodeSeq')" data-index="nodeSeq" :width="100" align="center" />
            <a-table-column :title="$t('flowTemplate.nodeName')" data-index="nodeName" />
            <a-table-column :title="$t('common.status')" :width="100" align="center">
              <template #cell="{ record }">
                <a-switch
                  v-permission="'system:flow:template:edit'"
                  :model-value="record.enabled === 1"
                  @change="(val: string | number | boolean) => handleEnableChange(record, Boolean(val))"
                />
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.createTime')" data-index="createTime" :width="170" />
            <a-table-column :title="$t('common.operate')" :width="140" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-permission="'system:flow:template:edit'" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</a-button>
                  <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button v-permission="'system:flow:template:delete'" size="small" status="danger">{{ $t('common.delete') }}</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>

        <div v-if="pagination.total > pagination.pageSize" class="mt-4 flex justify-end">
          <a-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      </a-spin>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:visible="editVisible" :title="isEdit ? $t('flowTemplate.editNode') : $t('flowTemplate.addNode')"
             :width="480" @ok="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <a-form :model="form" layout="vertical">
        <a-form-item field="bizType" :label="$t('flowTemplate.bizType')" required>
          <a-select v-model="form.bizType" :disabled="isEdit" :placeholder="$t('flowTemplate.bizTypePlaceholder')">
            <a-option v-for="item in bizTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="nodeSeq" :label="$t('flowTemplate.nodeSeq')" required>
          <a-input-number v-model="form.nodeSeq" :min="1" :max="99" :disabled="isEdit" style="width: 100%" />
        </a-form-item>
        <a-form-item field="nodeName" :label="$t('flowTemplate.nodeName')" required>
          <a-input v-model="form.nodeName" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-form-item field="enabled" :label="$t('common.status')">
          <a-radio-group v-model="form.enabled">
            <a-radio :value="1">{{ $t('common.enabled') }}</a-radio>
            <a-radio :value="0">{{ $t('common.disabled') }}</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
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
  if (!form.bizType) { Message.warning(t('flowTemplate.bizTypeRequired')); return }
  if (!form.nodeSeq) { Message.warning(t('flowTemplate.nodeSeqRequired')); return }
  if (!form.nodeName?.trim()) { Message.warning(t('flowTemplate.nodeNameRequired')); return }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateFlowTemplateApi(form)
    } else {
      await addFlowTemplateApi(form)
    }
    Message.success(t('common.saveSuccess'))
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: FlowTemplate) {
  await deleteFlowTemplateApi(record.id)
  Message.success(t('common.deleteSuccess'))
  loadData()
}

async function handleEnableChange(record: FlowTemplate, val: boolean) {
  await updateFlowTemplateApi({ id: record.id, enabled: val ? 1 : 0 })
  Message.success(t('common.saveSuccess'))
  record.enabled = val ? 1 : 0
}
</script>