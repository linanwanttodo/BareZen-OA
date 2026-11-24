<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('salary.title') }}</h1>
      <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('salary.subtitle') }}</p>
    </div>

    <t-loading :loading="payrollLoading">
      <div v-if="payrolls.length" class="space-y-3">
        <div
          v-for="item in payrolls"
          :key="item.id"
          class="flex cursor-pointer items-center justify-between gap-4 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5"
          @click="openDetail(item)"
        >
          <div class="min-w-0">
            <div class="mb-1 flex items-center gap-2">
              <span class="font-medium text-[var(--foreground)]">{{ item.paymentMonth }}</span>
              <t-tag :theme="item.paymentStatus === '1' ? 'success' : 'warning'">
                {{ statusText(item.paymentStatus) }}
              </t-tag>
            </div>
            <p class="text-xs text-[var(--muted-foreground)]">
              {{ item.userName }} · {{ $t('salary.paymentTime') }}：{{ item.paymentTime || '-' }}
            </p>
          </div>
          <div class="shrink-0 text-right">
            <p class="text-xs text-[var(--muted-foreground)]">{{ $t('salary.actualSalary') }}</p>
            <p class="text-lg font-semibold text-[var(--wecom-primary)]">{{ money(item.actualSalary) }}</p>
            <p class="mt-1 text-xs text-[var(--muted-foreground)]">{{ $t('salary.viewDetail') }}</p>
          </div>
        </div>
      </div>
      <t-empty v-else :description="$t('salary.noData')" />
    </t-loading>

    <t-dialog v-model:visible="detailVisible" :header="detailHeader" :width="600" :footer="false">
      <div v-if="detail" class="space-y-5">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-[var(--muted-foreground)]">{{ $t('salary.paymentMonth') }}</p>
            <p class="font-medium text-[var(--foreground)]">{{ detail.paymentMonth }}</p>
          </div>
          <t-tag :theme="detail.paymentStatus === '1' ? 'success' : 'warning'">
            {{ statusText(detail.paymentStatus) }}
          </t-tag>
        </div>

        <div>
          <p class="mb-2 text-sm font-medium text-[var(--foreground)]">{{ $t('salary.incomeSection') }}</p>
          <div class="divide-y divide-[var(--wecom-border)] rounded-wecom-md border border-[var(--wecom-border)]">
            <div v-for="row in incomeRows" :key="row.label" class="flex items-center justify-between px-4 py-2 text-sm">
              <span class="text-[var(--muted-foreground)]">{{ row.label }}</span>
              <span class="font-medium text-[var(--foreground)]">{{ money(row.value) }}</span>
            </div>
            <div class="flex items-center justify-between bg-[var(--wecom-bg-container)] px-4 py-2 text-sm">
              <span class="font-medium text-[var(--foreground)]">{{ $t('salary.totalIncome') }}</span>
              <span class="font-semibold text-[var(--foreground)]">{{ money(detail.totalIncome) }}</span>
            </div>
          </div>
        </div>

        <div>
          <p class="mb-2 text-sm font-medium text-[var(--foreground)]">{{ $t('salary.deductionSection') }}</p>
          <div class="divide-y divide-[var(--wecom-border)] rounded-wecom-md border border-[var(--wecom-border)]">
            <div v-for="row in deductionRows" :key="row.label" class="flex items-center justify-between px-4 py-2 text-sm">
              <span class="text-[var(--muted-foreground)]">{{ row.label }}</span>
              <span class="font-medium text-[var(--foreground)]">{{ money(row.value) }}</span>
            </div>
            <div class="flex items-center justify-between bg-[var(--wecom-bg-container)] px-4 py-2 text-sm">
              <span class="font-medium text-[var(--foreground)]">{{ $t('salary.totalDeduction') }}</span>
              <span class="font-semibold text-[var(--foreground)]">{{ money(detail.totalDeduction) }}</span>
            </div>
          </div>
        </div>

        <div class="flex items-center justify-between rounded-wecom-md border border-[var(--wecom-primary)] bg-[var(--wecom-primary-light)] px-4 py-3">
          <span class="font-medium text-[var(--foreground)]">{{ $t('salary.actualSalary') }}</span>
          <span class="text-xl font-semibold text-[var(--wecom-primary)]">{{ money(detail.actualSalary) }}</span>
        </div>

        <div class="space-y-1 text-xs text-[var(--muted-foreground)]">
          <p>{{ $t('salary.paymentTime') }}：{{ detail.paymentTime || '-' }}</p>
          <p v-if="detail.remark">{{ $t('salary.remark') }}：{{ detail.remark }}</p>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import { getMyPayrollsApi, getPayrollDetailApi, type SalaryPaymentRecord } from '@/api/salary'

const { t } = useI18n()
const payrollLoading = ref(false)
const payrolls = ref<SalaryPaymentRecord[]>([])
const detailVisible = ref(false)
const detail = ref<SalaryPaymentRecord | null>(null)

function money(value: number | null | undefined): string {
  if (value == null) return '-'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function statusText(status: string | undefined): string {
  return status === '1' ? t('salary.statusPaid') : t('salary.statusUnpaid')
}

const detailHeader = computed(() => detail.value ? `${t('salary.detail')} · ${detail.value.paymentMonth}` : t('salary.detail'))

const incomeRows = computed(() => {
  const item = detail.value
  if (!item) return []
  return [
    { label: t('salary.baseSalary'), value: item.baseSalary },
    { label: t('salary.performanceSalary'), value: item.performanceSalary },
    { label: t('salary.positionAllowance'), value: item.positionAllowance },
    { label: t('salary.transportAllowance'), value: item.transportAllowance },
    { label: t('salary.mealAllowance'), value: item.mealAllowance },
    { label: t('salary.otherAllowance'), value: item.otherAllowance },
  ]
})

const deductionRows = computed(() => {
  const item = detail.value
  if (!item) return []
  return [
    { label: t('salary.socialInsurancePersonal'), value: item.socialInsurancePersonal },
    { label: t('salary.housingFundPersonal'), value: item.housingFundPersonal },
    { label: t('salary.incomeTax'), value: item.incomeTax },
  ]
})

async function loadPayrolls() {
  payrollLoading.value = true
  try {
    const res = await getMyPayrollsApi({ page: 1, pageSize: 20 })
    payrolls.value = res.data.list || []
  } finally {
    payrollLoading.value = false
  }
}

async function openDetail(item: SalaryPaymentRecord) {
  try {
    const res = await getPayrollDetailApi(item.id)
    detail.value = res.data
    detailVisible.value = true
  } catch (err) {
    MessagePlugin.error((err as Error)?.message || t('salary.loadFailed'))
  }
}

onMounted(loadPayrolls)
</script>
