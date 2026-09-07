<template>
  <div class="page-card">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="销售额(元)" :value="stats.totalSales" :precision="2">
            <template #prefix><el-icon color="#f56c6c"><Money /></el-icon></template>
          </el-statistic>
          <div style="color: #909399; font-size: 12px; margin-top: 6px">今日 ¥{{ Number(stats.todaySales || 0).toFixed(2) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="订单总数" :value="stats.orderTotal">
            <template #prefix><el-icon color="#67c23a"><List /></el-icon></template>
          </el-statistic>
          <div style="color: #909399; font-size: 12px; margin-top: 6px">今日新增 {{ stats.todayOrders }} 笔</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="会员总数" :value="stats.memberTotal">
            <template #prefix><el-icon color="#409eff"><User /></el-icon></template>
          </el-statistic>
          <div style="color: #909399; font-size: 12px; margin-top: 6px">注册用户</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="待发货" :value="stats.orderToShip">
            <template #prefix><el-icon color="#e6a23c"><Van /></el-icon></template>
          </el-statistic>
          <div style="color: #909399; font-size: 12px; margin-top: 6px">已付款待发出</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="商品总数" :value="stats.productTotal">
            <template #prefix><el-icon color="#409eff"><Goods /></el-icon></template>
          </el-statistic>
          <div style="color: #909399; font-size: 12px; margin-top: 6px">上架 {{ stats.productOnSale }} / 下架 {{ stats.productOffSale }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="待付款" :value="stats.orderPending">
            <template #prefix><el-icon color="#e6a23c"><Timer /></el-icon></template>
          </el-statistic>
          <div style="color: #909399; font-size: 12px; margin-top: 6px">未支付订单</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="已发货" :value="stats.orderShipped">
            <template #prefix><el-icon color="#67c23a"><Van /></el-icon></template>
          </el-statistic>
          <div style="color: #909399; font-size: 12px; margin-top: 6px">待确认收货</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="已完成" :value="stats.orderDone">
            <template #prefix><el-icon color="#909399"><CircleCheck /></el-icon></template>
          </el-statistic>
          <div style="color: #909399; font-size: 12px; margin-top: 6px">已关闭 {{ stats.orderClosed }} 笔</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header><h3>最新订单</h3></template>
      <el-table :data="latestOrders" size="small">
        <el-table-column label="订单号" prop="orderNo" width="210" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }"><span class="price">¥{{ Number(row.payAmount).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="收货人" width="120" prop="receiverName" />
        <el-table-column label="下单时间" prop="createTime">
          <template #default="{ row }">{{ String(row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Money, List, User, Van, Goods, Timer, CircleCheck } from '@element-plus/icons-vue'
import { statsDashboard } from '../api'

const stats = reactive({})
const latestOrders = ref([])

const STATUS_MAP = { 0: ['warning', '待付款'], 1: ['primary', '已付款'], 2: ['success', '已发货'], 3: ['info', '已完成'], 4: ['danger', '已关闭'] }
const statusType = s => STATUS_MAP[s]?.[0] || 'info'
const statusText = s => STATUS_MAP[s]?.[1] || '未知'

onMounted(async () => {
  const data = await statsDashboard()
  Object.assign(stats, data)
  latestOrders.value = data.latestOrders || []
})
</script>
