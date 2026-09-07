<template>
  <div class="page-pad">
    <van-nav-bar title="商品分类" />
    <div class="cat-layout">
      <!-- 左侧一级分类 -->
      <div class="cat-side">
        <div v-for="c in categories" :key="c.id" class="cat-side-item"
          :class="{ active: String(activeId) === String(c.id) }" @click="pick(c.id)">
          {{ c.name }}
        </div>
      </div>
      <!-- 右侧商品 -->
      <div class="cat-main">
        <van-empty v-if="!loading && products.length === 0" description="该分类暂无商品" />
        <div v-for="p in products" :key="p.id" class="row-card" @click="$router.push(`/product/${p.id}`)">
          <div class="img-ph" :class="'c' + (p.id % 5)" style="width: 84px; height: 84px; border-radius: 8px; font-size: 26px; flex-shrink: 0">{{ p.name?.[0] }}</div>
          <div style="flex: 1; min-width: 0; padding-left: 10px">
            <div class="row-name">{{ p.name }}</div>
            <div class="row-sub">{{ p.subTitle }}</div>
            <div style="display: flex; align-items: baseline; margin-top: 8px">
              <span class="price" style="font-size: 16px">¥{{ money(p.price) }}</span>
              <span style="margin-left: auto; font-size: 11px; color: #969799">已售{{ p.sale || 0 }}</span>
            </div>
          </div>
        </div>
        <div style="padding: 12px 0" v-if="products.length">
          <van-button plain hairline size="small" block :loading="loading" :disabled="finished" @click="loadMore">
            {{ finished ? '没有更多了' : '加载更多' }}
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { categoryTree, productPage } from '../api'

const route = useRoute()
const categories = ref([])
const products = ref([])
const activeId = ref(null)
const loading = ref(false)
const finished = ref(false)
const query = reactive({ categoryId: null, pageNum: 1, pageSize: 10 })

const money = v => Number(v ?? 0).toFixed(2)

async function load(reset = false) {
  if (reset) { query.pageNum = 1; finished.value = false }
  loading.value = true
  try {
    const page = await productPage(query)
    const records = page.records || []
    products.value = reset ? records : products.value.concat(records)
    if (products.value.length >= Number(page.total || 0)) finished.value = true
  } finally {
    loading.value = false
  }
}

function pick(id) {
  activeId.value = id
  query.categoryId = id
  load(true)
}

function loadMore() { query.pageNum++; load() }

onMounted(async () => {
  categories.value = (await categoryTree()) || []
  const initId = route.query.id || categories.value[0]?.id
  if (initId) pick(initId)
})
</script>

<style scoped>
.cat-layout { display: flex; min-height: calc(100vh - 46px - 56px); }
.cat-side { width: 96px; background: #fff; flex-shrink: 0; }
.cat-side-item { padding: 15px 10px; font-size: 13px; color: #646566; text-align: center; cursor: pointer; position: relative; }
.cat-side-item.active { color: var(--brand); font-weight: 700; background: #f7f8fa; }
.cat-side-item.active::before { content: ''; position: absolute; left: 0; top: 20%; height: 60%; width: 3px; background: var(--brand); border-radius: 2px; }
.cat-main { flex: 1; padding: 10px; min-width: 0; }
.row-card { display: flex; background: #fff; border-radius: 10px; padding: 10px; margin-bottom: 8px; cursor: pointer; }
.row-name { font-size: 13px; line-height: 1.35; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.row-sub { font-size: 11px; color: #969799; margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
