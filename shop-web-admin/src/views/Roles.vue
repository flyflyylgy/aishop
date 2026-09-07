<template>
  <div class="page-card">
    <el-card>
      <template #header>
        <h3>角色权限配置 <span style="font-size: 12px; color: #909399; font-weight: normal">（选择角色后勾选权限点保存，该角色用户权限实时生效）</span></h3>
      </template>

      <el-table :data="roles" v-loading="loading" @row-click="selectRole" highlight-current-row>
        <el-table-column label="角色ID" prop="id" width="90" />
        <el-table-column label="角色编码" prop="code" width="180" />
        <el-table-column label="角色名称" prop="name" width="180" />
        <el-table-column label="描述" prop="description" min-width="200" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click.stop="selectRole(row)">配置权限</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="current" style="margin-top: 16px">
      <template #header>
        <h3>权限点配置 - {{ current.name }} <span style="font-size: 12px; color: #909399; font-weight: normal">（已选 {{ checkedIds.length }} 项）</span></h3>
      </template>
      <el-alert v-if="current.code === 'SUPER_ADMIN'" type="info" :closable="false" show-icon
        title="超级管理员默认拥有全部权限（* 通配），此处勾选仅作展示，无需配置。" style="margin-bottom: 14px" />
      <el-checkbox v-model="checkAll" :disabled="current.code === 'SUPER_ADMIN'" @change="onCheckAll" style="margin-bottom: 12px">
        全选
      </el-checkbox>
      <el-checkbox-group v-model="checkedIds" :disabled="current.code === 'SUPER_ADMIN'" @change="onCheckChange">
        <el-checkbox v-for="p in permissions" :key="p.id" :value="p.id" style="width: 300px; margin: 6px 0">
          {{ p.name }} <span style="color: #909399; font-size: 12px">{{ p.code }}</span>
        </el-checkbox>
      </el-checkbox-group>
      <div style="margin-top: 16px">
        <el-button v-if="current.code !== 'SUPER_ADMIN' && hasPerm('shop:role:assign')"
          type="primary" :loading="saving" @click="save">保存权限</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { roleList, permissionList, rolePermissionIds, saveRolePermissions } from '../api'
import { hasPerm } from '../utils/perm'

const roles = ref([])
const permissions = ref([])
const current = ref(null)
const checkedIds = ref([])
const loading = ref(false)
const saving = ref(false)

const checkAll = computed({
  get: () => permissions.value.length > 0 && checkedIds.value.length === permissions.value.length,
  set: () => {}
})

function onCheckAll(val) {
  checkedIds.value = val ? permissions.value.map(p => p.id) : []
}
function onCheckChange(val) {
  // 勾选状态由 v-model 维护
}

async function selectRole(row) {
  current.value = row
  loading.value = true
  try {
    if (row.code === 'SUPER_ADMIN') {
      checkedIds.value = permissions.value.map(p => p.id)
    } else {
      const ids = await rolePermissionIds(row.id)
      checkedIds.value = (ids || []).map(Number)
    }
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await saveRolePermissions(current.value.id, checkedIds.value)
    ElMessage.success('权限保存成功，该角色用户实时生效')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [rs, ps] = await Promise.all([roleList(), permissionList()])
    roles.value = rs || []
    permissions.value = ps || []
  } finally {
    loading.value = false
  }
})
</script>
