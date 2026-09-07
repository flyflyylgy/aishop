<template>
  <div class="container">
    <div class="section-title"><h2>我的退款</h2></div>
    <el-empty v-if="!loading && records.length === 0" description="暂无退款记录" />
    <el-table v-else :data="records" stripe style="margin-top: 12px">
      <el-table-column label="订单号" prop="orderNo" width="200" />
      <el-table-column label="退款金额" width="120">
        <template #default="{ row }"><span class="price">¥{{ Number(row.amount).toFixed(2) }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请原因" prop="reason" min-width="160" show-overflow-tooltip />
      <el-table-column label="客服回复" prop="adminRemark" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.adminRemark || '—' }}</template>
      </el-table-column>
      <el-table-column label="申请时间" width="160">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="处理时间" width="160">
        <template #default="{ row }">{{ fmt(row.handleTime) }}</template>
      </el-table-column>
    </el-table>
    <div style="display: flex; justify-content: center; margin-top: 20px" v-if="total > query.pageSize">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.pageSize"
        :current-page="query.pageNum" @current-change="onPage" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { myRefunds } from '../api'

const records = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10 })

const REFUND_MAP = { 0: ['warning', '待审核'], 1: ['success', '已同意'], 2: ['danger', '已拒绝'] }
const statusType = s => REFUND_MAP[s]?.[0] || 'info'
const statusText = s => REFUND_MAP[s]?.[1] || '未知'
const fmt = t => t ? String(t).replace('T', ' ').slice(0, 16) : '—'

async function load() {
  loading.value = true
  try {
    const page = await myRefunds(query)
    records.value = page.records || []
    total.value = Number(page.total || 0)
  } finally { loading.value = false }
}
function onPage(p) { query.pageNum = p; load() }

onMounted(load)
</script>
