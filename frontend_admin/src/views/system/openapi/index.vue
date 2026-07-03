<template>
  <div class="page-container">
    <!-- Tab：API Key / Webhook -->
    <t-tabs v-model:active-key="activeTab" class="mb-4">
      <t-tab-panel key="apiKey" :title="$t('openapi.apiKeyTab')" />
      <t-tab-panel key="webhook" :title="$t('openapi.webhookTab')" />
    </t-tabs>

    <!-- API Key 管理 -->
    <template v-if="activeTab === 'apiKey'">
      <t-card class="mb-4">
        <t-form :data="keySearchForm" :label-width="80" @submit-success="handleKeySearch">
          <t-form-item name="keyName" :label="$t('openapi.keyName')">
            <t-input v-model="keySearchForm.keyName" :placeholder="$t('common.inputPlaceholder')" allow-clear />
          </t-form-item>
          <t-form-item>
            <t-space>
              <t-button theme="primary" html-type="submit">{{ $t('common.search') }}</t-button>
              <t-button @click="handleKeyReset">{{ $t('common.reset') }}</t-button>
            </t-space>
          </t-form-item>
        </t-form>
      </t-card>

      <t-card>
        <template #title>
          <div class="flex items-center justify-between w-full">
            <span>{{ $t('openapi.apiKeyTab') }}</span>
            <t-button v-permission="PERM.system.apiKeyAdd" theme="primary" @click="openKeyCreate">
              <template #icon><icon-plus /></template>
              {{ $t('openapi.addApiKey') }}
            </t-button>
          </div>
        </template>

        
            <t-table :loading="keyLoading" row-key="id" :data="keyData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #maskedKey="{ row }">
                  <span class="font-mono text-sm">{{ row.maskedKey }}</span>
                </template>
    <template #scopes="{ row }">{{ row.scopes || '-' }}</template>
    <template #expireTime="{ row }">{{ row.expireTime || $t('openapi.neverExpire') }}</template>
    <template #status="{ row }">
                  <t-switch
                    :model-value="row.status === '0'"
                    @change="(val: string | number | boolean) => handleKeyStatusChange(row, Boolean(val))"
                  />
                </template>
    <template #col_5="{ row }">
                  <t-space>
                    <t-link theme="primary" v-permission="PERM.system.apiKeyEdit" @click="openKeyEdit(row)">{{ $t('common.edit') }}</t-link>
                    <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleKeyDelete(row)">
                      <t-button v-permission="PERM.system.apiKeyDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                    </t-popconfirm>
                  </t-space>
                </template>
  </t-table>

          <div v-if="keyPagination.total > 0" class="mt-4 flex justify-end">
            <t-pagination
              v-model:current="keyPagination.current"
              :total="keyPagination.total"
              :page-size="keyPagination.pageSize"
              @change="loadKeys"
            />
          </div>
        
      </t-card>
    </template>

    <!-- Webhook 管理 -->
    <template v-else>
      <t-card class="mb-4">
        <t-form :data="hookSearchForm" :label-width="80" @submit-success="handleHookSearch">
          <t-form-item name="name" :label="$t('openapi.name')">
            <t-input v-model="hookSearchForm.name" :placeholder="$t('common.inputPlaceholder')" allow-clear />
          </t-form-item>
          <t-form-item>
            <t-space>
              <t-button theme="primary" html-type="submit">{{ $t('common.search') }}</t-button>
              <t-button @click="handleHookReset">{{ $t('common.reset') }}</t-button>
            </t-space>
          </t-form-item>
        </t-form>
      </t-card>

      <t-card>
        <template #title>
          <div class="flex items-center justify-between w-full">
            <span>{{ $t('openapi.webhookTab') }}</span>
            <t-button v-permission="PERM.system.webhookAdd" theme="primary" @click="openHookCreate">
              <template #icon><icon-plus /></template>
              {{ $t('openapi.addWebhook') }}
            </t-button>
          </div>
        </template>

        
            <t-table :loading="hookLoading" row-key="id" :data="hookData" :columns="tableColumns2" :pagination="false" :hover="true" vertical-align="top">
    <template #url="{ row }">
                  <t-link :href="row.url" target="_blank">{{ row.url }}</t-link>
                </template>
    <template #secret="{ row }">
                  <span v-if="row.secret" class="font-mono text-sm">****{{ row.secret.slice(-4) }}</span>
                  <span v-else>-</span>
                </template>
    <template #status_2="{ row }">
                  <t-switch
                    :model-value="row.status === '0'"
                    @change="(val: string | number | boolean) => handleHookStatusChange(row, Boolean(val))"
                  />
                </template>
    <template #operate="{ row }">
                  <t-space>
                    <t-link theme="primary" v-permission="PERM.system.webhookEdit" @click="openHookEdit(row)">{{ $t('common.edit') }}</t-link>
                    <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleHookDelete(row)">
                      <t-button v-permission="PERM.system.webhookDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                    </t-popconfirm>
                  </t-space>
                </template>
  </t-table>

          <div v-if="hookPagination.total > 0" class="mt-4 flex justify-end">
            <t-pagination
              v-model:current="hookPagination.current"
              :total="hookPagination.total"
              :page-size="hookPagination.pageSize"
              @change="loadHooks"
            />
          </div>
        
      </t-card>
    </template>

    <!-- 创建 API Key 弹窗 -->
    <t-dialog v-model:visible="keyCreateVisible" :title="$t('openapi.addApiKey')" @confirm="handleKeyCreate" @cancel="keyCreateVisible = false">
      <t-form :data="keyCreateForm" auto-label-width>
        <t-form-item name="keyName" :label="$t('openapi.keyName')" required>
          <t-input v-model="keyCreateForm.keyName" :max-length="50" :placeholder="$t('openapi.keyNamePlaceholder')" />
        </t-form-item>
        <t-form-item name="scopes" :label="$t('openapi.scopes')">
          <t-input v-model="keyCreateForm.scopes" :placeholder="$t('openapi.scopesPlaceholder')" />
        </t-form-item>
        <t-form-item name="expireTime" :label="$t('openapi.expireTime')">
          
