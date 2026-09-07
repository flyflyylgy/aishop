<template>
  <div style="padding-bottom: 30px">
    <van-nav-bar title="我的订单" left-arrow @click-left="$router.back()" />

    <van-tabs v-model:active="activeTab" sticky color="#ee0a24" @change="load">
      <van-tab title="全部" name="all" />
      <van-tab title="待付款" name="0" />
      <van-tab title="已付款" name="1" />
      <van-tab title="已发货" name="2" />
      <van-tab title="已完成" name="3" />
      <van-tab title="已关闭" name="4" />
    </van-tabs>

    <van-empty v-if="orders.length === 0" description="暂无订单" />

    <div v-for="o in orders" :key="o.orderNo" class="order-card">
      <div class="order-head">
        <span style="font-size: 12px; color: #969799">{{ o.orderNo }}</span>
        <van-tag :type="{ 0: 'warning', 1: 'primary', 2: 'success', 3: 'default', 4: 'danger' }[o.status]">
          {{ o.statusDesc || { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已关闭' }[o.status] }}
        </van-tag>
      </div>
      <div v-for="it in o.items || []" :key="it.id" class="order-item">
        <div class="img-ph" :class="'c' + (it.productId % 5)" style="width: 48px; height: 48px; border-radius: 8px; font-size: 16px; flex-shrink: 0">{{ it.productName?.[0] }}</div>
        <div style="flex: 1; min-width: 0; padding-left: 10px">
          <div style="font-size: 13px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis">{{ it.productName }}</div>
          <div style="font-size: 11px; color: #969799; margin-top: 3px">¥{{ money(it.price) }} × {{ it.quantity }}</div>
        </div>
        <span class="price" style="font-size: 13px">¥{{ money(it.totalPrice) }}</span>
      </div>
      <div class="order-foot">
        <span>实付：<span class="price" style="font-size: 16px">¥{{ money(o.payAmount) }}</span></span>
        <div style="display: flex; gap: 8px">
          <template v-if="o.status === 0">
            <van-button plain round size="small" @click="cancel(o)">取消订单</van-button>
            <van-button round size="small" type="danger" @click="pay(o)">去支付</van-button>
          </template>
          <van-button v-if="o.status === 2" round size="small" type="success" @click="confirm(o)">确认收货</van-button>
        </div>
      </div>
    </div>

    <!-- 模拟支付弹窗 -->
    <van-dialog v-model:show="payShow" title="模拟支付" show-cancel-cancel confirm-button-text="模拟支付成功" cancel-button-text="暂不支付"
      :show-cancel-button="true" @confirm="doPay">
      <div style="text-align: center; padding: 10px 0 18px">
        <div class="img-ph c1" style="width: 84px; height: 84px; border-radius: 14px; margin: 0 auto 10px; font-size: 30px">¥</div>
        <div class="price" style="font-size: 26px">¥{{ money(payingOrder?.payAmount) }}</div>
        <div style="font-size: 11px; color: #969799; margin-top: 6px; padding: 0 30px">演示环境：确认后模拟第三方支付成功并回调商城</div>
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showSuccessToast } from 'vant'
import { orderPage, orderCancel, orderConfirm, payCreate, payNotifyMock } from '../api'

const route = useRoute()
const orders = ref([])
const activeTab = ref('all')
const payShow = ref(false)
const payingOrder = ref(null)

const money = v => Number(v ?? 0).toFixed(2)

async function load() {
  const params = { pageNum: 1, pageSize: 10 }
  if (activeTab.value !== 'all') params.status = Number(activeTab.value)
  const page = await orderPage(params)
  orders.value = page.records || []
}

async function cancel(o) {
  await orderCancel(o.orderNo)
  showSuccessToast('订单已取消')
  load()
}

async function confirm(o) {
  await orderConfirm(o.id)
  showSuccessToast('确认收货成功')
  load()
}

async function pay(o) {
  const data = await payCreate(o.orderNo)
  payingOrder.value = { ...o, payNo: data.payNo }
  payShow.value = true
}

async function doPay() {
  await payNotifyMock({
    payNo: payingOrder.value.payNo,
    orderNo: payingOrder.value.orderNo,
    amount: payingOrder.value.payAmount,
    success: true
  })
  showSuccessToast('支付成功！')
  load()
}

onMounted(async () => {
  await load()
  if (route.query.pay && route.query.orderNo) {
    const row = orders.value.find(o => o.orderNo === route.query.orderNo)
    if (row && row.status === 0) pay(row)
  }
})
</script>

<style scoped>
.order-card { background: #fff; border-radius: 10px; margin: 10px 12px 0; padding: 12px; }
.order-head { display: flex; justify-content: space-between; align-items: center; padding-bottom: 8px; border-bottom: 1px solid #f2f3f5; }
.order-item { display: flex; align-items: center; padding: 10px 0 0; }
.order-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; padding-top: 10px; border-top: 1px solid #f2f3f5; }
</style>
