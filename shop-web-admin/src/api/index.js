import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({ baseURL: '/admin-api', timeout: 15000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('admin_token')
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
      localStorage.removeItem('admin_token')
      if (!location.pathname.startsWith('/login')) {
        ElMessage.warning('登录已失效，请重新登录')
        location.href = '/login'
      }
    } else {
      ElMessage.error(body?.message || err.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

// ---------- 认证 ----------
export const adminCaptcha = () => request.get('/admin/captcha')
export const adminLogin = data => request.post('/admin/login', data)
export const adminLogout = () => request.post('/admin/logout')
export const adminPassword = data => request.post('/admin/password', data)

// ---------- 商品 ----------
export const productPage = params => request.get('/product/page', { params })
export const productCreate = data => request.post('/product/create', data)
export const productUpdate = (id, data) => request.post(`/product/update/${id}`, data)
export const productStatus = (id, status) => request.post(`/product/status/${id}/${status}`)
export const productDelete = id => request.post(`/product/delete/${id}`)
export const productCategories = () => request.get('/product/categories')
export const productSkus = productId => request.get(`/product/skus/${productId}`)
export const productSaveSkus = (productId, data) => request.post(`/product/skus/${productId}`, data)

// ---------- 分类 ----------
export const categoryList = () => request.get('/category/list')
export const categoryCreate = data => request.post('/category/create', data)
export const categoryUpdate = (id, data) => request.post(`/category/update/${id}`, data)
export const categoryDelete = id => request.post(`/category/delete/${id}`)

// ---------- 订单 ----------
export const orderPage = params => request.get('/order/page', { params })
export const orderDetail = id => request.get(`/order/${id}`)
export const orderShip = (orderId, data) => request.post(`/order/ship/${orderId}`, data)
export const orderRemark = (orderId, remark) => request.post(`/order/remark/${orderId}`, { remark })

// ---------- 管理员 ----------
export const adminUserPage = params => request.get('/admin-user/page', { params })
export const adminUserCreate = data => request.post('/admin-user/create', data)
export const adminUserUpdate = (id, data) => request.post(`/admin-user/update/${id}`, data)
export const adminUserResetPwd = (id, newPassword) => request.post(`/admin-user/reset-password/${id}`, { newPassword })
export const adminUserAssignRoles = (id, roleIds) => request.post(`/admin-user/roles/${id}`, { roleIds })

// ---------- 会员 ----------
export const memberPage = params => request.get('/member/page', { params })
export const memberStatus = (id, status) => request.post(`/member/status/${id}/${status}`)

// ---------- RBAC ----------
export const roleList = () => request.get('/role/list')
export const permissionList = () => request.get('/permission/list')
export const rolePermissionIds = roleId => request.get('/role/permissions', { params: { roleId } })
export const saveRolePermissions = (roleId, permissionIds) => request.post('/role/permissions', { roleId, permissionIds })
export const logPage = params => request.get('/log/page', { params })
export const memberLogPage = params => request.get('/member-log/page', { params })

// ---------- 评价管理 ----------
export const reviewPage = params => request.get('/review-refund/review/page', { params })

// ---------- 退款审批 ----------
export const refundPage = params => request.get('/review-refund/refund/page', { params })
export const refundHandle = (id, data) => request.post(`/review-refund/refund/handle/${id}`, data)

// ---------- 优惠券 ----------
export const couponPage = params => request.get('/coupon/page', { params })
export const couponCreate = data => request.post('/coupon/create', data)
export const couponUpdate = (id, data) => request.post(`/coupon/update/${id}`, data)
export const couponStatus = (id, status) => request.post(`/coupon/status/${id}/${status}`)

// ---------- 首页统计 ----------
export const statsDashboard = () => request.get('/stats/dashboard')
