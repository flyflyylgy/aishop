<template>
  <div>
    <van-nav-bar title="我的优惠券" left-arrow @click-left="$router.back()">
      <template #right>
        <span style="font-size: 13px; color: #ee0a24" @click="$router.push('/coupons')">领券中心</span>
      </template>
    </van-nav-bar>

    <van-tabs v-model:active="active" @change="load" sticky>
      <van-tab title="未使用" name="0" />
      <van-tab title="已使用" name="1" />
      <van-tab title="已过期" name="2" />
    </van-tabs>

    <van-empty v-if="list.length === 0" description="暂无优惠券" />

    <div style="padding: 12px">
      <div v-for="c in list" :key="c.historyId" class="coupon" :class="{ used: c.historyStatus !== 0 }">
        <div class="c-left" :class="'t' + c.type">
          <div class="cv">
            <template v-if="c.type === 2"><b>{{ (c.discount / 10).toFixed(1) }}</b><em>折</em></template>
            <template v-else><em>¥</em><b>{{ money(c.faceValue) }}</b></template>
          </div>
          <div class="th">{{ Number(c.minPoint) > 0 ? '满' + money(c.minPoint) + '可用' : '无门槛' }}</div>
        </div>
        <div class="c-right">
          <div class="cname">{{ c.name }}</div>
          <div class="cmeta">{{ typeText(c.type) }}</div>
          <div class="cmeta">有效期至 {{ c.expireTime ? c.expireTime.slice(0, 10) : '-' }}</div>
          <van-button v-if="c.historyStatus === 0" size="mini" round type="danger" plain
            style="margin-top: auto; align-self: flex-end" @click="$router.push('/')">去使用</van-button>
          <span v-else class="state">{{ c.historyStatus === 1 ? '已使用' : '已过期' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { myCoupons } from '../api'

const active = ref('0')
const list = ref([])

const money = v => Number(v ?? 0).toFixed(2)
const typeText = t => ({ 1: '满减券', 2: '折扣券', 3: '现金券' }[t] || '优惠券')

async function load() {
  const page = await myCoupons({ status: active.value, pageNum: 1, pageSize: 50 })
  list.value = page.records || []
}

onMounted(load)
</script>

<style scoped>
.coupon { display: flex; background: #fff; border-radius: 10px; overflow: hidden; margin-bottom: 12px; box-shadow: 0 2px 8px rgba(0,0,0,.05); }
.coupon.used { opacity: .65; }
.c-left { width: 104px; color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 14px 6px; }
.c-left.t1 { background: linear-gradient(135deg, #f56c6c, #e23c3c); }
.c-left.t2 { background: linear-gradient(135deg, #e6a23c, #cf8a1a); }
.c-left.t3 { background: linear-gradient(135deg, #f89898, #ee5a5a); }
.coupon.used .c-left { background: linear-gradient(135deg, #b0b3b8, #909399); }
.cv b { font-size: 26px; font-weight: 800; }
.cv em { font-style: normal; font-size: 13px; }
.th { font-size: 11px; margin-top: 3px; opacity: .92; }
.c-right { flex: 1; padding: 12px 14px; display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.cname { font-weight: 700; font-size: 14px; }
.cmeta { color: #969799; font-size: 11px; }
.state { margin-top: auto; align-self: flex-end; font-size: 12px; color: #969799; }
</style>
