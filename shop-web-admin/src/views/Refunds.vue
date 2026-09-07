<template>
  <div class="page-card">
    <el-card>
      <div class="table-toolbar">
        <el-radio-group v-model="query.status" @change="load">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button :value="0">待审核</el-radio-button>
          <el-radio-button :value="1">已同意</el-radio-button>
          <el-radio-button :value="2">已拒绝</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="list" v-loading="loading">
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="订单号" prop="orderNo" width="200" />
        <el-table-column label="会员ID" prop="memberId" width="100" />
        <el-table-column label="退款金额" width="120">
          <template #default="{ row }"><span class="price">¥{{ Number(row.amount).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="退款原因" prop="reason" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="{ 0: 'warning', 1: 'success', 2: 'danger' }[row.status]">
              {{ { 0: '待审核', 1: '已同意', 2: '已拒绝' }[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="管理员备注" prop="adminRemark" min-width="150" show-overflow-tooltip />
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="170">
          <template #default="{ row }">{{ fmt(row.handleTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="success" size="small" @click="openHandle(row, true)">同意</el-button>
            <el-button v-if="row.status === 0" type="danger" size="small" @click="openHandle(row, false)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: center; margin-top: 16px">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          :page-size="query.pageSize" :current-page="query.pageNum"
          @current-change="p => { query.pageNum = p; load() }" />
      </div>
    </el-card>

    <!-- 审批弹窗 -->
    <el-dialog v-model="handleDialog" :title="handleApproved ? '同意退款' : '拒绝退款'" width="440px">
      <el-form label-width="90px">
        <el-form-item label="订单号">
          <span>{{ handleRow?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <span class="price">¥{{ Number(handleRow?.amount || 0).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="退款原因">
          <span>{{ handleRow?.reason }}</span>
        </el-form-item>
        <el-form-item label="管理员备注">
          <el-input v-model="handleRemark" type="textarea" :rows="3" maxlength="200" show-word-limit
            :placeholder="handleApproved ? '可填写备注说明（选填）' : '请填写拒绝原因'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialog = false">取消</el-button>
        <el-button :type="handleApproved ? 'success' : 'danger'" :loading="saving" @click="submitHandle">
          确认{{ handleApproved ? '同意' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { refundPage, refundHandle } from '../api'

const fmt = t => t ? String(t).replace('T', ' ').slice(0, 19) : '-'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ status: null, pageNum: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.status !== null) params.status = query.status
    const page = await refundPage(params)
    list.value = page.records || []
    total.value = Number(page.total || 0)
  } finally {
    loading.value = false
  }
}

// 审批
const handleDialog = ref(false)
const handleRow = ref(null)
const handleApproved = ref(true)
const handleRemark = ref('')
const saving = ref(false)

function openHandle(row, approved) {
  handleRow.value = row
  handleApproved.value = approved
  handleRemark.value = ''
  handleDialog.value = true
}

async function submitHandle() {
  if (!handleApproved.value && !handleRemark.value?.trim()) {
    ElMessage.warning('拒绝时请填写原因')
    return
  }
  saving.value = true
  try {
    await refundHandle(handleRow.value.id, {
      approved: handleApproved.value,
      adminRemark: handleRemark.value.trim() || null
    })
    ElMessage.success(handleApproved.value ? '已同意退款' : '已拒绝退款')
    handleDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
