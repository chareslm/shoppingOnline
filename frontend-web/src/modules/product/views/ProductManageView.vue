<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { readApiError } from '@/services/http'
import { categoryApi, spuApi } from '../services/product'
import { SPU_STATUS_LABELS, type CategoryNode, type SpuItem } from '../types'

type Tab = 'list' | 'create' | 'category'
const tab = ref<Tab>('list')

const categories = ref<CategoryNode[]>([])
const message = ref('')
const isError = ref(false)
const loading = ref(false)

// ---------- 商品列表 ----------
const items = ref<SpuItem[]>([])
const statusFilter = ref('')
const keywordFilter = ref('')
const page = ref(1)
const pageSize = 20
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

// ---------- 创建商品 ----------
const createForm = reactive({
  shopId: '100',
  categoryId: '',
  brand: '',
  name: '',
  subtitle: '',
  mainImage: '',
  detail: '',
})
const skus = ref<{ skuCode: string; attributes: string; price: number; stock: number }[]>([
  { skuCode: '', attributes: '', price: 99.9, stock: 100 },
])
const submitting = ref(false)

// ---------- 类目 ----------
const categoryForm = reactive({ parentId: '0', name: '', level: 1, sortOrder: 0 })

const statusOptions = [
  { value: '', label: '全部状态' },
  ...Object.entries(SPU_STATUS_LABELS).map(([value, label]) => ({ value, label })),
]

function flatten(tree: CategoryNode[], depth = 0): { id: string; label: string }[] {
  const result: { id: string; label: string }[] = []
  for (const node of tree) {
    result.push({ id: node.id, label: '　'.repeat(depth) + node.name })
    result.push(...flatten(node.children, depth + 1))
  }
  return result
}

const categoryOptions = computed(() => flatten(categories.value))

function showMessage(text: string, error = false) {
  message.value = text
  isError.value = error
}

async function loadCategories() {
  try {
    categories.value = await categoryApi.tree()
  } catch (error) {
    showMessage(readApiError(error, '类目加载失败'), true)
  }
}

async function loadList() {
  loading.value = true
  try {
    const result = await spuApi.adminPage({
      status: statusFilter.value || undefined,
      keyword: keywordFilter.value.trim() || undefined,
      page: page.value,
      pageSize,
    })
    items.value = result.items
    total.value = Number(result.total)
  } catch (error) {
    showMessage(readApiError(error, '商品列表加载失败'), true)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void Promise.all([loadCategories(), loadList()])
})

function goPage(target: number) {
  if (target < 1 || target > totalPages.value) return
  page.value = target
  void loadList()
}

// ---------- 创建商品 ----------
function addSku() {
  skus.value.push({ skuCode: '', attributes: '', price: 99.9, stock: 100 })
}

function removeSku(index: number) {
  if (skus.value.length <= 1) return
  skus.value.splice(index, 1)
}

async function submitCreate() {
  if (!createForm.name.trim() || !createForm.categoryId) {
    showMessage('请填写商品名称并选择类目', true)
    return
  }
  submitting.value = true
  try {
    await spuApi.create({
      shopId: createForm.shopId.trim() || '1',
      categoryId: createForm.categoryId,
      brand: createForm.brand.trim() || undefined,
      name: createForm.name.trim(),
      subtitle: createForm.subtitle.trim() || undefined,
      mainImage: createForm.mainImage.trim() || undefined,
      detail: createForm.detail.trim() || undefined,
      skus: skus.value.map((sku) => ({
        skuCode: sku.skuCode.trim() || undefined,
        attributes: sku.attributes.trim() || undefined,
        price: Number(sku.price),
        stock: Number(sku.stock),
      })),
    })
    showMessage('商品创建成功（草稿状态）')
    resetCreateForm()
    await loadList()
  } catch (error) {
    showMessage(readApiError(error, '创建失败'), true)
  } finally {
    submitting.value = false
  }
}

function resetCreateForm() {
  createForm.name = ''
  createForm.brand = ''
  createForm.subtitle = ''
  createForm.mainImage = ''
  createForm.detail = ''
  skus.value = [{ skuCode: '', attributes: '', price: 99.9, stock: 100 }]
}

// ---------- 状态流转 ----------
async function changeStatus(item: SpuItem, action: 'SUBMIT' | 'PUBLISH' | 'OFF_SHELF') {
  try {
    await spuApi.changeStatus(item.id, action)
    showMessage('操作成功')
    await loadList()
  } catch (error) {
    showMessage(readApiError(error, '操作失败'), true)
  }
}

