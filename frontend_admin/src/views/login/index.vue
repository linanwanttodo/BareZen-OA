<template>
  <div class="login-page" :class="{ 'mode-register': mode === 'register' }">
    <!-- 图片面板：登录时在左，注册时滑到右 -->
    <div class="login-visual">
      <div class="login-visual__inner">
        <img src="/login-visual.png" alt="BareZen" class="login-visual__img" />
        <p class="login-visual__caption">{{ mode === 'login' ? $t('login.enterprisePlatform') : $t('login.registerTitle') }}</p>
      </div>
    </div>

    <!-- 表单面板：登录时在右，注册时滑到左 -->
    <div class="login-form-panel">
      <div class="login-form-wrap">
        <!-- 品牌头 -->
        <div class="login-brand">
          <img src="/logo.png" alt="BareZen" class="login-brand__logo" />
          <div class="login-brand__text">
            <h1 class="login-brand__title">BareZen</h1>
            <p class="login-brand__subtitle">{{ $t('login.title') }}</p>
          </div>
        </div>

        <!-- 登录 / 注册 双层表单：翻转过程中交叉淡入淡出 -->
        <div class="form-stack">
          <!-- ===== 登录 ===== -->
          <div class="form-layer" :class="{ 'form-layer--active': mode === 'login' }" :aria-hidden="mode !== 'login'">
            <div class="mb-6">
              <h2 class="text-lg font-semibold text-[var(--wecom-text-primary)]">{{ $t('login.login') }}</h2>
              <p class="mt-1 text-sm text-[var(--wecom-text-tertiary)]">{{ $t('login.loginInfo') }}</p>
            </div>

            <t-form :model="formData" layout="vertical" @submit-success="onSubmit" class="space-y-2">
              <t-form-item name="username" :label="$t('login.username')" :rules="[{ required: true, message: $t('login.usernameRequired') }]">
                <t-input v-model="formData.username" :placeholder="$t('login.usernameRequired')" size="large" clearable />
              </t-form-item>

              <t-form-item name="password" :label="$t('login.password')" :rules="[{ required: true, message: $t('login.passwordRequired') }]">
                <t-input v-model="formData.password" :placeholder="$t('login.passwordRequired')" size="large" />
              </t-form-item>

              <t-form-item name="captchaCode" :label="$t('login.captcha')" :rules="[{ required: true, message: $t('login.captchaRequired') }]">
                <div class="flex w-full items-center gap-3">
                  <t-input
                    v-model="formData.captchaCode"
                    :placeholder="$t('login.captchaRequired')"
                    size="large"
                    class="flex-1"
                    @keyup.enter="onSubmit"
                  />
                  <div class="login-captcha" :title="$t('common.refresh')" @click="refreshCaptcha">
                    <img v-if="captchaImg" :src="captchaImg" :alt="$t('login.captcha')" class="h-full w-[120px]" />
                    <span v-else class="text-xs text-[var(--wecom-text-tertiary)]">{{ $t('common.loading') }}</span>
                  </div>
                </div>
              </t-form-item>

              <t-checkbox v-model="formData.remember" class="login-remember">{{ $t('login.rememberMe') }}</t-checkbox>

              <t-form-item class="!mb-0 mt-2">
                <t-button theme="primary" type="submit" block size="large" :loading="loading">
                  {{ $t('login.login') }}
                </t-button>
              </t-form-item>
            </t-form>

            <p class="login-switch">
              {{ $t('login.noAccount') }}
              <button type="button" class="login-switch__link" @click="switchMode('register')">{{ $t('login.register') }}</button>
            </p>
          </div>

          <!-- ===== 注册 ===== -->
          <div class="form-layer" :class="{ 'form-layer--active': mode === 'register' }" :aria-hidden="mode !== 'register'">
            <div class="mb-6">
              <h2 class="text-lg font-semibold text-[var(--wecom-text-primary)]">{{ $t('login.registerTitle') }}</h2>
              <p class="mt-1 text-sm text-[var(--wecom-text-tertiary)]">{{ $t('login.registerInfo') }}</p>
            </div>

            <t-form ref="registerFormRef" :model="registerData" layout="vertical" class="space-y-2">
              <t-form-item name="userName" :label="$t('login.username')" :rules="[{ required: true, message: $t('login.usernameRequired') }]">
                <t-input v-model="registerData.userName" :placeholder="$t('login.usernameRequired')" size="large" clearable />
              </t-form-item>

              <t-form-item name="nickName" :label="$t('login.nickName')">
                <t-input v-model="registerData.nickName" :placeholder="$t('login.nickNameRequired')" size="large" clearable />
              </t-form-item>

              <t-form-item
                name="password"
                :label="$t('login.password')"
                :rules="[{ required: true, message: $t('login.passwordRequired') }, { minLength: 8, message: $t('login.pwdPolicyHint') }]"
              >
                <t-input v-model="registerData.password" :placeholder="$t('login.pwdPolicyHint')" size="large" />
              </t-form-item>

              <t-form-item
                name="confirmPassword"
                :label="$t('login.confirmPassword')"
                :rules="[{ required: true, message: $t('login.confirmPasswordRequired') }, { validator: validateConfirm }]"
              >
                <t-input v-model="registerData.confirmPassword" :placeholder="$t('login.confirmPasswordRequired')" size="large" @keyup.enter="onRegister" />
              </t-form-item>

              <t-form-item class="!mb-0 mt-2">
                <t-button theme="primary" block size="large" :loading="registering" @click="onRegister">
                  {{ $t('login.registerNow') }}
                </t-button>
              </t-form-item>
            </t-form>

            <p class="login-switch">
              {{ $t('login.haveAccount') }}
              <button type="button" class="login-switch__link" @click="switchMode('login')">{{ $t('login.backToLogin') }}</button>
            </p>
          </div>
        </div>

        <p class="login-copyright">© {{ new Date().getFullYear() }} BareZen · {{ $t('login.internalSystem') }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCaptchaApi, registerApi } from '@/api/auth'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import type { LoginParams } from '@/types/api'
