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
          <el-badge v-if="user.member" :value="unreadCount" :hidden="!unreadCount" :max="99">
            <el-button :icon="Bell" circle @click="$router.push('/messages')" />
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
                  <el-dropdown-item command="messages">我的消息<el-badge v-if="unreadCount" :value="unreadCount" :max="99" style="margin-left: 8px" /></el-dropdown-item>
                  <el-dropdown-item command="refunds">我的退款</el-dropdown-item>
                  <el-dropdown-item command="reviews">我的评价</el-dropdown-item>
                  <el-dropdown-item command="coupons">领券中心</el-dropdown-item>
                  <el-dropdown-item command="myCoupons">我的优惠券</el-dropdown-item>
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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, ShoppingCart, Bell } from '@element-plus/icons-vue'
import { useUserStore } from './store/user'
import { cartList, messageUnreadCount } from './api'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const keyword = ref('')
const cartCount = ref(0)
const unreadCount = ref(0)
let pollTimer = null

const showNav = computed(() => !['/login', '/register'].includes(route.path))

watch(() => route.query.keyword, v => { keyword.value = v || '' })

async function loadCartCount() {
  if (!user.token) { cartCount.value = 0; return }
  try {
    const list = await cartList()
    cartCount.value = (list || []).reduce((s, i) => s + i.quantity, 0)
  } catch { /* 未登录等场景忽略 */ }
}

async function loadUnread() {
  if (!user.token) { unreadCount.value = 0; return }
  try {
    const res = await messageUnreadCount()
    unreadCount.value = Number(res?.count || 0)
  } catch { /* 忽略 */ }
}

function doSearch() {
  router.push({ path: '/', query: keyword.value ? { keyword: keyword.value } : {} })
}

function onUserCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'orders') router.push('/orders')
  if (cmd === 'addresses') router.push('/addresses')
  if (cmd === 'reviews') router.push('/reviews')
  if (cmd === 'refunds') router.push('/refunds')
  if (cmd === 'coupons') router.push('/coupons')
  if (cmd === 'myCoupons') router.push('/my-coupons')
  if (cmd === 'messages') router.push('/messages')
  if (cmd === 'logout') { user.logout(); router.push('/login') }
}

watch(() => route.fullPath, () => { loadCartCount(); loadUnread() })

onMounted(() => {
  loadCartCount()
  loadUnread()
  pollTimer = setInterval(loadUnread, 60000)
})
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
defineExpose({ loadCartCount, loadUnread })
</script>
