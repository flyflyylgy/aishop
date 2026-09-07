<template>
  <div class="container page-bg">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3>收货地址</h3>
          <el-button type="danger" @click="openDialog()">新增地址</el-button>
        </div>
      </template>

      <el-empty v-if="addresses.length === 0" description="还没有收货地址" />

      <div v-else class="addr-list">
        <div v-for="a in addresses" :key="a.id" class="addr-item" :class="{ active: a.isDefault === 1 }">
          <div class="addr-main">
            <span class="addr-name">{{ a.receiverName }}</span>
            <span class="addr-phone">{{ a.receiverPhone }}</span>
            <el-tag v-if="a.isDefault === 1" type="danger" size="small" style="margin-left: 8px">默认</el-tag>
          </div>
          <div class="addr-detail">{{ a.receiverAddr }}</div>
          <div class="addr-ops">
            <el-button v-if="a.isDefault !== 1" link type="primary" @click="setDefault(a)">设为默认</el-button>
            <el-button link type="primary" @click="openDialog(a)">编辑</el-button>
            <el-popconfirm title="确定删除该地址？" @confirm="remove(a)">
              <template #reference><el-button link type="danger">删除</el-button></template>
            </el-popconfirm>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑地址' : '新增地址'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" maxlength="64" placeholder="收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="收货地址" prop="receiverAddr">
          <el-input v-model="form.receiverAddr" type="textarea" :rows="2" maxlength="255" placeholder="省市区 + 详细地址" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { addressList, addressCreate, addressUpdate, addressDelete, addressSetDefault } from '../api'

const addresses = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ receiverName: '', receiverPhone: '', receiverAddr: '', isDefault: false })
const rules = {
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  receiverAddr: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
}

async function load() {
  addresses.value = (await addressList()) || []
}

function openDialog(addr) {
  editingId.value = addr ? addr.id : null
  if (addr) {
    form.receiverName = addr.receiverName
    form.receiverPhone = addr.receiverPhone
    form.receiverAddr = addr.receiverAddr
    form.isDefault = addr.isDefault === 1
  } else {
    form.receiverName = ''
    form.receiverPhone = ''
    form.receiverAddr = ''
    form.isDefault = false
  }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await addressUpdate(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await addressCreate({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function setDefault(a) {
  await addressSetDefault(a.id)
  ElMessage.success('已设为默认地址')
  await load()
}

async function remove(a) {
  await addressDelete(a.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.addr-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.addr-item { border: 1px solid #e4e7ed; border-radius: 8px; padding: 16px; }
.addr-item.active { border-color: var(--el-color-danger); background: #fff8f8; }
.addr-main { display: flex; align-items: center; margin-bottom: 8px; }
.addr-name { font-weight: 600; margin-right: 12px; }
.addr-phone { color: #909399; }
.addr-detail { color: #606266; font-size: 14px; line-height: 1.5; min-height: 42px; }
.addr-ops { margin-top: 8px; display: flex; justify-content: flex-end; }
</style>
