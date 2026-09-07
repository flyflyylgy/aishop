<template>
  <div class="login-bg">
    <el-card class="login-card">
      <h2 style="text-align: center; margin-bottom: 6px">Cloude Shop 管理后台</h2>
      <p style="text-align: center; color: #909399; margin-bottom: 24px; font-size: 13px">默认账号 admin / admin123（首次登录后请修改）</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div style="display: flex; gap: 10px; width: 100%">
            <el-input v-model="form.captchaCode" placeholder="验证码" :prefix-icon="Key" maxlength="4" @keyup.enter="submit" />
            <img v-if="captchaImg" :src="captchaImg" title="点击刷新" class="captcha-img" @click="loadCaptcha"
              style="width: 120px; height: 40px; border-radius: 4px; cursor: pointer; border: 1px solid #dcdfe6" />
          </div>
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">登 录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { adminLogin, adminCaptcha } from '../api'

const route = useRoute()
const router = useRouter()
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
  const data = await adminCaptcha()
  captchaImg.value = data.img
  form.captchaKey = data.captchaKey
  form.captchaCode = ''
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await adminLogin(form)
    localStorage.setItem('admin_token', data.token)
    localStorage.setItem('admin_name', data.username || form.username)
    localStorage.setItem('admin_perms', JSON.stringify(data.perms || []))
    router.push(route.query.redirect || '/')
  } catch {
    // 登录失败（验证码错误/密码错误/锁定），刷新验证码
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.login-bg { min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1a2a4f 0%, #2b4b8c 100%); }
.login-card { width: 400px; padding: 12px 8px; }
.captcha-img { user-select: none; }
</style>
