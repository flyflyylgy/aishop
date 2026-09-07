<template>
  <div class="container page-bg">
    <el-card>
      <template #header><h3>确认订单</h3></template>

      <el-table :data="items" style="width: 100%">
        <el-table-column label="商品" min-width="300">
          <template #default="{ row }">
            <div style="display: flex; gap: 12px; align-items: center">
              <div class="img-ph" :class="'c' + (row.productId % 4)" style="width: 52px; height: 52px; border-radius: 6px; font-size: 18px">{{ row.productName?.[0] }}</div>
              <span>{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template #default="{ row }">¥{{ money(row.price) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="90" prop="quantity" />
        <el-table-column label="小计" width="120">
          <template #default="{ row }"><span class="price">¥{{ money(row.price * row.quantity) }}</span></template>
        </el-table-column>
      </el-table>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" style="max-width: 520px; margin-top: 24px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="收货地址" prop="receiverAddr">
          <el-input v-model="form.receiverAddr" type="textarea" :rows="2" placeholder="省市区 + 详细地址" />
        </el-form-item>
        <el-form-item label="订单备注">
          <el-input v-model="form.note" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>

      <div class="submit-bar">
        <span>共 <b>{{ totalCount }}</b> 件，应付：<span class="price" style="font-size: 24px">¥{{ money(totalPrice) }}</span></span>
        <el-button type="danger" size="large" :loading="submitting" @click="submitOrder">提交订单</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cartList, orderCreate } from '../api'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const submitting = ref(false)

const items = ref([])
const form = reactive({ receiverName: '', receiverPhone: '', receiverAddr: '', note: '' })
const rules = {
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  receiverAddr: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
}

const money = v => Number(v ?? 0).toFixed(2)
const totalCount = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
const totalPrice = computed(() => items.value.reduce((s, i) => s + i.price * i.quantity, 0))

onMounted(async () => {
  if (route.query.productId) {
    // 立即购买：单商品
    const productId = Number(route.query.productId)
    const quantity = Number(route.query.quantity || 1)
    items.value = [{ productId, quantity, productName: '加载中...', price: 0 }]
    // 复用购物车接口不可行，直接取商品详情
    const { productDetail } = await import('../api')
    const p = await productDetail(productId)
    items.value = [{ productId, quantity, productName: p.name, price: p.price }]
  } else {
    // 来自购物车：仅取勾选条目
    const all = (await cartList()) || []
    items.value = all.filter(i => i.selected === 1)
    if (items.value.length === 0) {
      ElMessage.warning('请先在购物车勾选商品')
      router.push('/cart')
    }
  }
})

async function submitOrder() {
  await formRef.value.validate()
  if (items.value.some(i => !i.productId)) return ElMessage.error('商品信息不完整')
  submitting.value = true
  try {
    const payload = {
      items: items.value.map(i => ({ productId: i.productId, quantity: i.quantity })),
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddr: form.receiverAddr,
      note: form.note
    }
    const idemKey = (crypto.randomUUID ? crypto.randomUUID() : Date.now() + '-' + Math.random().toString(16).slice(2))
    const order = await orderCreate(payload, idemKey)
    ElMessage.success('下单成功')
    router.replace(`/orders?orderNo=${order.orderNo}&pay=1`)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.submit-bar { display: flex; justify-content: flex-end; align-items: center; gap: 20px; margin-top: 16px; padding: 16px; background: #fff8f8; border-radius: 8px; }
</style>
