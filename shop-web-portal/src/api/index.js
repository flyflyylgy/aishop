import axios from 'axios'
import { ElMessage } from 'element-plus'

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
    ElMessage.error(body.message || '操作失败')
    return Promise.reject(new Error(body.message))
  },
  err => {
    const status = err.response?.status
    const body = err.response?.data
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('member')
      if (!location.pathname.startsWith('/login')) {
        ElMessage.warning('请先登录')
        location.href = '/login?redirect=' + encodeURIComponent(location.pathname)
      }
    } else {
      ElMessage.error(body?.message || err.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

// ---------- 会员 ----------
export const captcha = () => request.get('/member/captcha')
export const register = data => request.post('/member/register', data)
export const login = data => request.post('/member/login', data)

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

// ---------- 支付（模拟通道） ----------
export const payCreate = orderNo => request.post(`/pay/create/${orderNo}`)
// 模拟第三方支付平台向商城发起异步回调（演示闭环，生产环境由支付服务器调用）
export const payNotifyMock = data => request.post('/pay/notify', data)
