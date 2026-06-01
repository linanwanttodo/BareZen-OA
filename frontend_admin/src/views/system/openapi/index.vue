<template>
  <div class="page-container">
    <!-- Tab：API Key / Webhook -->
    <a-tabs v-model:active-key="activeTab" class="mb-4">
      <a-tab-pane key="apiKey" :title="$t('openapi.apiKeyTab')" />
      <a-tab-pane key="webhook" :title="$t('openapi.webhookTab')" />
    </a-tabs>

    <!-- API Key 管理 -->
    <template v-if="activeTab === 'apiKey'">
      <a-card class="mb-4">
        <a-form :model="keySearchForm" layout="inline" @submit-success="handleKeySearch">
          <a-form-item field="keyName" :label="$t('openapi.keyName')">
            <a-input v-model="keySearchForm.keyName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 200px" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit">{{ $t('common.search') }}</a-button>
              <a-button @click="handleKeyReset">{{ $t('common.reset') }}</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <a-card>
        <template #title>
          <div class="flex items-center justify-between w-full">
            <span>{{ $t('openapi.apiKeyTab') }}</span>
            <a-button v-permission="PERM.system.apiKeyAdd" type="primary" @click="openKeyCreate">
              <template #icon><icon-plus /></template>
              {{ $t('openapi.addApiKey') }}
            </a-button>
          </div>
        </template>

        <a-spin :loading="keyLoading">
          <a-table :data="keyData" :pagination="false" row-key="id">
            <template #columns>
              <a-table-column :title="$t('openapi.keyName')" data-index="keyName" :width="160" />
              <a-table-column :title="$t('openapi.maskedKey')" :width="180">
                <template #cell="{ record }">
                  <span class="font-mono text-sm">{{ record.maskedKey }}</span>
                </template>
              </a-table-column>
              <a-table-column :title="$t('openapi.scopes')" data-index="scopes" ellipsis>
                <template #cell="{ record }">{{ record.scopes || '-' }}</template>
              </a-table-column>
              <a-table-column :title="$t('openapi.expireTime')" :width="170">
                <template #cell="{ record }">{{ record.expireTime || $t('openapi.neverExpire') }}</template>
              </a-table-column>
              <a-table-column :title="$t('common.status')" :width="80">
                <template #cell="{ record }">
                  <a-switch
                    :model-value="record.status === '0'"
                    @change="(val: string | number | boolean) => handleKeyStatusChange(record, Boolean(val))"
                  />
                </template>
              </a-table-column>
              <a-table-column :title="$t('common.operate')" :width="160" align="right">
                <template #cell="{ record }">
                  <a-space>
                    <a-button v-permission="PERM.system.apiKeyEdit" size="small" @click="openKeyEdit(record)">{{ $t('common.edit') }}</a-button>
                    <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleKeyDelete(record)">
                      <a-button v-permission="PERM.system.apiKeyDelete" size="small" status="danger">{{ $t('common.delete') }}</a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </a-table-column>
            </template>
          </a-table>

          <div v-if="keyPagination.total > 0" class="mt-4 flex justify-end">
            <a-pagination
              v-model:current="keyPagination.current"
              :total="keyPagination.total"
              :page-size="keyPagination.pageSize"
              @change="loadKeys"
            />
          </div>
        </a-spin>
      </a-card>
    </template>

    <!-- Webhook 管理 -->
    <template v-else>
      <a-card class="mb-4">
        <a-form :model="hookSearchForm" layout="inline" @submit-success="handleHookSearch">
          <a-form-item field="name" :label="$t('openapi.name')">
            <a-input v-model="hookSearchForm.name" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 200px" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit">{{ $t('common.search') }}</a-button>
              <a-button @click="handleHookReset">{{ $t('common.reset') }}</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <a-card>
        <template #title>
          <div class="flex items-center justify-between w-full">
            <span>{{ $t('openapi.webhookTab') }}</span>
            <a-button v-permission="PERM.system.webhookAdd" type="primary" @click="openHookCreate">
              <template #icon><icon-plus /></template>
              {{ $t('openapi.addWebhook') }}
            </a-button>
          </div>
        </template>

        <a-spin :loading="hookLoading">
          <a-table :data="hookData" :pagination="false" row-key="id">
            <template #columns>
              <a-table-column :title="$t('openapi.name')" data-index="name" :width="140" />
              <a-table-column :title="$t('openapi.url')" data-index="url" ellipsis>
                <template #cell="{ record }">
                  <a-link :href="record.url" target="_blank">{{ record.url }}</a-link>
                </template>
              </a-table-column>
              <a-table-column :title="$t('openapi.events')" data-index="events" :width="200" ellipsis />
              <a-table-column :title="$t('openapi.secret')" :width="90">
                <template #cell="{ record }">
                  <span v-if="record.secret" class="font-mono text-sm">****{{ record.secret.slice(-4) }}</span>
                  <span v-else>-</span>
                </template>
              </a-table-column>
              <a-table-column :title="$t('common.status')" :width="80">
                <template #cell="{ record }">
                  <a-switch
                    :model-value="record.status === '0'"
                    @change="(val: string | number | boolean) => handleHookStatusChange(record, Boolean(val))"
                  />
                </template>
              </a-table-column>
              <a-table-column :title="$t('common.operate')" :width="160" align="right">
                <template #cell="{ record }">
                  <a-space>
                    <a-button v-permission="PERM.system.webhookEdit" size="small" @click="openHookEdit(record)">{{ $t('common.edit') }}</a-button>
                    <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleHookDelete(record)">
                      <a-button v-permission="PERM.system.webhookDelete" size="small" status="danger">{{ $t('common.delete') }}</a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </a-table-column>
            </template>
          </a-table>

          <div v-if="hookPagination.total > 0" class="mt-4 flex justify-end">
            <a-pagination
              v-model:current="hookPagination.current"
              :total="hookPagination.total"
              :page-size="hookPagination.pageSize"
              @change="loadHooks"
            />
          </div>
        </a-spin>
      </a-card>
    </template>

    <!-- 创建 API Key 弹窗 -->
    <a-modal v-model:visible="keyCreateVisible" :title="$t('openapi.addApiKey')" @ok="handleKeyCreate" @cancel="keyCreateVisible = false">
      <a-form :model="keyCreateForm" auto-label-width>
        <a-form-item field="keyName" :label="$t('openapi.keyName')" required>
          <a-input v-model="keyCreateForm.keyName" :max-length="50" :placeholder="$t('openapi.keyNamePlaceholder')" />
        </a-form-item>
        <a-form-item field="scopes" :label="$t('openapi.scopes')">
          <a-input v-model="keyCreateForm.scopes" :placeholder="$t('openapi.scopesPlaceholder')" />
        </a-form-item>
        <a-form-item field="expireTime" :label="$t('openapi.expireTime')">
          <a-date-picker v-model="keyCreateForm.expireTime" show-time style="width: 100%" :placeholder="$t('openapi.expireTimePlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 编辑 API Key 弹窗 -->
    <a-modal v-model:visible="keyEditVisible" :title="$t('openapi.editApiKey')" @ok="handleKeyEdit" @cancel="keyEditVisible = false">
      <a-form :model="keyEditForm" auto-label-width>
        <a-form-item field="keyName" :label="$t('openapi.keyName')" required>
          <a-input v-model="keyEditForm.keyName" :max-length="50" :placeholder="$t('openapi.keyNamePlaceholder')" />
        </a-form-item>
        <a-form-item field="scopes" :label="$t('openapi.scopes')">
          <a-input v-model="keyEditForm.scopes" :placeholder="$t('openapi.scopesPlaceholder')" />
        </a-form-item>
        <a-form-item field="expireTime" :label="$t('openapi.expireTime')">
          <a-date-picker v-model="keyEditForm.expireTime" show-time style="width: 100%" :placeholder="$t('openapi.expireTimePlaceholder')" allow-clear />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 创建成功：明文 Key 仅显示一次 -->
    <a-modal v-model:visible="keyResultVisible" :title="$t('openapi.createdTitle')" :footer="false" @cancel="keyResultVisible = false">
      <a-alert type="warning" class="mb-4">{{ $t('openapi.createdOnce') }}</a-alert>
      <a-input :model-value="createdPlainKey" readonly>
        <template #suffix>
          <a-button size="mini" type="text" @click="copyPlainKey">
            <template #icon><icon-copy /></template>
            {{ $t('openapi.copyKey') }}
          </a-button>
        </template>
      </a-input>
      <div class="mt-4 flex justify-end">
        <a-button type="primary" @click="keyResultVisible = false">{{ $t('common.confirm') }}</a-button>
      </div>
    </a-modal>

    <!-- 创建 Webhook 弹窗 -->
    <a-modal v-model:visible="hookCreateVisible" :title="$t('openapi.addWebhook')" @ok="handleHookCreate" @cancel="hookCreateVisible = false">
      <a-form :model="hookCreateForm" auto-label-width>
        <a-form-item field="name" :label="$t('openapi.name')" required>
          <a-input v-model="hookCreateForm.name" :max-length="50" :placeholder="$t('openapi.namePlaceholder')" />
        </a-form-item>
        <a-form-item field="url" :label="$t('openapi.url')" required>
          <a-input v-model="hookCreateForm.url" :placeholder="$t('openapi.urlPlaceholder')" />
        </a-form-item>
        <a-form-item field="events" :label="$t('openapi.events')" required>
          <a-input v-model="hookCreateForm.events" :placeholder="$t('openapi.eventsPlaceholder')" />
        </a-form-item>
        <a-form-item field="secret" :label="$t('openapi.secret')">
          <a-input-password v-model="hookCreateForm.secret" :placeholder="$t('openapi.secretTip')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 编辑 Webhook 弹窗 -->
    <a-modal v-model:visible="hookEditVisible" :title="$t('openapi.editWebhook')" @ok="handleHookEdit" @cancel="hookEditVisible = false">
      <a-form :model="hookEditForm" auto-label-width>
        <a-form-item field="name" :label="$t('openapi.name')" required>
          <a-input v-model="hookEditForm.name" :max-length="50" :placeholder="$t('openapi.namePlaceholder')" />
        </a-form-item>
        <a-form-item field="url" :label="$t('openapi.url')" required>
          <a-input v-model="hookEditForm.url" :placeholder="$t('openapi.urlPlaceholder')" />
        </a-form-item>
        <a-form-item field="events" :label="$t('openapi.events')" required>
          <a-input v-model="hookEditForm.events" :placeholder="$t('openapi.eventsPlaceholder')" />
        </a-form-item>
        <a-form-item field="secret" :label="$t('openapi.secret')">
          <a-input-password v-model="hookEditForm.secret" :placeholder="$t('openapi.secretTip')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
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
    Message.warning(t('openapi.keyNameRequired'))
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
  Message.success(t('openapi.createApiKeySuccess'))
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
    Message.warning(t('openapi.keyNameRequired'))
    return
  }
  await updateApiKeyApi(keyEditForm.id, {
    keyName: keyEditForm.keyName.trim(),
    scopes: keyEditForm.scopes.trim() || undefined,
    expireTime: keyEditForm.expireTime || undefined,
  })
  keyEditVisible.value = false
  Message.success(t('openapi.editSuccess'))
  loadKeys()
}

