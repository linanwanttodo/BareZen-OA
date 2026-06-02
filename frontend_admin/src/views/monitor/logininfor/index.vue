<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <t-form-item name="userName" :label="$t('monitor.logininfor.userName')">
          <t-input v-model="searchForm.userName" :placeholder="$t('common.inputPlaceholder')" clearable style="width: 200px" />
        </t-form-item>
        <t-form-item name="status" :label="$t('monitor.logininfor.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.selectPlaceholder')" allow-clear style="width: 120px">
            <t-option value="0">{{ $t('monitor.logininfor.success') }}</t-option>
            <t-option value="1">{{ $t('monitor.logininfor.fail') }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" type="submit">{{ $t('monitor.logininfor.search') }}</t-button>
            <t-button @click="handleReset">{{ $t('monitor.logininfor.reset') }}</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-card>

    <!-- 表格 -->
    <t-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('monitor.logininfor.title') }}</span>
          <t-popconfirm v-permission="PERM.monitor.logininforDelete" :content="$t('monitor.logininfor.cleanConfirm')" @confirm="handleClean">
            <t-button theme="danger" size="small">
              <template #icon><icon-delete /></template>
              {{ $t('monitor.logininfor.clean') }}
            </t-button>
          </t-popconfirm>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('monitor.logininfor.userName')" data-index="userName" :width="120" />
            <a-table-column :title="$t('monitor.logininfor.ipAddr')" data-index="ipAddr" :width="140" />
            <a-table-column :title="$t('monitor.logininfor.status')" :width="90">
              <template #cell="{ record }">
                <t-tag :color="record.status === '0' ? 'green' : 'red'">
                  {{ record.status === '0' ? $t('monitor.logininfor.success') : $t('monitor.logininfor.fail') }}
                </t-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('monitor.logininfor.msg')" data-index="msg" />
            <a-table-column :title="$t('monitor.logininfor.loginTime')" data-index="loginTime" :width="170" />
          </template>
        </a-table>

        <div v-if="pagination.total > pagination.pageSize" class="mt-4 flex justify-end">
          <t-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      </a-spin>
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, onMounted } from 'vue'
import {  MessagePlugin  } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import { getLoginLogPageApi, cleanLoginLogApi, type SysLogininfor } from '@/api/logininfor'

const { t } = useI18n()
const loading = ref(false)
const tableData = ref<SysLogininfor[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ userName: '', status: '' })

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getLoginLogPageApi({ ...searchForm, pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = res.data.list || []
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.userName = ''; searchForm.status = ''; pagination.current = 1; loadData() }

async function handleClean() {
  await cleanLoginLogApi()
  MessagePlugin.success(t('monitor.logininfor.cleanSuccess'))
  loadData()
}
</script>
