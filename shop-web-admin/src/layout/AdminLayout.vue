<template>
  <el-container style="height: 100vh">
    <el-aside width="210px" class="aside">
      <div class="brand">Cloude <span>Shop</span> 后台</div>
      <el-menu :default-active="$route.path" router background-color="#001529" text-color="#b7c0cd"
        active-text-color="#ffffff" style="border-right: none">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header" height="56px">
        <h3 style="flex: 1">{{ $route.meta.title }}</h3>
        <el-dropdown @command="onCommand">
          <span style="cursor: pointer; display: flex; align-items: center; gap: 6px">
            <el-avatar :size="30" style="background: #409eff">{{ adminName[0] }}</el-avatar>
            {{ adminName }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main style="padding: 0; background: #f5f6f8">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="pwdDialog" title="修改密码" width="420px">
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 8 位，含字母和数字" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirm">
        <el-input v-model="pwdForm.confirm" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdDialog = false">取消</el-button>
      <el-button type="primary" :loading="pwdLoading" @click="changePwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminPassword, adminLogout } from '../api'
import { hasPerm } from '../utils/perm'

const router = useRouter()
const menus = router.options.routes[1].children
  .filter(r => !r.meta.perm || hasPerm(r.meta.perm))
  .map(r => ({
    path: '/' + r.path, title: r.meta.title, icon: r.meta.icon
  }))

const adminName = computed(() => localStorage.getItem('admin_name') || 'admin')

const pwdDialog = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{8,}$/, message: '至少 8 位且包含字母和数字', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入', trigger: 'blur' },
    { validator: (r, v, cb) => v === pwdForm.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' }
  ]
}

function onCommand(cmd) {
  if (cmd === 'password') { pwdDialog.value = true }
  if (cmd === 'logout') {
    adminLogout().catch(() => {}).finally(() => {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_name')
      localStorage.removeItem('admin_perms')
      router.push('/login')
    })
  }
}

async function changePwd() {
  await pwdFormRef.value.validate()
  pwdLoading.value = true
  try {
    await adminPassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_name')
    localStorage.removeItem('admin_perms')
    router.push('/login')
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.aside { background: #001529; }
.brand { color: #fff; font-size: 17px; font-weight: 700; text-align: center; padding: 18px 0; }
.brand span { color: #409eff; }
.header { background: #fff; display: flex; align-items: center; box-shadow: 0 2px 8px rgba(0,0,0,.04); padding: 0 20px; }
</style>
