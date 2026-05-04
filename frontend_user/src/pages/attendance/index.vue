<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-[var(--foreground)]">{{ $t('attendance.title') }}</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('attendance.subtitle') }}</p>
      </div>
    </div>

    <!-- Tab 切换 -->
    <a-tabs v-model:active-key="activeTab" class="mb-6" @change="onTabChange">
      <a-tab-pane key="overtime" :title="$t('attendance.overtime')" />
      <a-tab-pane key="travel" :title="$t('attendance.travel')" />
    </a-tabs>

    <div v-if="activeTab === 'overtime'">
      <!-- 加班申请表单 -->
      <a-card class="mb-6" :title="$t('attendance.applyOvertime')">
        <a-form :model="overtimeForm" layout="vertical">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item :label="$t('attendance.timeRange')" required>
                <a-date-picker v-model="overtimeForm.startTime" show-time style="width: 100%; max-width: " :placeholder="$t('attendance.startTime')" />
                <span class="mx-2">~</span>
                <a-date-picker v-model="overtimeForm.endTime" show-time style="width: 100%; max-width: " :placeholder="$t('attendance.endTime')" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :label="$t('attendance.durationHours')">
                <a-input-number v-model="overtimeForm.durationHours" :min="0.5" :step="0.5" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :label="$t('attendance.overtimeDate')">
                <a-date-picker v-model="overtimeForm.overtimeDate" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item :label="$t('attendance.reason')" required>
            <a-textarea v-model="overtimeForm.reason" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
          </a-form-item>
          <a-button type="primary" :loading="submitting" @click="handleSubmitOvertime">
            {{ $t('attendance.submit') }}
          </a-button>
        </a-form>
      </a-card>

      <!-- 我的加班申请列表 -->
      <a-card :title="$t('attendance.myOvertimes')">
        <a-spin :loading="overtimeLoading">
          <div v-if="overtimes.length" class="space-y-3">
            <div v-for="item in overtimes" :key="item.id" class="flex items-start gap-4 rounded-lg border border-[var(--border)] bg-[var(--card)] p-5">
              <div class="min-w-0 flex-1">
                <div class="mb-1 flex items-center gap-2">
                  <a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
                  <span class="text-sm text-[var(--muted-foreground)]">{{ item.startTime }} ~ {{ item.endTime }}</span>
                  <span v-if="item.durationHours" class="text-sm text-[var(--muted-foreground)]">{{ item.durationHours }}h</span>
                </div>
                <p class="text-sm text-[var(--foreground)]">{{ item.reason || '-' }}</p>
                <p v-if="item.approveRemark" class="mt-1 text-xs text-[var(--muted-foreground)]">
                  {{ $t('attendance.approveRemark') }}：{{ item.approveRemark }}
                </p>
              </div>
              <a-button v-if="item.status === '0'" size="small" status="danger" @click="handleRevokeOvertime(item)">
                {{ $t('attendance.revoke') }}
              </a-button>
              <a-button size="small" @click="openFlow('overtime', item.id)">{{ $t('attendance.flow') }}</a-button>
            </div>
          </div>
          <a-empty v-else :description="$t('attendance.noData')" />
        </a-spin>
      </a-card>
    </div>

    <div v-else>
      <!-- 出差申请表单 -->
      <a-card class="mb-6" :title="$t('attendance.applyTravel')">
        <a-form :model="travelForm" layout="vertical">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item :label="$t('attendance.destination')" required>
                <a-input v-model="travelForm.destination" :placeholder="$t('common.inputPlaceholder')" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="$t('attendance.timeRange')" required>
                <a-date-picker v-model="travelForm.startTime" show-time style="width: 100%; max-width: " :placeholder="$t('attendance.startTime')" />
                <span class="mx-2">~</span>
                <a-date-picker v-model="travelForm.endTime" show-time style="width: 100%; max-width: " :placeholder="$t('attendance.endTime')" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :label="$t('attendance.days')">
                <a-input-number v-model="travelForm.days" :min="0.5" :step="0.5" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item :label="$t('attendance.purpose')" required>
            <a-textarea v-model="travelForm.purpose" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
          </a-form-item>
          <a-button type="primary" :loading="submitting" @click="handleSubmitTravel">
            {{ $t('attendance.submit') }}
          </a-button>
        </a-form>
      </a-card>

      <!-- 我的出差申请列表 -->
      <a-card :title="$t('attendance.myTravels')">
        <a-spin :loading="travelLoading">
          <div v-if="travels.length" class="space-y-3">
            <div v-for="item in travels" :key="item.id" class="flex items-start gap-4 rounded-lg border border-[var(--border)] bg-[var(--card)] p-5">
              <div class="min-w-0 flex-1">
                <div class="mb-1 flex items-center gap-2">
                  <a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
                  <span class="font-medium text-[var(--foreground)]">{{ item.destination }}</span>
                  <span class="text-sm text-[var(--muted-foreground)]">{{ item.startTime }} ~ {{ item.endTime }}</span>
                </div>
                <p class="text-sm text-[var(--foreground)]">{{ item.purpose || '-' }}</p>
                <p v-if="item.approveRemark" class="mt-1 text-xs text-[var(--muted-foreground)]">
                  {{ $t('attendance.approveRemark') }}：{{ item.approveRemark }}
                </p>
              </div>
              <a-button v-if="item.status === '0'" size="small" status="danger" @click="handleRevokeTravel(item)">
                {{ $t('attendance.revoke') }}
              </a-button>
              <a-button size="small" @click="openFlow('travel', item.id)">{{ $t('attendance.flow') }}</a-button>
            </div>
          </div>
          <a-empty v-else :description="$t('attendance.noData')" />
        </a-spin>
      </a-card>
    </div>

    <!-- 流程进度弹窗 -->
    <a-modal v-model:visible="flowVisible" :title="$t('attendance.flow')" :width="520" footer-only>
      <a-steps v-if="flowNodes.length" direction="vertical" :current="currentStep">
        <a-step
          v-for="node in flowNodes"
          :key="node.id"
          :title="node.nodeName"
          :description="`${node.operatorName || '-'} · ${node.createTime}${node.remark ? ' · ' + node.remark : ''}`"
        />
      </a-steps>
      <a-empty v-else :description="$t('attendance.flowEmpty')" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  getMyOvertimesApi, submitOvertimeApi, revokeOvertimeApi,
  getMyTravelsApi, submitTravelApi, revokeTravelApi,
  getFlowLogsApi,
  type OvertimeRequest, type TravelRequest, type FlowLog,
} from '@/api/enterprise'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const activeTab = ref('overtime')
const submitting = ref(false)
const overtimeLoading = ref(false)
const travelLoading = ref(false)
const overtimes = ref<OvertimeRequest[]>([])
const travels = ref<TravelRequest[]>([])

