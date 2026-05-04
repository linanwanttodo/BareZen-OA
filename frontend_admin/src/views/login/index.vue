<template>
  <div class="login-page flex min-h-screen">
    <!-- 左侧品牌区域 -->
    <div class="brand-panel relative hidden flex-col justify-between overflow-hidden p-14 lg:flex lg:w-[520px] lg:flex-shrink-0">
      <!-- 几何装饰层 -->
      <div class="pointer-events-none absolute inset-0">
        <!-- 右上圆环 -->
        <div class="absolute -right-24 -top-24 h-80 w-80 rounded-full border border-white/10"></div>
        <div class="absolute -right-10 -top-10 h-48 w-48 rounded-full border border-white/5"></div>
        <!-- 左下光晕 -->
        <div class="absolute -bottom-32 -left-24 h-96 w-96 rounded-full bg-[var(--primary)]/20 blur-3xl"></div>
        <!-- 网格点阵 -->
        <div class="absolute inset-0 opacity-[0.06]" style="background-image: radial-gradient(circle, #fff 1px, transparent 1px); background-size: 28px 28px"></div>
      </div>

      <!-- Logo 与品牌名 -->
      <div class="relative z-10 flex items-center gap-3">
        <img src="/logo.svg" alt="BareZen" class="h-9 w-9" />
        <span class="text-xl font-semibold tracking-wide text-white">BareZen</span>
      </div>

      <!-- 主标语区 -->
      <div class="relative z-10">
        <h2 class="mb-4 text-[32px] font-semibold leading-snug tracking-tight text-white">
          {{ $t('login.enterprisePlatform') }}
        </h2>
        <p class="max-w-[380px] text-sm leading-relaxed text-white/70">
          {{ $t('login.description') }}
        </p>

        <!-- 特性标签 -->
        <div class="mt-12 flex flex-wrap gap-3">
          <div
            v-for="item in features"
            :key="item"
            class="flex items-center gap-2 rounded-md border border-white/15 bg-white/5 px-4 py-2 text-sm text-white/85 backdrop-blur-sm"
          >
            <icon-check-circle class="text-[var(--primary-foreground)]" />
            <span>{{ item }}</span>
          </div>
        </div>
      </div>

      <!-- 底部版权 -->
      <div class="relative z-10 border-t border-white/10 pt-6 text-xs text-white/50">
        © {{ new Date().getFullYear() }} BareZen · {{ $t('login.internalSystem') }}
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="flex flex-1 items-center justify-center bg-[var(--muted)] p-6">
      <div class="w-full max-w-[400px] rounded-xl bg-[var(--card)] p-10 shadow-sm ring-1 ring-[var(--border)]">
        <!-- 移动端 Logo -->
        <div class="mb-8 text-center lg:hidden">
          <img src="/logo.svg" alt="BareZen" class="mx-auto mb-3 h-9 w-9" />
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
            />
          </a-form-item>

          <a-form-item field="password" :label="$t('login.password')" :rules="[{ required: true, message: $t('login.passwordRequired') }]">
            <a-input-password
              v-model="formData.password"
              :placeholder="$t('login.passwordRequired')"
              size="large"
            />
          </a-form-item>

          <a-form-item field="captchaCode" :label="$t('login.captcha')" :rules="[{ required: true, message: $t('login.captchaRequired') }]">
            <div class="flex w-full items-center gap-3">
              <a-input
                v-model="formData.captchaCode"
                :placeholder="$t('login.captchaRequired')"
                size="large"
                class="flex-1"
                @keyup.enter="onSubmit"
              />
              <div
                class="flex h-10 min-w-[120px] shrink-0 cursor-pointer items-center justify-center overflow-hidden rounded border border-[var(--border)] bg-[var(--muted)]"
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
    // 直接跳转真实存在的目标页：默认仪表盘（动态路由已由守卫加载），
    // 不要 push 到 '/'——它带有登录态重定向，已登录场景会被 vue-router
    // 判定为重复导航而卡在登录页
    const redirect = (route.query.redirect as string) || '/dashboard'
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
  /* 深色渐变品牌区：避免大面积纯色块，符合企业级信息层级 */
  background: linear-gradient(160deg, #1d2b4d 0%, #17223c 55%, #101a30 100%);
}
</style>
