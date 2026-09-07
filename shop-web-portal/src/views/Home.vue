<template>
  <div class="container page-bg">
    <!-- Banner -->
    <div class="banner img-ph" style="height: 220px; border-radius: 12px; font-size: 30px; flex-direction: column; gap: 8px; background: linear-gradient(135deg, #409eff 0%, #7b5bf5 60%, #f5576c 100%)">
      <div>Cloude Shop 云上商城</div>
      <div style="font-size: 15px; font-weight: 400; opacity: .9">企业级电商 · Spring Boot 3 + Vue 3 · 30 分钟未支付自动关单</div>
    </div>

    <!-- 分类导航 -->
    <div class="section" style="margin-top: 20px">
      <el-space wrap :size="10">
        <el-tag :type="!query.categoryId ? 'primary' : 'info'" size="large" style="cursor: pointer" @click="pickCategory(null)">全部</el-tag>
        <el-tag v-for="c in categories" :key="c.id" :type="query.categoryId === c.id ? 'primary' : 'info'"
          size="large" style="cursor: pointer" @click="pickCategory(c.id)">{{ c.name }}</el-tag>
      </el-space>
    </div>

    <!-- 商品列表 -->
    <div class="section" style="margin-top: 20px">
      <div class="section-title">
        <h3>{{ query.keyword ? `“${query.keyword}” 的搜索结果` : '全部商品' }}</h3>
        <span style="color: #909399; font-size: 13px">共 {{ total }} 件</span>
      </div>
      <el-empty v-if="!loading && products.length === 0" description="暂无商品" />
      <div v-else class="grid">
        <ProductCard v-for="p in products" :key="p.id" :product="p" />
      </div>
      <div style="display: flex; justify-content: center; margin-top: 24px">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.pageSize"
          :current-page="query.pageNum" @current-change="onPage" />
      </div>
    </div>

    <!-- 热销推荐 -->
    <div class="section" style="margin-top: 36px" v-if="hot.length">
      <div class="section-title"><h3>🔥 热销推荐</h3></div>
      <div class="grid">
        <ProductCard v-for="p in hot" :key="'h' + p.id" :product="p" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { productPage, productHot, categoryTree } from '../api'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const products = ref([])
const hot = ref([])
const categories = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ categoryId: null, keyword: '', pageNum: 1, pageSize: 12 })

async function loadProducts() {
  loading.value = true
  try {
    const page = await productPage(query)
    products.value = page.records || []
    total.value = Number(page.total || 0)
  } finally {
    loading.value = false
  }
}

function pickCategory(id) {
  query.categoryId = id
  query.pageNum = 1
}

function onPage(p) {
  query.pageNum = p
}

watch(() => route.query.keyword, v => {
  query.keyword = v || ''
  query.pageNum = 1
})

watch(query, loadProducts)

onMounted(async () => {
  query.keyword = route.query.keyword || ''
  try { categories.value = await categoryTree() } catch { /* 忽略 */ }
  try { hot.value = await productHot(6) } catch { /* 忽略 */ }
  loadProducts()
})
</script>

<style scoped>
.banner { color: #fff; }
.section-title { display: flex; align-items: baseline; gap: 10px; margin-bottom: 14px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
</style>
