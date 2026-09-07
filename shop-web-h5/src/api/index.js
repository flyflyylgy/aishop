import axios from 'axios'
import { showFailToast, showSuccessToast } from 'vant'

const request = axios.create({ baseURL: '/portal-api', timeout: 15000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

request.interceptors.response.use(
  res => {
    const body = res.data
    if (body.code === 200) return body.data
    showFailToast(body.message || '操作失败')
    return Promise.reject(new Error(body.message))
  },
  err => {
    const status = err.response?.status
    const body = err.response?.data
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('member')
      if (!location.pathname.startsWith('/login')) {
        showFailToast('请先登录')
        location.href = '/login?redirect=' + encodeURIComponent(location.pathname)
      }
    } else {
      showFailToast(body?.message || err.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

export const toast = showSuccessToast

// ---------- 会员 ----------
export const register = data => request.post('/member/register', data)
export const login = data => request.post('/member/login', data)
export const captcha = () => request.get('/member/captcha')
export const memberInfo = () => request.get('/member/info')
export const updateProfile = data => request.post('/member/profile', data)
export const sendResetCode = data => request.post('/member/forgot/send-code', data)
export const resetPassword = data => request.post('/member/forgot/reset-password', data)

// ---------- 商品 ----------
export const productPage = params => request.get('/product/page', { params })
export const productDetail = id => request.get(`/product/${id}`)
export const productHot = (limit = 6) => request.get('/product/hot', { params: { limit } })
export const categoryTree = () => request.get('/product/category/tree')

// ---------- 购物车 ----------
export const cartList = () => request.get('/cart/list')
export const cartAdd = (productId, quantity = 1) => request.post('/cart/add', { productId, quantity })
export const cartQuantity = (itemId, quantity) => request.post(`/cart/quantity/${itemId}/${quantity}`)
export const cartSelected = (itemId, selected) => request.post(`/cart/selected/${itemId}/${selected}`)
export const cartDelete = itemId => request.post(`/cart/delete/${itemId}`)
export const cartClear = () => request.post('/cart/clear')

// ---------- 订单 ----------
export const orderCreate = (data, idempotentKey) =>
  request.post('/order/create', data, { headers: { 'Idempotency-Key': idempotentKey } })
export const orderPage = params => request.get('/order/page', { params })
export const orderDetail = orderNo => request.get(`/order/${orderNo}`)
export const orderCancel = orderNo => request.post(`/order/cancel/${orderNo}`)
export const orderConfirm = orderId => request.post(`/order/confirm/${orderId}`)

// ---------- 收货地址 ----------
export const addressList = () => request.get('/address/list')
export const addressCreate = data => request.post('/address/create', data)
export const addressUpdate = (id, data) => request.post(`/address/update/${id}`, data)
export const addressDelete = id => request.post(`/address/delete/${id}`)
export const addressSetDefault = id => request.post(`/address/default/${id}`)

// ---------- 支付（模拟通道） ----------
export const payCreate = orderNo => request.post(`/pay/create/${orderNo}`)
export const payNotifyMock = data => request.post('/pay/notify', data)

// ---------- 评价 ----------
export const reviewCreate = data => request.post('/review', data)
export const reviewPage = (productId, params) => request.get(`/review/product/${productId}`, { params })
export const reviewStats = productId => request.get(`/review/product/${productId}/stats`)

// ---------- 退款 ----------
export const refundApply = data => request.post('/review/refund/apply', data)
export const myRefunds = params => request.get('/review/refund/my', { params })
