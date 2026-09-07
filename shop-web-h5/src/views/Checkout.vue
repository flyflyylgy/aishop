<template>
  <div style="padding-bottom: 66px">
    <van-nav-bar title="确认订单" left-arrow @click-left="$router.back()" />

    <!-- 收货信息 -->
    <van-cell-group inset style="margin-top: 10px">
      <van-field v-model="form.receiverName" label="收货人" placeholder="姓名" />
      <van-field v-model="form.receiverPhone" label="手机号" type="tel" maxlength="11" placeholder="11 位手机号" />
      <van-field v-model="form.receiverAddr" label="收货地址" type="textarea" rows="2" autosize placeholder="省市区 + 详细地址" />
      <van-field v-model="form.note" label="订单备注" placeholder="选填" />
    </van-cell-group>

    <!-- 商品清单 -->
    <van-cell-group inset style="margin-top: 10px">
      <van-cell title="商品清单" :value="`${totalCount} 件`" />
      <div v-for="(it, idx) in items" :key="idx" class="order-item">
        <div class="img-ph" :class="'c' + (it.productId % 5)" style="width: 52px; height: 52px; border-radius: 8px; font-size: 18px; flex-shrink: 0">{{ it.productName?.[0] }}</div>
        <div style="flex: 1; min-width: 0; padding-left: 10px">
          <div style="font-size: 13px">{{ it.productName }}</div>
          <div style="font-size: 12px; color: #969799; margin-top: 4px">¥{{ money(it.price) }} × {{ it.quantity }}</div>
        </div>
        <span class="price" style="font-size: 14px">¥{{ money(it.price * it.quantity) }}</span>
      </div>
    </van-cell-group>

    <van-submit-bar :price="totalPrice" button-text="提交订单" :loading="submitting" @submit="submitOrder" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { cartList, productDetail, orderCreate } from '../api'

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const items = ref([])
const form = reactive({ receiverName: '', receiverPhone: '', receiverAddr: '', note: '' })

const money = v => Number(v ?? 0).toFixed(2)
const totalCount = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
const totalPrice = computed(() => Math.round(items.value.reduce((s, i) => s + i.price * i.quantity, 0) * 100))

onMounted(async () => {
  if (route.query.productId) {
    const productId = Number(route.query.productId)
    const quantity = Number(route.query.quantity || 1)
    const p = await productDetail(productId)
    items.value = [{ productId, quantity, productName: p.name, price: p.price }]
  } else {
    const all = (await cartList()) || []
    items.value = all.filter(i => i.selected === 1)
    if (items.value.length === 0) {
      showFailToast('请先在购物车勾选商品')
      router.replace('/cart')
    }
  }
})

function validate() {
  if (!form.receiverName) return '请输入收货人'
  if (!/^1\d{10}$/.test(form.receiverPhone)) return '手机号格式不正确'
  if (!form.receiverAddr) return '请输入收货地址'
  return null
}

async function submitOrder() {
  const err = validate()
  if (err) return showFailToast(err)
  submitting.value = true
  try {
    const payload = {
      items: items.value.map(i => ({ productId: i.productId, quantity: i.quantity })),
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddr: form.receiverAddr,
      note: form.note
    }
    const idemKey = crypto.randomUUID ? crypto.randomUUID() : Date.now() + '-' + Math.random().toString(16).slice(2)
    const order = await orderCreate(payload, idemKey)
    showSuccessToast('下单成功')
    router.replace(`/orders?orderNo=${order.orderNo}&pay=1`)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.order-item { display: flex; align-items: center; padding: 10px 16px; }
</style>
