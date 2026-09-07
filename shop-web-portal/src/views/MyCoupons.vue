<template>
  <div class="container page-bg">
    <el-card>
      <template #header>
        <div style="display: flex; align-items: center">
          <h3 style="margin: 0">我的优惠券</h3>
          <el-button text type="primary" style="margin-left: auto" @click="$router.push('/coupons')">去领券中心 →</el-button>
        </div>
      </template>

      <el-tabs v-model="active" @tab-change="load">
        <el-tab-pane label="未使用" name="0" />
        <el-tab-pane label="已使用" name="1" />
        <el-tab-pane label="已过期" name="2" />
      </el-tabs>

      <el-empty v-if="list.length === 0" description="暂无优惠券" />

      <div v-else class="coupon-grid">
        <div v-for="c in list" :key="c.historyId" class="coupon" :class="{ used: c.historyStatus !== 0 }">
          <div class="coupon-left" :class="typeClass(c.type)">
            <div class="cv">
              <template v-if="c.type === 2"><b>{{ (c.discount / 10).toFixed(1) }}</b><em>折</em></template>
              <template v-else><em>¥</em><b>{{ money(c.faceValue) }}</b></template>
            </div>
            <div class="threshold">{{ c.minPoint > 0 ? '满' + money(c.minPoint) + '可用' : '无门槛' }}</div>
          </div>
          <div class="coupon-right">
            <div class="cname">{{ c.name }}</div>
            <div class="cmeta">{{ typeText(c.type) }}</div>
            <div class="cmeta">有效期至 {{ c.expireTime ? c.expireTime.slice(0, 10) : '-' }}</div>
            <el-button v-if="c.historyStatus === 0" type="danger" size="small" round style="margin-top: auto; align-self: flex-end"
              @click="$router.push('/')">去使用</el-button>
            <el-tag v-else :type="c.historyStatus === 1 ? 'info' : 'warning'" size="small"
              style="margin-top: auto; align-self: flex-end">{{ c.historyStatus === 1 ? '已使用' : '已过期' }}</el-tag>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { myCoupons } from '../api'

const active = ref('0')
const list = ref([])

const money = v => Number(v ?? 0).toFixed(2)
const typeText = t => ({ 1: '满减券', 2: '折扣券', 3: '现金券' }[t] || '优惠券')
const typeClass = t => ({ 1: 't1', 2: 't2', 3: 't3' }[t] || 't1')

async function load() {
  const page = await myCoupons({ status: active.value, pageNum: 1, pageSize: 50 })
  list.value = page.records || []
}

onMounted(load)
</script>

<style scoped>
.coupon-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.coupon { display: flex; background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,.06); }
.coupon.used { opacity: .7; }
.coupon-left { width: 116px; color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 16px 8px; }
.coupon-left.t1 { background: linear-gradient(135deg, #f56c6c, #e23c3c); }
.coupon-left.t2 { background: linear-gradient(135deg, #e6a23c, #cf8a1a); }
.coupon-left.t3 { background: linear-gradient(135deg, #f89898, #ee5a5a); }
.coupon.used .coupon-left { background: linear-gradient(135deg, #b0b3b8, #909399); }
.cv b { font-size: 30px; font-weight: 800; }
.cv em { font-style: normal; font-size: 14px; margin: 0 2px; }
.threshold { font-size: 12px; margin-top: 4px; opacity: .92; }
.coupon-right { flex: 1; padding: 14px 16px; display: flex; flex-direction: column; gap: 4px; }
.cname { font-weight: 700; font-size: 15px; }
.cmeta { color: #909399; font-size: 12px; }
</style>
