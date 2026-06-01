<template>
  <div class="page-container">
    <a-card class="mb-4">
      <a-form :model="{ tableName: searchKey }" layout="inline" @submit-success="handleSearch">
        <a-form-item field="tableName" :label="$t('tool.gen.tableNameLabel')">
          <a-input v-model="searchKey" :placeholder="$t('tool.gen.searchPlaceholder')" allow-clear style="width: 240px" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">{{ $t('common.search') }}</a-button>
            <a-button @click="searchKey = ''; handleSearch()">{{ $t('common.reset') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card>
      <template #title>
        <span>{{ $t('tool.gen.title') }}</span>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableList" :pagination="false" row-key="tableName">
          <template #columns>
            <a-table-column :title="$t('tool.gen.tableName')" data-index="tableName" :width="200">
              <template #cell="{ record }">
                <span class="font-mono">{{ record.tableName }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('tool.gen.tableComment')" data-index="tableComment" />
            <a-table-column :title="$t('tool.gen.entityName')" data-index="className" :width="160">
              <template #cell="{ record }">
                <span class="text-wecom-primary">{{ record.className }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="180" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button size="small" @click="handlePreview(record)">{{ $t('tool.gen.preview') }}</a-button>
                  <a-button size="small" type="primary" @click="handleGenerate(record)">{{ $t('tool.gen.generate') }}</a-button>
                </a-space>
              </template>
            </a-table-column>
          </template>
          <template #empty>
            <a-empty :description="$t('tool.gen.noTables')" />
          </template>
        </a-table>
      </a-spin>
    </a-card>

    <!-- 代码预览弹窗 -->
    <a-modal v-model:visible="previewVisible" :title="$t('tool.gen.codePreview')" :footer="false" :style="{ width: '80%' }" @cancel="previewVisible = false">
      <a-tabs v-if="previewData">
        <a-tab-pane v-for="(code, filename) in previewData" :key="filename" :title="getFileName(filename)">
          <div class="flex justify-between mb-2">
            <span class="text-sm text-wecom-text-tertiary">{{ filename }}</span>
            <a-button size="mini" @click="copyCode(code)">{{ $t('tool.gen.copy') }}</a-button>
          </div>
          <pre class="code-preview"><code>{{ code }}</code></pre>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { getGenTableListApi, generateCodeApi, type TableInfo } from '@/api/gen'

const { t } = useI18n()

const loading = ref(false)
const searchKey = ref('')
const tableList = ref<TableInfo[]>([])
const previewVisible = ref(false)
const previewData = ref<Record<string, string> | null>(null)

onMounted(() => loadTables())

async function loadTables() {
  loading.value = true
  try {
    const res = await getGenTableListApi(searchKey.value || undefined)
    tableList.value = res.data || []
  } finally {
    loading.value = false
  }
}

function handleSearch() { loadTables() }

function getFileName(fullPath: string) {
  return fullPath.split('/').pop() || fullPath
}

async function handlePreview(table: TableInfo) {
  try {
    const res = await generateCodeApi(table.tableName)
    previewData.value = res.data
    previewVisible.value = true
  } catch {
    Message.error(t('tool.gen.generateFailed'))
  }
}

async function handleGenerate(table: TableInfo) {
  try {
    const res = await generateCodeApi(table.tableName)
    const codes = res.data
    if (!codes) { Message.error(t('tool.gen.generateFailed')); return }

    // 下载为 zip（简单方式：逐个下载）
    const entries = Object.entries(codes)
    for (const [filename, code] of entries.slice(0, 1)) {
      downloadCode(filename, code)
    }
    Message.success(t('tool.gen.generateSuccess', { count: entries.length }))
  } catch {
    Message.error(t('tool.gen.generateFailed'))
  }
}

function downloadCode(filename: string, code: string) {
  const blob = new Blob([code], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename.split('/').pop() || 'code.txt'
  a.click()
  URL.revokeObjectURL(url)
}

async function copyCode(code: string) {
  try {
    await navigator.clipboard.writeText(code)
    Message.success(t('tool.gen.copySuccess'))
  } catch {
    Message.error(t('tool.gen.copyFailed'))
  }
}
</script>

<style scoped>
.code-preview {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.6;
  max-height: 500px;
  overflow-y: auto;
}
</style>
