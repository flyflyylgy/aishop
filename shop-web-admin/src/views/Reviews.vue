<template>
  <div class="page-card">
    <el-card>
      <div class="table-toolbar">
        <el-input v-model="query.productId" placeholder="商品ID" clearable style="width: 180px"
          @keyup.enter="load" @clear="load" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading">
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="商品ID" prop="productId" width="100" />
        <el-table-column label="评价人" prop="memberName" width="120" />
        <el-table-column label="评分" width="160">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="220">
          <template #default="{ row }">
            <div style="color: #606266">{{ row.content }}</div>
            <div v-if="row.pics" style="margin-top: 6px">
              <el-image v-for="(p, i) in row.pics.split(',')" :key="i" :src="p" :preview-src-list="row.pics.split(',')"
                style="width: 50px; height: 50px; border-radius: 4px; margin-right: 6px" fit="cover" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="订单号" prop="orderNo" width="200" />
        <el-table-column label="评价时间" width="170">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: center; margin-top: 16px">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          :page-size="query.pageSize" :current-page="query.pageNum"
          @current-change="p => { query.pageNum = p; load() }" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { reviewPage } from '../api'

const fmt = t => t ? String(t).replace('T', ' ').slice(0, 19) : ''

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ productId: '', pageNum: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.productId) params.productId = query.productId
    const page = await reviewPage(params)
    list.value = page.records || []
    total.value = Number(page.total || 0)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
