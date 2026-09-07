<template>
  <div class="auth-bg">
    <van-nav-bar title="注册账号" left-arrow @click-left="$router.back()" />
    <van-form @submit="submit" style="margin-top: 20px">
      <van-cell-group inset>
        <van-field v-model="form.username" label="用户名" placeholder="字母开头，3-20 位"
          :rules="[{ required: true, message: '请输入用户名' }, { pattern: /^[a-zA-Z][a-zA-Z0-9_]{2,19}$/, message: '字母开头，3-20 位字母数字下划线' }]" />
        <van-field v-model="form.nickname" label="昵称" placeholder="选填" />
        <van-field v-model="form.password" type="password" label="密码" placeholder="8-32位，含字母和数字"
          :rules="[{ required: true, message: '请输入密码' }, { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{8,32}$/, message: '密码需 8-32 位且包含字母和数字' }]" />
        <van-field v-model="form.confirm" type="password" label="确认密码" placeholder="再次输入密码"
          :rules="[{ required: true, message: '请再次输入密码' }, { validator: v => v === form.password, message: '两次密码不一致' }]" />
      </van-cell-group>
      <div style="margin: 20px 16px">
        <van-button round block type="danger" native-type="submit" :loading="loading">注 册</van-button>
        <div style="text-align: center; margin-top: 14px; font-size: 13px; color: #969799">
          已有账号？<span style="color: var(--brand)" @click="$router.push('/login')">去登录</span>
        </div>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register, toast } from '../api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '', confirm: '' })

async function submit() {
  loading.value = true
  try {
    await register({ username: form.username, password: form.password, nickname: form.nickname || form.username })
    toast('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-bg { min-height: 100vh; background: linear-gradient(160deg, #fff0f0 0%, #f7f8fa 40%); }
</style>
