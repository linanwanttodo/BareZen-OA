<template>
  <div class="mx-auto max-w-4xl px-6 py-8">
    <!-- 个人信息头部 -->
    <a-card :bordered="true" class="mb-6">
      <div class="flex items-center gap-6 p-4">
        <a-avatar :size="64" style="background-color: #3370FF">
          {{ userInitial }}
        </a-avatar>
        <div class="min-w-0 flex-1">
          <h2 class="text-xl font-bold" style="color: #1D2129">{{ userStore.userInfo?.nickName || '用户' }}</h2>
          <p class="mt-1 text-sm" style="color: #86909C">@{{ userStore.userInfo?.userName || '-' }}</p>
          <div class="mt-2 flex flex-wrap gap-2">
            <a-tag v-for="role in userStore.userInfo?.roles" :key="role" color="blue">{{ role }}</a-tag>
          </div>
        </div>
        <a-button @click="showEditDialog = true">
          <template #icon><icon-edit /></template>
          编辑资料
        </a-button>
      </div>
    </a-card>

    <!-- 详细信息 -->
    <a-card :bordered="true">
      <template #title>
        <div class="flex items-center gap-2">
          <icon-user style="color: #3370FF" />
          <span>基本信息</span>
        </div>
      </template>
      <a-descriptions :column="2" layout="vertical" :data="profileData" />
    </a-card>

    <!-- 编辑资料弹窗 -->
    <a-modal
      v-model:visible="showEditDialog"
      title="编辑个人资料"
      :width="480"
      :mask-closable="false"
      @ok="onEditSubmit"
      @cancel="showEditDialog = false"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item field="nickName" label="昵称" :rules="[{ required: true, message: '请输入昵称' }]">
          <a-input v-model="editForm.nickName" placeholder="请输入昵称" />
        </a-form-item>
        <a-form-item field="sex" label="性别">
          <a-radio-group v-model="editForm.sex">
            <a-radio value="1">男</a-radio>
            <a-radio value="2">女</a-radio>
            <a-radio value="0">未知</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="email" label="邮箱">
          <a-input v-model="editForm.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item field="phone" label="手机号">
          <a-input v-model="editForm.phone" placeholder="请输入手机号" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconEdit, IconUser } from '@arco-design/web-vue/es/icon'
import { useUserStore } from '@/stores/user'
import { updateProfileApi } from '@/api/enterprise'

const userStore = useUserStore()
const userInitial = computed(() => (userStore.userInfo?.nickName || '用').charAt(0).toUpperCase())

const sexLabel = computed(() => {
  const sex = userStore.userInfo?.sex
  if (sex === '1') return '男'
  if (sex === '2') return '女'
  return '未知'
})

const profileData = computed(() => [
  { label: '用户名', value: userStore.userInfo?.userName || '-' },
  { label: '昵称', value: userStore.userInfo?.nickName || '-' },
  { label: '邮箱', value: userStore.userInfo?.email || '-' },
  { label: '手机号', value: userStore.userInfo?.phone || '-' },
  { label: '性别', value: sexLabel.value },
  { label: '角色', value: userStore.userInfo?.roles?.join('、') || '-' },
])

// ---- 编辑资料 ----
const showEditDialog = ref(false)

const editForm = reactive({
  nickName: '',
  email: '',
  phone: '',
  sex: '0',
})

watch(showEditDialog, (val) => {
  if (val && userStore.userInfo) {
    editForm.nickName = userStore.userInfo.nickName || ''
    editForm.email = userStore.userInfo.email || ''
    editForm.phone = userStore.userInfo.phone || ''
    editForm.sex = userStore.userInfo.sex || '0'
  }
})

async function onEditSubmit() {
  try {
    await updateProfileApi({
      nickName: editForm.nickName,
      email: editForm.email,
      phone: editForm.phone,
      sex: editForm.sex,
    })
    Message.success('资料更新成功')
    showEditDialog.value = false
    await userStore.fetchUserInfo()
  } catch {
    // 错误已在拦截器中处理
  }
}
</script>
