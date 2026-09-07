<template>
  <div>
    <van-nav-bar title="领券中心" left-arrow @click-left="$router.back()">
      <template #right>
        <span style="font-size: 13px; color: #ee0a24" @click="$router.push('/my-coupons')">我的券</span>
      </template>
    </van-nav-bar>

    <van-empty v-if="list.length === 0" description="暂无可领取的优惠券" />

    <div style="padding: 12px">
      <div v-for="c in list" :key="c.id" class="coupon" :class="{ disabled: c.claimed }">
        <div class="c-left" :class="'t' + c.type">
          <div class="cv">
            <template v-if="c.type === 2"><b>{{ (c.discount / 10).toFixed(1) }}</b><em>折</em></template>
            <template v-else><em>¥</em><b>{{ money(c.faceValue) }}</b></template>
          </div>
          <div class="th">{{ Number(c.minPoint) > 0 ? '满' + money(c.minPoint) + '可用' : '无门槛' }}</div>
        </div>
        <div class="c-right">
          <div class="cname">{{ c.name }}</div>
          <div class="cmeta">{{ typeText(c.type) }} · 领后{{ c.validDays }}天有效</div>
          <div class="cmeta">剩 {{ c.remaining }} 张 · 限领 {{ c.perLimit }} 张</div>
          <van-button size="small" round type="danger" :disabled="c.claimed"
            :loading="claimingId === c.id" @click="claim(c)">
            {{ c.claimed ? '已领取' : '立即领取' }}
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { showSuccessToast } from 'vant'
import { couponCenter, couponReceive } from '../api'

const list = ref([])
const claimingId = ref(null)

const money = v => Number(v ?? 0).toFixed(2)
const typeText = t => ({ 1: '满减券', 2: '折扣券', 3: '现金券' }[t] || '优惠券')

async function load() {
  list.value = (await couponCenter()) || []
}

async function claim(c) {
  claimingId.value = c.id
  try {
    await couponReceive(c.id)
    showSuccessToast('领取成功')
    load()
  } finally {
    claimingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.coupon { display: flex; background: #fff; border-radius: 10px; overflow: hidden; margin-bottom: 12px; box-shadow: 0 2px 8px rgba(0,0,0,.05); }
.coupon.disabled { opacity: .6; }
.c-left { width: 104px; color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 14px 6px; }
.c-left.t1 { background: linear-gradient(135deg, #f56c6c, #e23c3c); }
.c-left.t2 { background: linear-gradient(135deg, #e6a23c, #cf8a1a); }
.c-left.t3 { background: linear-gradient(135deg, #f89898, #ee5a5a); }
.cv b { font-size: 26px; font-weight: 800; }
.cv em { font-style: normal; font-size: 13px; }
.th { font-size: 11px; margin-top: 3px; opacity: .92; }
.c-right { flex: 1; padding: 12px 14px; display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.cname { font-weight: 700; font-size: 14px; }
.cmeta { color: #969799; font-size: 11px; }
.c-right .van-button { margin-top: auto; align-self: flex-end; }
</style>
