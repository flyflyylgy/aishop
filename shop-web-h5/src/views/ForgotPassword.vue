<template>
  <div class="auth-bg">
    <van-nav-bar title="找回密码" left-arrow @click-left="$router.back()" />

    <van-form @submit="submit" style="margin-top: 20px">
      <van-cell-group inset>
        <van-field v-model="form.username" label="用户名" placeholder="请输入用户名" clearable
          :rules="[{ required: true, message: '请输入用户名' }]" />

        <van-field v-model="form.code" label="验证码" placeholder="请输入验证码" maxlength="6" clearable
          :rules="[{ required: true, message: '请输入验证码' }]">
          <template #button>
            <van-button size="small" type="primary" :loading="sending" :disabled="countdown > 0" @click.prevent="sendCode">
              {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
            </van-button>
          </template>
        </van-field>

        <van-field v-model="form.newPassword" type="password" label="新密码" placeholder="8-32位，含字母和数字" clearable
          :rules="[{ required: true, message: '请输入新密码' }, { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{8,32}$/, message: '密码需 8-32 位且包含字母和数字' }]" />
      </van-cell-group>

      <div v-if="demoCode" style="margin: 12px 16px 0; padding: 10px 12px; background: #fff7e6; border-radius: 6px; font-size: 13px; color: #ff976a">
        演示环境验证码：<span style="font-weight: 700; color: #ee0a24">{{ demoCode }}</span>
      </div>

      <div style="margin: 20px 16px">
        <van-button round block type="danger" native-type="submit" :loading="loading">重置密码</van-button>
        <div style="text-align: center; margin-top: 14px; font-size: 13px; color: #969799">
          想起密码了？<span style="color: var(--brand)" @click="$router.push('/login')">返回登录</span>
        </div>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { sendResetCode, resetPassword, toast } from '../api'

const router = useRouter()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const demoCode = ref('')
const form = reactive({ username: '', code: '', newPassword: '' })

function startCountdown() {
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function sendCode() {
  if (!form.username.trim()) {
    return toast('请输入用户名')
  }
  sending.value = true
  try {
    const code = await sendResetCode({ username: form.username.trim() })
    demoCode.value = typeof code === 'string' ? code : ''
    startCountdown()
    toast('验证码已发送')
  } finally {
    sending.value = false
  }
}

async function submit() {
  loading.value = true
  try {
    await resetPassword({
      username: form.username.trim(),
      code: form.code.trim(),
      newPassword: form.newPassword
    })
    toast('密码已重置，请登录')
    router.replace('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-bg { min-height: 100vh; background: linear-gradient(160deg, #fff0f0 0%, #f7f8fa 40%); }
</style>
