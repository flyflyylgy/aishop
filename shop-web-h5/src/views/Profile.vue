<template>
  <div style="min-height: 100vh; background: #f7f8fa">
    <van-nav-bar title="个人资料" left-arrow @click-left="$router.back()" />

    <van-form @submit="save" style="margin-top: 12px">
      <van-cell-group inset>
        <van-field v-model="form.username" label="用户名" readonly />
        <van-field v-model="form.nickname" label="昵称" placeholder="请输入昵称" maxlength="32" clearable
          :rules="[{ required: true, message: '请输入昵称' }]" />
        <van-field v-model="form.phone" label="手机号" type="tel" maxlength="11" placeholder="11 位手机号" clearable
          :rules="[{ required: true, message: '请输入手机号' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]" />
        <van-field v-model="form.icon" label="头像URL" placeholder="选填，图片地址" clearable />
      </van-cell-group>

      <div style="margin: 20px 16px">
        <van-button round block type="danger" native-type="submit" :loading="loading">保 存</van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { memberInfo, updateProfile, toast } from '../api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ id: null, username: '', nickname: '', phone: '', icon: '' })

async function load() {
  const data = await memberInfo()
  form.id = data.id
  form.username = data.username
  form.nickname = data.nickname || ''
  form.phone = data.phone || ''
  form.icon = data.icon || ''
}

async function save() {
  loading.value = true
  try {
    await updateProfile({
      nickname: form.nickname.trim(),
      phone: form.phone.trim(),
      icon: form.icon.trim() || undefined
    })
    toast('保存成功')
    await load()
    router.back()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
