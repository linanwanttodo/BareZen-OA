<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('supplyApply.title') }}</h1>
      <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('supplyApply.subtitle') }}</p>
    </div>

    <!-- Tab：可申领 / 我的申领 -->
    <t-tabs v-model:active-key="activeTab" class="mb-6" @change="onTabChange">
      <t-tab-panel key="supply" :title="$t('supplyApply.available')" />
      <t-tab-panel key="request" :title="$t('supplyApply.myRequests')" />
    </t-tabs>

    <!-- 可申领用品 -->
    <div v-if="activeTab === 'supply'">
      <div class="mb-4 flex justify-end">
        <t-input v-model="keyword" :placeholder="$t('supplyApply.searchPlaceholder')" allow-clear style="width: 100%; max-width: 320px" @press-enter="loadSupplies" @clear="loadSupplies">
          <template #prefix><icon-search /></template>
        </t-input>
      </div>
      <t-loading :loading="supplyLoading">
        <div v-if="supplies.length" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div v-for="item in supplies" :key="item.id" class="rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5">
            <div class="flex items-start justify-between">
              <div>
                <h3 class="font-semibold text-[var(--foreground)]">{{ item.supplyName }}</h3>
                <p class="mt-1 text-xs text-[var(--muted-foreground)]">{{ item.spec || '-' }} · {{ item.unit }}</p>
              </div>
              <t-tag theme="primary">{{ item.stock }} {{ item.unit }}</t-tag>
            </div>
            <div class="mt-4 flex items-center gap-2">
              <t-input-number v-model="applyQuantities[item.id]" :min="1" :max="item.stock" style="width: 120px" />
              <t-button theme="primary" :disabled="!item.stock" @click="openApply(item)">
                {{ $t('supplyApply.apply') }}
              </t-button>
            </div>
          </div>
        </div>
        <t-empty v-else :description="$t('supplyApply.noSupply')" />
      </t-loading>
    </div>

    <!-- 我的申领 -->
    <div v-else>
      <t-loading :loading="requestLoading">
        <div v-if="requests.length" class="space-y-3">
          <div v-for="item in requests" :key="item.id" class="flex items-start gap-4 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5">
            <div class="min-w-0 flex-1">
              <div class="mb-1 flex items-center gap-2">
                <t-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</t-tag>
                <span class="font-medium text-[var(--foreground)]">{{ item.supplyName }} × {{ item.quantity }}</span>
              </div>
              <p v-if="item.reason" class="text-sm text-[var(--muted-foreground)]">{{ item.reason }}</p>
              <p v-if="item.approveRemark" class="mt-1 text-xs text-[var(--muted-foreground)]">
                {{ $t('supplyApply.approveRemark') }}：{{ item.approveRemark }}
              </p>
            </div>
            <t-button v-if="item.status === '0'" size="small" theme="danger" @click="handleRevoke(item)">
              {{ $t('supplyApply.revoke') }}
            </t-button>
            <t-button size="small" @click="openFlow(item.id)">{{ $t('supplyApply.flow') }}</t-button>
          </div>
        </div>
        <t-empty v-else :description="$t('supplyApply.noRequest')" />
      </t-loading>
    </div>

    <!-- 流程进度弹窗 -->
    <t-dialog v-model="flowVisible" :title="$t('supplyApply.flow')" :width="520" footer-only>
      <t-steps v-if="flowNodes.length" direction="vertical" :current="currentStep">
        <t-step-item
          v-for="node in flowNodes"
          :key="node.id"
          :title="node.nodeName"
          :description="`${node.operatorName || '-'} · ${node.createTime}${node.remark ? ' · ' + node.remark : ''}`"
        />
      </t-steps>
      <t-empty v-else :description="$t('supplyApply.flowEmpty')" />
    </t-dialog>

    <!-- 申领弹窗 -->
    <t-dialog v-model="applyVisible" :title="$t('supplyApply.apply')" :width="480"
             @confirm="handleSubmit" @cancel="applyVisible = false" :ok-loading="submitting">
      <t-form :model="applyForm" layout="vertical">
        <t-form-item :label="$t('supplyApply.supplyName')">
          <t-input :model-value="applyForm.supplyName" disabled />
        </t-form-item>
        <t-form-item :label="$t('supplyApply.quantity')" required>
          <t-input-number v-model="applyForm.quantity" :min="1" :max="applyForm.maxStock" style="width: 100%" />
        </t-form-item>
        <t-form-item :label="$t('supplyApply.reason')">
          <t-textarea v-model="applyForm.reason" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import {
  getSuppliesApi, getMySupplyRequestsApi, submitSupplyRequestApi, revokeSupplyRequestApi,
  getFlowLogsApi,
  type Supply, type SupplyRequest, type FlowLog,
} from '@/api/enterprise'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const activeTab = ref('supply')
const keyword = ref('')
const supplyLoading = ref(false)
const requestLoading = ref(false)
const submitting = ref(false)
const supplies = ref<Supply[]>([])
const requests = ref<SupplyRequest[]>([])
const applyQuantities = reactive<Record<string, number>>({})
const applyVisible = ref(false)

