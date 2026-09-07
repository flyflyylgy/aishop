<template>
  <div class="page-card">
    <el-card>
      <div class="table-toolbar">
        <el-input v-model="query.memberId" placeholder="按会员 ID 过滤" clearable style="width: 180px" @keyup.enter="load" @clear="load" />
        <el-select v-model="query.operation" placeholder="操作类型" clearable style="width: 200px" @change="load">
          <el-option v-for="op in OP_OPTIONS" :key="op.value" :label="op.label" :value="op.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading" row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding: 8px 24px">
              <p v-if="row.params" style="margin: 4px 0">
                <b>请求参数：</b>
                <span style="font-family: monospace; white-space: pre-wrap; word-break: break-all">{{ pretty(row.params) }}</span>
              </p>
              <p v-if="row.errorMsg" style="margin: 4px 0; color: #f56c6c">
                <b>失败原因：</b>{{ row.errorMsg }}
              </p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="会员ID" prop="memberId" width="90" />
        <el-table-column label="账号" prop="username" width="120" />
        <el-table-column label="操作类型" width="170">
          <template #default="{ row }">
            <el-tag size="small" :type="opType(row.operation)">{{ opLabel(row.operation) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="请求" min-width="220">
          <template #default="{ row }">
            <el-tag size="small" :type="row.method === 'GET' ? 'info' : 'warning'" style="margin-right: 6px">{{ row.method }}</el-tag>
            <span style="font-family: monospace">{{ row.path }}</span>
          </template>
        </el-table-column>
        <el-table-column label="IP" prop="ip" width="130" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">{{ row.success === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ String(row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: center; margin-top: 16px">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          :page-size="query.pageSize" :current-page="query.pageNum"
          @current-change="p => { query.pageNum = p; load() }" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { memberLogPage } from '../api'

const OP_OPTIONS = [
  { value: 'MEMBER_REGISTER', label: '注册', type: 'primary' },
  { value: 'MEMBER_LOGIN', label: '登录', type: 'primary' },
  { value: 'MEMBER_LOGOUT', label: '退出登录', type: 'info' },
  { value: 'ORDER_CREATE', label: '下单', type: 'warning' },
  { value: 'ORDER_CANCEL', label: '取消订单', type: 'info' },
  { value: 'ORDER_CONFIRM', label: '确认收货', type: 'success' },
  { value: 'PAY_CREATE', label: '发起支付', type: 'warning' },
  { value: 'PAY_NOTIFY', label: '支付回调', type: 'success' },
  { value: 'CART_ADD', label: '加入购物车', type: 'info' },
  { value: 'CART_UPDATE_QUANTITY', label: '修改购物车数量', type: 'info' },
  { value: 'CART_DELETE', label: '删除购物车条目', type: 'info' },
  { value: 'CART_CLEAR', label: '清空购物车', type: 'danger' }
]

const opLabel = code => OP_OPTIONS.find(o => o.value === code)?.label || code
const opType = code => OP_OPTIONS.find(o => o.value === code)?.type || 'info'
const pretty = json => {
  try { return JSON.stringify(JSON.parse(json), null, 2) } catch { return json }
}

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ memberId: null, operation: null, pageNum: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.memberId) delete params.memberId
    if (!params.operation) delete params.operation
    const page = await memberLogPage(params)
    list.value = page.records || []
    total.value = Number(page.total || 0)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
