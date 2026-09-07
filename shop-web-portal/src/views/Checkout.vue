<template>
  <div class="container page-bg">
    <el-card>
      <template #header><h3>确认订单</h3></template>

      <el-table :data="items" style="width: 100%">
        <el-table-column label="商品" min-width="300">
          <template #default="{ row }">
            <div style="display: flex; gap: 12px; align-items: center">
              <div class="img-ph" :class="'c' + (row.productId % 4)" style="width: 52px; height: 52px; border-radius: 6px; font-size: 18px">{{ row.productName?.[0] }}</div>
              <div>
                <div>{{ row.productName }}</div>
                <div v-if="row.specValues" style="color: #909399; font-size: 12px">{{ formatSpec(row.specValues) }}</div>
              </div>
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

      <div class="addr-section">
        <h4 style="margin: 0 0 12px">收货地址</h4>
        <el-radio-group v-if="addresses.length > 0 && !useManualAddr" v-model="selectedAddrId" class="addr-radios">
          <el-radio v-for="a in addresses" :key="a.id" :value="a.id" border class="addr-radio">
            <span class="addr-line1">
              {{ a.receiverName }} {{ a.receiverPhone }}
              <el-tag v-if="a.isDefault === 1" type="danger" size="small">默认</el-tag>
            </span>
            <span class="addr-line2">{{ a.receiverAddr }}</span>
          </el-radio>
        </el-radio-group>
        <el-empty v-else description="暂无地址，请手动填写" :image-size="48" style="padding: 0" />
        <div style="margin-top: 8px">
          <el-button link type="primary" @click="toggleAddrMode">
            {{ useManualAddr ? '← 使用地址簿' : '使用新地址（手动填写）' }}
          </el-button>
          <el-button link type="primary" @click="router.push('/addresses')">管理地址</el-button>
        </div>
      </div>

      <el-form v-if="useManualAddr" ref="formRef" :model="form" :rules="rules" label-width="90px" style="max-width: 520px; margin-top: 16px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="收货地址" prop="receiverAddr">
          <el-input v-model="form.receiverAddr" type="textarea" :rows="2" placeholder="省市区 + 详细地址" />
        </el-form-item>
      </el-form>

      <el-form label-width="90px" style="max-width: 520px; margin-top: 8px">
        <el-form-item label="订单备注">
          <el-input v-model="form.note" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>

      <div class="coupon-bar">
        <span class="c-label">优惠券</span>
        <el-select v-model="couponHistoryId" placeholder="不使用优惠券" clearable style="width: 420px">
          <el-option v-for="c in usableCoupons" :key="c.historyId" :value="c.historyId"
            :label="couponLabel(c)" />
        </el-select>
        <el-button link type="primary" @click="router.push('/coupons')">去领券</el-button>
        <span v-if="couponDiscount > 0" style="margin-left: auto; color: #f56c6c; font-weight: 600">
          已优惠 ¥{{ money(couponDiscount) }}
        </span>
      </div>

      <div class="submit-bar">
        <div style="text-align: right; line-height: 1.9">
          <div>共 <b>{{ totalCount }}</b> 件，商品总额：¥{{ money(totalPrice) }}</div>
          <div v-if="couponDiscount > 0" style="color: #f56c6c">优惠抵扣：-¥{{ money(couponDiscount) }}</div>
          <div style="font-size: 16px">应付：<span class="price" style="font-size: 24px">¥{{ money(payAmount) }}</span></div>
        </div>
        <el-button type="danger" size="large" :loading="submitting" @click="submitOrder">提交订单</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cartList, orderCreate, addressList, myCoupons } from '../api'

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

// 地址簿
const addresses = ref([])
const selectedAddrId = ref(null)
const useManualAddr = ref(false)

const money = v => Number(v ?? 0).toFixed(2)
const formatSpec = sv => {
  try {
    const o = JSON.parse(sv)
    return Object.entries(o).map(([k, v]) => `${k}:${v}`).join(' ')
  } catch { return sv }
}
const totalCount = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
const totalPrice = computed(() => items.value.reduce((s, i) => s + i.price * i.quantity, 0))

