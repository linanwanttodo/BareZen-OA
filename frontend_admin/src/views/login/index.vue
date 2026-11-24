<template>
  <div class="login-page" :class="{ 'mode-register': mode === 'register' }">
    <!-- 图片面板：登录时在左，注册时换到右 -->
    <div class="login-visual">
      <div class="login-visual__inner">
        <img src="/login-visual.png" alt="BareZen" class="login-visual__img" />
        <p class="login-visual__caption">{{ mode === 'login' ? $t('login.enterprisePlatform') : $t('login.registerTitle') }}</p>
      </div>
    </div>

    <!-- 表单面板：登录时在右，注册时滑到左 -->
    <div class="login-form-panel">
      <div class="login-form-wrap">
        <!-- 登录 / 注册 双层表单：注册态瞬间互换面板 -->
        <div class="form-stack">
          <!-- ===== 登录 ===== -->
          <div class="form-layer" :class="{ 'form-layer--active': mode === 'login' }" :aria-hidden="mode !== 'login'">
            <div class="mb-6">
              <h2 class="text-lg font-semibold text-[var(--wecom-text-primary)]">{{ $t('login.login') }}</h2>
              <p class="mt-1 text-sm text-[var(--wecom-text-tertiary)]">{{ $t('login.loginInfo') }}</p>
            </div>

            <t-form :data="formData" layout="vertical" label-align="top" :on-submit="onSubmit">
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

              <t-form-item class="!mb-0">
                <t-button theme="primary" type="submit" block size="large" :loading="loading">
                  {{ $t('login.login') }}
                </t-button>
              </t-form-item>
            </t-form>

            <p class="login-switch">
              {{ $t('login.noAccount') }}
              <t-link theme="primary" hover="color" @click="switchMode('register')">{{ $t('login.register') }}</t-link>
            </p>
          </div>

          <!-- ===== 注册 ===== -->
          <div class="form-layer" :class="{ 'form-layer--active': mode === 'register' }" :aria-hidden="mode !== 'register'">
            <div class="mb-6">
              <h2 class="text-lg font-semibold text-[var(--wecom-text-primary)]">{{ $t('login.registerTitle') }}</h2>
              <p class="mt-1 text-sm text-[var(--wecom-text-tertiary)]">{{ $t('login.registerInfo') }}</p>
            </div>

            <t-form ref="registerFormRef" :data="registerData" layout="vertical" label-align="top">
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

              <t-form-item class="!mb-0">
                <t-button theme="primary" block size="large" :loading="registering" @click="onRegister">
                  {{ $t('login.registerNow') }}
                </t-button>
              </t-form-item>
            </t-form>

            <p class="login-switch">
              {{ $t('login.haveAccount') }}
              <t-link theme="primary" hover="color" @click="switchMode('login')">{{ $t('login.backToLogin') }}</t-link>
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
import { persistToken } from '@/utils/auth'
import { MessagePlugin, type FormInstanceFunctions } from 'tdesign-vue-next'
import { useI18n } from 'vue-i18n'
import type { LoginParams } from '@/types/api'
import { safeRedirect } from '@/utils/url'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

/** 当前面板：login 图左表单右；register 左右互换 */
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
      persistToken(userStore.token)
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
  // tdesign-vue-next 1.20.7 的 validate() 通过时返回 true、失败时返回错误对象，
  // 此处必须用 !== true 判定「未通过」，否则校验成功反而被当作失败而中断提交
  if ((await registerFormRef.value?.validate()) !== true) return
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
    // 注册成功：回填用户名并切回登录面板
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
   - 注册态：两面板位置互换（瞬间切换，无动效）
   ============================================================ */
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  /* 根容器兜底纯白：body 用的是 --td-bg-color-page（浅色 #f5f7fa），
     登录页按官方口径整屏纯白；暗色模式在下方切到页面底色 */
  background: #fff;
}

/* 暗色模式：登录页跟随页面底色。本项目暗色用 <html theme-mode="dark"> 属性驱动，
   见 utils/theme.ts。注意 scoped 样式里 :global(前缀)+后代选择器的组合不会被编译，
   必须直接写 html[theme-mode='dark'] 后代选择器 */
html[theme-mode='dark'] .login-page,
html[theme-mode='dark'] .login-form-panel {
  background: var(--td-bg-color-page);
}

/* ---- 面板通用 ---- */
.login-visual,
.login-form-panel {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 50%;
}

/* ---- 图片面板 ----
   对齐 TDesign Starter login/index.less：浅色模式背景是纯白 white，
   暗色模式才用 --td-bg-color-page。此处若用容器色 token，暗色下会发灰，
   且与 logo-on-white.png 的白底衔接出现色差。 */
.login-visual {
  left: 0;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
/* 暗色模式跟随页面底色（与 .login-page 同一口径） */
html[theme-mode='dark'] .login-visual {
  background: var(--td-bg-color-page);
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
/* 暗色下图片自带白底与深色页面反差生硬，加圆角做成白色徽章卡 */
html[theme-mode='dark'] .login-visual__img {
  border-radius: var(--td-radius-large);
}
.login-visual__caption {
  font-size: 14px;
  color: var(--wecom-text-tertiary);
  letter-spacing: 0.5px;
}

/* ---- 表单面板 ----
   对齐 TDesign Starter：登录页整屏是纯白（官方 .light.login-wrapper 用 white），
   两侧面板都不用页面灰底 --td-bg-color-page（该 token 本身是 #f3f3f3 灰）。
   暗色模式才切到页面底色。 */
.login-form-panel {
  left: 50%;
  background: #fff;
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
/* 统一表单行距：覆盖 TDesign 默认的过大 item 间距 */
.login-form-wrap :deep(.t-form__item) {
  margin-bottom: 16px;
}

/* ---- 双层表单：绝对堆叠，注册态瞬间切换 ---- */
.form-stack {
  position: relative;
  min-height: 520px;
}
.form-layer {
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
}
.form-layer--active {
  opacity: 1;
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
  margin: 0 0 16px;
  font-size: 14px;
  color: var(--wecom-text-secondary);
}

.login-switch {
  margin-top: var(--wecom-spacing-lg);
  text-align: center;
  font-size: 14px;
  color: var(--wecom-text-tertiary);
}

.login-copyright {
  margin-top: var(--wecom-spacing-xl);
  text-align: center;
  font-size: 12px;
  color: var(--wecom-text-tertiary);
}

/* ---- 窄屏：隐藏图片面板，仅保留表单 ---- */
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
}
</style>
