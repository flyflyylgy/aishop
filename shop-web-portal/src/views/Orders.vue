<template>
  <div class="container page-bg">
    <el-card>
      <template #header><h3>我的订单</h3></template>

      <el-tabs v-model="activeStatus" @tab-change="onTab">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待付款" name="0" />
        <el-tab-pane label="已付款" name="1" />
        <el-tab-pane label="已发货" name="2" />
        <el-tab-pane label="已完成" name="3" />
        <el-tab-pane label="已关闭" name="4" />
      </el-tabs>

      <el-table :data="orders" style="width: 100%">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding: 8px 24px">
              <div v-for="it in row.items || []" :key="it.id" style="display: flex; align-items: center; gap: 12px; padding: 6px 0">
                <div class="img-ph" :class="'c' + (it.productId % 4)" style="width: 44px; height: 44px; border-radius: 6px; font-size: 16px">{{ it.productName?.[0] }}</div>
                <span style="flex: 1">{{ it.productName }}</span>
                <span>¥{{ money(it.price) }} × {{ it.quantity }}</span>
                <span class="price">¥{{ money(it.totalPrice) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="订单号" prop="orderNo" width="200" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }"><span class="price">¥{{ money(row.payAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.statusDesc || statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="收货信息" min-width="220">
          <template #default="{ row }">{{ row.receiverName }} {{ row.receiverPhone }} · {{ row.receiverAddr }}</template>
        </el-table-column>
        <el-table-column label="下单时间" prop="createTime" width="170">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="danger" size="small" @click="pay(row)">去支付</el-button>
              <el-button size="small" @click="cancel(row)">取消</el-button>
            </template>
            <el-button v-if="row.status === 2" type="success" size="small" @click="confirm(row)">确认收货</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: center; margin-top: 20px">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.pageSize"
          :current-page="query.pageNum" @current-change="p => { query.pageNum = p; load() }" />
      </div>
    </el-card>

    <!-- 模拟支付对话框 -->
    <el-dialog v-model="payDialog" title="模拟支付" width="380px" align-center>
      <div style="text-align: center">
        <div class="img-ph c1" style="width: 120px; height: 120px; border-radius: 16px; margin: 0 auto 16px; font-size: 40px">¥</div>
        <p>订单号：{{ payingOrder?.orderNo }}</p>
        <p class="price" style="font-size: 30px; margin: 12px 0">¥{{ money(payingOrder?.payAmount) }}</p>
        <p style="color: #909399; font-size: 13px">演示环境：点击下方按钮即模拟第三方支付平台支付成功并回调商城</p>
      </div>
      <template #footer>
        <el-button @click="payDialog = false">暂不支付</el-button>
        <el-button type="success" :loading="paying" @click="doPay">模拟支付成功</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderPage, orderCancel, orderConfirm, payCreate, payNotifyMock } from '../api'

const route = useRoute()
const orders = ref([])
const total = ref(0)
const activeStatus = ref('all')
const query = reactive({ pageNum: 1, pageSize: 10 })
const payDialog = ref(false)
const payingOrder = ref(null)
const paying = ref(false)

const money = v => Number(v ?? 0).toFixed(2)
const statusText = s => ({ 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已关闭' }[s] ?? s)
const statusType = s => ({ 0: 'warning', 1: 'primary', 2: 'success', 3: 'info', 4: 'danger' }[s] ?? 'info')
const fmt = t => t ? String(t).replace('T', ' ').slice(0, 19) : ''

async function load() {
  const params = { ...query }
  if (activeStatus.value !== 'all') params.status = Number(activeStatus.value)
  const page = await orderPage(params)
  orders.value = page.records || []
  total.value = Number(page.total || 0)
}

function onTab() { query.pageNum = 1; load() }

async function cancel(row) {
  await orderCancel(row.orderNo)
  ElMessage.success('订单已取消')
  load()
}

async function confirm(row) {
  await orderConfirm(row.id)
  ElMessage.success('确认收货成功')
  load()
}

async function pay(row) {
  const data = await payCreate(row.orderNo)
  payingOrder.value = { ...row, payNo: data.payNo }
  payDialog.value = true
}

async function doPay() {
  paying.value = true
  try {
    await payNotifyMock({
      payNo: payingOrder.value.payNo,
      orderNo: payingOrder.value.orderNo,
      amount: payingOrder.value.payAmount,
      success: true
    })
    ElMessage.success('支付成功！')
    payDialog.value = false
    load()
  } finally {
    paying.value = false
  }
}

onMounted(async () => {
  if (route.query.orderNo) activeStatus.value = 'all'
  await load()
  // 下单成功后自动弹出支付
  if (route.query.pay && route.query.orderNo) {
    const row = orders.value.find(o => o.orderNo === route.query.orderNo)
    if (row && row.status === 0) pay(row)
  }
})
</script>
