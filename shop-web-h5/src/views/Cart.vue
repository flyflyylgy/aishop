<template>
  <div class="page-pad" style="min-height: 100vh; display: flex; flex-direction: column">
    <van-nav-bar title="购物车" />

    <van-empty v-if="list.length === 0" description="购物车空空如也">
      <van-button round type="danger" size="small" style="padding: 0 24px" @click="$router.push('/')">去逛逛</van-button>
    </van-empty>

    <template v-else>
      <van-checkbox-group v-model="checkedIds">
        <van-swipe-cell v-for="item in list" :key="item.id">
          <div class="cart-row">
            <van-checkbox :name="item.id" style="margin-right: 10px" @change="syncSelected(item)" />
            <div class="img-ph" :class="'c' + (item.productId % 5)" style="width: 72px; height: 72px; border-radius: 8px; font-size: 22px; flex-shrink: 0"
              @click="$router.push(`/product/${item.productId}`)">{{ item.productName?.[0] }}</div>
            <div style="flex: 1; min-width: 0; padding-left: 10px">
              <div style="font-size: 13px; line-height: 1.35">{{ item.productName }}</div>
              <div style="display: flex; align-items: center; margin-top: 10px">
                <span class="price">¥{{ money(item.price) }}</span>
                <van-stepper :model-value="item.quantity" :min="1" :max="item.availableStock || 99" theme="round"
                  style="margin-left: auto" @change="v => changeQty(item, v)" />
              </div>
            </div>
          </div>
          <template #right>
            <van-button square type="danger" text="删除" style="height: 100%" @click="remove(item)" />
          </template>
        </van-swipe-cell>
      </van-checkbox-group>

      <!-- 提交栏 -->
      <div style="margin-top: auto; position: sticky; bottom: 0">
        <van-submit-bar :price="totalPrice" button-text="去结算" :disabled="checkedIds.length === 0" @submit="goCheckout">
          <template #tip>
            <div style="display: flex; align-items: center; padding: 2px 16px">
              <van-checkbox checked-color="#ee0a24" :model-value="allChecked" @update:model-value="toggleAll">全选({{ checkedIds.length }})</van-checkbox>
              <van-button text type="danger" size="small" style="margin-left: auto" @click="clearAll">清空购物车</van-button>
            </div>
          </template>
        </van-submit-bar>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast } from 'vant'
import { cartList, cartQuantity, cartSelected, cartDelete, cartClear } from '../api'

const router = useRouter()
const refreshCartBadge = inject('refreshCartBadge')
const list = ref([])
const checkedIds = ref([])
const money = v => Number(v ?? 0).toFixed(2)

const checkedItems = computed(() => list.value.filter(i => checkedIds.value.includes(i.id)))
const totalPrice = computed(() => Math.round(checkedItems.value.reduce((s, i) => s + i.price * i.quantity, 0) * 100))
const allChecked = computed(() => list.value.length > 0 && checkedIds.value.length === list.value.length)

async function load() {
  list.value = (await cartList()) || []
  checkedIds.value = list.value.filter(i => i.selected === 1).map(i => i.id)
}

async function syncSelected(item) {
  await cartSelected(item.id, checkedIds.value.includes(item.id) ? 1 : 0)
}

async function toggleAll(v) {
  if (v) {
    await Promise.all(list.value.filter(i => i.selected !== 1).map(i => cartSelected(i.id, 1)))
  } else {
    await Promise.all(list.value.filter(i => i.selected === 1).map(i => cartSelected(i.id, 0)))
  }
  load()
}

async function changeQty(item, v) {
  await cartQuantity(item.id, v)
  refreshCartBadge?.()
}

async function remove(item) {
  await cartDelete(item.id)
  showSuccessToast('已删除')
  load()
  refreshCartBadge?.()
}

async function clearAll() {
  try {
    await showConfirmDialog({ title: '提示', message: '确定清空购物车？' })
  } catch { return }
  await cartClear()
  load()
  refreshCartBadge?.()
}

function goCheckout() { router.push('/checkout?from=cart') }

onMounted(load)
</script>

<style scoped>
.cart-row { display: flex; align-items: center; background: #fff; border-radius: 10px; padding: 12px; margin: 8px 12px 0; }
</style>
