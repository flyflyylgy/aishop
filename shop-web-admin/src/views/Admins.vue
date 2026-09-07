<template>
  <div class="page-card">
    <el-card>
      <div class="table-toolbar">
        <el-input v-model="query.keyword" placeholder="用户名/昵称" clearable style="width: 220px" @keyup.enter="load" @clear="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button v-if="hasPerm('shop:admin:create')" type="success" @click="openCreate">新增管理员</el-button>
      </div>

      <el-table :data="list" v-loading="loading">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="用户名" prop="username" width="150" />
        <el-table-column label="昵称" prop="nickName" width="150" />
        <el-table-column label="角色" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="rn in row.roleNames || []" :key="rn" size="small" style="margin-right: 6px">{{ rn }}</el-tag>
            <span v-if="!row.roleNames?.length" style="color: #c0c4cc">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPerm('shop:admin:assign')" size="small" type="primary" @click="openAssign(row)">分配角色</el-button>
            <el-button v-if="hasPerm('shop:admin:update')" size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button v-if="hasPerm('shop:admin:update')" size="small" type="warning" @click="openResetPwd(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: center; margin-top: 16px">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          :page-size="query.pageSize" :current-page="query.pageNum"
          @current-change="p => { query.pageNum = p; load() }" />
      </div>
    </el-card>

    <!-- 新增管理员 -->
    <el-dialog v-model="createDialog" title="新增管理员" width="440px">
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="至少 3 个字符" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickName">
          <el-input v-model="createForm.nickName" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="8-32位，包含字母和数字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="assignDialog" :title="`分配角色 - ${current?.username || ''}`" width="420px">
      <el-checkbox-group v-model="assignRoleIds">
        <el-checkbox v-for="r in roles" :key="r.id" :value="r.id" style="display: block; margin: 8px 0">
          {{ r.name }} <span style="color: #909399">（{{ r.code }}）</span>
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAssign">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="pwdDialog" :title="`重置密码 - ${current?.username || ''}`" width="420px">
      <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="8-32位，包含字母和数字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminUserPage, adminUserCreate, adminUserUpdate, adminUserResetPwd,
  adminUserAssignRoles, roleList
} from '../api'
import { hasPerm } from '../utils/perm'

const fmt = t => t ? String(t).replace('T', ' ').slice(0, 19) : ''

const list = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const roles = ref([])
const query = reactive({ keyword: '', pageNum: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const page = await adminUserPage(query)
    list.value = page.records || []
    total.value = Number(page.total || 0)
  } finally {
    loading.value = false
  }
}

// 新增
const createDialog = ref(false)
const createRef = ref()
const createForm = reactive({ username: '', nickName: '', password: '' })
const createRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{8,32}$/, message: '密码需 8-32 位且包含字母和数字', trigger: 'blur' }
  ]
}
function openCreate() {
  createForm.username = ''; createForm.nickName = ''; createForm.password = ''
  createDialog.value = true
}
async function submitCreate() {
  await createRef.value.validate()
  saving.value = true
  try {
    await adminUserCreate({ ...createForm })
    ElMessage.success('新增成功')
    createDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

// 分配角色
const assignDialog = ref(false)
const current = ref(null)
const assignRoleIds = ref([])
async function openAssign(row) {
  current.value = row
  assignRoleIds.value = (row.roleIds || []).map(Number)
  assignDialog.value = true
}
async function submitAssign() {
  saving.value = true
  try {
    await adminUserAssignRoles(current.value.id, assignRoleIds.value)
    ElMessage.success('角色分配成功，实时生效')
    assignDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

// 启用/禁用
async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认${target === 0 ? '禁用' : '启用'}管理员「${row.username}」？`, '提示', { type: 'warning' })
  await adminUserUpdate(row.id, { status: target })
  ElMessage.success('操作成功')
  load()
}

// 重置密码
const pwdDialog = ref(false)
const pwdRef = ref()
const pwdForm = reactive({ newPassword: '' })
const pwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{8,32}$/, message: '密码需 8-32 位且包含字母和数字', trigger: 'blur' }
  ]
}
function openResetPwd(row) {
  current.value = row
  pwdForm.newPassword = ''
  pwdDialog.value = true
}
async function submitResetPwd() {
  await pwdRef.value.validate()
  saving.value = true
  try {
    await adminUserResetPwd(current.value.id, pwdForm.newPassword)
    ElMessage.success('密码重置成功')
    pwdDialog.value = false
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  load()
  if (hasPerm('shop:admin:assign')) {
    try { roles.value = (await roleList()) || [] } catch { /* 无权限忽略 */ }
  }
})
</script>
