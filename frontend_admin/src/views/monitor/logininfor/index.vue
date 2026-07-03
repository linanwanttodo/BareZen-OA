<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :data="searchForm" :label-width="80" :on-submit="handleSearch">
        <t-form-item name="userName" :label="$t('monitor.logininfor.userName')">
          <t-input v-model="searchForm.userName" :placeholder="$t('common.inputPlaceholder')" clearable />
        </t-form-item>
        <t-form-item name="status" :label="$t('monitor.logininfor.status')">
          <t-select v-model="searchForm.status" :placeholder="$t('common.selectPlaceholder')" allow-clear>
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
      <div class="bz-list-toolbar bz-list-toolbar--first">
        <t-space>
          <t-button theme="danger" size="small">
              <template #icon><icon-delete /></template>
              {{ $t('monitor.logininfor.clean') }}
            </t-button>
        </t-space>
      </div>

      
          <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #status="{ row }">
                <t-tag :theme="row.status === '0' ? 'success' : 'danger'" variant="light">
                  {{ row.status === '0' ? $t('monitor.logininfor.success') : $t('monitor.logininfor.fail') }}
                </t-tag>
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
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
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

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'userName', title: t('monitor.logininfor.userName'), width: 120 },
    { colKey: 'ipAddr', title: t('monitor.logininfor.ipAddr'), width: 140 },
    { colKey: 'status', title: t('monitor.logininfor.status'), width: 90 },
    { colKey: 'msg', title: t('monitor.logininfor.msg') },
    { colKey: 'loginTime', title: t('monitor.logininfor.loginTime'), width: 170 },
  ])

</script>
