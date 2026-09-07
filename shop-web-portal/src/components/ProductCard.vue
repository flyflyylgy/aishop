<template>
  <div class="product-card" @click="$router.push(`/product/${product.id}`)">
    <div class="thumb img-ph" :class="'c' + (product.id % 4)">{{ product.name?.[0] }}</div>
    <div class="info">
      <div class="name">{{ product.name }}</div>
      <div class="price-row">
        <span class="price">¥{{ money(product.price) }}</span>
        <span v-if="product.originalPrice && product.originalPrice > product.price" class="price-origin">¥{{ money(product.originalPrice) }}</span>
        <span class="sale">已售 {{ product.sale || 0 }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ product: { type: Object, required: true } })
const money = v => Number(v ?? 0).toFixed(2)
</script>
