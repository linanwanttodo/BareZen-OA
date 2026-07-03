<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="bz-list-panel">
      <t-form :data="searchForm" :label-width="80" :on-submit="handleSearch">
        <t-row :gutter="[24, 24]">
        <t-col :span="3">
          <t-form-item name="logType" :label="$t('worklog.logType')">
            <t-select v-model="searchForm.logType" :placeholder="$t('common.all')" allow-clear>
              <t-option value="daily">{{ $t('worklog.typeDaily') }}</t-option>
              <t-option value="weekly">{{ $t('worklog.typeWeekly') }}</t-option>
            </t-select>
          </t-form-item>
        </t-col>
        <t-col :span="3">
          <t-form-item name="userName" :label="$t('worklog.userName')">
            <t-input v-model="searchForm.userName" :placeholder="$t('common.inputPlaceholder')" allow-clear />
          </t-form-item>
        </t-col>
        <t-col :span="3" class="bz-list-ops">
          <t-form-item>
            <t-space>
              <t-button theme="primary" type="submit">{{ $t('common.search') }}</t-button>
              <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
            </t-space>
          </t-form-item>
        </t-col>
        </t-row>
      </t-form>

      <div class="bz-list-toolbar bz-list-toolbar--first">
        <t-space>
          <t-button v-permission="PERM.hr.worklogAdd" theme="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </t-button>
        </t-space>
      </div>

      
          <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #logType="{ row }">
                <t-tag :theme="row.logType === 'weekly' ? 'primary' : 'default'" variant="light">
                  {{ row.logType === 'weekly' ? $t('worklog.typeWeekly') : $t('worklog.typeDaily') }}
                </t-tag>
              </template>
    <template #reviewerName="{ row }">{{ row.reviewerName || '-' }}</template>
    <template #operate="{ row }">
                <t-space>
                  <t-link theme="primary" v-permission="PERM.hr.worklogList" @click="handleView(row)">{{ $t('worklog.view') }}</t-link>
                  <t-link theme="primary" v-if="!row.reviewerName" v-permission="PERM.hr.worklogReview" @click="openReview(row)">
                    {{ $t('worklog.review') }}
                  </t-link>
                  <t-link theme="primary" v-permission="PERM.hr.worklogEdit" @click="handleEdit(row)">{{ $t('common.edit') }}</t-link>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
                    <t-button v-permission="PERM.hr.worklogDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
  </t-table>

        <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
          <t-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      
    </t-card>

    <!-- 新增/编辑弹窗 -->
    <t-dialog v-model:visible="editVisible" :title="isEdit ? $t('worklog.editLog') : $t('worklog.addLog')"
             :width="640" @confirm="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <t-form ref="formRef" :data="form" layout="vertical">
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item name="logType" :label="$t('worklog.logType')" :rules="[{ required: true, message: t('validation.required', { field: t('worklog.logType') }) }]">
              <t-select v-model="form.logType">
                <t-option value="daily">{{ $t('worklog.typeDaily') }}</t-option>
                <t-option value="weekly">{{ $t('worklog.typeWeekly') }}</t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item name="logDate" :label="$t('worklog.logDate')">
              
