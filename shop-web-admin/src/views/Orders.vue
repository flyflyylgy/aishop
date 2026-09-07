<template>
  <div class="page-card">
    <el-card>
      <div class="table-toolbar">
        <el-radio-group v-model="query.status" @change="load">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button :value="0">待付款</el-radio-button>
          <el-radio-button :value="1">已付款</el-radio-button>
          <el-radio-button :value="2">已发货</el-radio-button>
          <el-radio-button :value="3">已完成</el-radio-button>
          <el-radio-button :value="4">已关闭</el-radio-button>
        </el-radio-group>
        <el-input v-model="query.keyword" placeholder="订单号/收货人/电话" clearable style="width: 230px"
          @keyup.enter="load" @clear="load" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading">
        <el-table-column label="订单号" prop="orderNo" width="210" />
        <el-table-column label="收货人" prop="receiverName" width="100" />
        <el-table-column label="电话" prop="receiverPhone" width="130" />
        <el-table-column label="实付金额" width="110">
          <template #default="{ row }"><span class="price">¥{{ Number(row.payAmount).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="{ 0: 'warning', 1: 'primary', 2: 'success', 3: 'info', 4: 'danger' }[row.status]">
              {{ row.statusDesc || { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已关闭' }[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1 && hasPerm('shop:order:ship')" type="primary" size="small" @click="openShip(row)">发货</el-button>
            <el-button v-if="hasPerm('shop:order:remark')" size="small" type="warning" @click="openRemark(row)">备注</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: center; margin-top: 16px">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          :page-size="query.pageSize" :current-page="query.pageNum"
          @current-change="p => { query.pageNum = p; load() }" />
      </div>
    </el-card>

    <!-- 订单详情抽屉 -->
    <el-drawer v-model="detailDrawer" title="订单详情" size="520px">
      <div v-if="detail" v-loading="detailLoading">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="{ 0: 'warning', 1: 'primary', 2: 'success', 3: 'info', 4: 'danger' }[detail.status]" size="small">
              {{ detail.statusDesc || { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已关闭' }[detail.status] }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detail.receiverName }} {{ detail.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址">{{ detail.receiverAddr }}</el-descriptions-item>
          <el-descriptions-item label="买家备注">{{ detail.note || '-' }}</el-descriptions-item>
          <el-descriptions-item label="物流公司">{{ detail.expressCompany || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运单号">{{ detail.expressNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ fmt(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ fmt(detail.payTime) }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ fmt(detail.shipTime) }}</el-descriptions-item>
          <el-descriptions-item label="后台备注">
            <span style="white-space: pre-wrap">{{ detail.adminRemark || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 16px 0 8px">商品明细</h4>
        <div v-for="it in detail.items || []" :key="it.id"
          style="display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px dashed #ebeef5">
          <div class="img-ph" :class="'c' + (it.productId % 4)"
            style="width: 44px; height: 44px; border-radius: 6px; font-size: 16px; text-align: center; line-height: 44px">{{ it.productName?.[0] }}</div>
          <span style="flex: 1">{{ it.productName }}</span>
          <span>¥{{ Number(it.price).toFixed(2) }} × {{ it.quantity }}</span>
          <span class="price">¥{{ Number(it.totalPrice).toFixed(2) }}</span>
        </div>
        <div style="text-align: right; margin-top: 12px; font-weight: 600">
          实付：<span class="price">¥{{ Number(detail.payAmount).toFixed(2) }}</span>
        </div>
      </div>
    </el-drawer>

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipDialog" title="订单发货" width="440px">
      <el-form ref="shipRef" :model="shipForm" :rules="shipRules" label-width="90px">
        <el-form-item label="订单号">
          <el-input :model-value="shipOrder?.orderNo" disabled />
        </el-form-item>
        <el-form-item label="物流公司" prop="expressCompany">
          <el-select v-model="shipForm.expressCompany" placeholder="选择物流公司" style="width: 100%">
            <el-option v-for="c in expressCompanies" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单号" prop="expressNo">
          <el-input v-model="shipForm.expressNo" placeholder="请输入运单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- 后台备注弹窗 -->
    <el-dialog v-model="remarkDialog" title="后台备注" width="440px">
      <el-input v-model="remarkText" type="textarea" :rows="4" placeholder="仅后台可见，不会展示给买家" />
      <template #footer>
        <el-button @click="remarkDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRemark">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { orderPage, orderDetail, orderShip, orderRemark } from '../api'
import { hasPerm } from '../utils/perm'

const fmt = t => t ? String(t).replace('T', ' ').slice(0, 19) : ''
const expressCompanies = ['顺丰速运', '京东物流', '中通快递', '圆通速递', '韵达快递', '申通快递', '邮政EMS']

const list = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ status: null, keyword: '', pageNum: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const page = await orderPage(query)
    list.value = page.records || []
    total.value = Number(page.total || 0)
  } finally {
    loading.value = false
  }
}

// 详情
const detailDrawer = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
async function openDetail(row) {
  detailDrawer.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await orderDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

// 发货
const shipDialog = ref(false)
const shipRef = ref()
const shipOrder = ref(null)
const shipForm = reactive({ expressCompany: '', expressNo: '' })
const shipRules = {
  expressCompany: [{ required: true, message: '请选择物流公司', trigger: 'change' }],
  expressNo: [{ required: true, message: '请输入运单号', trigger: 'blur' }]
}
function openShip(row) {
  shipOrder.value = row
  shipForm.expressCompany = ''
  shipForm.expressNo = ''
  shipDialog.value = true
}
async function submitShip() {
  await shipRef.value.validate()
  saving.value = true
  try {
    await orderShip(shipOrder.value.id, { ...shipForm })
    ElMessage.success('发货成功')
    shipDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

// 备注
const remarkDialog = ref(false)
const remarkText = ref('')
const remarkOrder = ref(null)
function openRemark(row) {
  remarkOrder.value = row
  remarkText.value = row.adminRemark || ''
  remarkDialog.value = true
}
async function submitRemark() {
  saving.value = true
  try {
    await orderRemark(remarkOrder.value.id, remarkText.value)
    ElMessage.success('备注已保存')
    remarkDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
