<template>
  <div class="page-card">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="商品总数" :value="stats.productTotal">
            <template #prefix><el-icon color="#409eff"><Goods /></el-icon></template>
          </el-statistic>
          <div style="color: #909399; font-size: 12px; margin-top: 6px">上架 {{ stats.productOnSale }} / 下架 {{ stats.productOffSale }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="订单总数" :value="stats.orderTotal">
            <template #prefix><el-icon color="#67c23a"><List /></el-icon></template>
          </el-statistic>
          <div style="color: #909399; font-size: 12px; margin-top: 6px">待付款 {{ stats.orderPending }} 笔</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="角色数" :value="stats.roleTotal">
            <template #prefix><el-icon color="#e6a23c"><UserFilled /></el-icon></template>
          </el-statistic>
          <div style="color: #909399; font-size: 12px; margin-top: 6px">RBAC 权限管控中</div>
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
            <el-tag :type="{ 0: 'warning', 1: 'primary', 2: 'success', 3: 'info', 4: 'danger' }[row.status]">
              {{ { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已关闭' }[row.status] }}
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
import { Goods, List, UserFilled } from '@element-plus/icons-vue'
import { productPage, orderPage, roleList } from '../api'

const stats = reactive({ productTotal: 0, productOnSale: 0, productOffSale: 0, orderTotal: 0, orderPending: 0, roleTotal: 0 })
const latestOrders = ref([])

onMounted(async () => {
  const products = await productPage({ pageNum: 1, pageSize: 1 })
  stats.productTotal = Number(products.total || 0)
  const on = await productPage({ pageNum: 1, pageSize: 1, status: 1 })
  const off = await productPage({ pageNum: 1, pageSize: 1, status: 0 })
  stats.productOnSale = Number(on.total || 0)
  stats.productOffSale = Number(off.total || 0)

  const orders = await orderPage({ pageNum: 1, pageSize: 5 })
  stats.orderTotal = Number(orders.total || 0)
  latestOrders.value = orders.records || []
  const pending = await orderPage({ pageNum: 1, pageSize: 1, status: 0 })
  stats.orderPending = Number(pending.total || 0)

  const roles = await roleList()
  stats.roleTotal = (roles || []).length
})
</script>
