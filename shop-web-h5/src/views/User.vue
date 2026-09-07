<template>
  <div class="page-pad">
    <!-- 用户头部 -->
    <div class="user-head">
      <template v-if="member">
        <van-avatar size="56" style="background: #fff; color: #ee0a24; font-size: 22px">{{ (member.nickname || member.username || 'U')[0] }}</van-avatar>
        <div style="margin-left: 14px">
          <div style="font-size: 17px; font-weight: 700">{{ member.nickname || member.username }}</div>
          <div style="font-size: 12px; opacity: .85; margin-top: 3px">欢迎回来，云上商城会员</div>
        </div>
      </template>
      <template v-else>
        <van-avatar size="56" style="background: #fff; color: #ee0a24; font-size: 22px">?</van-avatar>
        <div style="margin-left: 14px">
          <van-button round size="small" style="color: #ee0a24; font-weight: 700" @click="$router.push('/login')">点击登录</van-button>
          <div style="font-size: 12px; opacity: .85; margin-top: 6px">登录后享受更多服务</div>
        </div>
      </template>
    </div>

    <!-- 订单入口 -->
    <van-cell-group inset style="margin-top: 12px">
      <van-cell title="我的订单" icon="orders-o" is-link @click="goOrders" />
      <van-cell title="收货地址" icon="location-o" is-link @click="$router.push('/addresses')" />
      <van-grid :column-num="4" :border="false" style="padding: 8px 0 12px">
        <van-grid-item icon="credit-pay" text="待付款" @click="goOrders(0)" />
        <van-grid-item icon="logistics" text="已发货" @click="goOrders(2)" />
        <van-grid-item icon="completed" text="已完成" @click="goOrders(3)" />
        <van-grid-item icon="cart-o" text="购物车" @click="$router.push('/cart')" />
      </van-grid>
    </van-cell-group>

    <van-cell-group inset style="margin-top: 12px">
      <van-cell title="我的消息" icon="chat-o" is-link
        :value="member && unread ? unread + ' 条未读' : ''"
        @click="$router.push('/messages')" />
      <van-cell title="领券中心" icon="coupon-o" is-link @click="$router.push('/coupons')" />
      <van-cell title="我的优惠券" icon="coupon" is-link @click="$router.push('/my-coupons')" />
      <van-cell title="我的评价" icon="comment-o" is-link @click="$router.push('/reviews')" />
      <van-cell title="我的退款" icon="after-sale" is-link @click="$router.push('/refunds')" />
      <van-cell title="个人资料" icon="user-o" is-link @click="$router.push('/profile')" />
      <van-cell title="热销推荐" icon="fire-o" is-link @click="$router.push('/')" />
      <van-cell title="关于商城" icon="info-o" is-link
        @click="showAbout = true" />
    </van-cell-group>

    <div style="text-align: center; padding: 20px 0" v-if="member">
      <van-button plain round type="danger" size="small" style="padding: 0 40px" @click="logout">退出登录</van-button>
    </div>

    <van-dialog v-model:show="showAbout" title="关于 Cloude Shop" :show-cancel-button="false" confirm-button-text="知道了">
      <div style="padding: 8px 20px 18px; font-size: 13px; color: #646566; line-height: 1.7">
        企业级 Java 商城演示项目<br />
        Spring Boot 3 + MyBatis Plus + Redis + RabbitMQ<br />
        Vue 3 + Vant 4 H5 · 支持 JWT 登录、幂等下单、模拟支付、MQ 延迟关单
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast } from 'vant'
import { messageUnreadCount } from '../api'

const router = useRouter()
const member = ref(JSON.parse(localStorage.getItem('member') || 'null'))
const showAbout = ref(false)
const unread = ref(0)

async function loadUnread() {
  if (!localStorage.getItem('token')) { unread.value = 0; return }
  try {
    const res = await messageUnreadCount()
    unread.value = Number(res?.count || 0)
  } catch { /* 忽略 */ }
}
onMounted(loadUnread)
onActivated(loadUnread)

function goOrders(status) {
  router.push(status === undefined ? '/orders' : '/orders')
}

async function logout() {
  try {
    await showConfirmDialog({ title: '提示', message: '确定退出登录？' })
  } catch { return }
  localStorage.removeItem('token')
  localStorage.removeItem('member')
  member.value = null
  showSuccessToast('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.user-head { display: flex; align-items: center; padding: 26px 20px;
  background: linear-gradient(135deg, #ee0a24 0%, #ff6034 100%); color: #fff; }
</style>
