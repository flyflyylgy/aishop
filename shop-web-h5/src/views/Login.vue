<template>
  <div class="auth-bg">
    <div style="text-align: center; margin-bottom: 28px">
      <div class="img-ph c1" style="width: 64px; height: 64px; border-radius: 16px; margin: 0 auto 10px; font-size: 28px">C</div>
      <div style="font-size: 20px; font-weight: 700">Cloude Shop 云上商城</div>
      <div style="font-size: 12px; color: #969799; margin-top: 4px">企业级电商 · H5 移动端</div>
    </div>

    <van-form @submit="submit">
      <van-cell-group inset>
        <van-field v-model="form.username" name="username" label="用户名" placeholder="用户名"
          :rules="[{ required: true, message: '请输入用户名' }]" />
        <van-field v-model="form.password" type="password" name="password" label="密码" placeholder="密码"
          :rules="[{ required: true, message: '请输入密码' }]" />
        <van-field v-model="form.captchaCode" name="captchaCode" label="验证码" placeholder="验证码" maxlength="4"
          :rules="[{ required: true, message: '请输入验证码' }]">
          <template #button>
            <img v-if="captchaImg" :src="captchaImg" @click="loadCaptcha"
              style="width: 80px; height: 32px; border-radius: 4px; cursor: pointer; border: 1px solid #ebedf0" />
          </template>
        </van-field>
      </van-cell-group>
      <div style="margin: 20px 16px">
        <van-button round block type="danger" native-type="submit" :loading="loading">登 录</van-button>
        <div style="display: flex; justify-content: space-between; margin-top: 14px; font-size: 13px; color: #969799">
          <span style="color: var(--brand)" @click="$router.push('/forgot-password')">忘记密码</span>
          <span>还没有账号？<span style="color: var(--brand)" @click="$router.push('/register')">立即注册</span></span>
        </div>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, captcha, toast } from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const captchaImg = ref('')
const form = reactive({ username: '', password: '', captchaKey: '', captchaCode: '' })

async function loadCaptcha() {
  const data = await captcha()
  captchaImg.value = data.img
  form.captchaKey = data.captchaKey
  form.captchaCode = ''
}

async function submit() {
  loading.value = true
  try {
    const data = await login(form)
    localStorage.setItem('token', data.token)
    localStorage.setItem('member', JSON.stringify({ id: data.userId, username: data.username }))
    toast('登录成功')
    router.replace(route.query.redirect || '/')
  } catch {
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.auth-bg { min-height: 100vh; display: flex; flex-direction: column; justify-content: center;
  background: linear-gradient(160deg, #fff0f0 0%, #f7f8fa 40%); padding-bottom: 60px; }
</style>
