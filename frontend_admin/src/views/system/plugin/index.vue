<template>
  <div class="page-container">
    <t-card :bordered="false" class="bz-list-panel">
      <div class="bz-list-toolbar bz-list-toolbar--first">
        <t-space>
          <t-button theme="primary" variant="outline" @click="loadData">
            <template #icon><icon-refresh /></template>
            {{ $t('common.refresh') }}
          </t-button>
        </t-space>
      </div>

      
        <t-table :loading="loading" row-key="pluginId" :data="plugins" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
          <template #status="{ row }">
            <t-tag :theme="statusTheme(row.status)" variant="light">{{ statusText(row.status) }}</t-tag>
          </template>
          <template #enabled="{ row }">
            <t-tag :theme="row.enabled ? 'success' : 'default'" variant="light">
              {{ row.enabled ? $t('plugin.running') : $t('plugin.stopped') }}
            </t-tag>
          </template>
          <template #operate="{ row }">
            <t-space>
              <t-popconfirm v-if="row.status !== 'STARTED'" :content="$t('plugin.startConfirm')" @confirm="handleStart(row)">
                <t-button v-permission="PERM.system.pluginManage" size="small" theme="success">{{ $t('plugin.start') }}</t-button>
              </t-popconfirm>
              <t-popconfirm v-else :content="$t('plugin.stopConfirm')" @confirm="handleStop(row)">
                <t-button v-permission="PERM.system.pluginManage" size="small" theme="warning">{{ $t('plugin.stop') }}</t-button>
              </t-popconfirm>
            </t-space>
          </template>
        </t-table>
        <t-empty v-if="!loading && !plugins.length" :description="$t('common.noData')" />
      
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import { getPluginListApi, startPluginApi, stopPluginApi, type PluginInfo } from '@/api/plugin'

const { t } = useI18n()
const loading = ref(false)
const plugins = ref<PluginInfo[]>([])

function statusText(status: string): string {
  const map: Record<string, string> = {
    INSTALLED: t('plugin.statusInstalled'),
    INITIALIZED: t('plugin.statusInitialized'),
    STARTED: t('plugin.statusStarted'),
    STOPPED: t('plugin.statusStopped'),
    FAILED: t('plugin.statusFailed'),
    UNINSTALLED: t('plugin.statusUninstalled'),
  }
  return map[status] || status
}

function statusTheme(status: string): 'success' | 'warning' | 'danger' | 'primary' | 'default' {
  if (status === 'STARTED') return 'success'
  if (status === 'STOPPED') return 'warning'
  if (status === 'FAILED') return 'danger'
  if (status === 'INITIALIZED' || status === 'INSTALLED') return 'primary'
  return 'default'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getPluginListApi()
    plugins.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleStart(row: PluginInfo) {
  try {
    await startPluginApi(row.pluginId)
    MessagePlugin.success(t('plugin.started'))
    loadData()
  } catch (e) {
    MessagePlugin.error((e as Error)?.message || t('common.failed'))
  }
}

async function handleStop(row: PluginInfo) {
  try {
    await stopPluginApi(row.pluginId)
    MessagePlugin.success(t('plugin.stoppedMsg'))
    loadData()
  } catch (e) {
    MessagePlugin.error((e as Error)?.message || t('common.failed'))
  }
}

onMounted(loadData)

const tableColumns = computed(() => [
  { colKey: 'pluginName', title: t('plugin.name'), width: 160 },
  { colKey: 'pluginId', title: t('plugin.id'), width: 220, ellipsis: true },
  { colKey: 'version', title: t('plugin.version'), width: 90 },
  { colKey: 'author', title: t('plugin.author'), width: 140 },
  { colKey: 'description', title: t('plugin.description'), ellipsis: true },
  { colKey: 'status', title: t('plugin.status'), width: 110 },
  { colKey: 'enabled', title: t('plugin.enabledState'), width: 90 },
  { colKey: 'operate', title: t('common.operate'), width: 100, align: 'right' },
])
</script>
