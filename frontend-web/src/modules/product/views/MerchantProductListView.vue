<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { readApiError } from '@/services/http'
import { spuApi } from '../services/product'
import { merchantApi } from '@/modules/merchant/services/merchant'
import { formatSkuAttributes, SPU_STATUS_LABELS, type Sku, type SpuDetail, type SpuItem } from '../types'

const route = useRoute()
const items = ref<SpuItem[]>([])
const loading = ref(false)
const message = ref('')
const isError = ref(false)
const keyword = ref('')
const shelf = ref<'ALL' | 'LISTED' | 'UNLISTED'>('ALL')
const page = ref(1)
const pageSize = 20
const total = ref(0)
const stockVisible = ref(false)
const stockLoading = ref(false)
const selected = ref<SpuDetail | null>(null)
const stockDraft = ref<Record<string, number>>({})
const shopName = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

function show(text: string, error = false) {
  message.value = text
  isError.value = error
}

async function loadList() {
  loading.value = true
  try {
    const result = await spuApi.merchantPage({
      keyword: keyword.value.trim() || undefined,
      shelf: shelf.value === 'ALL' ? undefined : shelf.value,
      page: page.value,
      pageSize,
    })
    items.value = result.items
    total.value = Number(result.total)
  } catch (error) {
    show(readApiError(error, '商品列表加载失败'), true)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (route.query.created === '1') show('商品已保存为草稿，请提交审核')
  if (route.query.created === 'submitted') show('商品已提交审核，等待平台通过后上架')
  void merchantApi.currentShop().then((shop) => {
    shopName.value = shop.name
  }).catch(() => {
    /* 列表接口仍按本店过滤；店铺名仅用于展示 */
  })
  void loadList()
})
watch(shelf, () => {
  page.value = 1
  void loadList()
})

async function changeStatus(item: SpuItem, action: 'SUBMIT' | 'PUBLISH' | 'OFF_SHELF') {
  try {
    await spuApi.changeStatus(item.id, action)
    show(action === 'SUBMIT' ? '已提交审核' : action === 'PUBLISH' ? '已重新上架' : '已下架')
    await loadList()
  } catch (error) {
    show(readApiError(error, '操作失败'), true)
  }
}

async function openStock(item: SpuItem) {
  stockLoading.value = true
  try {
    selected.value = await spuApi.merchantDetail(item.id)
    stockDraft.value = Object.fromEntries(
      (selected.value.skus ?? []).map((sku) => [sku.id, sku.availableStock]),
    )
    stockVisible.value = true
  } catch (error) {
    show(readApiError(error, '商品详情加载失败'), true)
  } finally {
    stockLoading.value = false
  }
}

async function saveStock(sku: Sku) {
  if (!selected.value) return
  const next = Number(stockDraft.value[sku.id])
  if (!Number.isInteger(next) || next < 0) {
    show('库存须为不小于 0 的整数', true)
    return
  }
  const change = next - sku.availableStock
  if (change === 0) return
  try {
    await spuApi.adjustStock(sku.id, change, '商家调整可售库存')
    show('库存已更新')
    selected.value = await spuApi.merchantDetail(selected.value.id)
    stockDraft.value = Object.fromEntries(
      (selected.value.skus ?? []).map((item) => [item.id, item.availableStock]),
    )
    await loadList()
  } catch (error) {
    show(readApiError(error, '库存调整失败'), true)
  }
}

function formatMoney(value: number | null) {
  return value == null ? '—' : `¥${Number(value).toFixed(2)}`
}
</script>

