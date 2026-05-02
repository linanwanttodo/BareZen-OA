<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="logType" :label="$t('worklog.logType')">
          <a-select v-model="searchForm.logType" :placeholder="$t('common.all')" allow-clear style="width: 140px">
            <a-option value="daily">{{ $t('worklog.typeDaily') }}</a-option>
            <a-option value="weekly">{{ $t('worklog.typeWeekly') }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="userName" :label="$t('worklog.userName')">
          <a-input v-model="searchForm.userName" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 160px" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">{{ $t('common.search') }}</a-button>
            <a-button @click="handleReset">{{ $t('common.reset') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 表格 -->
    <a-card>
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('worklog.title') }}</span>
          <a-button v-permission="'hr:worklog:add'" type="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            {{ $t('common.add') }}
          </a-button>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('worklog.userName')" data-index="userName" :width="100" />
            <a-table-column :title="$t('worklog.logType')" :width="90">
              <template #cell="{ record }">
                <a-tag :color="record.logType === 'weekly' ? 'purple' : 'blue'">
                  {{ record.logType === 'weekly' ? $t('worklog.typeWeekly') : $t('worklog.typeDaily') }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="$t('worklog.logDate')" data-index="logDate" :width="110" />
            <a-table-column :title="$t('worklog.content')" data-index="content" ellipsis />
            <a-table-column :title="$t('worklog.reviewerName')" :width="100">
              <template #cell="{ record }">{{ record.reviewerName || '-' }}</template>
            </a-table-column>
            <a-table-column :title="$t('common.operate')" :width="200" align="right">
              <template #cell="{ record }">
                <a-space>
                  <a-button v-permission="'hr:worklog:list'" size="small" @click="handleView(record)">{{ $t('worklog.view') }}</a-button>
                  <a-button v-if="!record.reviewerName" v-permission="'hr:worklog:review'" size="small" type="primary" @click="openReview(record)">
                    {{ $t('worklog.review') }}
                  </a-button>
                  <a-button v-permission="'hr:worklog:edit'" size="small" @click="handleEdit(record)">{{ $t('common.edit') }}</a-button>
                  <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDelete(record)">
                    <a-button v-permission="'hr:worklog:delete'" size="small" status="danger">{{ $t('common.delete') }}</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>

        <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
          <a-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadData"
          />
        </div>
      </a-spin>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:visible="editVisible" :title="isEdit ? $t('worklog.editLog') : $t('worklog.addLog')"
             :width="640" @ok="handleSave" @cancel="editVisible = false" :ok-loading="saving">
      <a-form :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="logType" :label="$t('worklog.logType')" required>
              <a-select v-model="form.logType">
                <a-option value="daily">{{ $t('worklog.typeDaily') }}</a-option>
                <a-option value="weekly">{{ $t('worklog.typeWeekly') }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="logDate" :label="$t('worklog.logDate')">
              <a-date-picker v-model="form.logDate" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item field="content" :label="$t('worklog.content')" required>
          <a-textarea v-model="form.content" :placeholder="$t('common.inputPlaceholder')" :auto-size="{ minRows: 6, maxRows: 14 }" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-modal v-model:visible="viewVisible" :title="$t('worklog.view')" :width="640" footer-only>
      <div v-if="currentLog">
        <div class="mb-3 flex items-center gap-2">
          <a-tag :color="currentLog.logType === 'weekly' ? 'purple' : 'blue'">
            {{ currentLog.logType === 'weekly' ? $t('worklog.typeWeekly') : $t('worklog.typeDaily') }}
          </a-tag>
          <span class="text-sm text-[var(--muted-foreground)]">{{ currentLog.userName }} · {{ currentLog.logDate }}</span>
        </div>
        <div class="whitespace-pre-wrap rounded-lg border border-[var(--border)] bg-[var(--muted)] p-4 text-sm leading-relaxed">
          {{ currentLog.content }}
        </div>
        <div v-if="currentLog.reviewerName" class="mt-4 rounded-lg bg-[var(--muted)] p-3 text-sm">
          <div class="mb-1 font-medium">{{ $t('worklog.reviewRemark') }}：{{ currentLog.reviewerName }}（{{ currentLog.reviewTime }}）</div>
          <div class="text-[var(--muted-foreground)]">{{ currentLog.reviewRemark || '-' }}</div>
        </div>
      </div>
    </a-modal>

    <!-- 批阅弹窗 -->
    <a-modal v-model:visible="reviewVisible" :title="$t('worklog.review')" :width="480"
             @ok="handleReview" @cancel="reviewVisible = false" :ok-loading="reviewing">
      <a-form :model="reviewForm" layout="vertical">
        <a-form-item field="remark" :label="$t('worklog.reviewRemark')">
          <a-textarea v-model="reviewForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getWorkLogPageApi, addWorkLogApi, updateWorkLogApi, deleteWorkLogApi, reviewWorkLogApi, type HrWorkLog } from '@/api/worklog'
import { useUserStore } from '@/stores/user'

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
const form = reactive<Partial<HrWorkLog>>({})
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
  if (!form.content) {
    Message.warning('日志内容必填')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateWorkLogApi(form)
    } else {
      await addWorkLogApi(form)
    }
    Message.success('保存成功')
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: HrWorkLog) {
  await deleteWorkLogApi(record.id)
  Message.success('删除成功')
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
  reviewing.value = true
  try {
    await reviewWorkLogApi(reviewForm.id, reviewForm.remark)
    Message.success('批阅完成')
    reviewVisible.value = false
    loadData()
  } finally {
    reviewing.value = false
  }
}

onMounted(loadData)
</script>
