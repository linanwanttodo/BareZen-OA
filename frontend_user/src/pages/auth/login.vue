<template>
  <div class="login-page">
    <!-- 图片面板：与管理端同一版分屏布局 -->
    <div class="login-visual">
      <div class="login-visual__inner">
        <img src="/login-visual.png" alt="BareZen" class="login-visual__img" />
        <p class="login-visual__caption">{{ $t('auth.enterprisePlatform') }}</p>
      </div>
    </div>

    <div class="login-form-panel">
      <div class="login-form-wrap">
        <div class="mb-6">
          <h2 class="text-lg font-semibold text-[var(--wecom-text-primary)]">{{ $t('auth.login') }}</h2>
          <p class="mt-1 text-sm text-[var(--wecom-text-tertiary)]">{{ $t('auth.loginSubtitle') }}</p>
        </div>

        <t-form :data="formData" layout="vertical" label-align="top" :on-submit="onSubmit">
          <t-form-item name="username" :label="$t('auth.username')" :rules="[{ required: true, message: $t('auth.usernameRequired') }]">
            <t-input v-model="formData.username" :placeholder="$t('auth.usernamePlaceholder')" size="large" clearable />
          </t-form-item>

          <t-form-item name="password" :label="$t('auth.password')" :rules="[{ required: true, message: $t('auth.passwordRequired') }]">
            <t-input v-model="formData.password" :placeholder="$t('auth.passwordPlaceholder')" size="large" />
          </t-form-item>

          <t-form-item name="captchaCode" :label="$t('auth.captcha')" :rules="[{ required: true, message: $t('auth.captchaRequired') }]">
            <div class="flex items-center gap-3 w-full">
              <t-input
                v-model="formData.captchaCode"
                :placeholder="$t('auth.captchaPlaceholder')"
                size="large"
                class="flex-1"
                @keyup.enter="onSubmit"
              />
              <div class="login-captcha" :title="$t('auth.refreshCaptcha')" @click="refreshCaptcha">
                <img v-if="captchaImg" :src="captchaImg" :alt="$t('auth.captcha')" class="h-full w-[120px]" />
                <span v-else class="text-xs text-[var(--wecom-text-tertiary)]">{{ $t('auth.captchaLoading') }}</span>
              </div>
            </div>
          </t-form-item>

          <t-checkbox v-model="formData.remember" class="login-remember">{{ $t('auth.rememberMe') }}</t-checkbox>

          <t-form-item class="!mb-0">
            <t-button theme="primary" type="submit" block size="large" :loading="loading">
              {{ $t('auth.login') }}
            </t-button>
          </t-form-item>
        </t-form>

        <p class="login-note">{{ $t('auth.contactAdmin') }}</p>
        <p class="login-copyright">© {{ new Date().getFullYear() }} BareZen</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { getCaptchaApi } from '@/api/auth'
import { persistToken } from '@/utils/auth'
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
  remember: false,
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
    if (formData.remember) {
      persistToken(userStore.token)
      localStorage.setItem('bz_user_remember', JSON.stringify({ username: formData.username }))
    } else {
      localStorage.removeItem('bz_user_remember')
    }
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
  const saved = localStorage.getItem('bz_user_remember')
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
/* 与管理端 login/index.vue 同版分屏布局：
   左图片面板、右表单面板，面板之间不做任何描边，整屏一体 */
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #fff;
}

.login-visual,
.login-form-panel {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 50%;
}

.login-visual {
  left: 0;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-form-panel {
  left: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--wecom-spacing-xl);
}

/* 暗色模式：整页跟随页面底色（theme-mode 属性驱动，见 utils/theme.ts）。
   注意 scoped 里 :global(前缀)+后代选择器的组合不会被编译，须直接写后代选择器 */
html[theme-mode='dark'] .login-page,
html[theme-mode='dark'] .login-visual,
html[theme-mode='dark'] .login-form-panel {
  background: var(--td-bg-color-page);
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
/* 暗色下图片自带白底与深色页面反差生硬，加圆角做成白色徽章卡（与管理端一致） */
html[theme-mode='dark'] .login-visual__img {
  border-radius: var(--td-radius-large);
}

.login-visual__caption {
  font-size: 14px;
  color: var(--wecom-text-tertiary);
  letter-spacing: 0.5px;
}

.login-form-wrap {
  width: 100%;
  max-width: 400px;
}
/* 统一表单行距：覆盖 TDesign 默认的过大 item 间距（与管理端一致） */
.login-form-wrap :deep(.t-form__item) {
  margin-bottom: 16px;
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

.login-note {
  margin-top: var(--wecom-spacing-lg);
  text-align: center;
  font-size: 12px;
  color: var(--wecom-text-tertiary);
}

.login-copyright {
  margin-top: var(--wecom-spacing-sm);
  text-align: center;
  font-size: 12px;
  color: var(--wecom-text-tertiary);
}

/* 窄屏：隐藏图片面板，仅保留表单 */
@media (max-width: 900px) {
  .login-visual {
    display: none;
  }
  .login-form-panel {
    left: 0;
    width: 100%;
  }
}
</style>
