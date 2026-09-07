<template>
  <div style="padding-bottom: 66px">
    <van-nav-bar title="确认订单" left-arrow @click-left="$router.back()" />

    <!-- 收货地址（有地址时默认选择列表，可切换手动填写） -->
    <van-cell-group v-if="addresses.length > 0" inset style="margin-top: 10px">
      <van-cell title="收货地址">
        <template #right-icon>
          <span style="font-size: 12px; color: #ee0a24" @click="toggleMode">
            {{ mode === 'address' ? '手动填写' : '选择已有地址' }}
          </span>
        </template>
      </van-cell>
      <template v-if="mode === 'address'">
        <van-cell v-for="a in addresses" :key="a.id" clickable @click="selectedAddressId = a.id">
          <template #title>
            <div style="display: flex; align-items: center">
              <van-icon :name="selectedAddressId === a.id ? 'checked' : 'circle'" :color="selectedAddressId === a.id ? '#ee0a24' : '#c8c9cc'" style="margin-right: 8px" />
              <span style="font-size: 14px; font-weight: 600">{{ a.receiverName }}</span>
              <span style="font-size: 12px; color: #969799; margin-left: 8px">{{ a.receiverPhone }}</span>
              <van-tag v-if="a.isDefault === 1" type="danger" style="margin-left: 8px">默认</van-tag>
            </div>
          </template>
          <template #label>
            <span style="font-size: 12px">{{ a.receiverAddr }}</span>
          </template>
        </van-cell>
      </template>
    </van-cell-group>

    <!-- 手动填写收货信息 -->
    <van-cell-group v-show="mode === 'manual'" inset style="margin-top: 10px">
      <van-field v-model="form.receiverName" label="收货人" maxlength="64" placeholder="姓名" />
      <van-field v-model="form.receiverPhone" label="手机号" type="tel" maxlength="11" placeholder="11 位手机号" />
      <van-field v-model="form.receiverAddr" label="收货地址" type="textarea" rows="2" autosize maxlength="255" placeholder="省市区 + 详细地址" />
    </van-cell-group>

    <van-cell-group inset style="margin-top: 10px">
      <van-field v-model="form.note" label="订单备注" placeholder="选填" />
    </van-cell-group>

    <!-- 商品清单 -->
    <van-cell-group inset style="margin-top: 10px">
      <van-cell title="商品清单" :value="`${totalCount} 件`" />
      <div v-for="(it, idx) in items" :key="idx" class="order-item">
        <div class="img-ph" :class="'c' + (it.productId % 5)" style="width: 52px; height: 52px; border-radius: 8px; font-size: 18px; flex-shrink: 0">{{ it.productName?.[0] }}</div>
        <div style="flex: 1; min-width: 0; padding-left: 10px">
          <div style="font-size: 13px">{{ it.productName }}</div>
          <div v-if="it.specValues" style="font-size: 11px; color: #969799; margin-top: 2px">{{ formatSpec(it.specValues) }}</div>
          <div style="font-size: 12px; color: #969799; margin-top: 4px">¥{{ money(it.price) }} × {{ it.quantity }}</div>
        </div>
        <span class="price" style="font-size: 14px">¥{{ money(it.price * it.quantity) }}</span>
      </div>
    </van-cell-group>

    <!-- 优惠券 -->
    <van-cell-group inset style="margin-top: 10px">
      <van-cell title="优惠券" is-link :border="false" @click="showCouponSheet = true">
        <template #value>
          <span v-if="selectedCoupon" style="color: #ee0a24">-¥{{ money(discountYuan) }}</span>
          <span v-else style="color: #969799">{{ usableCoupons.length }} 张可用</span>
        </template>
      </van-cell>
      <van-cell v-if="discountYuan > 0" title="商品合计" :value="'¥' + money(goodsYuan)" />
      <van-cell v-if="discountYuan > 0" title="优惠抵扣" :value="'-¥' + money(discountYuan)" value-class="discount" />
    </van-cell-group>

    <van-action-sheet v-model:show="showCouponSheet" title="选择优惠券" closeable>
      <div style="max-height: 50vh; overflow-y: auto">
        <van-cell
          v-for="(a, i) in couponActions"
          :key="i"
          :title="a.name"
          is-link
          :class="{ chosen: a.historyId === couponHistoryId }"
          @click="onCouponSelect(a)"
        />
      </div>
    </van-action-sheet>

    <van-submit-bar :price="payPrice" button-text="提交订单" :loading="submitting" @submit="submitOrder">
      <template #tip v-if="discountYuan > 0">
        <span>已优惠 ¥{{ money(discountYuan) }}</span>
      </template>
    </van-submit-bar>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { cartList, productDetail, orderCreate, addressList, myCoupons } from '../api'

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const items = ref([])
const addresses = ref([])
const selectedAddressId = ref(null)
const mode = ref('manual') // 'address' 选择已有地址 | 'manual' 手动填写
const form = reactive({ receiverName: '', receiverPhone: '', receiverAddr: '', note: '' })

