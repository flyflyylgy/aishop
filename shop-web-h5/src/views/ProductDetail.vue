<template>
  <div v-if="product" style="padding-bottom: 60px">
    <van-nav-bar title="商品详情" left-arrow @click-left="$router.back()" />
    <div class="img-ph hero" :class="'c' + (product.id % 5)">{{ product.name?.[0] }}</div>

    <van-cell-group inset style="margin-top: -18px; position: relative; border-radius: 10px; overflow: hidden">
      <div style="padding: 14px 16px">
        <div style="display: flex; align-items: baseline">
          <span class="price" style="font-size: 24px">¥{{ money(currentPrice) }}</span>
          <span v-if="product.originalPrice > currentPrice" style="color: #969799; text-decoration: line-through; font-size: 12px; margin-left: 8px">¥{{ money(product.originalPrice) }}</span>
          <span style="margin-left: auto; font-size: 12px; color: #969799">已售{{ product.sale || 0 }}</span>
        </div>
        <div style="font-size: 16px; font-weight: 600; margin-top: 6px">{{ product.name }}</div>
        <div style="font-size: 12px; color: #969799; margin-top: 4px">{{ product.subTitle }}</div>
        <van-tag :type="currentStock > 0 ? 'success' : 'danger'" style="margin-top: 8px">
          {{ currentStock > 0 ? `可售 ${currentStock}` : '暂时缺货' }}
        </van-tag>
      </div>
    </van-cell-group>

    <!-- 规格选择（展示用，点击打开 action-sheet） -->
    <van-cell-group inset style="margin-top: 10px" v-if="product.hasSku === 1">
      <van-cell title="规格" is-link :value="selectedSpecText" @click="openSheet('spec')" />
    </van-cell-group>

    <van-cell-group inset style="margin-top: 10px" v-if="product.detailHtml">
      <van-cell title="商品详情" />
      <div style="padding: 0 16px 14px; font-size: 13px; line-height: 1.6" v-html="product.detailHtml"></div>
    </van-cell-group>

    <!-- 评价区 -->
    <van-cell-group inset style="margin-top: 10px">
      <van-cell>
        <template #title>
          <span style="font-weight: 600">商品评价</span>
          <span v-if="reviewStatsData" style="color: #969799; font-size: 12px; margin-left: 8px">
            {{ reviewStatsData.count }}条 · 均分{{ reviewStatsData.avgRating }}
          </span>
        </template>
      </van-cell>
      <van-empty v-if="reviews.length === 0" description="暂无评价" image-size="60" />
      <div v-for="rv in reviews" :key="rv.id" style="padding: 10px 16px; border-top: 1px solid #f2f3f5">
        <div style="display: flex; align-items: center; gap: 6px">
          <van-rate :model-value="rv.rating" readonly size="12" />
          <span style="font-size: 12px; color: #969799">{{ rv.memberName }}</span>
          <span style="font-size: 11px; color: #c8c9cc; margin-left: auto">{{ fmt(rv.createTime) }}</span>
        </div>
        <p style="margin: 6px 0 0; font-size: 13px; color: #323233">{{ rv.content }}</p>
      </div>
      <van-cell v-if="reviewTotal > reviewPageSize" style="text-align: center">
        <van-button plain size="small" @click="loadMoreReviews">查看更多</van-button>
      </van-cell>
    </van-cell-group>

    <!-- 规格选择弹层 -->
    <van-action-sheet v-model:show="specSheetShow" title="选择规格">
      <div style="padding: 16px; max-height: 70vh; overflow-y: auto">
        <div v-for="(dim, di) in specDims" :key="di" style="margin-bottom: 16px">
          <div style="font-size: 14px; font-weight: 600; margin-bottom: 8px">{{ dim }}</div>
          <div style="display: flex; flex-wrap: wrap; gap: 8px">
            <van-tag v-for="v in specOptions[dim]" :key="v"
              :type="selectedSpec[dim] === v ? 'danger' : 'default'"
              :plain="selectedSpec[dim] !== v"
              size="large" style="padding: 6px 14px; cursor: pointer"
              @click="selectSpec(dim, v)">
              {{ v }}
            </van-tag>
          </div>
        </div>
        <div style="display: flex; align-items: center; justify-content: space-between; padding-top: 12px; border-top: 1px solid #f2f3f5">
          <span style="font-size: 13px">数量</span>
          <van-stepper v-model="quantity" :min="1" :max="Math.max(1, currentStock)" />
        </div>
        <van-button type="danger" round block style="margin-top: 16px" :disabled="!canBuy" @click="confirmSpec">
          确定
        </van-button>
      </div>
    </van-action-sheet>

    <!-- 底部操作栏 -->
    <van-goods-action>
      <van-goods-action-icon icon="wap-home-o" text="首页" @click="$router.push('/')" />
      <van-goods-action-icon icon="shopping-cart-o" text="购物车" :badge="cartBadge || ''" @click="$router.push('/cart')" />
      <van-goods-action-button type="warning" text="加入购物车" :disabled="!canBuy" @click="openSheet('cart')" />
      <van-goods-action-button type="danger" text="立即购买" :disabled="!canBuy" @click="openSheet('buy')" />
    </van-goods-action>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { productDetail, cartAdd, reviewPage, reviewStats } from '../api'