const overtimeForm = reactive<Partial<OvertimeRequest>>({
  userName: userStore.userInfo?.nickName || '',
  durationHours: 1,
})
const travelForm = reactive<Partial<TravelRequest>>({
  userName: userStore.userInfo?.nickName || '',
  days: 1,
})

function statusText(status: string | undefined): string {
  const map: Record<string, string> = { 0: '待审批', 1: '已通过', 2: '已驳回', 3: '已撤销' }
  return status != null ? map[status] || '-' : '-'
}
function statusColor(status: string | undefined): string {
  if (status === '0') return 'orange'
  if (status === '1') return 'green'
  if (status === '2') return 'red'
  return 'default'
}

async function loadOvertimes() {
  overtimeLoading.value = true
  try {
    const res = await getMyOvertimesApi({ page: 1, pageSize: 20 })
    overtimes.value = res.data.list || []
  } finally {
    overtimeLoading.value = false
  }
}

async function loadTravels() {
  travelLoading.value = true
  try {
    const res = await getMyTravelsApi({ page: 1, pageSize: 20 })
    travels.value = res.data.list || []
  } finally {
    travelLoading.value = false
  }
}

function onTabChange(key: string | number) {
  if (key === 'overtime') loadOvertimes()
  else loadTravels()
}

async function handleSubmitOvertime() {
  if (!overtimeForm.startTime || !overtimeForm.endTime) {
    Message.warning('请选择加班时间')
    return
  }
  if (!overtimeForm.reason) {
    Message.warning('请填写加班事由')
    return
  }
  submitting.value = true
  try {
    await submitOvertimeApi({
      userName: overtimeForm.userName,
      overtimeDate: overtimeForm.overtimeDate,
      startTime: overtimeForm.startTime,
      endTime: overtimeForm.endTime,
      durationHours: Number(overtimeForm.durationHours) || 1,
      reason: overtimeForm.reason,
    })
    Message.success('提交成功，等待审批')
    overtimeForm.startTime = ''
    overtimeForm.endTime = ''
    overtimeForm.overtimeDate = ''
    overtimeForm.reason = ''
    loadOvertimes()
  } finally {
    submitting.value = false
  }
}

async function handleSubmitTravel() {
  if (!travelForm.destination) {
    Message.warning('请填写目的地')
    return
  }
  if (!travelForm.startTime || !travelForm.endTime) {
    Message.warning('请选择出差时间')
    return
  }
  if (!travelForm.purpose) {
    Message.warning('请填写出差事由')
    return
  }
  submitting.value = true
  try {
    await submitTravelApi({
      userName: travelForm.userName,
      destination: travelForm.destination,
      startTime: travelForm.startTime,
      endTime: travelForm.endTime,
      days: Number(travelForm.days) || 1,
      purpose: travelForm.purpose,
    })
    Message.success('提交成功，等待审批')
    travelForm.destination = ''
    travelForm.startTime = ''
    travelForm.endTime = ''
    travelForm.purpose = ''
    loadTravels()
  } finally {
    submitting.value = false
  }
}

async function handleRevokeOvertime(item: OvertimeRequest) {
  await revokeOvertimeApi(item.id)
  Message.success('已撤销')
  loadOvertimes()
}

async function handleRevokeTravel(item: TravelRequest) {
  await revokeTravelApi(item.id)
  Message.success('已撤销')
  loadTravels()
}

// ============ 流程进度 ============
const flowVisible = ref(false)
const flowNodes = ref<FlowLog[]>([])
const currentStep = ref(0)

async function openFlow(bizType: string, bizId: string) {
  try {
    const res = await getFlowLogsApi(bizType, bizId)
    flowNodes.value = res.data || []
    currentStep.value = Math.max(0, flowNodes.value.length - 1)
    flowVisible.value = true
  } catch (err) {
    console.error('attendance flow error:', err)
  }
}

onMounted(loadOvertimes)
</script>
