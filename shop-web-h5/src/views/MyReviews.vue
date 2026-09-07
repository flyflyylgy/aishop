<template>
  <div class="page-pad">
    <van-nav-bar title="我的评价" left-arrow @click-left="$router.back()" />
    <van-empty v-if="!loading && records.length === 0" description="暂无评价记录" />
    <div v-for="r in records" :key="r.id" class="review-card" @click="$router.push(`/product/${r.productId}`)">
      <div class="r-head">
        <span class="r-name">{{ r.productName }}</span>
        <van-rate v-model="r.rating" readonly size="12" />
      </div>
      <div class="r-content">{{ r.content }}</div>
      <div v-if="r.pics" class="r-pics">
        <van-image v-for="(p, i) in r.pics.split(',')" :key="i" :src="p" fit="cover"
          width="64" height="64" radius="6" style="margin-right: 6px" />
      </div>
      <div class="r-time">{{ fmt(r.createTime) }} · 订单 {{ r.orderNo }}</div>
    </div>
    <div style="padding: 10px 0 16px" v-if="records.length">
      <van-button plain hairline size="small" block :loading="loading" :disabled="finished" @click="loadMore">
        {{ finished ? '没有更多了' : '加载更多' }}
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { myReviews } from '../api'

const records = ref([])
const loading = ref(false)
const finished = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10 })

const fmt = t => String(t || '').replace('T', ' ').slice(0, 16)

async function load(reset = false) {
  if (reset) { query.pageNum = 1; finished.value = false }
  loading.value = true
  try {
    const page = await myReviews(query)
    const list = page.records || []
    records.value = reset ? list : records.value.concat(list)
    if (records.value.length >= Number(page.total || 0)) finished.value = true
  } finally { loading.value = false }
}
function loadMore() { query.pageNum++; load() }

onMounted(() => load(true))
</script>

<style scoped>
.review-card { background: #fff; margin: 10px 12px; border-radius: 10px; padding: 12px; }
.r-head { display: flex; align-items: center; gap: 8px; }
.r-name { font-size: 14px; font-weight: 600; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.r-content { font-size: 13px; color: #323233; margin: 8px 0; line-height: 1.6; }
.r-pics { display: flex; margin-bottom: 6px; }
.r-time { font-size: 11px; color: #969799; }
</style>
