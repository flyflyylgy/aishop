<template>
  <el-container>
    <el-header v-if="showNav" class="navbar" height="60px">
      <div class="navbar-inner">
        <div class="logo" @click="$router.push('/')">Cloude <span>Shop</span> 云上商城</div>
        <el-input
          v-model="keyword"
          placeholder="搜索商品"
          style="max-width: 360px"
          clearable
          @keyup.enter="doSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="doSearch" />
          </template>
        </el-input>
        <div style="margin-left: auto; display: flex; align-items: center; gap: 16px">
          <el-badge :value="cartCount" :hidden="!cartCount">
            <el-button :icon="ShoppingCart" circle @click="$router.push('/cart')" />
          </el-badge>
          <template v-if="user.member">
            <el-dropdown @command="onUserCommand">
              <span style="cursor: pointer; display: flex; align-items: center; gap: 4px">
                <el-avatar :size="30" style="background: #409eff">{{ user.member.nickname?.[0] || user.member.username?.[0] }}</el-avatar>
                {{ user.member.nickname || user.member.username }}
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="addresses">收货地址</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <el-main style="padding: 0; min-height: calc(100vh - 60px)">
      <router-view @cart-changed="loadCartCount" />
    </el-main>

    <el-footer height="60px" style="display: flex; align-items: center; justify-content: center; color: #909399; font-size: 13px">
      Cloude Shop 云上商城 · Spring Boot + Vue 3 企业级电商演示
    </el-footer>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, ShoppingCart } from '@element-plus/icons-vue'
import { useUserStore } from './store/user'
import { cartList } from './api'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const keyword = ref('')
const cartCount = ref(0)

const showNav = computed(() => !['/login', '/register'].includes(route.path))

watch(() => route.query.keyword, v => { keyword.value = v || '' })

async function loadCartCount() {
  if (!user.token) { cartCount.value = 0; return }
  try {
    const list = await cartList()
    cartCount.value = (list || []).reduce((s, i) => s + i.quantity, 0)
  } catch { /* 未登录等场景忽略 */ }
}

function doSearch() {
  router.push({ path: '/', query: keyword.value ? { keyword: keyword.value } : {} })
}

function onUserCommand(cmd) {
  if (cmd === 'orders') router.push('/orders')
  if (cmd === 'addresses') router.push('/addresses')
  if (cmd === 'logout') { user.logout(); router.push('/login') }
}

onMounted(loadCartCount)
defineExpose({ loadCartCount })
</script>
