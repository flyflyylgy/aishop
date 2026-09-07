import { createRouter, createWebHistory } from 'vue-router'
import { hasPerm } from '../utils/perm'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '仪表盘', icon: 'Odometer' } },
      { path: 'products', component: () => import('../views/Products.vue'), meta: { title: '商品管理', icon: 'Goods', perm: 'shop:product:list' } },
      { path: 'categories', component: () => import('../views/Categories.vue'), meta: { title: '分类管理', icon: 'Menu', perm: 'shop:product:list' } },
      { path: 'orders', component: () => import('../views/Orders.vue'), meta: { title: '订单管理', icon: 'List', perm: 'shop:order:list' } },
      { path: 'members', component: () => import('../views/Members.vue'), meta: { title: '会员管理', icon: 'Avatar', perm: 'shop:member:list' } },
      { path: 'admins', component: () => import('../views/Admins.vue'), meta: { title: '管理员管理', icon: 'User', perm: 'shop:admin:list' } },
      { path: 'roles', component: () => import('../views/Roles.vue'), meta: { title: '角色权限', icon: 'UserFilled', perm: 'shop:rbac:list' } },
      { path: 'logs', component: () => import('../views/Logs.vue'), meta: { title: '管理审计日志', icon: 'Document', perm: 'shop:rbac:list' } },
      { path: 'member-logs', component: () => import('../views/MemberLogs.vue'), meta: { title: '会员操作日志', icon: 'Tickets', perm: 'shop:rbac:list' } },
      { path: 'reviews', component: () => import('../views/Reviews.vue'), meta: { title: '评价管理', icon: 'ChatLineSquare', perm: 'shop:review:list' } },
      { path: 'refunds', component: () => import('../views/Refunds.vue'), meta: { title: '退款审批', icon: 'Wallet', perm: 'shop:refund:handle' } },
      { path: 'coupons', component: () => import('../views/Coupons.vue'), meta: { title: '优惠券', icon: 'Ticket', perm: 'shop:coupon:list' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + 'Cloude Shop 管理后台'
  if (to.path !== '/login' && !localStorage.getItem('admin_token')) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && localStorage.getItem('admin_token')) {
    return '/dashboard'
  }
  // 无权限路由不可直接访问
  if (to.meta.perm && !hasPerm(to.meta.perm)) {
    return '/dashboard'
  }
})

export default router
