<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="userName" :label="$t('monitor.logininfor.userName')">
          <a-input v-model="searchForm.userName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 200px" />
        </a-form-item>
        <a-form-item field="status" :label="$t('monitor.logininfor.status')">
          <a-select v-model="searchForm.status" :placeholder="$t('common.selectPlaceholder')" allow-clear style="width: 120px">
            <a-option value="0">{{ $t('monitor.logininfor.success') }}</a-option>
            <a-option value="1">{{ $t('monitor.logininfor.fail') }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">{{ $t('monitor.logininfor.search') }}</a-button>
            <a-button @click="handleReset">{{ $t('monitor.logininfor.reset') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 表格 -->
    <a-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('monitor.logininfor.title') }}</span>
          <a-popconfirm v-permission="'monitor:logininfor:delete'" :content="$t('monitor.logininfor.cleanConfirm')" @ok="handleClean">
            <a-button status="danger" size="small">
              <template #icon><icon-delete /></template>
              {{ $t('monitor.logininfor.clean') }}
            </a-button>
          </a-popconfirm>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('monitor.logininfor.userName')" data-index="userName" :width="120" />
            <a-table-column :title="$t('monitor.logininfor.ipAddr')" data-index="ipAddr" :width="140" />
            <a-table-column :title="$t('monitor.logininfor.status')" :width="90">
              <template #cell="{ record }">
                <a-tag :color="record.status === '0' ? 'green' : 'red'">
                  {{ record.status === '0' ? $t('monitor.logininfor.success') : $t('monitor.logininfor.fail') }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('monitor.logininfor.msg')" data-index="msg" />
            <a-table-column :title="$t('monitor.logininfor.loginTime')" data-index="loginTime" :width="170" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
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
  Message.success(t('monitor.logininfor.cleanSuccess'))
  loadData()
}
</script>
