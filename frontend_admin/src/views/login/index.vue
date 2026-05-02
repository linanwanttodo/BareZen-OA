<template>
  <div class="login-container flex min-h-screen">
    <!-- 左侧品牌区域 -->
    <div class="brand-panel flex-col justify-between p-12 hidden md:flex w-[420px] flex-shrink-0">
      <div>
        <div class="mb-16 flex items-center gap-3">
          <img src="/logo.svg" alt="BareZen" class="h-8" />
          <span class="text-lg font-semibold text-white">BareZen</span>
        </div>
        <h2 class="mb-5 text-[28px] font-semibold leading-snug tracking-tight text-white">
          {{ $t('login.enterprisePlatform') }}
        </h2>
        <p class="text-sm leading-relaxed text-white/60">
          {{ $t('login.description') }}
        </p>
      </div>
      <div class="flex flex-wrap items-center gap-4">
        <div v-for="item in features" :key="item" class="flex items-center gap-1.5 text-xs text-white/50">
          <div class="h-1 w-1 rounded-full bg-white/40"></div>
          <span>{{ item }}</span>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="flex flex-1 items-center justify-center bg-[var(--background)] p-8">
      <div class="w-full max-w-[340px]">
        <!-- 移动端 Logo -->
        <div class="mb-8 text-center md:hidden">
          <img src="/logo.svg" alt="BareZen" class="mx-auto mb-3 h-8" />
          <h1 class="text-lg font-semibold text-[var(--foreground)]">BareZen</h1>
        </div>

        <div class="mb-8">
          <h2 class="mb-1.5 text-xl font-semibold tracking-tight text-[var(--foreground)]">{{ $t('login.login') }}</h2>
          <p class="text-sm text-[var(--muted-foreground)]">{{ $t('login.loginInfo') }}</p>
        </div>

        <a-form :model="formData" layout="vertical" @submit-success="onSubmit" class="space-y-4">
          <a-form-item field="username" :label="$t('login.username')" :rules="[{ required: true, message: $t('login.usernameRequired') }]">
            <a-input
              v-model="formData.username"
              :placeholder="$t('login.usernameRequired')"
              size="large"
              allow-clear
              class="!rounded-lg"
            />
          </a-form-item>

          <a-form-item field="password" :label="$t('login.password')" :rules="[{ required: true, message: $t('login.passwordRequired') }]">
            <a-input-password
              v-model="formData.password"
              :placeholder="$t('login.passwordRequired')"
              size="large"
              class="!rounded-lg"
            />
          </a-form-item>

          <a-form-item field="captchaCode" :label="$t('login.captcha')" :rules="[{ required: true, message: $t('login.captchaRequired') }]">
            <div class="flex items-center gap-3 w-full">
              <a-input
                v-model="formData.captchaCode"
                :placeholder="$t('login.captchaRequired')"
                size="large"
                class="flex-1 !rounded-lg"
                @keyup.enter="onSubmit"
              />
              <div
                class="flex h-10 min-w-[120px] shrink-0 cursor-pointer items-center justify-center overflow-hidden rounded-lg border border-[var(--border)] bg-[var(--muted)]"
                :title="$t('common.refresh')"
                @click="refreshCaptcha"
              >
                <img v-if="captchaImg" :src="captchaImg" :alt="$t('login.captcha')" class="h-full w-[120px]" />
                <span v-else class="text-xs text-[var(--muted-foreground)]">{{ $t('common.loading') }}</span>
              </div>
            </div>
          </a-form-item>

          <div class="flex items-center justify-between">
            <a-checkbox v-model="formData.remember" class="text-sm text-[var(--muted-foreground)]">{{ $t('login.rememberMe') }}</a-checkbox>
          </div>

          <a-form-item class="!mb-0">
            <a-button
              type="primary"
              html-type="submit"
              long
              size="large"
              :loading="loading"
              class="!rounded-lg !h-10 text-sm font-medium"
            >
              {{ $t('login.login') }}
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCaptchaApi } from '@/api/auth'
import { Message } from '@arco-design/web-vue'
import { useI18n } from 'vue-i18n'
import type { LoginParams } from '@/types/api'
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
  remember: false,
})

const features = [t('login.features.0'), t('login.features.1'), t('login.features.2')]

async function refreshCaptcha() {
  try {
    const res = await getCaptchaApi()
    captchaImg.value = res.data.captchaBase64
    captchaUuid.value = res.data.uuid
  } catch {
    // 错误已在拦截器中处理
  }
}

async function onSubmit() {
  if (!formData.username || !formData.password || !formData.captchaCode) {
    Message.warning(t('login.fullLoginInfo'))
    return
  }

  loading.value = true
  try {
    await userStore.login({
      username: formData.username,
      password: formData.password,
      captchaCode: formData.captchaCode,
      uuid: captchaUuid.value,
    } as LoginParams)

    if (formData.remember) {
      localStorage.setItem('bz_admin_remember', JSON.stringify({ username: formData.username }))
    } else {
      localStorage.removeItem('bz_admin_remember')
    }

    Message.success(t('login.loginSuccess'))
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
  const saved = localStorage.getItem('bz_admin_remember')
  if (saved) {
    try {
      const data = JSON.parse(saved)
      formData.username = data.username || ''
      formData.remember = true
    } catch {
      // ignore
    }
  }
})
</script>

<style scoped>
.brand-panel {
  background-color: var(--primary);
}
</style>
