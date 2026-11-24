<template>
  <div class="profile-page p-6 max-w-4xl mx-auto">
    <!-- 个人信息头部 -->
    <t-card :bordered="true" class="mb-6">
      <div class="flex items-center gap-6">
        <t-avatar v-if="userStore.userInfo?.avatar" size="64px" :image="userStore.userInfo.avatar" />
        <t-avatar v-else size="64px" style="background-color: var(--primary)">
          {{ userInitial }}
        </t-avatar>
        <div class="flex-1 min-w-0">
          <h2 class="text-xl font-semibold" style="color: var(--wecom-text-primary)">{{ userStore.userInfo?.nickName || $t('user.adminUser') }}</h2>
          <p class="text-sm mt-1" style="color: var(--wecom-text-tertiary)">@{{ userStore.userInfo?.userName || '-' }}</p>
          <div class="flex items-center gap-2 mt-2">
            <t-tag v-for="role in userStore.roles" :key="role" theme="primary">{{ role }}</t-tag>
          </div>
        </div>
        <t-space direction="vertical" size="small" align="end">
          <t-button @click="showEditDialog = true">
            <template #icon><icon-edit /></template>
            {{ $t('profile.editProfile') }}
          </t-button>
          <t-button variant="outline" :loading="avatarUploading" @click="avatarInputRef?.click()">
            <template #icon><icon-upload /></template>
            {{ $t('profile.uploadAvatar') }}
          </t-button>
        </t-space>
        <input ref="avatarInputRef" type="file" accept="image/*" class="hidden" @change="onAvatarChange" />
      </div>
    </t-card>

    <!-- 基本信息 -->
    <t-card :bordered="true" class="mb-6">
      <template #title>
        <div class="flex items-center gap-2">
          <icon-user style="color: var(--primary)" />
          <span>{{ $t('profile.basicInfo') }}</span>
        </div>
      </template>
      <t-descriptions :column="2" :data="profileData" layout="inline-vertical" />
    </t-card>

    <!-- 编辑资料弹窗 -->
    <t-dialog
      v-model:visible="showEditDialog"
      :header="$t('profile.editProfileTitle')"
      width="480px"
      :close-on-overlay-click="false"
      @confirm="onEditSubmit"
      @close="showEditDialog = false"
    >
      <t-form ref="editFormRef" :data="editForm" layout="vertical">
        <t-form-item name="nickName" :label="$t('profile.nickName')" :rules="formRules.nickName">
          <t-input v-model="editForm.nickName" :placeholder="$t('profile.nickNamePlaceholder')" />
        </t-form-item>
        <t-form-item name="email" :label="$t('profile.email')">
          <t-input v-model="editForm.email" :placeholder="$t('profile.emailPlaceholder')" />
        </t-form-item>
        <t-form-item name="phone" :label="$t('profile.phone')">
          <t-input v-model="editForm.phone" :placeholder="$t('profile.phonePlaceholder')" />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { useUserStore } from '@/stores/user'
import { updateUserApi } from '@/api/user'
import { uploadAvatarApi } from '@/api/auth'

const { t } = useI18n()

const userStore = useUserStore()
const userInitial = computed(() => (userStore.userInfo?.nickName || t('user.adminUser')).charAt(0).toUpperCase())

// ---- 头像上传 ----
const avatarInputRef = ref<HTMLInputElement>()
const avatarUploading = ref(false)
const AVATAR_MAX_MB = 5

async function onAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  // 允许重复选择同一文件：先清空 value 再校验
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    MessagePlugin.warning(t('profile.avatarTypeError'))
    return
  }
  if (file.size > AVATAR_MAX_MB * 1024 * 1024) {
    MessagePlugin.warning(t('profile.avatarTooLarge', { max: AVATAR_MAX_MB }))
    return
  }
  avatarUploading.value = true
  try {
    await uploadAvatarApi(file)
    await userStore.fetchUserInfo()
    MessagePlugin.success(t('profile.avatarUploaded'))
  } catch {
    // 错误已在拦截器中处理
  } finally {
    avatarUploading.value = false
  }
}

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

const editFormRef = ref<FormInstanceFunctions>()
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
  // tdesign 1.20.7 的 validate() 通过时返回 true；用 !== true 判定「未通过」
  if ((await editFormRef.value?.validate()) !== true) return
  const userId = userStore.userInfo?.userId
  if (!userId) return

  try {
    await updateUserApi({
      id: userId,
      nickName: editForm.nickName,
      email: editForm.email,
      phone: editForm.phone,
    })
    MessagePlugin.success(t('profile.updateSuccess'))
    showEditDialog.value = false
    await userStore.fetchUserInfo()
  } catch {
    // 错误已在拦截器中处理
  }
}
</script>