<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">MERCHANT CATALOG</p>
        <h1>商品浏览</h1>
        <p>仅显示本店商品。审核通过后可下架、再上架，并调整可售数量。{{ shopName ? `当前店铺：${shopName}` : '' }}</p>
      </div>
    </div>
    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>
    <div class="section-card filter-row">
      <div class="shelf-tabs">
        <button type="button" :class="['tab', { active: shelf === 'ALL' }]" @click="shelf = 'ALL'">全部</button>
        <button type="button" :class="['tab', { active: shelf === 'LISTED' }]" @click="shelf = 'LISTED'">已上架</button>
        <button type="button" :class="['tab', { active: shelf === 'UNLISTED' }]" @click="shelf = 'UNLISTED'">未上架</button>
      </div>
      <input v-model="keyword" placeholder="按名称搜索" @keyup.enter="page = 1; loadList()" />
      <button class="primary-button" type="button" @click="page = 1; loadList()">查询</button>
    </div>
    <div v-if="loading" class="loading-card">正在加载本店商品…</div>
    <div v-else-if="!items.length" class="section-card empty-state">
      <h2>暂无商品</h2>
      <p>请先在「添加商品」创建草稿并提交审核。</p>
    </div>
    <div v-else class="section-card manage-table">
      <table>
        <thead>
          <tr><th>商品</th><th>店铺</th><th>价格</th><th>状态</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td>
              <strong>{{ item.name }}</strong>
              <small class="muted block">{{ item.brand || '未填品牌' }}</small>
            </td>
            <td>{{ item.shopName || shopName || '本店' }}</td>
            <td>{{ formatMoney(item.priceMin) }}</td>
            <td><span class="status-pill">{{ SPU_STATUS_LABELS[item.status] ?? item.status }}</span></td>
            <td>
              <div class="action-group">
                <button v-if="item.status === 'DRAFT' || item.status === 'AUDIT_REJECTED'" class="text-button" type="button" @click="changeStatus(item, 'SUBMIT')">提交审核</button>
                <button v-if="item.status === 'AUDIT_APPROVED' || item.status === 'OFF_SALE'" class="text-button" type="button" @click="changeStatus(item, 'PUBLISH')">上架</button>
                <button v-if="item.status === 'ON_SALE'" class="text-button danger" type="button" @click="changeStatus(item, 'OFF_SHELF')">下架</button>
                <button class="text-button" type="button" @click="openStock(item)">库存</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="pagination">
        <button class="secondary-button" type="button" :disabled="page <= 1" @click="page -= 1; loadList()">上一页</button>
        <span class="muted">第 {{ page }} / {{ totalPages }} 页 · 共 {{ total }} 件</span>
        <button class="secondary-button" type="button" :disabled="page >= totalPages" @click="page += 1; loadList()">下一页</button>
      </div>
    </div>

    <div v-if="stockVisible && selected" class="section-card">
      <h2>{{ selected.name }} · 库存</h2>
      <p class="muted">可售数量立即生效。减少数量不能低于 0。</p>
      <div v-for="sku in selected.skus" :key="sku.id" class="stock-row">
        <div>
          <strong>{{ sku.skuCode || sku.id }}</strong>
          <small class="muted block">{{ formatSkuAttributes(sku.attributes) }} · 当前 {{ sku.availableStock }}</small>
        </div>
        <input v-model.number="stockDraft[sku.id]" type="number" min="0" step="1" />
        <button class="primary-button" type="button" :disabled="stockLoading" @click="saveStock(sku)">保存</button>
      </div>
      <button class="secondary-button" type="button" @click="stockVisible = false">关闭</button>
    </div>
  </section>
</template>

<style scoped>
.filter-row { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; }
.shelf-tabs, .action-group { display: flex; gap: 6px; }
.tab { padding: 8px 14px; border: 1px solid var(--line); border-radius: 999px; background: #fff; }
.tab.active { background: var(--green); color: #fff; border-color: var(--green); }
.manage-table { padding: 0; overflow: auto; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 16px 20px; text-align: left; border-bottom: 1px solid var(--line); }
.block { display: block; margin-top: 4px; }
.status-pill { padding: 4px 10px; border-radius: 999px; background: #edf2ed; font-size: 12px; font-weight: 700; }
.pagination { display: flex; justify-content: center; gap: 18px; padding: 18px; }
.stock-row { display: grid; grid-template-columns: 1fr 140px auto; gap: 12px; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--line); }
</style>