const money = v => Number(v ?? 0).toFixed(2)
const formatSpec = sv => {
  try {
    const o = JSON.parse(sv)
    return Object.entries(o).map(([k, v]) => `${k}:${v}`).join(' ')
  } catch { return sv }
}
const totalCount = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
const goodsYuan = computed(() => items.value.reduce((s, i) => s + i.price * i.quantity, 0))
const totalPrice = computed(() => Math.round(goodsYuan.value * 100))

// 优惠券
const coupons = ref([])
const couponHistoryId = ref(null)
const showCouponSheet = ref(false)
const usableCoupons = computed(() => coupons.value.filter(c => goodsYuan.value >= Number(c.minPoint || 0)))
const selectedCoupon = computed(() => coupons.value.find(c => c.historyId === couponHistoryId.value))
const discountYuan = computed(() => {
  if (!couponHistoryId.value) return 0
  const c = coupons.value.find(x => x.historyId === couponHistoryId.value)
  if (!c || goodsYuan.value < Number(c.minPoint || 0)) return 0
  let d = c.type === 2 ? Math.round(goodsYuan.value * (100 - c.discount)) / 100 : Number(c.faceValue)
  const max = Math.max(goodsYuan.value - 0.01, 0)
  return Math.min(Math.round(d * 100) / 100, Math.round(max * 100) / 100)
})
const payPrice = computed(() => Math.round(Math.max(goodsYuan.value - discountYuan.value, 0) * 100))
const couponActions = computed(() => [
  { name: '不使用优惠券', historyId: null },
  ...usableCoupons.value.map(c => ({ name: couponLabel(c), historyId: c.historyId }))
])
function couponLabel(c) {
  const rule = c.type === 2 ? (c.discount / 10).toFixed(1) + '折' : '减¥' + money(c.faceValue)
  const cond = Number(c.minPoint) > 0 ? '满' + money(c.minPoint) + '可用' : '无门槛'
  return `${c.name}（${cond}，${rule}）`
}
function onCouponSelect(action) {
  couponHistoryId.value = action.historyId
  showCouponSheet.value = false
}
async function loadCoupons() {
  try {
    const page = await myCoupons({ status: 0, pageNum: 1, pageSize: 50 })
    coupons.value = page.records || []
  } catch { coupons.value = [] }
}

const selectedAddress = computed(() => addresses.value.find(a => a.id === selectedAddressId.value))

onMounted(async () => {
  if (route.query.productId) {
    const productId = Number(route.query.productId)
    const quantity = Number(route.query.quantity || 1)
    const skuId = route.query.skuId ? Number(route.query.skuId) : null
    const p = await productDetail(productId)
    let price = p.price
    let specValues = null
    if (skuId && p.skus) {
      const sku = p.skus.find(s => s.id === skuId)
      if (sku) { price = sku.price; specValues = sku.specValues }
    }
    items.value = [{ productId, skuId, quantity, productName: p.name, price, specValues }]
  } else {
    const all = (await cartList()) || []
    items.value = all.filter(i => i.selected === 1)
    if (items.value.length === 0) {
      showFailToast('请先在购物车勾选商品')
      router.replace('/cart')
    }
  }
  const list = (await addressList()) || []
  if (list.length > 0) {
    addresses.value = list
    selectedAddressId.value = list[0].id // 后端默认地址排最前
    mode.value = 'address'
  }

  loadCoupons()
})

function toggleMode() {
  mode.value = mode.value === 'address' ? 'manual' : 'address'
}

function validate() {
  if (!form.receiverName) return '请输入收货人'
  if (!/^1[3-9]\d{9}$/.test(form.receiverPhone)) return '手机号格式不正确'
  if (!form.receiverAddr) return '请输入收货地址'
  return null
}

async function submitOrder() {
  if (mode.value === 'manual') {
    const err = validate()
    if (err) return showFailToast(err)
  }
  submitting.value = true
  try {
    const payload = {
      items: items.value.map(i => ({ productId: i.productId, skuId: i.skuId, quantity: i.quantity })),
      couponHistoryId: couponHistoryId.value || null
    }
    if (mode.value === 'address' && selectedAddress.value) {
      payload.addressId = selectedAddress.value.id
    } else {
      payload.receiverName = form.receiverName
      payload.receiverPhone = form.receiverPhone
      payload.receiverAddr = form.receiverAddr
    }
    payload.note = form.note
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