<t-date-picker v-model="keyCreateForm.expireTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('openapi.expireTimePlaceholder')" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 编辑 API Key 弹窗 -->
    <t-dialog v-model:visible="keyEditVisible" :title="$t('openapi.editApiKey')" @confirm="handleKeyEdit" @cancel="keyEditVisible = false">
      <t-form :data="keyEditForm" auto-label-width>
        <t-form-item name="keyName" :label="$t('openapi.keyName')" required>
          <t-input v-model="keyEditForm.keyName" :max-length="50" :placeholder="$t('openapi.keyNamePlaceholder')" />
        </t-form-item>
        <t-form-item name="scopes" :label="$t('openapi.scopes')">
          <t-input v-model="keyEditForm.scopes" :placeholder="$t('openapi.scopesPlaceholder')" />
        </t-form-item>
        <t-form-item name="expireTime" :label="$t('openapi.expireTime')">
          
<t-date-picker v-model="keyEditForm.expireTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('openapi.expireTimePlaceholder')" allow-clear />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 创建成功：明文 Key 仅显示一次 -->
    <t-dialog v-model:visible="keyResultVisible" :title="$t('openapi.createdTitle')" :footer="false" @cancel="keyResultVisible = false">
      <t-alert type="warning" class="mb-4">{{ $t('openapi.createdOnce') }}</t-alert>
      <t-input :model-value="createdPlainKey" readonly>
        <template #suffix>
          <t-button size="mini" variant="text" @click="copyPlainKey">
            <template #icon><icon-copy /></template>
            {{ $t('openapi.copyKey') }}
          </t-button>
        </template>
      </t-input>
      <div class="mt-4 flex justify-end">
        <t-button theme="primary" @click="keyResultVisible = false">{{ $t('common.confirm') }}</t-button>
      </div>
    </t-dialog>

    <!-- 创建 Webhook 弹窗 -->
    <t-dialog v-model:visible="hookCreateVisible" :title="$t('openapi.addWebhook')" @confirm="handleHookCreate" @cancel="hookCreateVisible = false">
      <t-form :data="hookCreateForm" auto-label-width>
        <t-form-item name="name" :label="$t('openapi.name')" required>
          <t-input v-model="hookCreateForm.name" :max-length="50" :placeholder="$t('openapi.namePlaceholder')" />
        </t-form-item>
        <t-form-item name="url" :label="$t('openapi.url')" required>
          <t-input v-model="hookCreateForm.url" :placeholder="$t('openapi.urlPlaceholder')" />
        </t-form-item>
        <t-form-item name="events" :label="$t('openapi.events')" required>
          <t-input v-model="hookCreateForm.events" :placeholder="$t('openapi.eventsPlaceholder')" />
        </t-form-item>
        <t-form-item name="secret" :label="$t('openapi.secret')">
          <t-input v-model="hookCreateForm.secret" :placeholder="$t('openapi.secretTip')" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 编辑 Webhook 弹窗 -->
    <t-dialog v-model:visible="hookEditVisible" :title="$t('openapi.editWebhook')" @confirm="handleHookEdit" @cancel="hookEditVisible = false">
      <t-form :data="hookEditForm" auto-label-width>
        <t-form-item name="name" :label="$t('openapi.name')" required>
          <t-input v-model="hookEditForm.name" :max-length="50" :placeholder="$t('openapi.namePlaceholder')" />
        </t-form-item>
        <t-form-item name="url" :label="$t('openapi.url')" required>
          <t-input v-model="hookEditForm.url" :placeholder="$t('openapi.urlPlaceholder')" />
        </t-form-item>
        <t-form-item name="events" :label="$t('openapi.events')" required>
          <t-input v-model="hookEditForm.events" :placeholder="$t('openapi.eventsPlaceholder')" />
        </t-form-item>
        <t-form-item name="secret" :label="$t('openapi.secret')">
          <t-input v-model="hookEditForm.secret" :placeholder="$t('openapi.secretTip')" />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import {
  getApiKeyPageApi,
  createApiKeyApi,
  updateApiKeyApi,
  changeApiKeyStatusApi,
  deleteApiKeyApi,
  getWebhookPageApi,
  createWebhookApi,
  updateWebhookApi,
  changeWebhookStatusApi,
  deleteWebhookApi,
  type ApiKeyItem,
  type WebhookItem,
} from '@/api/openapi'

