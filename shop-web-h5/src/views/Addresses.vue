<template>
  <div style="padding-bottom: 80px">
    <van-nav-bar title="收货地址" left-arrow @click-left="$router.back()" />

    <van-empty v-if="list.length === 0" description="暂无收货地址" />

    <div v-for="a in list" :key="a.id" class="addr-card">
      <div class="addr-info">
        <div class="addr-line1">
          <span style="font-size: 15px; font-weight: 600">{{ a.receiverName }}</span>
          <span style="font-size: 13px; color: #969799; margin-left: 10px">{{ a.receiverPhone }}</span>
          <van-tag v-if="a.isDefault === 1" type="danger" style="margin-left: 8px">默认</van-tag>
        </div>
        <div style="font-size: 13px; color: #646566; margin-top: 6px; line-height: 1.5">{{ a.receiverAddr }}</div>
      </div>
      <div class="addr-foot">
        <div style="display: flex; gap: 14px; align-items: center">
          <span class="addr-action" @click="setDefault(a)">
            <van-icon name="certificate" :color="a.isDefault === 1 ? '#ee0a24' : '#969799'" />
            {{ a.isDefault === 1 ? '默认地址' : '设为默认' }}
          </span>
        </div>
        <div style="display: flex; gap: 14px; align-items: center">
          <span class="addr-action" @click="openForm(a)">
            <van-icon name="edit" /> 编辑
          </span>
          <span class="addr-action" @click="remove(a)">
            <van-icon name="delete-o" /> 删除
          </span>
        </div>
      </div>
    </div>

    <div class="submit-safe">
      <van-button round type="danger" block style="font-weight: 600" @click="openForm()">
        <van-icon name="plus" /> 新增收货地址
      </van-button>
    </div>

    <!-- 新增/编辑弹层 -->
    <van-popup v-model:show="formShow" position="bottom" round style="padding: 20px 16px calc(20px + env(safe-area-inset-bottom))">
      <div style="font-size: 16px; font-weight: 600; text-align: center; margin-bottom: 14px">
        {{ editingId ? '编辑收货地址' : '新增收货地址' }}
      </div>
      <van-cell-group inset>
        <van-field v-model="form.receiverName" label="收货人" maxlength="64" placeholder="姓名" clearable />
        <van-field v-model="form.receiverPhone" label="手机号" type="tel" maxlength="11" placeholder="11 位手机号" clearable />
        <van-field v-model="form.receiverAddr" label="收货地址" type="textarea" rows="2" autosize maxlength="255" placeholder="省市区 + 详细地址" />
        <van-cell center title="设为默认地址">
          <template #right-icon>
            <van-switch v-model="form.isDefault" size="22" />
          </template>
        </van-cell>
      </van-cell-group>
      <van-button round type="danger" block style="margin-top: 16px; font-weight: 600" :loading="saving" @click="save">保存</van-button>
    </van-popup>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import { addressList, addressCreate, addressUpdate, addressDelete, addressSetDefault } from '../api'

const list = ref([])
const formShow = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = reactive({ receiverName: '', receiverPhone: '', receiverAddr: '', isDefault: false })

async function load() {
  list.value = (await addressList()) || []
}

function openForm(a) {
  editingId.value = a ? a.id : null
  form.receiverName = a ? a.receiverName : ''
  form.receiverPhone = a ? a.receiverPhone : ''
  form.receiverAddr = a ? a.receiverAddr : ''
  form.isDefault = a ? a.isDefault === 1 : false
  formShow.value = true
}

function validate() {
  if (!form.receiverName.trim()) return '请输入收货人'
  if (form.receiverName.trim().length > 64) return '收货人最长 64 个字符'
  if (!/^1[3-9]\d{9}$/.test(form.receiverPhone)) return '手机号格式不正确'
  if (!form.receiverAddr.trim()) return '请输入收货地址'
  if (form.receiverAddr.trim().length > 255) return '收货地址最长 255 个字符'
  return null
}

async function save() {
  const err = validate()
  if (err) return showFailToast(err)
  saving.value = true
  try {
    const data = {
      receiverName: form.receiverName.trim(),
      receiverPhone: form.receiverPhone.trim(),
      receiverAddr: form.receiverAddr.trim(),
      isDefault: form.isDefault
    }
    if (editingId.value) {
      await addressUpdate(editingId.value, data)
      showSuccessToast('修改成功')
    } else {
      await addressCreate(data)
      showSuccessToast('添加成功')
    }
    formShow.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(a) {
  try {
    await showConfirmDialog({ title: '提示', message: `确定删除「${a.receiverName}」的收货地址？` })
  } catch { return }
  await addressDelete(a.id)
  showSuccessToast('删除成功')
  load()
}

async function setDefault(a) {
  if (a.isDefault === 1) return
  await addressSetDefault(a.id)
  showSuccessToast('已设为默认地址')
  load()
}

onMounted(load)
</script>

<style scoped>
.addr-card { background: #fff; border-radius: 10px; margin: 10px 12px 0; padding: 14px; }
.addr-line1 { display: flex; align-items: center; }
.addr-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; padding-top: 10px; border-top: 1px solid #f2f3f5; }
.addr-action { display: inline-flex; align-items: center; gap: 4px; font-size: 13px; color: #646566; }
.submit-safe { position: fixed; left: 0; right: 0; bottom: 0; padding: 10px 16px calc(10px + env(safe-area-inset-bottom)); background: #f7f8fa; }
</style>
