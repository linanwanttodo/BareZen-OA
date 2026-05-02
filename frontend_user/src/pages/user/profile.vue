<template>
  <div class="profile-page max-w-2xl mx-auto py-8 px-4">
    <!-- 页面标题 -->
    <PageHeader :title="$t('profile.title')" description="查看和编辑你的个人信息" />

    <!-- 头像区域 -->
    <div class="flex flex-col items-center mb-8">
      <div class="w-24 h-24 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white text-3xl font-bold shadow-lg mb-3">
        {{ userInitial }}
      </div>
      <h2 class="text-xl font-semibold text-gray-800">{{ userStore.userInfo?.nickName }}</h2>
      <p class="text-sm text-gray-500">@{{ userStore.userInfo?.userName }}</p>
    </div>

    <!-- 信息卡片 -->
    <Card>
      <CardContent class="p-6">
        <div class="space-y-4">
          <div v-for="(item, index) in profileData" :key="index"
               class="flex items-center justify-between py-2 border-b border-gray-100 last:border-0">
            <span class="text-gray-500 text-sm">{{ item.label }}</span>
            <span class="text-gray-800 font-medium">{{ item.value }}</span>
          </div>
        </div>

        <div class="mt-6 flex justify-center">
          <Button variant="outline" @click="showEditDialog = true">
            {{ $t('profile.editProfile') }}
          </Button>
        </div>
      </CardContent>
    </Card>

    <!-- 编辑资料对话框 -->
    <a-modal v-model:visible="showEditDialog" :title="$t('profile.editProfile')" @ok="onEditSubmit" @cancel="showEditDialog = false">
      <a-form :model="editForm" auto-label-width>
        <a-form-item field="nickName" :label="$t('profile.nickName')" :rules="[{ required: true, message: '请输入昵称' }]">
          <a-input v-model="editForm.nickName" placeholder="请输入昵称" />
        </a-form-item>
        <a-form-item field="sex" :label="$t('profile.sex')">
          <a-radio-group v-model="editForm.sex">
            <a-radio value="0">{{ $t('profile.male') }}</a-radio>
            <a-radio value="1">{{ $t('profile.female') }}</a-radio>
            <a-radio value="2">{{ $t('profile.unknown') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="email" :label="$t('profile.email')">
          <a-input v-model="editForm.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item field="phone" :label="$t('profile.phone')">
          <a-input v-model="editForm.phone" placeholder="请输入手机号" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconEdit } from '@arco-design/web-vue/es/icon'
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
    Message.success(t('profile.updateSuccess'))
    showEditDialog.value = false
    await userStore.fetchUserInfo()
  } catch {
    // 错误已在拦截器中处理
  }
}
</script>