const { t } = useI18n()

const activeTab = ref<'apiKey' | 'webhook'>('apiKey')

/* ---------------- API Key ---------------- */
const keyLoading = ref(false)
const keyData = ref<ApiKeyItem[]>([])
const keyPagination = reactive({ current: 1, pageSize: 10, total: 0 })
const keySearchForm = reactive({ keyName: '' })

const keyCreateVisible = ref(false)
const keyCreateForm = reactive({ keyName: '', scopes: '', expireTime: '' })
const keyResultVisible = ref(false)
const createdPlainKey = ref('')

const keyEditVisible = ref(false)
const keyEditForm = reactive({ id: '', keyName: '', scopes: '', expireTime: '' as string | undefined })

onMounted(() => loadKeys())

async function loadKeys() {
  keyLoading.value = true
  try {
    const res = await getApiKeyPageApi({ ...keySearchForm, pageNum: keyPagination.current, pageSize: keyPagination.pageSize })
    keyData.value = res.data.list || []
    keyPagination.total = res.data.total
  } finally {
    keyLoading.value = false
  }
}

function handleKeySearch() { keyPagination.current = 1; loadKeys() }
function handleKeyReset() { keySearchForm.keyName = ''; keyPagination.current = 1; loadKeys() }

function openKeyCreate() {
  keyCreateForm.keyName = ''
  keyCreateForm.scopes = ''
  keyCreateForm.expireTime = ''
  keyCreateVisible.value = true
}

async function handleKeyCreate() {
  if (!keyCreateForm.keyName.trim()) {
    MessagePlugin.warning(t('openapi.keyNameRequired'))
    return
  }
  const res = await createApiKeyApi({
    keyName: keyCreateForm.keyName.trim(),
    scopes: keyCreateForm.scopes.trim() || undefined,
    expireTime: keyCreateForm.expireTime || undefined,
  })
  keyCreateVisible.value = false
  createdPlainKey.value = res.data.apiKey
  keyResultVisible.value = true
  MessagePlugin.success(t('openapi.createApiKeySuccess'))
  loadKeys()
}

function openKeyEdit(row: ApiKeyItem) {
  keyEditForm.id = row.id
  keyEditForm.keyName = row.keyName
  keyEditForm.scopes = row.scopes || ''
  keyEditForm.expireTime = row.expireTime || undefined
  keyEditVisible.value = true
}

async function handleKeyEdit() {
  if (!keyEditForm.keyName.trim()) {
    MessagePlugin.warning(t('openapi.keyNameRequired'))
    return
  }
  await updateApiKeyApi(keyEditForm.id, {
    keyName: keyEditForm.keyName.trim(),
    scopes: keyEditForm.scopes.trim() || undefined,
    expireTime: keyEditForm.expireTime || undefined,
  })
  keyEditVisible.value = false
  MessagePlugin.success(t('openapi.editSuccess'))
  loadKeys()
}

async function handleKeyStatusChange(row: ApiKeyItem, val: boolean) {
  await changeApiKeyStatusApi(row.id, val ? '0' : '1')
  MessagePlugin.success(t('openapi.statusUpdated'))
  loadKeys()
}

async function handleKeyDelete(row: ApiKeyItem) {
  await deleteApiKeyApi(row.id)
  MessagePlugin.success(t('openapi.deleteSuccess'))
  loadKeys()
}

