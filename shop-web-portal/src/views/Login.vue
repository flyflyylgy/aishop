<template>
  <div class="auth-bg">
    <el-card class="auth-card">
      <h2 style="text-align: center; margin-bottom: 24px">登录 Cloude Shop</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div style="display: flex; gap: 10px; width: 100%">
            <el-input v-model="form.captchaCode" placeholder="验证码" :prefix-icon="Key" maxlength="4" @keyup.enter="submit" />
            <img v-if="captchaImg" :src="captchaImg" title="点击刷新" @click="loadCaptcha"
              style="width: 120px; height: 40px; border-radius: 4px; cursor: pointer; border: 1px solid #dcdfe6" />
          </div>
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">登 录</el-button>
        <div style="margin-top: 14px; text-align: center; color: #909399">
          还没有账号？<el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { login, captcha } from '../api'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const formRef = ref()
const loading = ref(false)
const captchaImg = ref('')
const form = reactive({ username: '', password: '', captchaKey: '', captchaCode: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function loadCaptcha() {
  const data = await captcha()
  captchaImg.value = data.img
  form.captchaKey = data.captchaKey
  form.captchaCode = ''
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await login(form)
    user.setLogin(data.token, { id: data.userId, username: data.username })
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } catch {
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.auth-bg { min-height: calc(100vh - 120px); display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #e0eaff 0%, #f5f6f8 100%); }
.auth-card { width: 400px; padding: 12px 8px; }
</style>