async function audit(item: SpuItem, result: 'APPROVE' | 'REJECT') {
  try {
    await spuApi.audit(item.id, result)
    showMessage(result === 'APPROVE' ? '已通过审核' : '已驳回')
    await loadList()
  } catch (error) {
    showMessage(readApiError(error, '审核失败'), true)
  }
}

// ---------- 类目管理 ----------
async function submitCategory() {
  if (!categoryForm.name.trim()) {
    showMessage('请填写类目名称', true)
    return
  }
  try {
    await categoryApi.create({
      parentId: categoryForm.parentId,
      name: categoryForm.name.trim(),
      level: Number(categoryForm.level),
      sortOrder: Number(categoryForm.sortOrder),
      status: 1,
    })
    showMessage('类目创建成功')
    categoryForm.name = ''
    await loadCategories()
  } catch (error) {
    showMessage(readApiError(error, '类目创建失败'), true)
  }
}

function formatMoney(value: number | null) {
  return value == null ? '—' : `¥${value.toFixed(2)}`
}
</script>

<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">MERCHANT CONSOLE</p>
        <h1>商品管理</h1>
        <p>创建类目与商品、提交审核、上架下架、审核商品。</p>
      </div>
    </div>

    <div class="tab-bar">
      <button :class="['tab', { active: tab === 'list' }]" type="button" @click="tab = 'list'">商品列表</button>
      <button :class="['tab', { active: tab === 'create' }]" type="button" @click="tab = 'create'">创建商品</button>
      <button :class="['tab', { active: tab === 'category' }]" type="button" @click="tab = 'category'">类目管理</button>
    </div>

    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>

    <!-- 商品列表 -->
    <template v-if="tab === 'list'">
      <div class="section-card filter-row">
        <input v-model="keywordFilter" placeholder="按名称搜索" @keyup.enter="page = 1; loadList()" />
        <select v-model="statusFilter" @change="page = 1; loadList()">
          <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
        <button class="primary-button" type="button" @click="page = 1; loadList()">查询</button>
      </div>

      <div v-if="loading" class="loading-card">正在加载商品…</div>
      <div v-else-if="!items.length" class="section-card empty-state">
        <span>📦</span>
        <h2>暂无商品</h2>
        <p>切换到「创建商品」标签新建商品。</p>
      </div>
      <template v-else>
        <div class="section-card manage-table">
          <table>
            <thead>
              <tr><th>商品</th><th>价格区间</th><th>销量</th><th>评分</th><th>状态</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="item in items" :key="item.id">
                <td>
                  <strong>{{ item.name }}</strong>
                  <small class="muted block">{{ item.brand || '—' }} · #{{ item.id }}</small>
                </td>
                <td>{{ formatMoney(item.priceMin) }}</td>
                <td>{{ item.sales }}</td>
                <td>{{ Number(item.rating).toFixed(1) }}</td>
                <td><span class="status-pill">{{ SPU_STATUS_LABELS[item.status] ?? item.status }}</span></td>
                <td>
                  <div class="action-group">
                    <template v-if="item.status === 'DRAFT' || item.status === 'AUDIT_REJECTED'">
                      <button class="text-button" type="button" @click="changeStatus(item, 'SUBMIT')">提交审核</button>
                    </template>
                    <template v-if="item.status === 'PENDING_AUDIT'">
                      <button class="text-button" type="button" @click="audit(item, 'APPROVE')">通过</button>
                      <button class="text-button danger" type="button" @click="audit(item, 'REJECT')">驳回</button>
                    </template>
                    <template v-if="item.status === 'AUDIT_APPROVED' || item.status === 'OFF_SALE'">
                      <button class="text-button" type="button" @click="changeStatus(item, 'PUBLISH')">上架</button>
                    </template>
                    <template v-if="item.status === 'ON_SALE'">
                      <button class="text-button danger" type="button" @click="changeStatus(item, 'OFF_SHELF')">下架</button>
                    </template>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination">
          <button class="secondary-button" type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
          <span class="muted">第 {{ page }} / {{ totalPages }} 页 · 共 {{ total }} 件</span>
          <button class="secondary-button" type="button" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
        </div>
      </template>
    </template>

    <!-- 创建商品 -->
    <template v-else-if="tab === 'create'">
      <div class="section-card form-card">
        <p class="eyebrow">NEW PRODUCT</p>
        <h2>创建商品</h2>
        <div class="form-grid">
          <label>店铺 ID<input v-model="createForm.shopId" placeholder="100" /></label>
          <label>类目
            <select v-model="createForm.categoryId">
              <option value="" disabled>请选择类目</option>
              <option v-for="option in categoryOptions" :key="option.id" :value="option.id">{{ option.label }}</option>
            </select>
          </label>
          <label class="full">商品名称<input v-model="createForm.name" placeholder="如 iPhone 17 Pro" /></label>
          <label>品牌<input v-model="createForm.brand" placeholder="Apple" /></label>
          <label>副标题<input v-model="createForm.subtitle" placeholder="旗舰智能手机" /></label>
          <label class="full">主图 URL<input v-model="createForm.mainImage" placeholder="https://..." /></label>
          <label class="full">图文详情<textarea v-model="createForm.detail" rows="3" placeholder="商品详情描述"></textarea></label>
        </div>

        <h3 class="sku-title">SKU 规格与库存</h3>
        <div v-for="(sku, index) in skus" :key="index" class="sku-form-row">
          <input v-model="sku.skuCode" placeholder="SKU 编码" />
          <input v-model="sku.attributes" placeholder='规格 JSON，如 {"颜色":"黑色"}' />
          <input v-model.number="sku.price" type="number" min="0.01" step="0.01" placeholder="价格" />
          <input v-model.number="sku.stock" type="number" min="0" step="1" placeholder="库存" />
          <button class="icon-button" type="button" :disabled="skus.length <= 1" @click="removeSku(index)">−</button>
        </div>
        <button class="secondary-button" type="button" @click="addSku">＋ 添加 SKU</button>

        <div class="form-actions">
          <button class="primary-button" type="button" :disabled="submitting" @click="submitCreate">创建商品</button>
        </div>
      </div>
    </template>

    <!-- 类目管理 -->
    <template v-else>
      <div class="two-col">
        <div class="section-card form-card">
          <p class="eyebrow">NEW CATEGORY</p>
          <h2>新建类目</h2>
          <div class="form-grid">
            <label>父类目
              <select v-model="categoryForm.parentId">
                <option value="0">（根类目）</option>
                <option v-for="option in categoryOptions" :key="option.id" :value="option.id">{{ option.label }}</option>
              </select>
            </label>
            <label>层级
              <select v-model.number="categoryForm.level">
                <option :value="1">1</option>
                <option :value="2">2</option>
                <option :value="3">3</option>
              </select>
            </label>
            <label class="full">类目名称<input v-model="categoryForm.name" placeholder="如 手机数码" /></label>
            <label>排序值<input v-model.number="categoryForm.sortOrder" type="number" /></label>
          </div>
          <div class="form-actions">
            <button class="primary-button" type="button" @click="submitCategory">创建类目</button>
          </div>
        </div>

        <div class="section-card">
          <p class="eyebrow">CATEGORY TREE</p>
          <h2>现有类目</h2>
          <ul class="category-list">
            <li v-for="option in categoryOptions" :key="option.id">{{ option.label }}</li>
          </ul>
          <p v-if="!categoryOptions.length" class="muted">暂无类目，先创建根类目。</p>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.tab-bar {
  display: flex;
  gap: 6px;
  border-bottom: 1px solid var(--line);
}

