<template>
  <div class="mx-auto max-w-6xl px-6 py-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('meetingBooking.title') }}</h1>
        <p class="mt-1 text-sm text-[var(--muted-foreground)]">{{ $t('meetingBooking.subtitle') }}</p>
      </div>
      <t-button theme="primary" @click="openBook">
        <template #icon><add-icon /></template>
        {{ $t('meetingBooking.book') }}
      </t-button>
    </div>

    <!-- 我的会议预订列表 -->
    <t-loading :loading="bookingLoading">
      <div v-if="bookings.length" class="space-y-3">
        <div v-for="item in bookings" :key="item.id" class="flex items-start gap-4 rounded-wecom-md border border-[var(--wecom-border)] bg-[var(--wecom-bg-card)] p-5">
          <div class="min-w-0 flex-1">
            <div class="mb-1 flex items-center gap-2">
              <t-tag theme="primary">{{ item.roomName || '-' }}</t-tag>
              <span class="font-medium text-[var(--foreground)]">{{ item.title }}</span>
            </div>
            <p class="text-sm text-[var(--muted-foreground)]">{{ item.startTime }} ~ {{ item.endTime }}</p>
            <p v-if="item.attendees" class="mt-1 text-sm text-[var(--foreground)]">
              {{ $t('meetingBooking.attendees') }}：{{ item.attendees }}
            </p>
            <p v-if="item.remark" class="mt-1 text-sm text-[var(--muted-foreground)]">
              {{ $t('meetingBooking.purpose') }}：{{ item.remark }}
            </p>
          </div>
          <t-button size="small" theme="danger" @click="handleCancel(item)">
            {{ $t('meetingBooking.cancel') }}
          </t-button>
        </div>
      </div>
      <t-empty v-else :description="$t('meetingBooking.noData')" />
    </t-loading>

    <!-- 预订会议室弹窗 -->
    <t-dialog v-model:visible="bookVisible" :header="$t('meetingBooking.book')" :width="520"
             :confirm-btn="{ content: $t('common.submit'), loading: submitting }"
             @confirm="handleSubmit" @cancel="bookVisible = false" :close-on-overlay-click="false">
      <t-form ref="bookFormRef" :data="bookForm" layout="vertical">
        <t-form-item :label="$t('meetingBooking.room')" required name="roomId" :rules="[{ required: true, message: $t('meetingBooking.roomRequired') }]">
          <t-select v-model="bookForm.roomId" :placeholder="$t('meetingBooking.roomPlaceholder')" :loading="roomLoading">
            <t-option v-for="room in roomOptions" :key="room.id" :value="room.id" :label="room.roomName">
              <span>{{ room.roomName }}</span>
              <span class="ml-2 text-xs text-[var(--muted-foreground)]">
                {{ $t('meetingBooking.capacity') }} {{ room.capacity ?? '-' }}<template v-if="room.location"> · {{ room.location }}</template>
              </span>
            </t-option>
          </t-select>
        </t-form-item>
        <t-form-item :label="$t('meetingBooking.timeRange')" required name="startTime" :rules="[{ required: true, message: $t('meetingBooking.timeRequired') }]">
          <div class="flex w-full items-center">
            <t-date-picker v-model="bookForm.startTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('meetingBooking.startTime')" @change="refreshRoomOptions" />
            <span class="mx-2">~</span>
            <t-date-picker v-model="bookForm.endTime" show-time value-type="YYYY-MM-DD HH:mm" style="width: 100%" :placeholder="$t('meetingBooking.endTime')" @change="refreshRoomOptions" />
          </div>
        </t-form-item>
        <t-form-item :label="$t('meetingBooking.subject')" required name="title" :rules="[{ required: true, message: $t('meetingBooking.subjectRequired') }]">
          <t-input v-model="bookForm.title" :placeholder="$t('common.inputPlaceholder')" :max-length="100" />
        </t-form-item>
        <t-form-item :label="$t('meetingBooking.attendees')" name="attendees">
          <t-input v-model="bookForm.attendees" :placeholder="$t('meetingBooking.attendeesPlaceholder')" :max-length="200" />
        </t-form-item>
        <t-form-item :label="$t('meetingBooking.purpose')" name="remark">
          <t-textarea v-model="bookForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="200" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { AddIcon } from 'tdesign-icons-vue-next'
