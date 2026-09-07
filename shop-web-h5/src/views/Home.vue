<template>
  <div class="page-pad">
    <van-sticky>
      <van-search v-model="keyword" placeholder="搜索商品" shape="round" @search="onSearch" />
    </van-sticky>

    <!-- 轮播 Banner -->
    <van-swipe :autoplay="3500" style="margin: 8px 12px 0; border-radius: 10px; overflow: hidden">
      <van-swipe-item v-for="(b, i) in banners" :key="i">
        <div class="img-ph banner" :class="'c' + (i + 1)">
          <div class="banner-title">{{ b.title }}</div>
          <div class="banner-sub">{{ b.sub }}</div>
        </div>
      </van-swipe-item>
    </van-swipe>

    <!-- 分类宫格 -->
    <van-grid :column-num="4" :border="false" style="margin-top: 12px">
      <van-grid-item v-for="c in categories" :key="c.id" @click="$router.push({ path: '/category', query: { id: c.id } })">
        <div class="img-ph" :class="'c' + (c.id % 5)" style="width: 44px; height: 44px; border-radius: 12px; font-size: 18px">{{ c.name?.[0] }}</div>
        <div style="font-size: 12px; margin-top: 6px">{{ c.name }}</div>
      </van-grid-item>
    </van-grid>

    <!-- 热销推荐 -->
    <div class="section-title" v-if="hot.length">🔥 热销推荐</div>
    <div class="card-list" v-if="hot.length">
      <div v-for="p in hot" :key="'h' + p.id" class="h-card" @click="$router.push(`/product/${p.id}`)">
        <div class="img-ph thumb" :class="'c' + (p.id % 5)">{{ p.name?.[0] }}</div>
        <div class="h-card-info">
          <div class="h-card-name">{{ p.name }}</div>
          <div class="h-card-price">
            <span class="price">¥{{ money(p.price) }}</span>
            <span class="sale">已售{{ p.sale || 0 }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="section-title">{{ query.keyword ? `“${query.keyword}”的结果` : '全部商品' }} <span class="total">共{{ total }}件</span></div>
    <van-empty v-if="!loading && products.length === 0" description="暂无商品" />
    <div class="card-list">
      <div v-for="p in products" :key="p.id" class="h-card" @click="$router.push(`/product/${p.id}`)">
        <div class="img-ph thumb" :class="'c' + (p.id % 5)">{{ p.name?.[0] }}</div>
        <div class="h-card-info">
          <div class="h-card-name">{{ p.name }}</div>
          <div class="h-card-sub">{{ p.subTitle }}</div>
          <div class="h-card-price">
            <span class="price">¥{{ money(p.price) }}</span>
            <span v-if="p.originalPrice > p.price" class="origin">¥{{ money(p.originalPrice) }}</span>
          </div>
        </div>
      </div>
    </div>
    <div style="padding: 10px 0 16px" v-if="products.length">
      <van-button plain hairline size="small" block :loading="loading" :disabled="finished" @click="loadMore">
        {{ finished ? '没有更多了' : '加载更多' }}
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { productPage, productHot, categoryTree } from '../api'

const keyword = ref('')
const products = ref([])
const hot = ref([])
const categories = ref([])
const total = ref(0)
const loading = ref(false)
const finished = ref(false)
const query = reactive({ keyword: '', pageNum: 1, pageSize: 10 })

const banners = [
  { title: '新品首发', sub: '旗舰手机 星尘 X1 Pro' },
  { title: '超值专区', sub: '爆款直降 · 限时特惠' },
  { title: '云上优选', sub: '正品保障 · 极速发货' }
]

const money = v => Number(v ?? 0).toFixed(2)

async function load(reset = false) {
  if (reset) { query.pageNum = 1; finished.value = false }
  loading.value = true
  try {
    const page = await productPage(query)
    const records = page.records || []
    products.value = reset ? records : products.value.concat(records)
    total.value = Number(page.total || 0)
    if (products.value.length >= total.value) finished.value = true
  } finally {
    loading.value = false
  }
}

function onSearch() { load(true) }
function loadMore() { query.pageNum++; load() }

onMounted(async () => {
  try { hot.value = await productHot(4) } catch { /* 忽略 */ }
  try { categories.value = (await categoryTree()).slice(0, 8) } catch { /* 忽略 */ }
  load(true)
})
</script>

<style scoped>
.banner { height: 120px; flex-direction: column; gap: 4px; }
.banner-title { font-size: 22px; }
.banner-sub { font-size: 12px; opacity: .92; font-weight: 400; }
.section-title { font-size: 15px; font-weight: 700; margin: 16px 12px 8px; display: flex; align-items: baseline; }
.section-title .total { margin-left: auto; font-size: 12px; color: #969799; font-weight: 400; }
.card-list { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; padding: 0 12px; }
.h-card { background: #fff; border-radius: 10px; overflow: hidden; }
.h-card .thumb { height: 130px; font-size: 34px; }
.h-card-info { padding: 8px 10px 12px; }
.h-card-name { font-size: 13px; line-height: 1.35; height: 35px; overflow: hidden;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.h-card-sub { font-size: 11px; color: #969799; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.h-card-price { margin-top: 6px; display: flex; align-items: baseline; }
.h-card-price .price { font-size: 16px; }
.origin { color: #c8c9cc; text-decoration: line-through; font-size: 11px; margin-left: 5px; }
.sale { margin-left: auto; font-size: 11px; color: #969799; }
</style>
