<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-[var(--foreground)]">{{ $t('supplyApply.title') }}</h1>
      <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('supplyApply.subtitle') }}</p>
    </div>

    <!-- Tab：可申领 / 我的申领 -->
    <a-tabs v-model:active-key="activeTab" class="mb-6" @change="onTabChange">
      <a-tab-pane key="supply" :title="$t('supplyApply.available')" />
      <a-tab-pane key="request" :title="$t('supplyApply.myRequests')" />
    </a-tabs>

    <!-- 可申领用品 -->
    <div v-if="activeTab === 'supply'">
      <div class="mb-4 flex justify-end">
        <a-input v-model="keyword" :placeholder="$t('supplyApply.searchPlaceholder')" allow-clear style="width: 100%; max-width: " @press-enter="loadSupplies" @clear="loadSupplies">
          <template #prefix><icon-search /></template>
        </a-input>
      </div>
      <a-spin :loading="supplyLoading">
        <div v-if="supplies.length" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div v-for="item in supplies" :key="item.id" class="rounded-lg border border-[var(--border)] bg-[var(--card)] p-5">
            <div class="flex items-start justify-between">
              <div>
                <h3 class="font-semibold text-[var(--foreground)]">{{ item.supplyName }}</h3>
                <p class="mt-1 text-xs text-[var(--muted-foreground)]">{{ item.spec || '-' }} · {{ item.unit }}</p>
              </div>
              <a-tag color="blue">{{ item.stock }} {{ item.unit }}</a-tag>
            </div>
            <div class="mt-4 flex items-center gap-2">
              <a-input-number v-model="applyQuantities[item.id]" :min="1" :max="item.stock" style="width: 120px" />
              <a-button type="primary" :disabled="!item.stock" @click="openApply(item)">
                {{ $t('supplyApply.apply') }}
              </a-button>
            </div>
          </div>
        </div>
        <a-empty v-else :description="$t('supplyApply.noSupply')" />
      </a-spin>
    </div>

    <!-- 我的申领 -->
    <div v-else>
      <a-spin :loading="requestLoading">
        <div v-if="requests.length" class="space-y-3">
          <div v-for="item in requests" :key="item.id" class="flex items-start gap-4 rounded-lg border border-[var(--border)] bg-[var(--card)] p-5">
            <div class="min-w-0 flex-1">
              <div class="mb-1 flex items-center gap-2">
                <a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
                <span class="font-medium text-[var(--foreground)]">{{ item.supplyName }} × {{ item.quantity }}</span>
              </div>
              <p v-if="item.reason" class="text-sm text-[var(--muted-foreground)]">{{ item.reason }}</p>
              <p v-if="item.approveRemark" class="mt-1 text-xs text-[var(--muted-foreground)]">
                {{ $t('supplyApply.approveRemark') }}：{{ item.approveRemark }}
              </p>
            </div>
            <a-button v-if="item.status === '0'" size="small" status="danger" @click="handleRevoke(item)">
              {{ $t('supplyApply.revoke') }}
            </a-button>
            <a-button size="small" @click="openFlow(item.id)">{{ $t('supplyApply.flow') }}</a-button>
          </div>
        </div>
        <a-empty v-else :description="$t('supplyApply.noRequest')" />
      </a-spin>
    </div>

    <!-- 流程进度弹窗 -->
    <a-modal v-model:visible="flowVisible" :title="$t('supplyApply.flow')" :width="520" footer-only>
      <a-steps v-if="flowNodes.length" direction="vertical" :current="currentStep">
        <a-step
          v-for="node in flowNodes"
          :key="node.id"
          :title="node.nodeName"
          :description="`${node.operatorName || '-'} · ${node.createTime}${node.remark ? ' · ' + node.remark : ''}`"
        />
      </a-steps>
      <a-empty v-else :description="$t('supplyApply.flowEmpty')" />
    </a-modal>

    <!-- 申领弹窗 -->
    <a-modal v-model:visible="applyVisible" :title="$t('supplyApply.apply')" :width="480"
             @ok="handleSubmit" @cancel="applyVisible = false" :ok-loading="submitting">
      <a-form :model="applyForm" layout="vertical">
        <a-form-item :label="$t('supplyApply.supplyName')">
          <a-input :model-value="applyForm.supplyName" disabled />
        </a-form-item>
        <a-form-item :label="$t('supplyApply.quantity')" required>
          <a-input-number v-model="applyForm.quantity" :min="1" :max="applyForm.maxStock" style="width: 100%" />
        </a-form-item>
        <a-form-item :label="$t('supplyApply.reason')">
          <a-textarea v-model="applyForm.reason" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
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
    Message.warning('请填写申领数量')
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
    Message.success('申领成功，等待审批')
    applyVisible.value = false
    loadSupplies()
  } finally { submitting.value = false }
}

async function handleRevoke(item: SupplyRequest) {
  await revokeSupplyRequestApi(item.id)
  Message.success('已撤销')
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
