<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <t-card class="mb-4">
      <t-form :data="searchForm" :label-width="80" :on-submit="handleSearch">
        <t-form-item name="title" :label="$t('office.bookingTitle')">
          <t-input v-model="searchForm.title" :placeholder="$t('common.inputPlaceholder')" allow-clear />
        </t-form-item>
        <t-form-item name="roomId" :label="$t('office.roomName')">
          <t-select v-model="searchForm.roomId" :placeholder="$t('common.all')" allow-clear>
            <t-option v-for="room in roomOptions" :key="room.id" :value="room.id">{{ room.roomName }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" html-type="submit">{{ $t('common.search') }}</t-button>
            <t-button @click="handleReset">{{ $t('common.reset') }}</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-card>

    <!-- 会议室卡片列表 -->
    <t-card class="mb-4">
      <div class="bz-list-toolbar bz-list-toolbar--first">
        <t-space>
          <t-button v-permission="PERM.office.meetingAdd" theme="primary" @click="openRoomAdd">
              <template #icon><icon-plus /></template>
              {{ $t('office.addRoom') }}
            </t-button>
          <t-button v-permission="PERM.office.meetingAdd" variant="outline" @click="bookingVisible = true">
              <template #icon><icon-calendar /></template>
              {{ $t('office.bookMeeting') }}
            </t-button>
        </t-space>
      </div>
      <t-loading :loading="roomLoading">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <t-card v-for="room in rooms" :key="room.id" class="room-card" hoverable>
            <div class="flex items-center justify-between">
              <span class="font-medium">{{ room.roomName }}</span>
              <t-tag :theme="room.status === '0' ? 'success' : 'danger'">
                {{ room.status === '0' ? $t('office.roomAvailable') : $t('office.roomDisabled') }}
              </t-tag>
            </div>
            <div class="mt-3 space-y-1 text-sm text-[var(--muted-foreground)]">
              <div>{{ $t('office.capacity') }}：{{ room.capacity }} 人</div>
              <div>{{ $t('office.location') }}：{{ room.location || '-' }}</div>
              <div>{{ $t('office.equipment') }}：{{ room.equipment || '-' }}</div>
            </div>
            <div class="mt-3 flex justify-end gap-2">
              <t-button v-permission="PERM.office.meetingEdit" size="small" @click="openRoomEdit(room)">{{ $t('common.edit') }}</t-button>
              <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDeleteRoom(room)">
                <t-button v-permission="PERM.office.meetingDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
              </t-popconfirm>
            </div>
          </t-card>
        </div>
      </t-loading>
    </t-card>

    <!-- 预订记录 -->
    <t-card>
      <template #title>
        <span>{{ $t('office.bookingList') }}</span>
      </template>
      
          <t-table :loading="loading" row-key="id" :data="tableData" :columns="tableColumns" :pagination="false" :hover="true" vertical-align="top">
    <template #startTime="{ row }">
                <span class="text-sm">{{ row.startTime }} ~ {{ row.endTime }}</span>
              </template>
    <template #col_5="{ row }">
                <t-space>
                  <t-link theme="primary" v-permission="PERM.office.meetingEdit" @click="openBookingEdit(row)">{{ $t('common.edit') }}</t-link>
                  <t-popconfirm :content="$t('common.deleteConfirm')" @confirm="handleDeleteBooking(row)">
                    <t-button v-permission="PERM.office.meetingDelete" size="small" theme="danger">{{ $t('common.delete') }}</t-button>
                  </t-popconfirm>
                </t-space>
              </template>
  </t-table>
        <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
          <t-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadBookings"
          />
        </div>
      
    </t-card>

    <!-- 新增会议室弹窗 -->
    <t-dialog v-model:visible="roomDialogVisible" :title="isRoomEdit ? $t('office.editRoom') : $t('office.addRoom')" :width="520"
             @confirm="handleSaveRoom" @cancel="cancelRoomEdit" :ok-loading="saving">
      <t-form ref="roomFormRef" :data="roomForm" layout="vertical">
        <t-form-item name="roomName" :label="$t('office.roomName')" required>
          <t-input v-model="roomForm.roomName" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item name="capacity" :label="$t('office.capacity')">
          <t-input-number v-model="roomForm.capacity" :min="0" style="width: 100%" />
        </t-form-item>
        <t-form-item name="location" :label="$t('office.location')">
          <t-input v-model="roomForm.location" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item name="equipment" :label="$t('office.equipment')">
          <t-input v-model="roomForm.equipment" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item name="status" :label="$t('common.status')">
          <t-select v-model="roomForm.status">
            <t-option value="0">{{ $t('office.roomAvailable') }}</t-option>
            <t-option value="1">{{ $t('office.roomDisabled') }}</t-option>
          </t-select>
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 会议预订弹窗（新增/编辑共用） -->
    <t-dialog v-model:visible="bookingVisible" :title="isBookingEdit ? $t('office.editBooking') : $t('office.bookMeeting')" :width="560"
             @confirm="handleSaveBooking" @cancel="cancelBooking" :ok-loading="saving">
      <t-form ref="bookingFormRef" :data="bookingForm" layout="vertical">
        <t-form-item name="roomId" :label="$t('office.roomName')" required>
          <t-select v-model="bookingForm.roomId" :placeholder="$t('common.selectPlaceholder')">
            <t-option v-for="room in roomOptions" :key="room.id" :value="room.id">{{ room.roomName }}</t-option>
          </t-select>
        </t-form-item>
        <t-form-item name="title" :label="$t('office.bookingTitle')" required>
          <t-input v-model="bookingForm.title" :placeholder="$t('common.inputPlaceholder')" />
        </t-form-item>
        <t-form-item name="timeRange" :label="$t('office.timeRange')" required>
          
<t-date-picker v-model="bookingForm.startTime" show-time value-type="YYYY-MM-DD HH:mm" :placeholder="$t('office.startTime')" />
          <span class="mx-2">~</span>
          
<t-date-picker v-model="bookingForm.endTime" show-time value-type="YYYY-MM-DD HH:mm" :placeholder="$t('office.endTime')" />
        </t-form-item>
        <t-form-item name="attendees" :label="$t('office.attendees')">
          <t-input v-model="bookingForm.attendees" :placeholder="$t('office.attendeesPlaceholder')" />
        </t-form-item>
        <t-form-item name="remark" :label="$t('common.remark')">
          <t-textarea v-model="bookingForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { PERM } from '@/utils/constants'
import { ref, reactive, onMounted, onActivated, computed } from 'vue'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import {
  getRoomPageApi, getAvailableRoomsApi, addRoomApi, updateRoomApi, deleteRoomApi,
  getBookingPageApi, addBookingApi, updateBookingApi, deleteBookingApi,
  type OfficeMeetingRoom, type OfficeMeetingBooking,
} from '@/api/office'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const userStore = useUserStore()
const roomLoading = ref(false)
const loading = ref(false)
const saving = ref(false)
const rooms = ref<OfficeMeetingRoom[]>([])
const roomOptions = ref<OfficeMeetingRoom[]>([])
const tableData = ref<OfficeMeetingBooking[]>([])
const roomDialogVisible = ref(false)
const bookingVisible = ref(false)
const isBookingEdit = ref(false)
const isRoomEdit = ref(false)

const searchForm = reactive({ title: '', roomId: '' })

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const roomFormRef = ref<FormInstanceFunctions>()
const roomForm = reactive<Partial<OfficeMeetingRoom>>({})
const bookingFormRef = ref<FormInstanceFunctions>()
const bookingForm = reactive<Partial<OfficeMeetingBooking>>({})

function openRoomAdd() {
  isRoomEdit.value = false
  roomForm.id = undefined
  roomForm.roomName = ''
  roomForm.capacity = undefined
  roomForm.location = ''
  roomForm.equipment = ''
  roomDialogVisible.value = true
}

function cancelRoomEdit() {
  roomDialogVisible.value = false
  if (!isRoomEdit.value) {
    // 新增模式下取消，清空表单
    roomForm.id = undefined
    roomForm.roomName = ''
    roomForm.capacity = undefined
    roomForm.location = ''
    roomForm.equipment = ''
  }
}

async function loadRooms() {
  roomLoading.value = true
  try {
    const res = await getRoomPageApi({ pageNum: 1, pageSize: 100 })
    rooms.value = res.data.list || []
    roomOptions.value = rooms.value.filter((r) => r.status === '0')
  } finally {
    roomLoading.value = false
  }
}

async function loadBookings() {
  loading.value = true
  try {
    const res = await getBookingPageApi({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      title: searchForm.title || undefined,
      roomId: searchForm.roomId || undefined,
    })
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadBookings()
}

function handleReset() {
  searchForm.title = ''
  searchForm.roomId = ''
  pagination.current = 1
  loadBookings()
}

function openRoomEdit(room: OfficeMeetingRoom) {
  isRoomEdit.value = true
  Object.assign(roomForm, room)
  roomDialogVisible.value = true
}

async function handleSaveRoom() {
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await roomFormRef.value?.validate()) !== true) return
  if (!roomForm.roomName) {
    MessagePlugin.warning(t('common.msg.meetingRoomRequired'))
    return
  }
  saving.value = true
  try {
    if (roomForm.id) {
      await updateRoomApi(roomForm)
    } else {
      await addRoomApi(roomForm)
    }
    MessagePlugin.success(t('common.saveSuccess'))
    roomDialogVisible.value = false
    Object.keys(roomForm).forEach((k) => delete (roomForm as Record<string, unknown>)[k])
    loadRooms()
  } finally {
    saving.value = false
  }
}

