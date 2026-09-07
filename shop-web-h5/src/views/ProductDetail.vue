<template>
  <div v-if="product" style="padding-bottom: 60px">
    <van-nav-bar title="商品详情" left-arrow @click-left="$router.back()" />
    <div class="img-ph hero" :class="'c' + (product.id % 5)">{{ product.name?.[0] }}</div>

    <van-cell-group inset style="margin-top: -18px; position: relative; border-radius: 10px; overflow: hidden">
      <div style="padding: 14px 16px">
        <div style="display: flex; align-items: baseline">
          <span class="price" style="font-size: 24px">¥{{ money(product.price) }}</span>
          <span v-if="product.originalPrice > product.price" style="color: #969799; text-decoration: line-through; font-size: 12px; margin-left: 8px">¥{{ money(product.originalPrice) }}</span>
          <span style="margin-left: auto; font-size: 12px; color: #969799">已售{{ product.sale || 0 }}</span>
        </div>
        <div style="font-size: 16px; font-weight: 600; margin-top: 6px">{{ product.name }}</div>
        <div style="font-size: 12px; color: #969799; margin-top: 4px">{{ product.subTitle }}</div>
        <van-tag :type="product.availableStock > 0 ? 'success' : 'danger'" style="margin-top: 8px">
          {{ product.availableStock > 0 ? `可售 ${product.availableStock}` : '暂时缺货' }}
        </van-tag>
      </div>
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

    <!-- 数量选择弹层 -->
    <van-action-sheet v-model:show="sheetShow" :title="sheetMode === 'cart' ? '加入购物车' : '立即购买'">
      <div style="padding: 16px">
        <div style="display: flex; gap: 12px; align-items: center">
          <div class="img-ph" :class="'c' + (product.id % 5)" style="width: 60px; height: 60px; border-radius: 8px; font-size: 20px">{{ product.name?.[0] }}</div>
          <div>
            <div class="price" style="font-size: 18px">¥{{ money(product.price) }}</div>
            <div style="font-size: 12px; color: #969799">库存 {{ product.availableStock }}</div>
          </div>
        </div>
        <div style="display: flex; align-items: center; margin-top: 16px">
          <span>购买数量</span>
          <van-stepper v-model="quantity" :min="1" :max="Math.max(1, product.availableStock)" style="margin-left: auto" />
        </div>
        <van-button type="danger" round block style="margin-top: 16px" :disabled="product.availableStock <= 0" @click="confirmSheet">
          确定
        </van-button>
      </div>
    </van-action-sheet>

    <!-- 底部操作栏 -->
    <van-goods-action>
      <van-goods-action-icon icon="wap-home-o" text="首页" @click="$router.push('/')" />
      <van-goods-action-icon icon="shopping-cart-o" text="购物车" :badge="cartBadge || ''" @click="$router.push('/cart')" />
      <van-goods-action-button type="warning" text="加入购物车" :disabled="product.availableStock <= 0" @click="openSheet('cart')" />
      <van-goods-action-button type="danger" text="立即购买" :disabled="product.availableStock <= 0" @click="openSheet('buy')" />
    </van-goods-action>
  </div>
</template>

<script setup>
import { inject, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { productDetail, cartAdd, reviewPage, reviewStats } from '../api'

const route = useRoute()
const router = useRouter()
const refreshCartBadge = inject('refreshCartBadge')
const product = ref(null)
const quantity = ref(1)
const sheetShow = ref(false)
const sheetMode = ref('cart')
const cartBadge = ref(localStorage.getItem('token') ? '' : '')

const money = v => Number(v ?? 0).toFixed(2)

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
  sheetMode.value = mode
  quantity.value = 1
  sheetShow.value = true
}

async function confirmSheet() {
  sheetShow.value = false
  if (sheetMode.value === 'cart') {
    await cartAdd(product.value.id, quantity.value)
    showSuccessToast('已加入购物车')
    refreshCartBadge?.()
  } else {
    router.push({ path: '/checkout', query: { productId: product.value.id, quantity: quantity.value } })
  }
}

onMounted(async () => {
  product.value = await productDetail(route.params.id)
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
