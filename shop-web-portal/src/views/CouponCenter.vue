<template>
  <div class="container page-bg">
    <el-card>
      <template #header>
        <div style="display: flex; align-items: center">
          <h3 style="margin: 0">领券中心</h3>
          <el-button text type="primary" style="margin-left: auto" @click="$router.push('/my-coupons')">我的优惠券 →</el-button>
        </div>
      </template>

      <el-empty v-if="list.length === 0" description="暂无可领取的优惠券" />

      <div v-else class="coupon-grid">
        <div v-for="c in list" :key="c.id" class="coupon" :class="{ disabled: c.claimed }">
          <div class="coupon-left" :class="typeClass(c.type)">
            <div class="cv">
              <template v-if="c.type === 2"><b>{{ (c.discount / 10).toFixed(1) }}</b><em>折</em></template>
              <template v-else><em>¥</em><b>{{ money(c.faceValue) }}</b></template>
            </div>
            <div class="threshold">{{ c.minPoint > 0 ? '满' + money(c.minPoint) + '可用' : '无门槛' }}</div>
          </div>
          <div class="coupon-right">
            <div class="cname">{{ c.name }}</div>
            <div class="cmeta">{{ typeText(c.type) }} · 领后 {{ c.validDays }} 天有效</div>
            <div class="cmeta">剩余 {{ c.remaining }} 张 · 限领 {{ c.perLimit }} 张</div>
            <el-button type="danger" size="small" round :disabled="c.claimed" :loading="claimingId === c.id"
              @click="claim(c)">{{ c.claimed ? '已领取' : '立即领取' }}</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { couponCenter, couponReceive } from '../api'

const router = useRouter()
const list = ref([])
const claimingId = ref(null)

const money = v => Number(v ?? 0).toFixed(2)
const typeText = t => ({ 1: '满减券', 2: '折扣券', 3: '现金券' }[t] || '优惠券')
const typeClass = t => ({ 1: 't1', 2: 't2', 3: 't3' }[t] || 't1')

async function load() {
  list.value = (await couponCenter()) || []
}

async function claim(c) {
  claimingId.value = c.id
  try {
    await couponReceive(c.id)
    ElMessage.success('领取成功')
    load()
  } finally {
    claimingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.coupon-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.coupon { display: flex; background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,.06); }
.coupon.disabled { opacity: .65; }
.coupon-left { width: 116px; color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 16px 8px; }
.coupon-left.t1 { background: linear-gradient(135deg, #f56c6c, #e23c3c); }
.coupon-left.t2 { background: linear-gradient(135deg, #e6a23c, #cf8a1a); }
.coupon-left.t3 { background: linear-gradient(135deg, #f89898, #ee5a5a); }
.cv b { font-size: 30px; font-weight: 800; }
.cv em { font-style: normal; font-size: 14px; margin: 0 2px; }
.threshold { font-size: 12px; margin-top: 4px; opacity: .92; }
.coupon-right { flex: 1; padding: 14px 16px; display: flex; flex-direction: column; gap: 4px; }
.cname { font-weight: 700; font-size: 15px; }
.cmeta { color: #909399; font-size: 12px; }
.coupon-right .el-button { margin-top: auto; align-self: flex-end; }
</style>
