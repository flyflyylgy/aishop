<template>
  <div class="page-card">
    <el-card>
      <div class="table-toolbar">
        <el-button type="success" :icon="Plus" @click="openCreate">新建分类</el-button>
      </div>

      <el-table :data="list" v-loading="loading" row-key="id" default-expand-all>
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="分类名称" prop="name" min-width="220" />
        <el-table-column label="排序" prop="sort" width="90" />
        <el-table-column label="显示" width="90">
          <template #default="{ row }">
            <el-tag :type="row.showFlag === 1 ? 'success' : 'info'">{{ row.showFlag === 1 ? '显示' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ String(row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该分类？" @confirm="remove(row)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" :title="editingId ? '编辑分类' : '新建分类'" width="440px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option label="无（一级分类）" :value="0" />
            <el-option v-for="c in topCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="显示" prop="showFlag">
          <el-switch v-model="form.showFlag" :active-value="1" :inactive-value="0" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { categoryList, categoryCreate, categoryUpdate, categoryDelete } from '../api'

const list = ref([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = reactive({ parentId: 0, name: '', sort: 0, showFlag: 1 })
const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}
const topCategories = computed(() => list.value.filter(c => c.parentId === 0))

async function load() {
  loading.value = true
  try {
    list.value = (await categoryList()) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { parentId: 0, name: '', sort: 0, showFlag: 1 })
  dialog.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, { parentId: row.parentId, name: row.name, sort: row.sort, showFlag: row.showFlag })
  dialog.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await categoryUpdate(editingId.value, form)
      ElMessage.success('分类已更新')
    } else {
      await categoryCreate(form)
      ElMessage.success('分类已创建')
    }
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await categoryDelete(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