import { safeRedirect } from '@/utils/url'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

/** 当前面板：login 图左表单右；register 左右互换（CSS transform 过渡） */
const mode = ref<'login' | 'register'>('login')

const loading = ref(false)
const registering = ref(false)
const captchaImg = ref('')
const captchaUuid = ref('')
const registerFormRef = ref<FormInstanceFunctions>()

const formData = reactive({
  username: '',
  password: '',
  captchaCode: '',
  remember: false,
})

const registerData = reactive({
  userName: '',
  nickName: '',
  password: '',
  confirmPassword: '',
})

function switchMode(next: 'login' | 'register') {
  if (mode.value === next) return
  mode.value = next
}

function validateConfirm(value: string, callback: (error?: string) => void) {
  if (value && value !== registerData.password) {
    callback(t('login.passwordMismatch'))
  } else {
    callback()
  }
}

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
    MessagePlugin.warning(t('login.fullLoginInfo'))
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

    MessagePlugin.success(t('login.loginSuccess'))
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

async function onRegister() {
  const errors = await registerFormRef.value?.validate()
  if (errors) return
  if (!registerData.userName || !registerData.password || !registerData.confirmPassword) {
    MessagePlugin.warning(t('login.fullRegisterInfo'))
    return
  }

  registering.value = true
  try {
    await registerApi({
      userName: registerData.userName,
      nickName: registerData.nickName || undefined,
      password: registerData.password,
    })
    MessagePlugin.success(t('login.registerSuccess'))
    // 注册成功：回填用户名并翻回登录面板
    formData.username = registerData.userName
    formData.password = ''
    registerData.userName = ''
    registerData.nickName = ''
    registerData.password = ''
    registerData.confirmPassword = ''
    switchMode('login')
  } catch {
    // 错误（含「注册功能已关闭」）已在拦截器中提示
  } finally {
    registering.value = false
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
/* ============================================================
   左右分栏登录页：
   - 登录态：图片面板在左、表单面板在右
   - 注册态：两面板 translateX 互换位置（0.6s 平滑过渡）
   - 表单内容在翻转中途交叉淡切
   ============================================================ */
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
}

/* ---- 面板通用 ---- */
.login-visual,
.login-form-panel {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 50%;
  transition: transform 0.6s cubic-bezier(0.65, 0, 0.35, 1);
  will-change: transform;
}

/* ---- 图片面板（白底，与 design-system/brand/logo-on-white.png 白底无缝融合） ---- */
.login-visual {
  left: 0;
  background: var(--wecom-bg-card);
  border-right: 1px solid var(--wecom-border);
  display: flex;
  align-items: center;
  justify-content: center;
}
.mode-register .login-visual {
  transform: translateX(100%);
}
.login-visual__inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--wecom-spacing-lg);
  padding: var(--wecom-spacing-xxl);
}
.login-visual__img {
  width: min(56vh, 420px);
  max-width: 80%;
  height: auto;
}
.login-visual__caption {
  font-size: 14px;
  color: var(--wecom-text-tertiary);
  letter-spacing: 0.5px;
}

