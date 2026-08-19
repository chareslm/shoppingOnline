<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Menu } from '@element-plus/icons-vue'
import { readApiError } from '../../../services/http'
import { categoryAdminApi } from '../services/product'
import type { AdminCategory } from '../types'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref<AdminCategory | null>(null)
const items = ref<AdminCategory[]>([])
const form = reactive({ parentId: '0', name: '', sortOrder: 0, status: 1 })

const parentOptions = computed(() => [
  { id: '0', label: '作为一级类目' },
  ...items.value
    .filter((item) => item.level < 3 && item.id !== editing.value?.id)
    .map((item) => ({ id: item.id, label: `${'　'.repeat(Math.max(0, item.level - 1))}${item.name}` })),
])

function parentName(parentId: string) {
  if (parentId === '0') return '—'
  return items.value.find((item) => item.id === parentId)?.name ?? parentId
}

async function load() {
  loading.value = true
  try {
    items.value = await categoryAdminApi.list()
  } catch (error) {
    ElMessage.error(readApiError(error, '类目加载失败'))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.parentId = '0'
  form.name = ''
  form.sortOrder = 0
  form.status = 1
  dialogVisible.value = true
}

function openEdit(row: AdminCategory) {
  editing.value = row
  form.parentId = row.parentId
  form.name = row.name
  form.sortOrder = row.sortOrder
  form.status = row.status
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写类目名称')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await categoryAdminApi.update(editing.value.id, {
        name: form.name.trim(),
        sortOrder: Number(form.sortOrder) || 0,
        status: form.status,
        icon: editing.value.icon,
        level: editing.value.level,
      })
      ElMessage.success('类目已更新')
    } else {
      await categoryAdminApi.create({
        parentId: form.parentId,
        name: form.name.trim(),
        sortOrder: Number(form.sortOrder) || 0,
        status: form.status,
      })
      ElMessage.success('类目已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(readApiError(error, '保存失败'))
  } finally {
    saving.value = false
  }
}

async function remove(row: AdminCategory) {
  try {
    await ElMessageBox.confirm(`确认删除类目「${row.name}」？有子类目时无法删除。`, '删除类目', { type: 'warning' })
  } catch {
    return
  }
  try {
    await categoryAdminApi.remove(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(readApiError(error, '删除失败'))
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">PRODUCT GOVERNANCE</p>
        <h1>商品类目</h1>
        <p>维护平台类目树。商家添加商品时只能选择已启用的类目。</p>
      </div>
      <div class="heading-actions">
        <el-button type="primary" :icon="Menu" @click="openCreate">新增类目</el-button>
      </div>
    </div>
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="items" empty-text="暂无类目">
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column label="上级" min-width="160">
          <template #default="{ row }">{{ parentName(row.parentId) }}</template>
        </el-table-column>
        <el-table-column prop="level" label="层级" width="90" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑类目' : '新增类目'" width="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item v-if="!editing" label="上级类目">
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option v-for="option in parentOptions" :key="option.id" :label="option.label" :value="option.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
