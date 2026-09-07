<template>
  <div class="container page-bg" v-if="product">
    <el-card>
      <div class="detail">
        <div class="img-ph c" style="width: 380px; height: 380px; font-size: 90px; border-radius: 12px" :class="'c' + (product.id % 4)">
          {{ product.name?.[0] }}
        </div>
        <div class="detail-info">
          <h2>{{ product.name }}</h2>
          <p style="color: #909399; margin: 8px 0 16px">{{ product.subTitle }}</p>
          <div class="price-panel">
            <span class="price" style="font-size: 28px">¥{{ money(currentPrice) }}</span>
            <span v-if="product.originalPrice && product.originalPrice > currentPrice" class="price-origin" style="font-size: 14px">¥{{ money(product.originalPrice) }}</span>
            <span style="margin-left: auto; color: #909399; font-size: 13px">已售 {{ product.sale || 0 }}</span>
          </div>

          <!-- 规格选择 -->
          <div v-if="product.hasSku === 1 && specDims.length" style="margin-top: 16px">
            <div v-for="(dim, di) in specDims" :key="di" class="spec-row">
              <span class="spec-label">{{ dim }}</span>
              <el-tag v-for="v in specOptions[dim]" :key="v"
                :type="selectedSpec[dim] === v ? 'danger' : 'info'"
                :effect="selectedSpec[dim] === v ? 'dark' : 'plain'"
                style="margin-right: 8px; cursor: pointer"
                @click="selectSpec(dim, v)">
                {{ v }}
              </el-tag>
            </div>
          </div>

          <el-descriptions :column="2" style="margin-top: 16px">
            <el-descriptions-item label="库存">
              <el-tag :type="currentStock > 0 ? 'success' : 'danger'">
                {{ currentStock > 0 ? `可售 ${currentStock}` : '暂时缺货' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="product.status === 1 ? 'success' : 'info'">{{ product.status === 1 ? '在售' : '已下架' }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div style="margin-top: 20px; display: flex; align-items: center; gap: 16px">
            <span>数量</span>
            <el-input-number v-model="quantity" :min="1" :max="Math.max(1, currentStock)" />
            <el-button type="warning" size="large" :icon="ShoppingCart" :disabled="!canBuy" @click="addToCart">
              加入购物车
            </el-button>
            <el-button type="danger" size="large" :disabled="!canBuy" @click="buyNow">
              立即购买
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top: 20px" v-if="product.detailHtml">
      <h3 style="margin-bottom: 12px">商品详情</h3>
      <div v-html="product.detailHtml" class="detail-html"></div>
    </el-card>

    <!-- 评价区 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3>商品评价</h3>
          <span v-if="reviewStatsData" style="color: #909399; font-size: 14px">
            {{ reviewStatsData.count }} 条评价 · 均分 {{ reviewStatsData.avgRating }}
          </span>
        </div>
      </template>

      <div v-if="reviews.length === 0" style="text-align: center; color: #c0c4cc; padding: 20px">暂无评价</div>

      <div v-for="rv in reviews" :key="rv.id" style="padding: 12px 0; border-bottom: 1px solid #f0f0f0">
        <div style="display: flex; align-items: center; gap: 8px">
          <el-rate :model-value="rv.rating" disabled size="small" />
          <span style="color: #909399; font-size: 13px">{{ rv.memberName }}</span>
          <span style="color: #c0c4cc; font-size: 12px; margin-left: auto">{{ fmt(rv.createTime) }}</span>
        </div>
        <p style="margin: 8px 0 0; color: #606266">{{ rv.content }}</p>
      </div>

      <div style="display: flex; justify-content: center; margin-top: 16px" v-if="reviewTotal > reviewPageSize">
        <el-pagination background layout="prev, pager, next" :total="reviewTotal" :page-size="reviewPageSize"
          :current-page="reviewPageNum" @current-change="p => { reviewPageNum = p; loadReviews() }" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { productDetail, cartAdd, reviewPage, reviewStats } from '../api'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const quantity = ref(1)
const money = v => Number(v ?? 0).toFixed(2)

// SKU 规格选择
const specDims = ref([])
const specOptions = reactive({})
const selectedSpec = reactive({})
const matchedSku = ref(null)

const currentPrice = computed(() => {
  if (product.value?.hasSku === 1 && matchedSku.value) return matchedSku.value.price
  return product.value?.price || 0
})
const currentStock = computed(() => {
  if (product.value?.hasSku === 1) {
    return matchedSku.value ? matchedSku.value.availableStock : 0
  }
  return product.value?.availableStock || 0
})
const canBuy = computed(() => {
  if (product.value?.status !== 1) return false
  if (product.value?.hasSku === 1) return matchedSku.value && matchedSku.value.availableStock > 0
  return (product.value?.availableStock || 0) > 0
})

function buildSpecSelector() {
  if (!product.value || product.value.hasSku !== 1 || !product.value.skus) return
  const dims = (product.value.specNames || '').split(',').map(s => s.trim()).filter(Boolean)
  specDims.value = dims
  // 收集每个维度的可选值
  const opts = {}
  dims.forEach(d => opts[d] = new Set())
  product.value.skus.forEach(sku => {
    try {
      const vals = JSON.parse(sku.specValues)
      dims.forEach(d => { if (vals[d]) opts[d].add(vals[d]) })
    } catch { /* ignore */ }
  })
  dims.forEach(d => { specOptions[d] = [...opts[d]] })
  // 默认选第一项
  dims.forEach(d => { selectedSpec[d] = specOptions[d][0] })
  matchSku()
}

function selectSpec(dim, val) {
  selectedSpec[dim] = val
  matchSku()
}

function matchSku() {
  if (!product.value?.skus) { matchedSku.value = null; return }
  const dims = specDims.value
  const allSelected = dims.every(d => selectedSpec[d])
  if (!allSelected) { matchedSku.value = null; return }
  matchedSku.value = product.value.skus.find(sku => {
    try {
      const vals = JSON.parse(sku.specValues)
      return dims.every(d => vals[d] === selectedSpec[d])
    } catch { return false }
  }) || null
  quantity.value = 1
}

// 评价
const reviews = ref([])
const reviewStatsData = ref(null)
const reviewTotal = ref(0)
const reviewPageNum = ref(1)
const reviewPageSize = ref(5)
const fmt = t => t ? String(t).replace('T', ' ').slice(0, 10) : ''

async function loadReviews() {
  const page = await reviewPage(route.params.id, { pageNum: reviewPageNum.value, pageSize: reviewPageSize.value })
  reviews.value = page.records || []
  reviewTotal.value = Number(page.total || 0)
}

onMounted(async () => {
  product.value = await productDetail(route.params.id)
  quantity.value = 1
  buildSpecSelector()
  loadReviews()
  reviewStatsData.value = await reviewStats(route.params.id)
})

async function addToCart() {
  const skuId = product.value.hasSku === 1 ? matchedSku.value?.id : null
  if (product.value.hasSku === 1 && !skuId) {
    ElMessage.warning('请选择完整规格')
    return
  }
  await cartAdd(product.value.id, quantity.value, skuId)
  ElMessage.success('已加入购物车')
}

function buyNow() {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    return router.push({ path: '/login', query: { redirect: route.fullPath } })
  }
  const skuId = product.value.hasSku === 1 ? matchedSku.value?.id : null
  if (product.value.hasSku === 1 && !skuId) {
    ElMessage.warning('请选择完整规格')
    return
  }
  router.push({ path: '/checkout', query: { productId: product.value.id, quantity: quantity.value, ...(skuId ? { skuId } : {}) } })
}
</script>

<style scoped>
.detail { display: flex; gap: 36px; }
.detail-info { flex: 1; }
.price-panel { display: flex; align-items: baseline; background: #fff5f5; padding: 14px 16px; border-radius: 8px; }
.spec-row { margin-bottom: 12px; }
.spec-label { display: inline-block; width: 60px; color: #909399; font-size: 14px; }
@media (max-width: 768px) { .detail { flex-direction: column; } .detail > div:first-child { width: 100% !important; height: 260px !important; } }
</style>
