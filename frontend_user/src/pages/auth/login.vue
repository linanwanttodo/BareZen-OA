<template>
  <div>
    <div class="mb-8">
      <h2 class="text-2xl font-bold" style="color: #1D2129">登录</h2>
      <p class="text-sm" style="color: #86909C">请使用企业账号登录</p>
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

    <p class="mt-6 text-center text-sm" style="color: #86909C">如需账号，请联系企业管理员</p>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCaptchaApi } from '@/api/auth'
import { Message } from '@arco-design/web-vue'

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
    })
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
})
</script>