// 优惠券
const coupons = ref([])
const couponHistoryId = ref(null)
const usableCoupons = computed(() => coupons.value.filter(c => totalPrice.value >= Number(c.minPoint || 0)))
const couponDiscount = computed(() => {
  if (!couponHistoryId.value) return 0
  const c = coupons.value.find(x => x.historyId === couponHistoryId.value)
  if (!c || totalPrice.value < Number(c.minPoint || 0)) return 0
  let d = c.type === 2
    ? Math.round(totalPrice.value * (100 - c.discount)) / 100
    : Number(c.faceValue)
  const max = Math.max(totalPrice.value - 0.01, 0)
  return Math.min(Math.round(d * 100) / 100, Math.round(max * 100) / 100)
})
const payAmount = computed(() => Math.max(totalPrice.value - couponDiscount.value, 0))

async function loadCoupons() {
  try {
    const page = await myCoupons({ status: 0, pageNum: 1, pageSize: 50 })
    coupons.value = page.records || []
  } catch { coupons.value = [] }
}

const couponLabel = c => {
  const rule = c.type === 2 ? (c.discount / 10).toFixed(1) + '折' : '减¥' + money(c.faceValue)
  const cond = Number(c.minPoint) > 0 ? '满' + money(c.minPoint) + '可用' : '无门槛'
  return `${c.name}（${cond}，${rule}）`
}

function toggleAddrMode() {
  useManualAddr.value = !useManualAddr.value
}

onMounted(async () => {
  // 加载地址簿，默认选中默认地址
  const list = (await addressList()) || []
  addresses.value = list
  if (list.length > 0) {
    selectedAddrId.value = (list.find(a => a.isDefault === 1) || list[0]).id
  } else {
    useManualAddr.value = true
  }

  if (route.query.productId) {
    // 立即购买：单商品
    const productId = Number(route.query.productId)
    const quantity = Number(route.query.quantity || 1)
    const skuId = route.query.skuId ? Number(route.query.skuId) : null
    items.value = [{ productId, skuId, quantity, productName: '加载中...', price: 0 }]
    const { productDetail } = await import('../api')
    const p = await productDetail(productId)
    // 多规格商品从选中 SKU 取价
    let price = p.price
    let specValues = null
    if (skuId && p.skus) {
      const sku = p.skus.find(s => s.id === skuId)
      if (sku) { price = sku.price; specValues = sku.specValues }
    }
    items.value = [{ productId, skuId, quantity, productName: p.name, price, specValues }]
  } else {
    // 来自购物车：仅取勾选条目
    const all = (await cartList()) || []
    items.value = all.filter(i => i.selected === 1)
    if (items.value.length === 0) {
      ElMessage.warning('请先在购物车勾选商品')
      router.push('/cart')
    }
  }

  loadCoupons()
})

async function submitOrder() {
  if (items.value.some(i => !i.productId)) return ElMessage.error('商品信息不完整')
  if (!useManualAddr.value && selectedAddrId.value) {
    // 地址簿下单
  } else {
    await formRef.value.validate()
  }
  submitting.value = true
  try {
    const payload = {
      items: items.value.map(i => ({ productId: i.productId, skuId: i.skuId, quantity: i.quantity })),
      note: form.note,
      couponHistoryId: couponHistoryId.value || null
    }
    if (!useManualAddr.value && selectedAddrId.value) {
      payload.addressId = selectedAddrId.value
    } else {
      payload.receiverName = form.receiverName
      payload.receiverPhone = form.receiverPhone
      payload.receiverAddr = form.receiverAddr
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
.coupon-bar { display: flex; align-items: center; gap: 10px; margin-top: 8px; padding: 12px 0; border-top: 1px dashed #ebeef5; }
.c-label { font-weight: 600; }
.addr-radios { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 10px; width: 100%; }
.addr-radio { height: auto; padding: 10px 14px; margin-right: 0 !important; }
.addr-line1 { display: block; font-weight: 600; }
.addr-line2 { display: block; color: #909399; font-size: 13px; white-space: normal; }
</style>
