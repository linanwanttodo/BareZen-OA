<template>
  <div class="page-container">
    <t-card class="mb-4">
      <div class="flex items-center justify-between">
        <div>
          <h3 class="text-base font-medium">{{ $t('monitor.backup.backupTitle') }}</h3>
          <p class="text-sm text-wecom-text-tertiary mt-1">{{ $t('monitor.backup.backupDesc') }}</p>
        </div>
        <t-button v-permission="PERM.system.backupAdd" theme="primary" :loading="creating" @click="handleCreate">
          <template #icon><icon-plus /></template>
          {{ $t('monitor.backup.createBackup') }}
        </t-button>
      </div>
    </t-card>

    <t-card>
      <template #title>
        <span>{{ $t('monitor.backup.backupList') }}</span>
      </template>

      <a-spin :loading="loading">
        <a-table :data="backupList" :pagination="false" row-key="filename">
          <template #columns>
            <a-table-column :title="$t('monitor.backup.filename')" data-index="filename">
              <template #cell="{ record }">
                <span class="font-mono text-sm">{{ record.filename }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('monitor.backup.fileSize')" :width="120">
              <template #cell="{ record }">
                {{ formatSize(record.fileSize) }}
              </template>
            </a-table-column>
            <a-table-column :title="$t('monitor.backup.backupTime')" data-index="createTime" :width="170" />
            <a-table-column :title="$t('common.operate')" :width="160" align="right">
              <template #cell="{ record }">
                <t-space>
                  <t-button v-permission="PERM.system.backupList" size="small" @click="handleDownload(record)">{{ $t('monitor.backup.download') }}</t-button>
                  <t-popconfirm v-permission="PERM.system.backupDelete" :content="$t('monitor.backup.deleteConfirm')" @confirm="handleDelete(record)">
                    <t-button size="small" theme="danger">{{ $t('monitor.backup.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
            </a-table-column>
          </template>
          <template #empty>
            <t-empty :description="$t('monitor.backup.emptyDesc')" />
          </template>
        </a-table>
      </a-spin>
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {  MessagePlugin  } from 'tdesign-vue-next'
import { createBackupApi, getBackupListApi, downloadBackupApi, deleteBackupApi, type BackupInfo } from '@/api/backup'

const { t } = useI18n()

const loading = ref(false)
const creating = ref(false)
const backupList = ref<BackupInfo[]>([])

onMounted(() => loadList())

async function loadList() {
  loading.value = true
  try {
    const res = await getBackupListApi()
    backupList.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  creating.value = true
  try {
    const res = await createBackupApi()
    if (res.data?.success) {
      MessagePlugin.success(t('monitor.backup.createSuccess'))
      loadList()
    } else {
      MessagePlugin.error(res.data?.message || t('monitor.backup.createFailed'))
    }
  } finally {
    creating.value = false
  }
}

async function handleDownload(record: BackupInfo) {
  try {
    const res = await downloadBackupApi(record.filename) as any
    const blob = new Blob([res.data], { type: 'application/octet-stream' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = record.filename
    a.click()
    URL.revokeObjectURL(url)
    MessagePlugin.success(t('monitor.backup.downloadStart'))
  } catch {
    MessagePlugin.error(t('monitor.backup.downloadFailed'))
  }
}

async function handleDelete(record: BackupInfo) {
  await deleteBackupApi(record.filename)
  MessagePlugin.success(t('monitor.backup.deleteSuccess'))
  loadList()
}

function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(2) + ' ' + units[i]
}
</script>
