<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <t-form-item name="configName" :label="$t('config.configName')">
          <t-input v-model="searchForm.configName" :placeholder="$t('common.inputPlaceholder')" clearable style="width: 200px" />
        </t-form-item>
        <t-form-item name="configKey" :label="$t('config.configKey')">
          <t-input v-model="searchForm.configKey" :placeholder="$t('common.inputPlaceholder')" clearable style="width: 200px" />
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" type="submit">{{ $t('common.search') }}</t-button>
            <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-card>

    <!-- 表格 -->
    <t-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('config.systemConfig') }}</span>
          <t-space>
            <t-button v-permission="PERM.system.configEdit" @click="handleClearCache" :loading="clearing">
              <template #icon><icon-refresh /></template>
              {{ $t('config.clearCache') }}
            </t-button>
          </t-space>
        </div>
      </template>

      <t-loading :loading="loading">
          <t-table row-key="id" :data="tableData" :columns="tableColumns" :pagination="false">
    <template #configValue="{ row }">
                <span class="font-mono text-sm">{{ row.configValue }}</span>
              </template>
    <template #configType="{ row }">
                <t-tag :color="row.configType === 'Y' ? 'blue' : 'default'">
                  {{ row.configType === 'Y' ? $t('common.yes') : $t('common.no') }}
                </t-tag>
              </template>
    <template #status="{ row }">
                <t-switch
                  :model-value="row.status === '1'"
                  @change="(val: string | number | boolean) => handleStatusChange(row, Boolean(val))"
                />
              </template>
    <template #col_5="{ row }">
                <t-button v-permission="PERM.system.configEdit" size="small" @click="handleEdit(row)">{{ $t('common.edit') }}</t-button>
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
      </t-loading>
    </t-card>

    <!-- 编辑弹窗 -->
    <t-dialog v-model="editVisible" :header="$t('config.editConfig')" @confirm="handleSave" @close="editVisible = false">
      <t-form :model="editForm" auto-label-width>
        <t-form-item name="configName" :label="$t('config.configName')">
          <t-input v-model="editForm.configName" disabled />
        </t-form-item>
        <t-form-item name="configKey" :label="$t('config.configKey')">
          <t-input v-model="editForm.configKey" disabled />
        </t-form-item>
        <t-form-item name="configValue" :label="$t('config.configValue')">
          <t-input v-model="editForm.configValue" />
        </t-form-item>
        <t-form-item name="status" :label="$t('common.status')">
          <t-radio-group v-model="editForm.status">
            <t-radio value="1">{{ $t('common.normal') }}</t-radio>
            <t-radio value="0">{{ $t('common.disable') }}</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item name="remark" :label="$t('common.remark')">
          <t-textarea v-model="editForm.remark" />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
import {  MessagePlugin  } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import { getConfigPageApi, updateConfigApi, clearConfigCacheApi, type SysConfig } from '@/api/config'

const { t } = useI18n()
const loading = ref(false)
const clearing = ref(false)
const tableData = ref<SysConfig[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ configName: '', configKey: '' })
const editVisible = ref(false)
const editForm = reactive({ id: '', configName: '', configKey: '', configValue: '', status: '1', remark: '' })

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getConfigPageApi({ ...searchForm, pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = res.data.list || []
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.configName = ''; searchForm.configKey = ''; pagination.current = 1; loadData() }

function handleEdit(row: SysConfig) {
  Object.assign(editForm, { id: row.id, configName: row.configName, configKey: row.configKey, configValue: row.configValue, status: row.status, remark: row.remark })
  editVisible.value = true
}

async function handleSave() {
  await updateConfigApi({ id: editForm.id, configValue: editForm.configValue, status: editForm.status, remark: editForm.remark })
  MessagePlugin.success(t('config.updateSuccess'))
  editVisible.value = false
  loadData()
}

async function handleStatusChange(row: SysConfig, val: boolean) {
  await updateConfigApi({ id: row.id, status: val ? '1' : '0' })
  MessagePlugin.success(t('config.statusUpdated'))
  loadData()
}

async function handleClearCache() {
  clearing.value = true
  try {
    const keys = tableData.value.map(r => r.configKey)
    for (const key of keys) {
      await clearConfigCacheApi(key)
    }
    MessagePlugin.success(t('config.cacheCleared'))
  } finally {
    clearing.value = false
  }
}

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'configName', title: t('config.configName') },
    { colKey: 'configKey', title: t('config.configKey'), width: 220 },
    { colKey: 'configValue', title: t('config.configValue'), width: 300 },
    { colKey: 'configType', title: t('config.configType'), width: 90 },
    { colKey: 'status', title: t('common.status'), width: 80 },
    { colKey: 'col_5', title: t('common.operate'), width: 180, align: 'right' },
  ])

</script>