async function handleKeyStatusChange(row: ApiKeyItem, val: boolean) {
  await changeApiKeyStatusApi(row.id, val ? '0' : '1')
  Message.success(t('openapi.statusUpdated'))
  loadKeys()
}

async function handleKeyDelete(row: ApiKeyItem) {
  await deleteApiKeyApi(row.id)
  Message.success(t('openapi.deleteSuccess'))
  loadKeys()
}

async function copyPlainKey() {
  try {
    await navigator.clipboard.writeText(createdPlainKey.value)
    Message.success(t('openapi.keyCopied'))
  } catch {
    Message.error(t('openapi.copyFailed'))
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
    Message.warning(t('openapi.hookRequired'))
    return
  }
  if (!/^https?:\/\//.test(hookCreateForm.url.trim())) {
    Message.warning(t('openapi.urlProtocol'))
    return
  }
  await createWebhookApi({
    name: hookCreateForm.name.trim(),
    url: hookCreateForm.url.trim(),
    events: hookCreateForm.events.trim(),
    secret: hookCreateForm.secret.trim() || undefined,
  })
  hookCreateVisible.value = false
  Message.success(t('openapi.createWebhookSuccess'))
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
    Message.warning(t('openapi.hookRequired'))
    return
  }
  if (!/^https?:\/\//.test(hookEditForm.url.trim())) {
    Message.warning(t('openapi.urlProtocol'))
    return
  }
  await updateWebhookApi(hookEditForm.id, {
    name: hookEditForm.name.trim(),
    url: hookEditForm.url.trim(),
    events: hookEditForm.events.trim(),
    secret: hookEditForm.secret.trim() || undefined,
  })
  hookEditVisible.value = false
  Message.success(t('openapi.editSuccess'))
  loadHooks()
}

async function handleHookStatusChange(row: WebhookItem, val: boolean) {
  await changeWebhookStatusApi(row.id, val ? '0' : '1')
  Message.success(t('openapi.statusUpdated'))
  loadHooks()
}

async function handleHookDelete(row: WebhookItem) {
  await deleteWebhookApi(row.id)
  Message.success(t('openapi.deleteSuccess'))
  loadHooks()
}
</script>