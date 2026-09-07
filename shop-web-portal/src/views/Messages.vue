<template>
  <div class="container">
    <div class="section-title">
      <h2>我的消息</h2>
      <el-button type="primary" text :disabled="!unread" @click="readAll">全部标记已读</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTab">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="未读" name="unread" />
      <el-tab-pane label="已读" name="read" />
    </el-tabs>

    <el-empty v-if="!loading && records.length === 0" description="暂无消息" />

    <div v-loading="loading" class="msg-list">
      <div v-for="m in records" :key="m.id" class="msg-item" :class="{ unread: m.isRead === 0 }"
           @click="openMessage(m)">
        <div class="msg-head">
          <el-tag size="small" :type="typeTag(m.type)">{{ typeText(m.type) }}</el-tag>
          <span class="msg-title">{{ m.title }}</span>
          <span v-if="m.isRead === 0" class="dot" />
          <span class="msg-time">{{ fmt(m.createTime) }}</span>
        </div>
        <div class="msg-content">{{ m.content }}</div>
      </div>
    </div>

    <div style="display: flex; justify-content: center; margin-top: 20px" v-if="total > query.pageSize">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.pageSize"
        :current-page="query.pageNum" @current-change="onPage" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { messagePage, messageRead, messageReadAll, messageUnreadCount } from '../api'

const router = useRouter()
const records = ref([])
const total = ref(0)
const unread = ref(0)
const loading = ref(false)
const activeTab = ref('all')
const query = reactive({ pageNum: 1, pageSize: 10, isRead: null })

const TYPE_MAP = { 1: '订单', 2: '支付', 3: '物流', 4: '售后', 5: '系统' }
const typeText = t => TYPE_MAP[t] || '系统'
const typeTag = t => ({ 1: 'primary', 2: 'success', 3: 'warning', 4: 'danger', 5: 'info' }[t] || 'info')
const fmt = t => t ? String(t).replace('T', ' ').slice(0, 16) : ''

async function load() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.isRead !== null) params.isRead = query.isRead
    const page = await messagePage(params)
    records.value = page.records || []
    total.value = Number(page.total || 0)
  } finally { loading.value = false }
}

async function refreshUnread() {
  const res = await messageUnreadCount()
  unread.value = Number(res?.count || 0)
}

function onTab(name) {
  query.isRead = name === 'unread' ? 0 : name === 'read' ? 1 : null
  query.pageNum = 1
  load()
}
function onPage(p) { query.pageNum = p; load() }

async function openMessage(m) {
  if (m.isRead === 0) {
    await messageRead(m.id)
    m.isRead = 1
    refreshUnread()
  }
  if (m.bizType === 'order' && m.bizId) router.push('/orders')
  else if (m.bizType === 'refund') router.push('/refunds')
}

async function readAll() {
  await messageReadAll()
  ElMessage.success('已全部标记为已读')
  load()
  refreshUnread()
}

onMounted(() => { load(); refreshUnread() })
</script>

<style scoped>
.msg-list { margin-top: 8px; }
.msg-item {
  padding: 14px 16px; border: 1px solid #ebeef5; border-radius: 8px;
  margin-bottom: 12px; cursor: pointer; transition: box-shadow .2s;
}
.msg-item:hover { box-shadow: 0 2px 12px rgba(0,0,0,.08); }
.msg-item.unread { border-left: 3px solid #409eff; background: #f5f9ff; }
.msg-head { display: flex; align-items: center; gap: 10px; }
.msg-title { font-weight: 600; }
.msg-time { margin-left: auto; color: #909399; font-size: 12px; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #f56c6c; }
.msg-content { margin-top: 8px; color: #606266; font-size: 14px; line-height: 1.6; }
</style>