const applyForm = reactive<{ supplyId: string; supplyName: string; quantity: number; maxStock: number; reason: string }>({
  supplyId: '', supplyName: '', quantity: 1, maxStock: 99, reason: '',
})

function statusText(status: string | undefined): string {
  const map: Record<string, string> = { 0: '待审批', 1: '已通过', 2: '已驳回', 3: '已取消' }
  return status != null ? map[status] || '-' : '-'
}
function statusColor(status: string | undefined): string {
  if (status === '0') return 'orange'
  if (status === '1') return 'green'
  if (status === '2') return 'red'
  return 'default'
}

async function loadSupplies() {
  supplyLoading.value = true
  try {
    const res = await getSuppliesApi({ page: 1, pageSize: 50, keyword: keyword.value || undefined })
    supplies.value = res.data.list || []
    supplies.value.forEach((s) => { applyQuantities[s.id] = 1 })
  } finally { supplyLoading.value = false }
}

async function loadRequests() {
  requestLoading.value = true
  try {
    const res = await getMySupplyRequestsApi({ page: 1, pageSize: 20 })
    requests.value = res.data.list || []
  } finally { requestLoading.value = false }
}

function onTabChange(key: string | number) {
  if (key === 'supply') loadSupplies()
  else loadRequests()
}

function openApply(item: Supply) {
  applyForm.supplyId = item.id
  applyForm.supplyName = item.supplyName
  applyForm.maxStock = item.stock
  applyForm.quantity = Math.min(applyQuantities[item.id] || 1, item.stock)
  applyForm.reason = ''
  applyVisible.value = true
}

async function handleSubmit() {
  if (!applyForm.quantity || applyForm.quantity < 1) {
    MessagePlugin.warning('请填写申领数量')
    return
  }
  submitting.value = true
  try {
    await submitSupplyRequestApi({
      supplyId: applyForm.supplyId,
      quantity: applyForm.quantity,
      reason: applyForm.reason,
      userName: userStore.userInfo?.nickName || '',
    })
    MessagePlugin.success('申领成功，等待审批')
    applyVisible.value = false
    loadSupplies()
  } finally { submitting.value = false }
}

async function handleRevoke(item: SupplyRequest) {
  await revokeSupplyRequestApi(item.id)
  MessagePlugin.success('已撤销')
  loadRequests()
}

// ============ 流程进度 ============
const flowVisible = ref(false)
const flowNodes = ref<FlowLog[]>([])
const currentStep = ref(0)

async function openFlow(bizId: string) {
  try {
    const res = await getFlowLogsApi('supply', bizId)
    flowNodes.value = res.data || []
    currentStep.value = Math.max(0, flowNodes.value.length - 1)
    flowVisible.value = true
  } catch (err) {
    console.error('supply flow error:', err)
  }
}

onMounted(loadSupplies)
</script>