const route = useRoute()
const router = useRouter()
const refreshCartBadge = inject('refreshCartBadge')
const product = ref(null)
const quantity = ref(1)
const cartBadge = ref(localStorage.getItem('token') ? '' : '')
const specSheetShow = ref(false)
const sheetMode = ref('cart')

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
  if (product.value?.hasSku === 1) return matchedSku.value ? matchedSku.value.availableStock : 0
  return product.value?.availableStock || 0
})
const canBuy = computed(() => {
  if (product.value?.status !== 1) return false
  if (product.value?.hasSku === 1) return matchedSku.value && matchedSku.value.availableStock > 0
  return (product.value?.availableStock || 0) > 0
})
const selectedSpecText = computed(() => {
  if (specDims.value.length === 0) return ''
  const allSelected = specDims.value.every(d => selectedSpec[d])
  if (!allSelected) return '请选择'
  return specDims.value.map(d => selectedSpec[d]).join(' ')
})

function buildSpecSelector() {
  if (!product.value || product.value.hasSku !== 1 || !product.value.skus) return
  const dims = (product.value.specNames || '').split(',').map(s => s.trim()).filter(Boolean)
  specDims.value = dims
  const opts = {}
  dims.forEach(d => opts[d] = new Set())
  product.value.skus.forEach(sku => {
    try {
      const vals = JSON.parse(sku.specValues)
      dims.forEach(d => { if (vals[d]) opts[d].add(vals[d]) })
    } catch { /* ignore */ }
  })
  dims.forEach(d => { specOptions[d] = [...opts[d]] })
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

function loadMoreReviews() {
  reviewPageNum.value += 1
  reviewPage(route.params.id, { pageNum: reviewPageNum.value, pageSize: reviewPageSize.value })
    .then(page => {
      reviews.value = reviews.value.concat(page.records || [])
      reviewTotal.value = Number(page.total || 0)
    })
}

function openSheet(mode) {
  if (!localStorage.getItem('token')) {
    return router.push({ path: '/login', query: { redirect: route.fullPath } })
  }
  // 多规格商品必须先选规格
  if (product.value.hasSku === 1) {
    sheetMode.value = mode
    specSheetShow.value = true
    return
  }
  // 单规格直接跳转或加购
  if (mode === 'cart') {
    cartAdd(product.value.id, 1, null).then(() => { showSuccessToast('已加入购物车'); refreshCartBadge?.() })
  } else {
    router.push({ path: '/checkout', query: { productId: product.value.id, quantity: 1 } })
  }
}

function confirmSpec() {
  const skuId = matchedSku.value?.id
  if (product.value.hasSku === 1 && !skuId) return
  specSheetShow.value = false
  if (sheetMode.value === 'cart') {
    cartAdd(product.value.id, quantity.value, skuId).then(() => {
      showSuccessToast('已加入购物车')
      refreshCartBadge?.()
    })
  } else {
    router.push({ path: '/checkout', query: { productId: product.value.id, quantity: quantity.value, skuId } })
  }
}

onMounted(async () => {
  product.value = await productDetail(route.params.id)
  buildSpecSelector()
  loadReviews()
  reviewStatsData.value = await reviewStats(route.params.id).catch(() => null)
  try {
    if (localStorage.getItem('token')) {
      const { cartList } = await import('../api')
      const list = (await cartList()) || []
      cartBadge.value = list.reduce((s, i) => s + i.quantity, 0) || ''
    }
  } catch { /* 忽略 */ }
})
</script>

<style scoped>
.hero { height: 300px; font-size: 80px; }
</style>
