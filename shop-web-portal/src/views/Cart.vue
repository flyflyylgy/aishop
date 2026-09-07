<template>
  <div class="container page-bg">
    <el-card>
      <template #header><h3>购物车（{{ list.length }} 件）</h3></template>
      <el-empty v-if="list.length === 0" description="购物车空空如也">
        <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
      </el-empty>
      <template v-else>
        <el-table :data="list" style="width: 100%">
          <el-table-column width="60">
            <template #default="{ row }">
              <el-checkbox :model-value="row.selected === 1" @change="v => toggleSelected(row, v)" />
            </template>
          </el-table-column>
          <el-table-column label="商品" min-width="320">
            <template #default="{ row }">
              <div style="display: flex; gap: 12px; align-items: center; cursor: pointer" @click="$router.push(`/product/${row.productId}`)">
                <div class="img-ph" :class="'c' + (row.productId % 4)" style="width: 56px; height: 56px; border-radius: 6px; font-size: 20px">{{ row.productName?.[0] }}</div>
                <div>
                  <div>{{ row.productName }}</div>
                  <div v-if="row.specValues" style="color: #909399; font-size: 12px">{{ formatSpec(row.specValues) }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }"><span class="price">¥{{ money(row.price) }}</span></template>
          </el-table-column>
          <el-table-column label="数量" width="160">
            <template #default="{ row }">
              <el-input-number :model-value="row.quantity" :min="1" :max="row.availableStock || 99" size="small"
                @change="v => changeQuantity(row, v)" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="120">
            <template #default="{ row }"><span class="price">¥{{ money(row.price * row.quantity) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-popconfirm title="确定删除该商品？" @confirm="remove(row)">
                <template #reference><el-button type="danger" text>删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <div class="footer-bar">
          <div>
            <el-checkbox :model-value="allSelected" @change="toggleAll">全选</el-checkbox>
            <el-button text type="danger" style="margin-left: 16px" @click="clearAll">清空购物车</el-button>
          </div>
          <div style="display: flex; align-items: center; gap: 16px">
            <span>已选 <b>{{ selectedCount }}</b> 件，合计：<span class="price" style="font-size: 22px">¥{{ money(totalPrice) }}</span></span>
            <el-button type="danger" size="large" :disabled="selectedCount === 0" @click="goCheckout">去结算</el-button>
          </div>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cartList, cartQuantity, cartSelected, cartDelete, cartClear } from '../api'

const router = useRouter()
const list = ref([])
const money = v => Number(v ?? 0).toFixed(2)
const formatSpec = sv => {
  try {
    const o = JSON.parse(sv)
    return Object.entries(o).map(([k, v]) => `${k}:${v}`).join(' ')
  } catch { return sv }
}

const selectedItems = computed(() => list.value.filter(i => i.selected === 1))
const selectedCount = computed(() => selectedItems.value.reduce((s, i) => s + i.quantity, 0))
const totalPrice = computed(() => selectedItems.value.reduce((s, i) => s + i.price * i.quantity, 0))
const allSelected = computed(() => list.value.length > 0 && list.value.every(i => i.selected === 1))

async function load() { list.value = (await cartList()) || [] }

async function changeQuantity(row, v) {
  await cartQuantity(row.id, v)
  load()
}

async function toggleSelected(row, v) {
  await cartSelected(row.id, v ? 1 : 0)
  load()
}

async function toggleAll(v) {
  await Promise.all(list.value.filter(i => (i.selected === 1) !== v).map(i => cartSelected(i.id, v ? 1 : 0)))
  load()
}

async function remove(row) {
  await cartDelete(row.id)
  ElMessage.success('已删除')
  load()
}

async function clearAll() {
  await cartClear()
  load()
}

function goCheckout() {
  router.push({ path: '/checkout', query: { from: 'cart' } })
}

onMounted(load)
</script>

<style scoped>
.footer-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; padding: 14px 16px; background: #f8f9fb; border-radius: 8px; }
</style>
