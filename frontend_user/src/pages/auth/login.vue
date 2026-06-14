<template>
  <div>
    <div class="mb-8 text-center">
      <img src="/logo.png" alt="BareZen" class="mx-auto mb-4 h-10" />
      <h2 class="text-xl font-semibold text-[var(--wecom-text-primary)]">{{ $t('auth.loginTitle') }}</h2>
      <p class="mt-2 text-sm text-[var(--wecom-text-tertiary)]">登录你的企业账户</p>
    </div>

    <t-form :model="formData" layout="vertical" @submit-success="onSubmit" class="space-y-2">
      <t-form-item field="username" :label="$t('auth.username')" :rules="[{ required: true, message: '请输入用户名' }]">
        <t-input
          v-model="formData.username"
          placeholder="请输入用户名"
          size="large"
          allow-clear
        />
      </t-form-item>

      <t-form-item field="password" :label="$t('auth.password')" :rules="[{ required: true, message: '请输入密码' }]">
        <t-input
          v-model="formData.password"
          placeholder="请输入密码"
          size="large"
        />
      </t-form-item>

      <t-form-item field="captchaCode" :label="$t('auth.captcha')" :rules="[{ required: true, message: '请输入验证码' }]">
        <div class="flex items-center gap-3 w-full">
          <t-input
            v-model="formData.captchaCode"
            placeholder="请输入验证码"
            size="large"
            class="flex-1"
            @keyup.enter="onSubmit"
          />
          <div
            class="flex h-10 min-w-[120px] shrink-0 cursor-pointer items-center justify-center overflow-hidden rounded-wecom-sm border border-[var(--wecom-border)] bg-[var(--wecom-bg-container)]"
            title="点击刷新"
            @click="refreshCaptcha"
          >
            <img v-if="captchaImg" :src="captchaImg" alt="验证码" class="h-full w-[120px]" />
            <span v-else class="text-xs text-[var(--wecom-text-tertiary)]">加载中</span>
          </div>
        </div>
      </t-form-item>

      <t-form-item class="!mb-0 mt-2">
        <t-button
          theme="primary"
          html-type="submit"
          long
          size="large"
          :loading="loading"
        >
          {{ $t('auth.login') }}
        </t-button>
      </t-form-item>
    </t-form>

    <div class="mt-6 text-center">
      <p class="text-xs text-[var(--muted-foreground)]">如需账号，请联系企业管理员</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { getCaptchaApi } from '@/api/auth'
import { MessagePlugin } from 'tdesign-vue-next'
import { safeRedirect } from '@/utils/url'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const captchaImg = ref('')
const captchaUuid = ref('')

const formData = reactive({
  username: '',
  password: '',
  captchaCode: '',
})

async function refreshCaptcha() {
  try {
    const res = await getCaptchaApi()
    captchaImg.value = res.data.captchaBase64
    captchaUuid.value = res.data.uuid
  } catch (err) {
    console.error('captcha refresh error:', err)
  }
}

async function onSubmit() {
  loading.value = true
  try {
    await userStore.login({
      username: formData.username,
      password: formData.password,
      captchaCode: formData.captchaCode,
      uuid: captchaUuid.value,
    })
    MessagePlugin.success(t('auth.loginSuccess'))
    const redirect = (route.query.redirect as string) || '/'
    router.push(safeRedirect(redirect))
  } catch {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>
