<template>
  <div style="padding: 16px">
    <el-card>
      <template #header>
        <div style="display: flex; align-items: center; gap: 12px">
          <h3 style="margin: 0">优惠券管理</h3>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
          <el-select v-model="query.type" placeholder="类型" clearable style="width: 130px" @change="load">
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
            <el-option label="现金券" :value="3" />
          </el-select>
          <el-input v-model="query.keyword" placeholder="券名称" clearable style="width: 180px" @keyup.enter="load" />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" style="margin-left: auto" @click="openCreate">新建优惠券</el-button>
          <el-button :loading="exporting" @click="doExport">导出Excel</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column label="券名称" prop="name" min-width="160" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠" width="130">
          <template #default="{ row }">
            <span v-if="row.type === 2">{{ (row.discount / 10).toFixed(1) }} 折</span>
            <span v-else>减 ¥{{ money(row.faceValue) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="门槛" width="110">
          <template #default="{ row }">
            {{ row.minPoint > 0 ? '满 ¥' + money(row.minPoint) : '无门槛' }}
          </template>
        </el-table-column>
        <el-table-column label="领取/总量" width="110">
          <template #default="{ row }">{{ row.receivedCount }} / {{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="已用" prop="usedCount" width="70" />
        <el-table-column label="限领" prop="perLimit" width="60" />
        <el-table-column label="领取周期" min-width="240">
          <template #default="{ row }">
            <div style="font-size: 12px; color: #606266">{{ row.startTime }} ~ {{ row.endTime }}</div>
            <div style="font-size: 12px; color: #909399">领后 {{ row.validDays }} 天有效</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination style="margin-top: 16px; justify-content: flex-end" background layout="total, prev, pager, next"
        :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
        @current-change="p => { query.pageNum = p; load() }" />
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑优惠券' : '新建优惠券'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="券名称" prop="name">
          <el-input v-model="form.name" placeholder="如：满100减20" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减券</el-radio>
            <el-radio :value="2">折扣券</el-radio>
            <el-radio :value="3">现金券(无门槛)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type === 2" label="折扣" prop="discount">
          <el-input-number v-model="form.discount" :min="1" :max="99" :step="1" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px">85 表示 8.5 折</span>
        </el-form-item>
        <el-form-item v-else label="面额(元)" prop="faceValue">
          <el-input-number v-model="form.faceValue" :min="0.01" :precision="2" :step="5" />
        </el-form-item>
        <el-form-item label="门槛(元)" prop="minPoint">
          <el-input-number v-model="form.minPoint" :min="0" :precision="2" :step="10" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px">0 为无门槛</span>
        </el-form-item>
        <el-form-item label="发行总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" :step="100" />
        </el-form-item>
        <el-form-item label="每人限领" prop="perLimit">
          <el-input-number v-model="form.perLimit" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="领取开始" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="领取结束" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效天数" prop="validDays">
          <el-input-number v-model="form.validDays" :min="1" :step="7" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px">领取后 N 天内有效</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { couponPage, couponCreate, couponUpdate, couponStatus, exportCoupons } from '../api'

const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ status: null, type: null, keyword: '', pageNum: 1, pageSize: 10 })

const dialog = ref(false)
const formRef = ref()
const form = reactive({})

const rules = {
  name: [{ required: true, message: '请输入券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  totalCount: [{ required: true, message: '请输入发行总量', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const money = v => Number(v ?? 0).toFixed(2)
const typeText = t => ({ 1: '满减券', 2: '折扣券', 3: '现金券' }[t] || '未知')
const typeTag = t => ({ 1: 'danger', 2: 'warning', 3: 'success' }[t] || 'info')

async function load() {
  loading.value = true
  try {
    const page = await couponPage(query)
    list.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: null, name: '', type: 1, faceValue: 10, discount: 85, minPoint: 100,
    totalCount: 100, perLimit: 1, validDays: 30, startTime: '', endTime: '', status: 1
  })
}

function openCreate() {
  resetForm()
  dialog.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id, name: row.name, type: row.type,
    faceValue: row.faceValue ? Number(row.faceValue) : null,
    discount: row.discount, minPoint: Number(row.minPoint),
    totalCount: row.totalCount, perLimit: row.perLimit, validDays: row.validDays,
    startTime: row.startTime, endTime: row.endTime, status: row.status
  })
  dialog.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (form.id) {
      await couponUpdate(form.id, payload)
      ElMessage.success('更新成功')
    } else {
      await couponCreate(payload)
      ElMessage.success('创建成功')
    }
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function doExport() {
  exporting.value = true
  try {
    const p = {}
    if (query.status !== null && query.status !== '') p.status = query.status
    if (query.type !== null && query.type !== '') p.type = query.type
    if (query.keyword) p.keyword = query.keyword
    await exportCoupons(p)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确定${target === 1 ? '上架' : '下架'}该优惠券？`, '提示', { type: 'warning' })
  await couponStatus(row.id, target)
  ElMessage.success('操作成功')
  load()
}

onMounted(load)
</script>