async function copyPlainKey() {
  try {
    await navigator.clipboard.writeText(createdPlainKey.value)
    MessagePlugin.success(t('openapi.keyCopied'))
  } catch {
    MessagePlugin.error(t('openapi.copyFailed'))
  }
}

/* ---------------- Webhook ---------------- */
const hookLoading = ref(false)
const hookData = ref<WebhookItem[]>([])
const hookPagination = reactive({ current: 1, pageSize: 10, total: 0 })
const hookSearchForm = reactive({ name: '' })

const hookCreateVisible = ref(false)
const hookCreateForm = reactive({ name: '', url: '', events: '', secret: '' })

const hookEditVisible = ref(false)
const hookEditForm = reactive({ id: '', name: '', url: '', events: '', secret: '' })

async function loadHooks() {
  hookLoading.value = true
  try {
    const res = await getWebhookPageApi({ ...hookSearchForm, pageNum: hookPagination.current, pageSize: hookPagination.pageSize })
    hookData.value = res.data.list || []
    hookPagination.total = res.data.total
  } finally {
    hookLoading.value = false
  }
}

function handleHookSearch() { hookPagination.current = 1; loadHooks() }
function handleHookReset() { hookSearchForm.name = ''; hookPagination.current = 1; loadHooks() }

function openHookCreate() {
  hookCreateForm.name = ''
  hookCreateForm.url = ''
  hookCreateForm.events = ''
  hookCreateForm.secret = ''
  hookCreateVisible.value = true
}

async function handleHookCreate() {
  if (!hookCreateForm.name.trim() || !hookCreateForm.url.trim() || !hookCreateForm.events.trim()) {
    MessagePlugin.warning(t('openapi.hookRequired'))
    return
  }
  if (!/^https?:\/\//.test(hookCreateForm.url.trim())) {
    MessagePlugin.warning(t('openapi.urlProtocol'))
    return
  }
  await createWebhookApi({
    name: hookCreateForm.name.trim(),
    url: hookCreateForm.url.trim(),
    events: hookCreateForm.events.trim(),
    secret: hookCreateForm.secret.trim() || undefined,
  })
  hookCreateVisible.value = false
  MessagePlugin.success(t('openapi.createWebhookSuccess'))
  loadHooks()
}

function openHookEdit(row: WebhookItem) {
  hookEditForm.id = row.id
  hookEditForm.name = row.name
  hookEditForm.url = row.url
  hookEditForm.events = row.events
  hookEditForm.secret = ''
  hookEditVisible.value = true
}

async function handleHookEdit() {
  if (!hookEditForm.name.trim() || !hookEditForm.url.trim() || !hookEditForm.events.trim()) {
    MessagePlugin.warning(t('openapi.hookRequired'))
    return
  }
  if (!/^https?:\/\//.test(hookEditForm.url.trim())) {
    MessagePlugin.warning(t('openapi.urlProtocol'))
    return
  }
  await updateWebhookApi(hookEditForm.id, {
    name: hookEditForm.name.trim(),
    url: hookEditForm.url.trim(),
    events: hookEditForm.events.trim(),
    secret: hookEditForm.secret.trim() || undefined,
  })
  hookEditVisible.value = false
  MessagePlugin.success(t('openapi.editSuccess'))
  loadHooks()
}

async function handleHookStatusChange(row: WebhookItem, val: boolean) {
  await changeWebhookStatusApi(row.id, val ? '0' : '1')
  MessagePlugin.success(t('openapi.statusUpdated'))
  loadHooks()
}

async function handleHookDelete(row: WebhookItem) {
  await deleteWebhookApi(row.id)
  MessagePlugin.success(t('openapi.deleteSuccess'))
  loadHooks()
}

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'keyName', title: t('openapi.keyName'), width: 160 },
    { colKey: 'maskedKey', title: t('openapi.maskedKey'), width: 180 },
    { colKey: 'scopes', title: t('openapi.scopes'), ellipsis: true },
    { colKey: 'expireTime', title: t('openapi.expireTime'), width: 170 },
    { colKey: 'status', title: t('common.status'), width: 80 },
    { colKey: 'col_5', title: t('common.operate'), width: 160, align: 'right' },
  ])

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns2 = computed(() => [
    { colKey: 'name', title: t('openapi.name'), width: 140 },
    { colKey: 'url', title: t('openapi.url'), ellipsis: true },
    { colKey: 'events', title: t('openapi.events'), width: 200, ellipsis: true },
    { colKey: 'secret', title: t('openapi.secret'), width: 90 },
    { colKey: 'status_2', title: t('common.status'), width: 80 },
    { colKey: 'operate', title: t('common.operate'), width: 160, align: 'right' },
  ])

</script>