import { useI18n } from 'vue-i18n'
import {
  getMyMeetingsApi, getMeetingRoomsApi, getAvailableMeetingRoomsApi, submitMeetingApi, cancelMeetingApi,
  type MeetingBooking, type MeetingRoom,
} from '@/api/enterprise'

const { t } = useI18n()
const bookingLoading = ref(false)
const submitting = ref(false)
const bookings = ref<MeetingBooking[]>([])
const bookVisible = ref(false)
const roomLoading = ref(false)
const roomOptions = ref<MeetingRoom[]>([])

const bookFormRef = ref<FormInstanceFunctions>()

const bookForm = reactive<{ roomId: string; startTime: string; endTime: string; title: string; attendees: string; remark: string }>({
  roomId: '', startTime: '', endTime: '', title: '', attendees: '', remark: '',
})

async function loadBookings() {
  bookingLoading.value = true
  try {
    const res = await getMyMeetingsApi({ page: 1, pageSize: 20 })
    bookings.value = res.data.list || []
  } finally {
    bookingLoading.value = false
  }
}

function openBook() {
  bookForm.roomId = ''
  bookForm.startTime = ''
  bookForm.endTime = ''
  bookForm.title = ''
  bookForm.attendees = ''
  bookForm.remark = ''
  bookVisible.value = true
  refreshRoomOptions()
}

/** 时间选定后切换为该时段的可用会议室；时间不全时回退启用全量列表 */
async function refreshRoomOptions() {
  if (!bookVisible.value) return
  const hasRange = bookForm.startTime && bookForm.endTime && bookForm.startTime < bookForm.endTime
  roomLoading.value = true
  try {
    const res = hasRange
      ? await getAvailableMeetingRoomsApi({ start: bookForm.startTime, end: bookForm.endTime })
      : await getMeetingRoomsApi()
    roomOptions.value = res.data || []
    // 已选会议室在该时段不可用时清空选择
    if (bookForm.roomId && !roomOptions.value.some((room) => room.id === bookForm.roomId)) {
      bookForm.roomId = ''
    }
  } finally {
    roomLoading.value = false
  }
}

async function handleSubmit() {
  // 校验不通过则中止提交，字段错误就地显示
  if (await bookFormRef.value?.validate() !== true) return
  if (!bookForm.roomId) {
    MessagePlugin.warning(t('meetingBooking.roomRequired'))
    return
  }
  if (!bookForm.startTime || !bookForm.endTime) {
    MessagePlugin.warning(t('meetingBooking.timeRequired'))
    return
  }
  if (bookForm.startTime >= bookForm.endTime) {
    MessagePlugin.warning(t('meetingBooking.timeReversed'))
    return
  }
  if (!bookForm.title) {
    MessagePlugin.warning(t('meetingBooking.subjectRequired'))
    return
  }
  submitting.value = true
  try {
    // 时段冲突时后端返回错误，统一拦截器 Toast 展示报错信息
    await submitMeetingApi({
      roomId: bookForm.roomId,
      title: bookForm.title,
      startTime: bookForm.startTime,
      endTime: bookForm.endTime,
      attendees: bookForm.attendees,
      remark: bookForm.remark,
    })
    MessagePlugin.success(t('meetingBooking.submitSuccess'))
    bookVisible.value = false
    loadBookings()
  } catch {
    // 后端报错（如时段冲突）已由统一拦截器提示，此处仅吞掉 rejection
  } finally {
    submitting.value = false
  }
}

async function handleCancel(item: MeetingBooking) {
  try {
    await cancelMeetingApi(item.id)
    MessagePlugin.success(t('meetingBooking.cancelled'))
    loadBookings()
  } catch {
    // 后端报错已由统一拦截器提示
  }
}

onMounted(loadBookings)
</script>
