<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { readApiError } from '@/services/http'
import { mediaUrl } from '@/utils/media'
import { categoryApi, searchApi } from '../services/product'
import { SORT_OPTIONS, type CategoryNode, type SearchItem } from '../types'

const router = useRouter()
const items = ref<SearchItem[]>([])
const categories = ref<CategoryNode[]>([])
const keyword = ref('')
const categoryId = ref('')
const sort = ref('DEFAULT')
const page = ref(1)
const pageSize = 12
const total = ref(0)
const loading = ref(false)
const message = ref('')
const isError = ref(false)
const hotWords = ref<{ keyword: string; count: string }[]>([])

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

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
  } catch {
    /* 类目加载失败不阻断商品列表 */
  }
}

async function loadHotWords() {
  try {
    hotWords.value = (await searchApi.hotWords(8)).words
  } catch {
    /* ignore */
  }
}

async function load() {
  loading.value = true
  try {
    const result = await searchApi.search({
      keyword: keyword.value.trim() || undefined,
      categoryId: categoryId.value || undefined,
      sort: sort.value,
      page: page.value,
      pageSize,
    })
    items.value = result.items
    total.value = Number(result.total)
  } catch (error) {
    showMessage(readApiError(error, '商品加载失败'), true)
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void load()
}

function applyHotWord(word: string) {
  keyword.value = word
  search()
}

function goPage(target: number) {
  if (target < 1 || target > totalPages.value) return
  page.value = target
  void load()
}

function goDetail(item: SearchItem) {
  void router.push({ name: 'product-detail', params: { spuId: item.spuId } })
}

function formatMoney(value: number | null) {
  return value == null ? '—' : `¥${value.toFixed(2)}`
}

function formatPriceRange(item: SearchItem) {
  if (item.priceMin == null && item.priceMax == null) return '价格待定'
  if (item.priceMin != null && item.priceMax != null && item.priceMin !== item.priceMax) {
    return `¥${item.priceMin.toFixed(2)} - ¥${item.priceMax.toFixed(2)}`
  }
  return formatMoney(item.priceMin ?? item.priceMax)
}

onMounted(() => {
  void Promise.all([loadCategories(), loadHotWords(), load()])
})
</script>

<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">PRODUCTS</p>
        <h1>商品</h1>
        <p>关键词搜索、类目筛选与多维排序（ES 优先，MySQL 降级）。</p>
      </div>
    </div>

    <div class="section-card search-bar">
      <div class="search-row">
        <input v-model="keyword" class="search-input" placeholder="搜索商品名称、副标题或品牌" @keyup.enter="search" />
        <select v-model="categoryId" class="search-select">
          <option value="">全部类目</option>
          <option v-for="option in categoryOptions" :key="option.id" :value="option.id">{{ option.label }}</option>
        </select>
        <select v-model="sort" class="search-select" @change="search">
          <option v-for="option in SORT_OPTIONS" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
        <button class="primary-button" type="button" @click="search">搜索</button>
      </div>
      <div v-if="hotWords.length" class="hot-words">
        <span class="muted">热门搜索：</span>
        <button v-for="word in hotWords" :key="word.keyword" class="hot-word" type="button" @click="applyHotWord(word.keyword)">
          {{ word.keyword }}
        </button>
      </div>
    </div>

    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>

    <div v-if="loading" class="loading-card">正在搜索商品…</div>

    <div v-else-if="!items.length" class="section-card empty-state">
      <span>🔍</span>
      <h2>没有找到商品</h2>
      <p>试试更换关键词或类目。</p>
    </div>

    <template v-else>
      <div class="product-grid">
        <button v-for="item in items" :key="item.spuId" class="product-card" type="button" @click="goDetail(item)">
          <div class="product-thumb">
            <img v-if="item.mainImage" :src="mediaUrl(item.mainImage)" :alt="item.name" />
            <span v-else>🛍️</span>
          </div>
          <div class="product-body">
            <strong class="product-name">{{ item.name }}</strong>
            <small class="muted product-subtitle">{{ item.subtitle || item.brand || '—' }}</small>
            <div class="product-meta">
              <span class="product-price">{{ formatPriceRange(item) }}</span>
              <span class="muted">已售 {{ item.sales }}</span>
            </div>
            <div class="product-foot">
              <span class="rating">{{ '★'.repeat(Math.round(Number(item.rating))) }}{{ Number(item.rating).toFixed(1) }}</span>
              <span class="muted">查看详情 →</span>
            </div>
          </div>
        </button>
      </div>

      <div class="pagination">
        <button class="secondary-button" type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
        <span class="muted">第 {{ page }} / {{ totalPages }} 页 · 共 {{ total }} 件</span>
        <button class="secondary-button" type="button" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.search-bar {
  padding: 22px 26px;
}

.search-row {
  display: grid;
  grid-template-columns: 1fr 200px 150px auto;
  gap: 12px;
}

.search-input,
.search-select {
  min-height: 46px;
}

.hot-words {
  margin-top: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
}

.hot-word {
  padding: 5px 11px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #f2f5f2;
  color: var(--green);
  font-size: 13px;
}

.hot-word:hover {
  border-color: var(--green);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.product-card {
  display: flex;
  flex-direction: column;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 20px;
  background: var(--paper);
  text-align: left;
  overflow: hidden;
  transition: transform .18s, border-color .18s, box-shadow .18s;
}

.product-card:hover {
  transform: translateY(-4px);
  border-color: #a8c4b7;
  box-shadow: 0 14px 34px rgba(28, 52, 42, .09);
}

.product-thumb {
  height: 150px;
  display: grid;
  place-items: center;
  font-size: 52px;
  background: #eef2ef;
  overflow: hidden;
}
.product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px;
  flex: 1;
}

.product-name {
  font-size: 17px;
  line-height: 1.35;
}

.product-subtitle {
  min-height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.product-price {
  color: var(--green-dark);
  font-size: 18px;
  font-weight: 800;
}

.product-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--line);
  font-size: 13px;
}

.rating {
  color: #d18900;
  font-weight: 700;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
}

@media (max-width: 900px) {
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .search-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
