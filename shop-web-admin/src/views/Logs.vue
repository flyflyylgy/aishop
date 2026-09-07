<template>
  <div class="page-card">
    <el-card>
      <div class="table-toolbar">
        <el-input v-model="query.adminId" placeholder="按管理员 ID 过滤" clearable style="width: 200px" @keyup.enter="load" @clear="load" />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="管理员ID" prop="adminId" width="100" />
        <el-table-column label="权限编码" prop="operation" width="170" />
        <el-table-column label="请求" min-width="260">
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
        <el-table-column label="时间" width="180">
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
import { logPage } from '../api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ adminId: null, pageNum: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.adminId) delete params.adminId
    const page = await logPage(params)
    list.value = page.records || []
    total.value = Number(page.total || 0)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
