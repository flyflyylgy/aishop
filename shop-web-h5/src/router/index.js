import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { title: '注册' } },
  { path: '/', component: () => import('../views/Home.vue'), meta: { title: '首页', tab: true } },
  { path: '/category', component: () => import('../views/Category.vue'), meta: { title: '分类', tab: true } },
  { path: '/cart', component: () => import('../views/Cart.vue'), meta: { title: '购物车', tab: true, auth: true } },
  { path: '/user', component: () => import('../views/User.vue'), meta: { title: '我的', tab: true } },
  { path: '/product/:id', component: () => import('../views/ProductDetail.vue'), meta: { title: '商品详情' } },
  { path: '/checkout', component: () => import('../views/Checkout.vue'), meta: { title: '确认订单', auth: true } },
  { path: '/orders', component: () => import('../views/Orders.vue'), meta: { title: '我的订单', auth: true } }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + 'Cloude Shop 云上商城'
  if (to.meta.auth && !localStorage.getItem('token')) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
