<template>
  <div class="container">
    <div class="section-title"><h2>我的评价</h2></div>
    <el-empty v-if="!loading && records.length === 0" description="暂无评价记录" />
    <div v-else class="review-list">
      <el-card v-for="r in records" :key="r.id" shadow="hover" style="margin-bottom: 14px">
        <div class="review-item">
          <div class="review-head">
            <el-link type="primary" :underline="false" @click="$router.push(`/product/${r.productId}`)">{{ r.productName }}</el-link>
            <el-rate v-model="r.rating" disabled size="small" style="margin-left: 12px" />
            <span style="margin-left: auto; color: #909399; font-size: 13px">{{ fmt(r.createTime) }}</span>
          </div>
          <div class="review-content">{{ r.content }}</div>
          <div v-if="r.pics" class="review-pics">
            <el-image v-for="(p, i) in r.pics.split(',')" :key="i" :src="p" :preview-src-list="r.pics.split(',')"
              fit="cover" style="width: 72px; height: 72px; border-radius: 6px; margin-right: 8px" />
          </div>
          <div style="color: #c0c4cc; font-size: 12px">订单号: {{ r.orderNo }}</div>
        </div>
      </el-card>
    </div>
    <div style="display: flex; justify-content: center; margin-top: 20px" v-if="total > query.pageSize">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.pageSize"
        :current-page="query.pageNum" @current-change="onPage" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { myReviews } from '../api'

const records = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10 })

const fmt = t => String(t || '').replace('T', ' ').slice(0, 16)

async function load() {
  loading.value = true
  try {
    const page = await myReviews(query)
    records.value = page.records || []
    total.value = Number(page.total || 0)
  } finally { loading.value = false }
}
function onPage(p) { query.pageNum = p; load() }

onMounted(load)
</script>

<style scoped>
.review-head { display: flex; align-items: center; }
.review-content { margin: 10px 0; line-height: 1.6; color: #303133; }
.review-pics { display: flex; margin-bottom: 8px; }
</style>
