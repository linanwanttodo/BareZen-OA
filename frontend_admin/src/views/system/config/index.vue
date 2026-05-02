<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="configName" :label="$t('config.configName')">
          <a-input v-model="searchForm.configName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 200px" />
        </a-form-item>
        <a-form-item field="configKey" :label="$t('config.configKey')">
          <a-input v-model="searchForm.configKey" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 200px" />
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
          <span>{{ $t('config.systemConfig') }}</span>
          <a-space>
            <a-button v-permission="'system:config:edit'" @click="handleClearCache" :loading="clearing">
              <template #icon><icon-refresh /></template>
              {{ $t('config.clearCache') }}
            </a-button>
          </a-space>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('config.configName')" data-index="configName" />
            <a-table-column :title="$t('config.configKey')" data-index="configKey" :width="220" />
            <a-table-column :title="$t('config.configValue')" :width="300">
              <template #cell="{ record }">
                <span class="font-mono text-sm">{{ record.configValue }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('config.configType')" :width="90">
              <template #cell="{ record }">
                <a-tag :color="record.configType === 'Y' ? 'blue' : 'default'">
                  {{ record.configType === 'Y' ? $t('common.yes') : $t('common.no') }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.status')" :width="80">
              <template #cell="{ record }">
                <a-switch
                  :model-value="record.status === '1'"
                  @change="(val: string | number | boolean) => handleStatusChange(record, Boolean(val))"
                />
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="180" align="right">
              <template #cell="{ record }">
                <a-button v-permission="'system:config:edit'" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</a-button>
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

    <!-- 编辑弹窗 -->
    <a-modal v-model:visible="editVisible" :title="$t('config.editConfig')" @ok="handleSave" @cancel="editVisible = false">
      <a-form :model="editForm" auto-label-width>
        <a-form-item field="configName" :label="$t('config.configName')">
          <a-input v-model="editForm.configName" disabled />
        </a-form-item>
        <a-form-item field="configKey" :label="$t('config.configKey')">
          <a-input v-model="editForm.configKey" disabled />
        </a-form-item>
        <a-form-item field="configValue" :label="$t('config.configValue')">
          <a-input v-model="editForm.configValue" />
        </a-form-item>
        <a-form-item field="status" :label="$t('common.status')">
          <a-radio-group v-model="editForm.status">
            <a-radio value="1">{{ $t('common.normal') }}</a-radio>
            <a-radio value="0">{{ $t('common.disable') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="remark" :label="$t('common.remark')">
          <a-textarea v-model="editForm.remark" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
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
  Message.success(t('config.updateSuccess'))
  editVisible.value = false
  loadData()
}

async function handleStatusChange(row: SysConfig, val: boolean) {
  await updateConfigApi({ id: row.id, status: val ? '1' : '0' })
  Message.success(t('config.statusUpdated'))
  loadData()
}

async function handleClearCache() {
  clearing.value = true
  try {
    const keys = tableData.value.map(r => r.configKey)
    for (const key of keys) {
      await clearConfigCacheApi(key)
    }
    Message.success(t('config.cacheCleared'))
  } finally {
    clearing.value = false
  }
}
</script>
