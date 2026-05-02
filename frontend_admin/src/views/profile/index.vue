<template>
  <div class="profile-page p-6 max-w-4xl mx-auto">
    <!-- 个人信息头部 -->
    <a-card :bordered="true" class="mb-6">
      <div class="flex items-center gap-6">
        <a-avatar :size="64" style="background-color: var(--primary)">
          {{ userInitial }}
        </a-avatar>
        <div class="flex-1 min-w-0">
          <h2 class="text-xl font-bold" style="color: #1D2129">{{ userStore.userInfo?.nickName || $t('user.adminUser') }}</h2>
          <p class="text-sm mt-1" style="color: #86909C">@{{ userStore.userInfo?.userName || '-' }}</p>
          <div class="flex items-center gap-2 mt-2">
            <a-tag v-for="role in userStore.roles" :key="role" color="blue">{{ role }}</a-tag>
          </div>
        </div>
        <a-button @click="showEditDialog = true">
          <template #icon><icon-edit /></template>
          {{ $t('profile.editProfile') }}
        </a-button>
      </div>
    </a-card>

    <!-- 基本信息 -->
    <a-card :bordered="true" class="mb-6">
      <template #title>
        <div class="flex items-center gap-2">
          <icon-user style="color: var(--primary)" />
          <span>{{ $t('profile.basicInfo') }}</span>
        </div>
      </template>
      <a-descriptions :column="2" :data="profileData" layout="inline-vertical" />
    </a-card>

    <!-- 编辑资料弹窗 -->
    <a-modal
      v-model:visible="showEditDialog"
      :title="$t('profile.editProfileTitle')"
      :width="480"
      :mask-closable="false"
      @ok="onEditSubmit"
      @cancel="showEditDialog = false"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item field="nickName" :label="$t('profile.nickName')" :rules="formRules.nickName">
          <a-input v-model="editForm.nickName" :placeholder="$t('profile.nickNamePlaceholder')" />
        </a-form-item>
        <a-form-item field="email" :label="$t('profile.email')">
          <a-input v-model="editForm.email" :placeholder="$t('profile.emailPlaceholder')" />
        </a-form-item>
        <a-form-item field="phone" :label="$t('profile.phone')">
          <a-input v-model="editForm.phone" :placeholder="$t('profile.phonePlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconEdit, IconUser } from '@arco-design/web-vue/es/icon'
import { useUserStore } from '@/stores/user'
import { updateUserApi } from '@/api/user'

const { t } = useI18n()

const userStore = useUserStore()
const userInitial = computed(() => (userStore.userInfo?.nickName || t('user.adminUser')).charAt(0).toUpperCase())

const profileData = computed(() => [
  { label: t('user.userName'), value: userStore.userInfo?.userName || '-' },
  { label: t('user.nickName'), value: userStore.userInfo?.nickName || '-' },
  { label: t('profile.email'), value: userStore.userInfo?.email || '-' },
  { label: t('profile.phone'), value: userStore.userInfo?.phone || '-' },
  { label: t('profile.avatar'), value: userStore.userInfo?.avatar ? t('profile.avatarSet') : t('profile.avatarNotSet') },
  { label: t('profile.permissions'), value: userStore.permissions.slice(0, 5).join(', ') + (userStore.permissions.length > 5 ? ` +${userStore.permissions.length - 5}` : '') },
])

// ---- 编辑资料 ----
const showEditDialog = ref(false)

const editForm = reactive({
  nickName: '',
  email: '',
  phone: '',
})

const formRules = computed(() => ({
  nickName: [{ required: true, message: t('validation.required', { field: t('profile.nickName') }) }],
}))

// 弹窗打开时回填数据
watch(showEditDialog, (val) => {
  if (val && userStore.userInfo) {
    editForm.nickName = userStore.userInfo.nickName || ''
    editForm.email = userStore.userInfo.email || ''
    editForm.phone = userStore.userInfo.phone || ''
  }
})

async function onEditSubmit() {
  const userId = userStore.userInfo?.userId
  if (!userId) return

  try {
    await updateUserApi({
      id: userId,
      nickName: editForm.nickName,
      email: editForm.email,
      phone: editForm.phone,
    })
    Message.success(t('profile.updateSuccess'))
    showEditDialog.value = false
    await userStore.fetchUserInfo()
  } catch {
    // 错误已在拦截器中处理
  }
}
</script>
