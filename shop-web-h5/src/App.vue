<template>
  <router-view v-slot="{ Component }">
    <component :is="Component" />
  </router-view>

  <van-tabbar route fixed placeholder v-if="$route.meta.tab">
    <van-tabbar-item replace to="/" icon="home-o">首页</van-tabbar-item>
    <van-tabbar-item replace to="/category" icon="apps-o">分类</van-tabbar-item>
    <van-tabbar-item replace to="/cart" icon="shopping-cart-o" :badge="cartBadge || ''">购物车</van-tabbar-item>
    <van-tabbar-item replace to="/user" icon="user-o">我的</van-tabbar-item>
  </van-tabbar>
</template>

<script setup>
import { onMounted, provide, ref } from 'vue'
import { cartList } from './api'

const cartBadge = ref(0)

async function refreshCartBadge() {
  if (!localStorage.getItem('token')) { cartBadge.value = 0; return }
  try {
    const list = (await cartList()) || []
    cartBadge.value = list.reduce((s, i) => s + i.quantity, 0) || ''
  } catch { /* 忽略 */ }
}

provide('refreshCartBadge', refreshCartBadge)
onMounted(refreshCartBadge)
</script>
