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
            <span class="price" style="font-size: 28px">¥{{ money(product.price) }}</span>
            <span v-if="product.originalPrice && product.originalPrice > product.price" class="price-origin" style="font-size: 14px">¥{{ money(product.originalPrice) }}</span>
            <span style="margin-left: auto; color: #909399; font-size: 13px">已售 {{ product.sale || 0 }}</span>
          </div>
          <el-descriptions :column="2" style="margin-top: 16px">
            <el-descriptions-item label="库存">
              <el-tag :type="product.availableStock > 0 ? 'success' : 'danger'">
                {{ product.availableStock > 0 ? `可售 ${product.availableStock}` : '暂时缺货' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="product.status === 1 ? 'success' : 'info'">{{ product.status === 1 ? '在售' : '已下架' }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div style="margin-top: 20px; display: flex; align-items: center; gap: 16px">
            <span>数量</span>
            <el-input-number v-model="quantity" :min="1" :max="Math.max(1, product.availableStock)" />
            <el-button type="warning" size="large" :icon="ShoppingCart" :disabled="product.availableStock <= 0 || product.status !== 1" @click="addToCart">
              加入购物车
            </el-button>
            <el-button type="danger" size="large" :disabled="product.availableStock <= 0 || product.status !== 1" @click="buyNow">
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
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { productDetail, cartAdd, reviewPage, reviewStats } from '../api'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const quantity = ref(1)
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

onMounted(async () => {
  product.value = await productDetail(route.params.id)
  quantity.value = 1
  loadReviews()
  reviewStatsData.value = await reviewStats(route.params.id)
})

async function addToCart() {
  await cartAdd(product.value.id, quantity.value)
  ElMessage.success('已加入购物车')
}

function buyNow() {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    return router.push({ path: '/login', query: { redirect: route.fullPath } })
  }
  router.push({ path: '/checkout', query: { productId: product.value.id, quantity: quantity.value } })
}
</script>

<style scoped>
.detail { display: flex; gap: 36px; }
.detail-info { flex: 1; }
.price-panel { display: flex; align-items: baseline; background: #fff5f5; padding: 14px 16px; border-radius: 8px; }
@media (max-width: 768px) { .detail { flex-direction: column; } .detail > div:first-child { width: 100% !important; height: 260px !important; } }
</style>
