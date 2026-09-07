<template>
  <div class="page-card">
    <el-card>
      <div class="table-toolbar">
        <el-input v-model="query.keyword" placeholder="用户名/昵称/手机号" clearable style="width: 220px"
          @keyup.enter="load" @clear="load" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load">
          <el-option label="正常" :value="1" />
          <el-option label="封禁" :value="0" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button :loading="exporting" @click="doExport">导出Excel</el-button>
      </div>

      <el-table :data="list" v-loading="loading">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="用户名" prop="username" width="140" />
        <el-table-column label="昵称" prop="nickname" width="140" />
        <el-table-column label="手机号" prop="phone" width="140" />
        <el-table-column label="订单数" prop="orderCount" width="90" />
        <el-table-column label="累计消费" width="120">
          <template #default="{ row }"><span class="price">¥{{ Number(row.totalAmount || 0).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '封禁' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-popconfirm v-if="row.status === 1" :title="`确认封禁会员「${row.username}」？封禁后立即踢下线`" @confirm="changeStatus(row, 0)">
              <template #reference><el-button size="small" type="danger">封禁</el-button></template>
            </el-popconfirm>
            <el-popconfirm v-else :title="`确认解封「${row.username}」？`" @confirm="changeStatus(row, 1)">
              <template #reference><el-button size="small" type="success">解封</el-button></template>
            </el-popconfirm>
          </template>
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
import { ElMessage } from 'element-plus'
import { memberPage, memberStatus, exportMembers } from '../api'

const fmt = t => t ? String(t).replace('T', ' ').slice(0, 19) : ''

const list = ref([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const query = reactive({ keyword: '', status: null, pageNum: 1, pageSize: 10 })

async function doExport() {
  exporting.value = true
  try {
    const p = {}
    if (query.keyword) p.keyword = query.keyword
    if (query.status !== null && query.status !== '') p.status = query.status
    await exportMembers(p)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const page = await memberPage(query)
    list.value = page.records || []
    total.value = Number(page.total || 0)
  } finally {
    loading.value = false
  }
}

async function changeStatus(row, status) {
  await memberStatus(row.id, status)
  ElMessage.success(status === 0 ? '已封禁，登录态已吊销' : '已解封')
  load()
}

onMounted(load)
</script>
