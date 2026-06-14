<template>
  <div class="profile-page max-w-2xl mx-auto py-8 px-4">
    <!-- 页面标题 -->
    <PageHeader :title="$t('profile.title')" description="查看和编辑你的个人信息" />

    <!-- 头像区域 -->
    <div class="flex flex-col items-center mb-8">
      <t-avatar :size="80" :style="{ background: 'var(--td-brand-color)', fontSize: '28px' }">{{ userInitial }}</t-avatar>
      <h2 class="mt-3 text-xl font-semibold text-[var(--wecom-text-primary)]">{{ userStore.userInfo?.nickName }}</h2>
      <p class="text-sm text-[var(--wecom-text-tertiary)]">@{{ userStore.userInfo?.userName }}</p>
    </div>

    <!-- 信息卡片 -->
    <t-card>
      <div class="space-y-4">
        <div v-for="(item, index) in profileData" :key="index"
             class="flex items-center justify-between py-2 border-b border-[var(--wecom-border)] last:border-0">
          <span class="text-[var(--wecom-text-tertiary)] text-sm">{{ item.label }}</span>
          <span class="text-[var(--wecom-text-primary)] font-medium">{{ item.value }}</span>
        </div>
      </div>

      <div class="mt-6 flex justify-center">
        <t-button variant="outline" @click="showEditDialog = true">
          {{ $t('profile.editProfile') }}
        </t-button>
      </div>
    </t-card>

    <!-- 编辑资料对话框 -->
    <t-dialog v-model="showEditDialog" :title="$t('profile.editProfile')" @confirm="onEditSubmit" @cancel="showEditDialog = false">
      <t-form :model="editForm" auto-label-width>
        <t-form-item field="nickName" :label="$t('profile.nickName')" :rules="[{ required: true, message: '请输入昵称' }]">
          <t-input v-model="editForm.nickName" placeholder="请输入昵称" />
        </t-form-item>
        <t-form-item field="sex" :label="$t('profile.sex')">
          <t-radio-group v-model="editForm.sex">
            <t-radio value="0">{{ $t('profile.male') }}</t-radio>
            <t-radio value="1">{{ $t('profile.female') }}</t-radio>
            <t-radio value="2">{{ $t('profile.unknown') }}</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item field="email" :label="$t('profile.email')">
          <t-input v-model="editForm.email" placeholder="请输入邮箱" />
        </t-form-item>
        <t-form-item field="phone" :label="$t('profile.phone')">
          <t-input v-model="editForm.phone" placeholder="请输入手机号" />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MessagePlugin } from 'tdesign-vue-next'
import { useUserStore } from '@/stores/user'
import { updateProfileApi } from '@/api/enterprise'

const { t } = useI18n()
const userStore = useUserStore()
const userInitial = computed(() => (userStore.userInfo?.nickName || '用').charAt(0).toUpperCase())

const sexLabel = computed(() => {
  const sex = userStore.userInfo?.sex
  if (sex === '0') return t('profile.male')
  if (sex === '1') return t('profile.female')
  return t('profile.unknown')
})

const profileData = computed(() => [
  { label: '用户名', value: userStore.userInfo?.userName || '-' },
  { label: t('profile.nickName'), value: userStore.userInfo?.nickName || '-' },
  { label: t('profile.email'), value: userStore.userInfo?.email || '-' },
  { label: t('profile.phone'), value: userStore.userInfo?.phone || '-' },
  { label: t('profile.sex'), value: sexLabel.value },
  { label: t('profile.roles'), value: userStore.userInfo?.roles?.join('、') || '-' },
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
    MessagePlugin.success(t('profile.updateSuccess'))
    showEditDialog.value = false
    await userStore.fetchUserInfo()
  } catch {
    // 错误已在拦截器中处理
  }
}
</script>
