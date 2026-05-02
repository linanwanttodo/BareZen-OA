<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <a-card class="mb-4">
      <a-form :model="searchForm" layout="inline" @submit-success="handleSearch">
        <a-form-item field="title" :label="$t('office.bookingTitle')">
          <a-input v-model="searchForm.title" :placeholder="$t('common.inputPlaceholder')" allow-clear style="width: 200px" />
        </a-form-item>
        <a-form-item field="roomId" :label="$t('office.roomName')">
          <a-select v-model="searchForm.roomId" :placeholder="$t('common.all')" allow-clear style="width: 180px">
            <a-option v-for="room in roomOptions" :key="room.id" :value="room.id">{{ room.roomName }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">{{ $t('common.search') }}</a-button>
            <a-button @click="handleReset">{{ $t('common.reset') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 会议室卡片列表 -->
    <a-card class="mb-4">
      <template #title>
        <div class="flex items-center justify-between w-full">
          <span>{{ $t('office.roomList') }}</span>
          <a-space>
            <a-button v-permission="'office:meeting:add'" type="primary" @click="roomDialogVisible = true">
              <template #icon><icon-plus /></template>
              {{ $t('office.addRoom') }}
            </a-button>
            <a-button v-permission="'office:meeting:add'" type="outline" @click="bookingVisible = true">
              <template #icon><icon-calendar /></template>
              {{ $t('office.bookMeeting') }}
            </a-button>
          </a-space>
        </div>
      </template>
      <a-spin :loading="roomLoading">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <a-card v-for="room in rooms" :key="room.id" class="room-card" hoverable>
            <div class="flex items-center justify-between">
              <span class="font-medium">{{ room.roomName }}</span>
              <a-tag :color="room.status === '0' ? 'green' : 'red'">
                {{ room.status === '0' ? $t('office.roomAvailable') : $t('office.roomDisabled') }}
              </a-tag>
            </div>
            <div class="mt-3 space-y-1 text-sm text-[var(--muted-foreground)]">
              <div>{{ $t('office.capacity') }}：{{ room.capacity }} 人</div>
              <div>{{ $t('office.location') }}：{{ room.location || '-' }}</div>
              <div>{{ $t('office.equipment') }}：{{ room.equipment || '-' }}</div>
            </div>
            <div class="mt-3 flex justify-end gap-2">
              <a-button v-permission="'office:meeting:edit'" size="small" @click="openRoomEdit(room)">{{ $t('common.edit') }}</a-button>
              <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDeleteRoom(room)">
                <a-button v-permission="'office:meeting:delete'" size="small" status="danger">{{ $t('common.delete') }}</a-button>
              </a-popconfirm>
            </div>
          </a-card>
        </div>
      </a-spin>
    </a-card>

    <!-- 预订记录 -->
    <a-card>
      <template #title>
        <span>{{ $t('office.bookingList') }}</span>
      </template>
      <a-spin :loading="loading">
        <a-table :data="tableData" :pagination="false" row-key="id">
          <template #columns>
            <a-table-column :title="$t('office.roomName')" data-index="roomName" :width="120" />
            <a-table-column :title="$t('office.bookingTitle')" data-index="title" />
            <a-table-column :title="$t('office.timeRange')">
              <template #cell="{ record }">
                <span class="text-sm">{{ record.startTime }} ~ {{ record.endTime }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="$t('office.bookerName')" data-index="bookerName" :width="100" />
            <a-table-column :title="$t('office.attendees')" data-index="attendees" ellipsis :width="160" />
            <a-table-column :title="$t('common.operate')" :width="90" align="right">
              <template #cell="{ record }">
                <a-popconfirm :content="$t('common.deleteConfirm')" @ok="handleDeleteBooking(record)">
                  <a-button v-permission="'office:meeting:delete'" size="small" status="danger">{{ $t('common.delete') }}</a-button>
                </a-popconfirm>
              </template>
            </a-table-column>
          </template>
        </a-table>
        <div v-if="pagination.total > 0" class="mt-4 flex justify-end">
          <a-pagination
            v-model:current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="loadBookings"
          />
        </div>
      </a-spin>
    </a-card>

    <!-- 新增会议室弹窗 -->
    <a-modal v-model:visible="roomDialogVisible" :title="$t('office.addRoom')" :width="520"
             @ok="handleSaveRoom" @cancel="roomDialogVisible = false" :ok-loading="saving">
      <a-form :model="roomForm" layout="vertical">
        <a-form-item field="roomName" :label="$t('office.roomName')" required>
          <a-input v-model="roomForm.roomName" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-form-item field="capacity" :label="$t('office.capacity')">
          <a-input-number v-model="roomForm.capacity" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item field="location" :label="$t('office.location')">
          <a-input v-model="roomForm.location" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-form-item field="equipment" :label="$t('office.equipment')">
          <a-input v-model="roomForm.equipment" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-form-item field="status" :label="$t('common.status')">
          <a-select v-model="roomForm.status">
            <a-option value="0">{{ $t('office.roomAvailable') }}</a-option>
            <a-option value="1">{{ $t('office.roomDisabled') }}</a-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 会议预订弹窗 -->
    <a-modal v-model:visible="bookingVisible" :title="$t('office.bookMeeting')" :width="560"
             @ok="handleSaveBooking" @cancel="bookingVisible = false" :ok-loading="saving">
      <a-form :model="bookingForm" layout="vertical">
        <a-form-item field="roomId" :label="$t('office.roomName')" required>
          <a-select v-model="bookingForm.roomId" :placeholder="$t('common.selectPlaceholder')">
            <a-option v-for="room in roomOptions" :key="room.id" :value="room.id">{{ room.roomName }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="title" :label="$t('office.bookingTitle')" required>
          <a-input v-model="bookingForm.title" :placeholder="$t('common.inputPlaceholder')" />
        </a-form-item>
        <a-form-item field="timeRange" :label="$t('office.timeRange')" required>
          <a-date-picker v-model="bookingForm.startTime" show-time style="width: 240px" :placeholder="$t('office.startTime')" />
          <span class="mx-2">~</span>
          <a-date-picker v-model="bookingForm.endTime" show-time style="width: 240px" :placeholder="$t('office.endTime')" />
        </a-form-item>
        <a-form-item field="attendees" :label="$t('office.attendees')">
          <a-input v-model="bookingForm.attendees" :placeholder="$t('office.attendeesPlaceholder')" />
        </a-form-item>
        <a-form-item field="remark" :label="$t('common.remark')">
          <a-textarea v-model="bookingForm.remark" :placeholder="$t('common.inputPlaceholder')" :max-length="500" show-word-limit />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  getRoomPageApi, getAvailableRoomsApi, addRoomApi, updateRoomApi, deleteRoomApi,
  getBookingPageApi, addBookingApi, deleteBookingApi,
  type OfficeMeetingRoom, type OfficeMeetingBooking,
} from '@/api/office'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const roomLoading = ref(false)
const loading = ref(false)
const saving = ref(false)
const rooms = ref<OfficeMeetingRoom[]>([])
const roomOptions = ref<OfficeMeetingRoom[]>([])
const tableData = ref<OfficeMeetingBooking[]>([])
const roomDialogVisible = ref(false)
const bookingVisible = ref(false)

const searchForm = reactive({ title: '', roomId: '' })

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const roomForm = reactive<Partial<OfficeMeetingRoom>>({})
const bookingForm = reactive<Partial<OfficeMeetingBooking>>({})

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
  Object.assign(roomForm, room)
  roomDialogVisible.value = true
}

async function handleSaveRoom() {
  if (!roomForm.roomName) {
    Message.warning('会议室名称必填')
    return
  }
  saving.value = true
  try {
    if (roomForm.id) {
      await updateRoomApi(roomForm)
    } else {
      await addRoomApi(roomForm)
    }
    Message.success('保存成功')
    roomDialogVisible.value = false
    Object.keys(roomForm).forEach((k) => delete (roomForm as Record<string, unknown>)[k])
    loadRooms()
  } finally {
    saving.value = false
  }
}

async function handleDeleteRoom(room: OfficeMeetingRoom) {
  await deleteRoomApi(room.id)
  Message.success('删除成功')
  loadRooms()
}

async function handleSaveBooking() {
  if (!bookingForm.roomId || !bookingForm.title) {
    Message.warning('请选择会议室并填写会议主题')
    return
  }
  if (!bookingForm.startTime || !bookingForm.endTime) {
    Message.warning('请选择会议时间')
    return
  }
  saving.value = true
  try {
    await addBookingApi({
      roomId: bookingForm.roomId,
      title: bookingForm.title,
      startTime: bookingForm.startTime,
      endTime: bookingForm.endTime,
      attendees: bookingForm.attendees,
      remark: bookingForm.remark,
      bookerName: userStore.userInfo?.nickName || '',
    })
    Message.success('预订成功')
    bookingVisible.value = false
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
  Message.success('删除成功')
  loadBookings()
}

onMounted(() => {
  loadRooms()
  loadBookings()
})
</script>
