<template>
  <div>
    <van-nav-bar title="我的消息" left-arrow @click-left="$router.back()" fixed placeholder>
      <template #right>
        <span style="font-size: 13px; color: #1989fa" @click="readAll">全部已读</span>
      </template>
    </van-nav-bar>

    <van-tabs v-model:active="activeTab" @change="onTab" sticky>
      <van-tab title="全部" name="all" />
      <van-tab title="未读" name="unread" />
      <van-tab title="已读" name="read" />
    </van-tabs>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh" class="list-area">
      <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="onLoad">
        <div v-for="m in list" :key="m.id" class="msg-item" :class="{ unread: m.isRead === 0 }" @click="openMessage(m)">
          <div class="msg-head">
            <van-tag plain :type="tagType(m.type)">{{ typeText(m.type) }}</van-tag>
            <span class="msg-title">{{ m.title }}</span>
            <span v-if="m.isRead === 0" class="dot" />
          </div>
          <div class="msg-content">{{ m.content }}</div>
          <div class="msg-time">{{ fmt(m.createTime) }}</div>
        </div>
        <van-empty v-if="!loading && list.length === 0" description="暂无消息" />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { messagePage, messageRead, messageReadAll } from '../api'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const activeTab = ref('all')
const pageNum = ref(1)
const pageSize = 10

const TYPE_MAP = { 1: '订单', 2: '支付', 3: '物流', 4: '售后', 5: '系统' }
const typeText = t => TYPE_MAP[t] || '系统'
const tagType = t => ({ 1: 'primary', 2: 'success', 3: 'warning', 4: 'danger', 5: 'default' }[t] || 'default')
const fmt = t => t ? String(t).replace('T', ' ').slice(0, 16) : ''

function isReadFilter() {
  return activeTab.value === 'unread' ? 0 : activeTab.value === 'read' ? 1 : null
}

async function onLoad() {
  try {
    const params = { pageNum: pageNum.value, pageSize }
    const f = isReadFilter()
    if (f !== null) params.isRead = f
    const page = await messagePage(params)
    const records = page.records || []
    list.value.push(...records)
    finished.value = list.value.length >= Number(page.total || 0) || records.length < pageSize
    pageNum.value += 1
  } finally {
    loading.value = false
    if (refreshing.value) refreshing.value = false
  }
}

function reset() {
  list.value = []
  finished.value = false
  loading.value = true
  pageNum.value = 1
  onLoad()
}

function onTab() { reset() }

function onRefresh() { reset() }

async function openMessage(m) {
  if (m.isRead === 0) {
    await messageRead(m.id)
    m.isRead = 1
  }
  if (m.bizType === 'order' && m.bizId) router.push('/orders')
  else if (m.bizType === 'refund') router.push('/refunds')
}

async function readAll() {
  await messageReadAll()
  showSuccessToast('已全部标记为已读')
  reset()
}
</script>

<style scoped>
.list-area { min-height: 60vh; }
.msg-item { margin: 10px 12px; padding: 12px 14px; background: #fff; border-radius: 8px;
  border: 1px solid #f2f2f2; }
.msg-item.unread { border-left: 3px solid #1989fa; background: #f7fbff; }
.msg-head { display: flex; align-items: center; gap: 8px; }
.msg-title { font-weight: 600; font-size: 15px; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #ee0a24; margin-left: auto; }
.msg-content { margin-top: 8px; color: #646566; font-size: 13px; line-height: 1.6; }
.msg-time { margin-top: 8px; color: #969799; font-size: 12px; }
</style>
