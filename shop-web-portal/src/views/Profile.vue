<template>
  <div class="container page-bg">
    <el-card style="max-width: 600px; margin: 0 auto">
      <template #header><h3>个人资料</h3></template>

      <div class="profile-avatar">
        <el-avatar :size="72" style="background: #409eff; font-size: 28px">{{ form.nickname?.[0] || form.username?.[0] }}</el-avatar>
      </div>

      <el-form ref="formRef" :model="form" label-width="90px" style="margin-top: 16px">
        <el-form-item label="用户名">
          <el-input :model-value="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="32" placeholder="昵称" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="头像 URL">
          <el-input v-model="form.icon" maxlength="255" placeholder="选填，图片 URL" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" :loading="saving" @click="save">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { memberInfo, updateProfile } from '../api'

const formRef = ref()
const saving = ref(false)
const form = reactive({ username: '', nickname: '', phone: '', icon: '' })

onMounted(async () => {
  const info = await memberInfo()
  form.username = info.username
  form.nickname = info.nickname || ''
  form.phone = info.phone || ''
  form.icon = info.icon || ''
})

async function save() {
  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname,
      phone: form.phone,
      icon: form.icon
    })
    ElMessage.success('修改成功')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-avatar { text-align: center; }
</style>