async function handleDeleteRoom(room: OfficeMeetingRoom) {
  await deleteRoomApi(room.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadRooms()
}

function cancelBooking() {
  bookingVisible.value = false
  isBookingEdit.value = false
  bookingForm.id = ''
  bookingForm.roomId = ''
  bookingForm.title = ''
  bookingForm.startTime = ''
  bookingForm.endTime = ''
  bookingForm.attendees = ''
  bookingForm.remark = ''
}

function openBookingEdit(record: OfficeMeetingBooking) {
  isBookingEdit.value = true
  bookingForm.id = record.id
  bookingForm.roomId = record.roomId
  bookingForm.title = record.title
  bookingForm.startTime = record.startTime
  bookingForm.endTime = record.endTime
  bookingForm.attendees = record.attendees
  bookingForm.remark = record.remark
  bookingVisible.value = true
}

async function handleSaveBooking() {
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await bookingFormRef.value?.validate()) !== true) return
  if (!bookingForm.roomId || !bookingForm.title) {
    MessagePlugin.warning(t('common.msg.meetingInfoRequired'))
    return
  }
  if (!bookingForm.startTime || !bookingForm.endTime) {
    MessagePlugin.warning(t('common.msg.meetingTimeRequired'))
    return
  }
  saving.value = true
  try {
    if (isBookingEdit.value) {
      await updateBookingApi({
        id: bookingForm.id,
        roomId: bookingForm.roomId,
        title: bookingForm.title,
        startTime: bookingForm.startTime,
        endTime: bookingForm.endTime,
        attendees: bookingForm.attendees,
        remark: bookingForm.remark,
      })
    } else {
      await addBookingApi({
        roomId: bookingForm.roomId,
        title: bookingForm.title,
        startTime: bookingForm.startTime,
        endTime: bookingForm.endTime,
        attendees: bookingForm.attendees,
        remark: bookingForm.remark,
        bookerName: userStore.userInfo?.nickName || '',
      })
    }
    MessagePlugin.success(t('common.msg.bookingSuccess'))
    bookingVisible.value = false
    isBookingEdit.value = false
    bookingForm.id = ''
    bookingForm.roomId = ''
    bookingForm.title = ''
    bookingForm.startTime = ''
    bookingForm.endTime = ''
    bookingForm.attendees = ''
    bookingForm.remark = ''
    loadBookings()
  } finally {
    saving.value = false
  }
}

async function handleDeleteBooking(record: OfficeMeetingBooking) {
  await deleteBookingApi(record.id)
  MessagePlugin.success(t('common.deleteSuccess'))
  loadBookings()
}

onMounted(() => {
  loadRooms()
  loadBookings()
})
onActivated(() => { loadRooms(); loadBookings() })

/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */
const tableColumns = computed(() => [
    { colKey: 'roomName', title: t('office.roomName'), width: 120 },
    { colKey: 'title', title: t('office.bookingTitle') },
    { colKey: 'startTime', title: t('office.timeRange') },
    { colKey: 'bookerName', title: t('office.bookerName'), width: 100 },
    { colKey: 'attendees', title: t('office.attendees'), width: 160, ellipsis: true },
    { colKey: 'col_5', title: t('common.operate'), width: 160, align: 'right' },
  ])

</script>
