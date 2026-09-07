<template>
  <div class="page-card">
    <el-card>
      <div class="table-toolbar">
        <el-input v-model="query.keyword" placeholder="商品名称" clearable style="width: 220px" @keyup.enter="load" @clear="load" />
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 160px" @change="load">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="load">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
        <div style="flex: 1"></div>
        <el-button type="success" :icon="Plus" @click="openCreate">新建商品</el-button>
      </div>

      <el-table :data="list" v-loading="loading">
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="商品" min-width="280">
          <template #default="{ row }">
            <div style="display: flex; gap: 10px; align-items: center">
              <div class="img-ph" :class="'c' + (row.id % 4)" style="width: 44px; height: 44px; border-radius: 6px; font-size: 16px">{{ row.name?.[0] }}</div>
              <div>
                <div>{{ row.name }}</div>
                <div style="color: #909399; font-size: 12px">{{ row.subTitle }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="售价" width="110">
          <template #default="{ row }"><span class="price">¥{{ Number(row.price).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="库存(可/锁/售)" width="140">
          <template #default="{ row }">
            <span v-if="row.hasSku === 1"><el-tag size="small" type="warning">多规格</el-tag></span>
            <span v-else>{{ row.availableStock }} / {{ row.lockedStock }} / {{ row.soldStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="销量" prop="sale" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-popconfirm title="确定删除该商品？" @confirm="remove(row)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
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

    <el-dialog v-model="dialog" :title="editingId ? '编辑商品' : '新建商品'" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="商品名称" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subTitle" placeholder="选填" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="售价" prop="price" v-if="form.hasSku !== 1">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" style="width: 180px" />
        </el-form-item>
        <el-form-item label="原价" v-if="form.hasSku !== 1">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width: 180px" />
        </el-form-item>
        <el-form-item label="可售库存" prop="availableStock" v-if="!editingId && form.hasSku !== 1">
          <el-input-number v-model="form.availableStock" :min="0" style="width: 180px" />
        </el-form-item>
        <el-form-item label="销量">
          <el-input-number v-model="form.sale" :min="0" style="width: 180px" />
        </el-form-item>
        <el-form-item label="多规格">
          <el-switch v-model="form.hasSku" :active-value="1" :inactive-value="0" @change="onSkuToggle" />
          <span style="color: #909399; font-size: 12px; margin-left: 8px">启用后商品按 SKU 独立定价/库存</span>
        </el-form-item>
        <template v-if="form.hasSku === 1">
          <el-form-item label="规格维度">
            <el-input v-model="form.specNames" placeholder="逗号分隔，如：颜色,内存,版本" style="width: 100%" />
          </el-form-item>
          <el-form-item label="SKU 列表">
            <el-table :data="skuList" size="small" border style="width: 100%">
              <el-table-column label="规格值 JSON" min-width="220">
                <template #default="{ row }">
                  <el-input v-model="row.specValues" size="small" placeholder='{"颜色":"红","内存":"8G"}' />
                </template>
              </el-table-column>
              <el-table-column label="价格" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.price" :min="0.01" :precision="2" size="small" style="width: 110px" />
                </template>
              </el-table-column>
              <el-table-column label="库存" width="110">
                <template #default="{ row }">
                  <el-input-number v-model="row.availableStock" :min="0" size="small" style="width: 100px" />
                </template>
              </el-table-column>
              <el-table-column label="编码" width="120">
                <template #default="{ row }">
                  <el-input v-model="row.skuCode" size="small" placeholder="选填" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="70">
                <template #default="{ $index }">
                  <el-button text type="danger" size="small" @click="skuList.splice($index, 1)">删</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button size="small" type="primary" plain style="margin-top: 8px" @click="addSkuRow">+ 添加 SKU</el-button>
          </el-form-item>
        </template>
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
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { productPage, productCreate, productUpdate, productStatus, productDelete, productCategories, productSkus, productSaveSkus } from '../api'

const list = ref([])
const categories = ref([])
const total = ref(0)
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref()
const skuList = ref([])

const query = reactive({ keyword: '', categoryId: null, status: null, pageNum: 1, pageSize: 10 })
const form = reactive({ name: '', subTitle: '', categoryId: null, price: 1, originalPrice: null, availableStock: 100, sale: 0, hasSku: 0, specNames: '' })
const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach(k => { if (params[k] === '' || params[k] == null) delete params[k] })
    const page = await productPage(params)
    list.value = page.records || []
    total.value = Number(page.total || 0)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', subTitle: '', categoryId: null, price: 1, originalPrice: null, availableStock: 100, sale: 0, hasSku: 0, specNames: '' })
  skuList.value = []
  dialog.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name, subTitle: row.subTitle, categoryId: row.categoryId,
    price: Number(row.price), originalPrice: row.originalPrice ? Number(row.originalPrice) : null,
    availableStock: row.availableStock, sale: row.sale, hasSku: row.hasSku || 0, specNames: row.specNames || ''
  })
  skuList.value = []
  // 多规格商品加载 SKU 列表
  if (row.hasSku === 1) {
    try {
      const skus = await productSkus(row.id)
      skuList.value = (skus || []).map(s => ({
        id: s.id, specValues: s.specValues, price: Number(s.price),
        availableStock: s.availableStock, skuCode: s.skuCode || '', status: s.status
      }))
    } catch { /* 忽略 */ }
  }
  dialog.value = true
}

function onSkuToggle() {
  if (form.hasSku === 1 && skuList.value.length === 0) {
    addSkuRow()
  }
}

function addSkuRow() {
  skuList.value.push({ specValues: '', price: 1, availableStock: 100, skuCode: '', status: 1 })
}

async function save() {
  await formRef.value.validate()
  // 多规格商品必须至少一个 SKU
  if (form.hasSku === 1) {
    if (!form.specNames) {
      ElMessage.warning('请填写规格维度')
      return
    }
    if (skuList.value.length === 0) {
      ElMessage.warning('请至少添加一个 SKU')
      return
    }
    for (const s of skuList.value) {
      if (!s.specValues) {
        ElMessage.warning('请填写所有 SKU 的规格值')
        return
      }
      if (!s.price || s.price < 0.01) {
        ElMessage.warning('SKU 价格不能小于 0.01')
        return
      }
    }
  }
  saving.value = true
  try {
    if (editingId.value) {
      await productUpdate(editingId.value, form)
      // 多规格商品保存 SKU
      if (form.hasSku === 1) {
        await productSaveSkus(editingId.value, { specNames: form.specNames, skus: skuList.value })
      }
      ElMessage.success('商品已更新')
    } else {
      await productCreate(form)
      ElMessage.success('商品已创建')
      // 新建商品后无法立即保存 SKU（缺 productId），提示用户编辑后补充
      if (form.hasSku === 1) {
        ElMessage.info('新建商品后请编辑该商品补充 SKU')
      }
    }
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  await productStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已下架' : '已上架')
  load()
}

async function remove(row) {
  await productDelete(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  categories.value = (await productCategories()) || []
})
</script>