<t-date-picker v-model="form.logDate" style="width: 100%"  value-type="YYYY-MM-DD" />
            </t-form-item>
          </t-col>
        </t-row>
        <t-form-item name="content" :label="$t('worklog.content')" :rules="[{ required: true, message: t('validation.required', { field: t('worklog.content') }) }]">
          <t-textarea v-model="form.content" :placeholder="$t('common.inputPlaceholder')" :auto-size="{ minRows: 6, maxRows: 14 }" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 详情弹窗 -->
    <t-dialog v-model:visible="viewVisible" :title="$t('worklog.view')" :width="640" footer-only>
      <div v-if="currentLog">
        <div class="mb-3 flex items-center gap-2">
          <t-tag :theme="currentLog.logType === 'weekly' ? 'primary' : 'default'">
            {{ currentLog.logType === 'weekly' ? $t('worklog.typeWeekly') : $t('worklog.typeDaily') }}
          </t-tag>
          <span class="text-sm text-[var(--muted-foreground)]">{{ currentLog.userName }} · {{ currentLog.logDate }}</span>
        </div>
        <div class="whitespace-pre-wrap rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-container)] p-4 text-sm leading-relaxed">
          {{ currentLog.content }}
        </div>
        <div v-if="currentLog.reviewerName" class="mt-4 rounded-wecom-md bg-[var(--wecom-bg-container)] p-3 text-sm">
          <div class="mb-1 font-medium">{{ $t('worklog.reviewRemark') }}：{{ currentLog.reviewerName }}（{{ currentLog.reviewTime }}）</div>
          <div class="text-[var(--muted-foreground)]">{{ currentLog.reviewRemark || '-' }}</div>
        </div>
      </div>
    </t-dialog>

    <!-- 批阅弹窗 -->
    <t-dialog v-model:visible="reviewVisible" :title="$t('worklog.review')" :width="480"
             @confirm="handleReview" @cancel="reviewVisible = false" :ok-loading="reviewing">
      <t-form ref="reviewFormRef" :data="reviewForm" layout="vertical">
        <t-form-item name="remark" :label="$t('worklog.reviewRemark')">
          <t-textarea v-model="reviewForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, computed, reactive, onMounted } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { getWorkLogPageApi, addWorkLogApi, updateWorkLogApi, deleteWorkLogApi, reviewWorkLogApi, type HrWorkLog } from '@/api/worklog'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const reviewing = ref(false)
const tableData = ref<HrWorkLog[]>([])
const editVisible = ref(false)
const viewVisible = ref(false)
const reviewVisible = ref(false)
const isEdit = ref(false)
const currentLog = ref<HrWorkLog | null>(null)

const searchForm = reactive({ logType: '', userName: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const formRef = ref<FormInstanceFunctions>()
const form = reactive<Partial<HrWorkLog>>({})
const reviewFormRef = ref<FormInstanceFunctions>()
const reviewForm = reactive<{ id: string; remark: string }>({ id: '', remark: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await getWorkLogPageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      logType: searchForm.logType || undefined,
      userName: searchForm.userName || undefined,
    })
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.logType = ''; searchForm.userName = ''; pagination.current = 1; loadData() }

function handleAdd() {
  isEdit.value = false
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k])
  form.logType = 'daily'
  form.userName = userStore.userInfo?.nickName || ''
  editVisible.value = true
}

function handleEdit(record: HrWorkLog) {
  isEdit.value = true
  Object.assign(form, record)
  editVisible.value = true
}

async function handleSave() {
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await formRef.value?.validate()) !== true) return
  if (!form.content) {
    MessagePlugin.warning(t('common.msg.contentRequired'))
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateWorkLogApi(form)
    } else {
      await addWorkLogApi(form)
    }
    MessagePlugin.success(t('common.saveSuccess'))
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: HrWorkLog) {
  await deleteWorkLogApi(record.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadData()
}

function handleView(record: HrWorkLog) {
  currentLog.value = record
  viewVisible.value = true
}

function openReview(record: HrWorkLog) {
  reviewForm.id = record.id
  reviewForm.remark = ''
  reviewVisible.value = true
}

async function handleReview() {
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await reviewFormRef.value?.validate()) !== true) return
  reviewing.value = true
  try {
    await reviewWorkLogApi(reviewForm.id, reviewForm.remark)
    MessagePlugin.success(t('common.msg.reviewDone'))
    reviewVisible.value = false
    loadData()
  } finally {
    reviewing.value = false
  }
}

onMounted(loadData)

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'userName', title: t('worklog.userName'), width: 100 },
    { colKey: 'logType', title: t('worklog.logType'), width: 90 },
    { colKey: 'logDate', title: t('worklog.logDate'), width: 110 },
    { colKey: 'content', title: t('worklog.content'), ellipsis: true },
    { colKey: 'reviewerName', title: t('worklog.reviewerName'), width: 100 },
    { colKey: 'operate', title: t('common.operate'), width: 200, align: 'right' },
  ])

</script>
