<template>
  <div class="page-container">
    <t-card class="mb-4">
      <t-form :model="{ tableName: searchKey }" layout="inline" @submit-success="handleSearch">
        <t-form-item field="tableName" :label="$t('tool.gen.tableNameLabel')">
          <t-input v-model="searchKey" :placeholder="$t('tool.gen.searchPlaceholder')" allow-clear style="width: 240px" />
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" html-type="submit">{{ $t('common.search') }}</t-button>
            <t-button @click="searchKey = ''; handleSearch()">{{ $t('common.reset') }}</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-card>

    <t-card>
      <template #title>
        <span>{{ $t('tool.gen.title') }}</span>
      </template>

      <t-loading :loading="loading">
          <t-table row-key="tableName" :data="tableList" :columns="tableColumns" :pagination="false">
    <template #tableName="{ row }">
                <span class="font-mono">{{ row.tableName }}</span>
              </template>
    <template #className="{ row }">
                <span class="text-wecom-primary">{{ row.className }}</span>
              </template>
    <template #col_3="{ row }">
                <t-space>
                  <t-button size="small" @click="handlePreview(row)">{{ $t('tool.gen.preview') }}</t-button>
                  <t-button size="small" theme="primary" @click="handleGenerate(row)">{{ $t('tool.gen.generate') }}</t-button>
                </t-space>
              </template>
  </t-table>
      </t-loading>
    </t-card>

    <!-- 代码预览弹窗 -->
    <t-dialog v-model="previewVisible" :title="$t('tool.gen.codePreview')" :footer="false" :style="{ width: '80%' }" @cancel="previewVisible = false">
      <t-tabs v-if="previewData">
        <t-tab-panel v-for="(code, filename) in previewData" :key="filename" :title="getFileName(filename)">
          <div class="flex justify-between mb-2">
            <span class="text-sm text-wecom-text-tertiary">{{ filename }}</span>
            <t-button size="mini" @click="copyCode(code)">{{ $t('tool.gen.copy') }}</t-button>
          </div>
          <pre class="code-preview"><code>{{ code }}</code></pre>
        </t-tab-panel>
      </t-tabs>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { MessagePlugin } from 'tdesign-vue-next'
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
    MessagePlugin.error(t('tool.gen.generateFailed'))
  }
}

async function handleGenerate(table: TableInfo) {
  try {
    const res = await generateCodeApi(table.tableName)
    const codes = res.data
    if (!codes) { MessagePlugin.error(t('tool.gen.generateFailed')); return }

    // 下载为 zip（简单方式：逐个下载）
    const entries = Object.entries(codes)
    for (const [filename, code] of entries.slice(0, 1)) {
      downloadCode(filename, code)
    }
    MessagePlugin.success(t('tool.gen.generateSuccess', { count: entries.length }))
  } catch {
    MessagePlugin.error(t('tool.gen.generateFailed'))
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
    MessagePlugin.success(t('tool.gen.copySuccess'))
  } catch {
    MessagePlugin.error(t('tool.gen.copyFailed'))
  }
}

/** 表格列定义（自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'tableName', title: t('tool.gen.tableName'), width: 200 },
    { colKey: 'tableComment', title: t('tool.gen.tableComment') },
    { colKey: 'className', title: t('tool.gen.entityName'), width: 160 },
    { colKey: 'col_3', title: t('common.operate'), width: 180, align: 'right' },
  ])

</script>

<style scoped>
.code-preview {
  /* 代码预览区为固定的深色终端样式，不随主题切换，色值为刻意保留的字面量 */
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: var(--td-radius-medium);
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.6;
  max-height: 500px;
  overflow-y: auto;
}
</style>
