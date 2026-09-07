<template>
  <div class="page-pad">
    <van-nav-bar title="我的退款" left-arrow @click-left="$router.back()" />
    <van-empty v-if="!loading && records.length === 0" description="暂无退款记录" />
    <div v-for="r in records" :key="r.id" class="refund-card">
      <div class="rf-head">
        <span class="rf-no">订单 {{ r.orderNo }}</span>
        <van-tag :type="tagType(r.status)">{{ statusText(r.status) }}</van-tag>
      </div>
      <div class="rf-amount">退款金额: <span class="price">¥{{ Number(r.amount).toFixed(2) }}</span></div>
      <div class="rf-reason">原因: {{ r.reason }}</div>
      <div class="rf-reply" v-if="r.adminRemark">客服回复: {{ r.adminRemark }}</div>
      <div class="rf-time">申请: {{ fmt(r.createTime) }}<span v-if="r.handleTime"> · 处理: {{ fmt(r.handleTime) }}</span></div>
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
import { myRefunds } from '../api'

const records = ref([])
const loading = ref(false)
const finished = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10 })

const MAP = { 0: ['warning', '待审核'], 1: ['success', '已同意'], 2: ['danger', '已拒绝'] }
const tagType = s => MAP[s]?.[0] || 'default'
const statusText = s => MAP[s]?.[1] || '未知'
const fmt = t => t ? String(t).replace('T', ' ').slice(0, 16) : '—'

async function load(reset = false) {
  if (reset) { query.pageNum = 1; finished.value = false }
  loading.value = true
  try {
    const page = await myRefunds(query)
    const list = page.records || []
    records.value = reset ? list : records.value.concat(list)
    if (records.value.length >= Number(page.total || 0)) finished.value = true
  } finally { loading.value = false }
}
function loadMore() { query.pageNum++; load() }

onMounted(() => load(true))
</script>

<style scoped>
.refund-card { background: #fff; margin: 10px 12px; border-radius: 10px; padding: 12px; }
.rf-head { display: flex; align-items: center; justify-content: space-between; }
.rf-no { font-size: 13px; color: #323233; font-weight: 600; }
.rf-amount { margin: 8px 0; font-size: 13px; }
.rf-amount .price { color: #ee0a24; font-size: 16px; font-weight: 700; }
.rf-reason, .rf-reply { font-size: 12px; color: #646566; margin: 4px 0; line-height: 1.5; }
.rf-time { font-size: 11px; color: #969799; margin-top: 6px; }
</style>
