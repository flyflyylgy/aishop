<template>
  <div class="auth-bg">
    <el-card class="auth-card">
      <h2 style="text-align: center; margin-bottom: 24px">注册账号</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（字母开头，3-20 位）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（选填）" :prefix-icon="Postcard" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码（至少 6 位）" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="confirm">
          <el-input v-model="form.confirm" type="password" show-password placeholder="确认密码" :prefix-icon="Lock" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">注 册</el-button>
        <div style="margin-top: 14px; text-align: center; color: #909399">
          已有账号？<el-link type="primary" @click="$router.push('/login')">去登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard } from '@element-plus/icons-vue'
import { register } from '../api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '', confirm: '' })
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]{2,19}$/, message: '字母开头，3-20 位字母数字下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{8,32}$/, message: '密码需 8-32 位且包含字母和数字', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: (r, v, cb) => v === form.password ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' }
  ]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await register({ username: form.username, password: form.password, nickname: form.nickname || form.username })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-bg { min-height: calc(100vh - 120px); display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #e0eaff 0%, #f5f6f8 100%); }
.auth-card { width: 400px; padding: 12px 8px; }
</style>