/* ---- 表单面板 ---- */
.login-form-panel {
  left: 50%;
  background: var(--wecom-bg-page);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--wecom-spacing-xl);
}
.mode-register .login-form-panel {
  transform: translateX(-100%);
}

.login-form-wrap {
  width: 100%;
  max-width: 400px;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: var(--wecom-spacing-xl);
}
.login-brand__logo {
  width: 40px;
  height: 40px;
}
.login-brand__title {
  font-size: 20px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--wecom-text-primary);
}
.login-brand__subtitle {
  margin-top: 2px;
  font-size: 13px;
  color: var(--wecom-text-tertiary);
}

/* ---- 双层表单：绝对堆叠，翻转中途交叉淡切 ---- */
.form-stack {
  position: relative;
  min-height: 520px;
}
.form-layer {
  position: absolute;
  inset: 0;
  opacity: 0;
  transform: translateY(10px);
  pointer-events: none;
  transition: opacity 0.28s ease-in-out 0.22s, transform 0.28s ease-in-out 0.22s;
}
.form-layer--active {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.login-captcha {
  display: flex;
  height: 40px;
  min-width: 120px;
  flex-shrink: 0;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--wecom-border);
  border-radius: var(--wecom-radius-sm);
  background: var(--wecom-bg-container);
}

.login-remember {
  font-size: 14px;
  color: var(--wecom-text-secondary);
}

.login-switch {
  margin-top: var(--wecom-spacing-lg);
  text-align: center;
  font-size: 14px;
  color: var(--wecom-text-tertiary);
}
.login-switch__link {
  color: var(--wecom-primary);
  font-weight: 500;
}
.login-switch__link:hover {
  color: var(--wecom-primary-hover);
}

.login-copyright {
  margin-top: var(--wecom-spacing-xl);
  text-align: center;
  font-size: 12px;
  color: var(--wecom-text-tertiary);
}

/* ---- 窄屏：隐藏图片面板，仅做表单交叉淡切 ---- */
@media (max-width: 900px) {
  .login-visual {
    display: none;
  }
  .login-form-panel {
    left: 0;
    width: 100%;
  }
  .mode-register .login-form-panel {
    transform: none;
  }
  .form-layer {
    transition-delay: 0s;
  }
}

/* ---- 尊重系统「减少动效」偏好 ---- */
@media (prefers-reduced-motion: reduce) {
  .login-visual,
  .login-form-panel,
  .form-layer {
    transition-duration: 0.01ms;
    transition-delay: 0s;
  }
}
</style>