.tab {
  padding: 13px 22px;
  border: 0;
  background: none;
  color: var(--muted);
  font-weight: 700;
  border-bottom: 2px solid transparent;
}

.tab.active {
  color: var(--green);
  border-bottom-color: var(--green);
}

.filter-row {
  display: flex;
  gap: 12px;
  padding: 20px 24px;
}

.filter-row input {
  max-width: 320px;
}

.filter-row select {
  max-width: 200px;
}

.manage-table {
  padding: 0;
  overflow: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

th,
td {
  padding: 16px 20px;
  text-align: left;
  border-bottom: 1px solid var(--line);
  white-space: nowrap;
}

th {
  color: var(--muted);
  font-size: 13px;
  background: #f4f7f4;
}

.block {
  display: block;
  margin-top: 4px;
}

.status-pill {
  padding: 4px 10px;
  border-radius: 999px;
  background: #edf2ed;
  font-size: 12px;
  font-weight: 700;
}

.action-group {
  display: flex;
  gap: 2px;
}

.sku-title {
  margin: 28px 0 14px;
}

.sku-form-row {
  display: grid;
  grid-template-columns: 1fr 1.6fr 1fr 1fr auto;
  gap: 10px;
  margin-bottom: 10px;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.category-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.category-list li {
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
  font-size: 14px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
}

@media (max-width: 900px) {
  .two-col {
    grid-template-columns: 1fr;
  }
  .sku-form-row {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
