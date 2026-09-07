import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { title: '注册' } },
  { path: '/', component: () => import('../views/Home.vue'), meta: { title: '首页' } },
  { path: '/product/:id', component: () => import('../views/ProductDetail.vue'), meta: { title: '商品详情' } },
  { path: '/cart', component: () => import('../views/Cart.vue'), meta: { title: '购物车', auth: true } },
  { path: '/checkout', component: () => import('../views/Checkout.vue'), meta: { title: '确认订单', auth: true } },
  { path: '/orders', component: () => import('../views/Orders.vue'), meta: { title: '我的订单', auth: true } },
  { path: '/addresses', component: () => import('../views/Addresses.vue'), meta: { title: '收货地址', auth: true } },
  { path: '/reviews', component: () => import('../views/MyReviews.vue'), meta: { title: '我的评价', auth: true } },
  { path: '/refunds', component: () => import('../views/MyRefunds.vue'), meta: { title: '我的退款', auth: true } },
  { path: '/coupons', component: () => import('../views/CouponCenter.vue'), meta: { title: '领券中心', auth: true } },
  { path: '/my-coupons', component: () => import('../views/MyCoupons.vue'), meta: { title: '我的优惠券', auth: true } },
  { path: '/profile', component: () => import('../views/Profile.vue'), meta: { title: '个人资料', auth: true } }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + 'Cloude Shop 云上商城'
  if (to.meta.auth && !localStorage.getItem('token')) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
