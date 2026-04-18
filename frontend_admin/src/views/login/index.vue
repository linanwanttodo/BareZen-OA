<template>
  <div class="login-container flex min-h-screen">
    <!-- 左侧品牌区域 -->
    <div class="brand-panel hidden flex-col justify-between p-16 lg:flex w-[480px] flex-shrink-0">
      <div>
        <div class="mb-20 flex items-center gap-3">
          <div class="flex h-10 w-10 items-center justify-center rounded bg-white">
            <span class="text-lg font-bold text-[#3370FF]">B</span>
          </div>
          <span class="text-xl font-semibold text-white">BareZen</span>
        </div>
        <h2 class="mb-6 text-3xl font-bold leading-snug text-white">
          极简模块化<br />全栈框架
        </h2>
        <p class="text-base leading-relaxed text-white/70">
          微内核 + 插件架构，按需组装。<br />
          Java 21 虚拟线程 + Vue 3。
        </p>
      </div>
      <div class="flex items-center gap-6">
        <div v-for="item in features" :key="item" class="flex items-center gap-2 text-sm text-white/60">
          <icon-check-circle :size="16" />
          <span>{{ item }}</span>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="flex flex-1 items-center justify-center bg-white p-8">
      <div class="w-full max-w-[360px]">
        <!-- 移动端 Logo -->
        <div class="mb-10 text-center lg:hidden">
          <div class="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded bg-[#3370FF]">
            <span class="text-xl font-bold text-white">B</span>
          </div>
          <h1 class="text-xl font-semibold" style="color: #1D2129">BareZen Admin</h1>
        </div>

        <div class="mb-10">
          <h2 class="mb-2 text-2xl font-bold" style="color: #1D2129">登录</h2>
          <p class="text-sm" style="color: #86909C">请输入您的账号信息登录管理后台</p>
        </div>

        <a-form :model="formData" layout="vertical" @submit-success="onSubmit">
          <a-form-item field="username" label="用户名" :rules="[{ required: true, message: '请输入用户名' }]">
            <a-input
              v-model="formData.username"
              placeholder="请输入用户名"
              size="large"
              allow-clear
            />
          </a-form-item>

          <a-form-item field="password" label="密码" :rules="[{ required: true, message: '请输入密码' }]">
            <a-input-password
              v-model="formData.password"
              placeholder="请输入密码"
              size="large"
            />
          </a-form-item>

          <a-form-item field="captchaCode" label="验证码" :rules="[{ required: true, message: '请输入验证码' }]">
            <div class="flex items-center gap-3 w-full">
              <a-input
                v-model="formData.captchaCode"
                placeholder="请输入验证码"
                size="large"
                class="flex-1"
                @keyup.enter="onSubmit"
              />
              <div
                class="flex h-10 min-w-[120px] shrink-0 cursor-pointer items-center justify-center overflow-hidden rounded border"
                style="border-color: #E5E6EB; background: #F7F8FA"
                title="点击刷新"
                @click="refreshCaptcha"
              >
                <img v-if="captchaImg" :src="captchaImg" alt="验证码" class="h-full w-[120px]" />
                <span v-else class="text-xs" style="color: #86909C">加载中</span>
              </div>
            </div>
          </a-form-item>

          <a-form-item>
            <a-checkbox v-model="formData.remember">记住我</a-checkbox>
          </a-form-item>

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              long
              size="large"
              :loading="loading"
            >
              登录
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
import type { LoginParams } from '@/types/api'

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

const features = ['开箱即用', '轻量可控', '双端前端']

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
    Message.warning('请填写完整的登录信息')
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

    Message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
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
  background-color: #3370FF;
}
</style>
