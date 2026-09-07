<template>
  <div class="container page-bg">
    <el-card style="max-width: 460px; margin: 80px auto">
      <template #header><h3>找回密码</h3></template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item>
          <el-button @click="sendCode" :disabled="countdown > 0" :loading="sending">
            {{ countdown > 0 ? `${countdown}s 后重试` : '发送验证码' }}
          </el-button>
        </el-form-item>
        <el-form-item v-if="resetCode" label="验证码">
          <el-alert :title="'演示验证码: ' + resetCode" type="info" :closable="false" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <el-input v-model="form.code" maxlength="6" placeholder="6 位验证码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password maxlength="32" placeholder="8-32 位含字母和数字" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" :loading="saving" @click="reset">重置密码</el-button>
          <el-button @click="$router.push('/login')">返回登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendResetCode, resetPassword } from '../api'

const router = useRouter()
const formRef = ref()
const sending = ref(false)
const saving = ref(false)
const countdown = ref(0)
const resetCode = ref('')
const form = reactive({ username: '', code: '', newPassword: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,32}$/, message: '密码需 8-32 位且含字母和数字', trigger: 'blur' }
  ]
}

let timer = null
async function sendCode() {
  if (!form.username) return ElMessage.warning('请输入用户名')
  sending.value = true
  try {
    const code = await sendResetCode({ username: form.username })
    resetCode.value = code
    ElMessage.success('验证码已发送')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } finally {
    sending.value = false
  }
}

async function reset() {
  await formRef.value.validate()
  saving.value = true
  try {
    await resetPassword({ ...form })
    ElMessage.success('密码重置成功，请用新密码登录')
    router.push('/login')
  } finally {
    saving.value = false
  }
}
</script>
