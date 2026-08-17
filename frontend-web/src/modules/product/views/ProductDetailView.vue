<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { readApiError } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import { reviewApi, spuApi } from '../services/product'
import { SPU_STATUS_LABELS, type Review, type ReviewStats, type SpuDetail } from '../types'

const route = useRoute()
const auth = useAuthStore()
const spuId = route.params.spuId as string

const detail = ref<SpuDetail | null>(null)
const stats = ref<ReviewStats | null>(null)
const reviews = ref<Review[]>([])
const loading = ref(true)
const message = ref('')
const isError = ref(false)
const replyText = ref('')

const canReply = computed(() => auth.session?.permissions?.includes('review:reply') ?? false)
const canAudit = computed(() => auth.session?.permissions?.includes('review:audit') ?? false)

const ratingPercent = (count: string) => {
  const total = Number(stats.value?.totalCount ?? 0)
  if (!total) return 0
  return (Number(count) / total) * 100
}

function showMessage(text: string, error = false) {
  message.value = text
  isError.value = error
}

async function load() {
  loading.value = true
  try {
    ;[detail.value, stats.value, reviews.value] = await Promise.all([
      spuApi.detail(spuId),
      reviewApi.stats(spuId),
      reviewApi.listBySpu(spuId).then((page) => page.items),
    ])
  } catch (error) {
    showMessage(readApiError(error, '商品加载失败'), true)
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function reply(review: Review) {
  if (!replyText.value.trim()) {
    showMessage('请输入回复内容', true)
    return
  }
  try {
    await reviewApi.reply(review.id, replyText.value.trim())
    replyText.value = ''
    showMessage('回复成功')
    await load()
  } catch (error) {
    showMessage(readApiError(error, '回复失败'), true)
  }
}

async function audit(review: Review, action: 'HIDE' | 'DISPLAY') {
  try {
    await reviewApi.audit(review.id, action)
    showMessage(action === 'HIDE' ? '已隐藏评价' : '已恢复评价')
    await load()
  } catch (error) {
    showMessage(readApiError(error, '操作失败'), true)
  }
}

function formatMoney(value: number | null) {
  return value == null ? '—' : `¥${value.toFixed(2)}`
}

function formatDate(value: string) {
  return value?.replace('T', ' ').slice(0, 16) ?? ''
}
</script>

<template>
  <section class="page-stack">
    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>

    <div v-if="loading" class="loading-card">正在加载商品详情…</div>

    <template v-else-if="detail">
      <div class="section-card product-hero">
        <div class="gallery">
          <div class="gallery-main">🛍️</div>
          <div v-if="detail.images.length" class="gallery-list">
            <span v-for="(_, index) in detail.images" :key="index">图{{ index + 1 }}</span>
          </div>
        </div>
        <div class="product-info">
          <div class="info-head">
            <span v-if="detail.brand" class="brand-badge">{{ detail.brand }}</span>
            <span class="status-badge">{{ SPU_STATUS_LABELS[detail.status] ?? detail.status }}</span>
          </div>
          <h1>{{ detail.name }}</h1>
          <p class="muted subtitle">{{ detail.subtitle || '暂无副标题' }}</p>
          <div class="price-block">
            <span class="price">{{ formatMoney(detail.priceMin) }}</span>
            <span class="muted">已售 {{ detail.sales }} 件</span>
            <span class="muted">评分 {{ Number(detail.rating).toFixed(1) }}</span>
          </div>
          <div class="sku-table">
            <div class="sku-row sku-head">
              <span>SKU 编码</span><span>规格</span><span>价格</span><span>可售库存</span>
            </div>
            <div v-for="sku in detail.skus" :key="sku.id" class="sku-row">
              <span>{{ sku.skuCode || `#${sku.id}` }}</span>
              <span class="muted">{{ sku.attributes || '—' }}</span>
              <strong>{{ formatMoney(sku.price) }}</strong>
              <span>{{ sku.availableStock }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="two-col">
        <div class="section-card">
          <p class="eyebrow">RATINGS</p>
          <h2 class="section-title">商品评价</h2>
          <div v-if="stats" class="rating-summary">
            <div class="rating-big">
              <strong>{{ Number(stats.averageRating).toFixed(1) }}</strong>
              <span class="muted">共 {{ stats.totalCount }} 条评价</span>
              <span class="positive">好评率 {{ Number(stats.positiveRate).toFixed(0) }}%</span>
            </div>
            <div class="rating-dist">
              <div v-for="star in 5" :key="star" class="dist-row">
                <span>{{ 6 - star }} 星</span>
                <div class="dist-bar"><i :style="{ width: `${ratingPercent((stats as any)[`${6 - star}Star`])}%` }" /></div>
                <span class="muted">{{ (stats as any)[`${6 - star}Star`] }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="section-card">
          <p class="eyebrow">DETAIL</p>
          <h2 class="section-title">图文详情</h2>
          <p class="muted">{{ detail.detail || '暂无详情' }}</p>
        </div>
      </div>

      <div class="section-card">
        <p class="eyebrow">REVIEWS</p>
        <h2 class="section-title">买家评价（{{ reviews.length }}）</h2>
        <div v-if="!reviews.length" class="empty-state">
          <span>💬</span>
          <p>还没有评价，下单完成后可发表评价。</p>
        </div>
        <div v-for="review in reviews" :key="review.id" class="review-item">
          <div class="review-head">
            <span class="avatar">{{ review.anonymous ? '匿' : review.userId.slice(0, 1) }}</span>
            <div>
              <strong>{{ review.anonymous ? '匿名用户' : `用户 #${review.userId}` }}</strong>
              <div class="stars">{{ '★'.repeat(review.rating) }}{{ '☆'.repeat(5 - review.rating) }}</div>
            </div>
            <span class="muted review-time">{{ formatDate(review.createdAt) }}</span>
          </div>
          <p class="review-content">{{ review.content || '此用户没有填写评价内容' }}</p>
          <div v-if="review.images.length" class="review-images">
            <span v-for="(_, index) in review.images" :key="index">🖼️</span>
          </div>
          <div v-if="review.reply" class="review-reply">
            <strong>商家回复：</strong>{{ review.reply }}
          </div>
          <div v-if="canReply || canAudit" class="review-actions">
            <template v-if="canReply">
              <input v-model="replyText" class="reply-input" placeholder="输入商家回复" />
              <button class="primary-button" type="button" @click="reply(review)">回复</button>
            </template>
            <template v-if="canAudit">
              <button class="secondary-button" type="button" @click="audit(review, 'HIDE')">隐藏</button>
              <button class="secondary-button" type="button" @click="audit(review, 'DISPLAY')">恢复</button>
            </template>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.product-hero {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 40px;
}

.gallery {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.gallery-main {
  height: 320px;
  display: grid;
  place-items: center;
  font-size: 120px;
  border-radius: 18px;
  background: #eef2ef;
}

.gallery-list {
  display: flex;
  gap: 10px;
}

.gallery-list span {
  padding: 8px 14px;
  border: 1px solid var(--line);
  border-radius: 10px;
  font-size: 13px;
  color: var(--muted);
}

.info-head {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.brand-badge,
.status-badge {
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.brand-badge {
  background: #edf2ed;
  color: #5c6b63;
}

.status-badge {
  background: var(--lime);
  color: var(--green-dark);
}

.product-info h1 {
  margin: 0 0 10px;
  font-size: clamp(26px, 3vw, 36px);
  letter-spacing: -.03em;
}

.subtitle {
  font-size: 16px;
}

.price-block {
  display: flex;
  align-items: baseline;
  gap: 22px;
  margin: 22px 0;
  padding: 20px;
  border-radius: 14px;
  background: #f4f7f4;
}

.price {
  color: var(--green-dark);
  font-size: 30px;
  font-weight: 800;
}

.sku-table {
  border: 1px solid var(--line);
  border-radius: 12px;
  overflow: hidden;
}

.sku-row {
  display: grid;
  grid-template-columns: 1.2fr 1.4fr 1fr 1fr;
  gap: 12px;
  padding: 13px 16px;
  font-size: 14px;
  border-bottom: 1px solid var(--line);
}

.sku-row:last-child {
  border-bottom: 0;
}

.sku-head {
  background: #f4f7f4;
  font-weight: 700;
  color: var(--muted);
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.section-title {
  margin: 0 0 22px;
}

.rating-summary {
  display: flex;
  gap: 30px;
  align-items: center;
}

.rating-big {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 130px;
}

.rating-big strong {
  font-size: 52px;
  color: var(--green-dark);
}

.positive {
  color: #d18900;
  font-weight: 700;
  font-size: 13px;
}

.rating-dist {
  flex: 1;
  display: grid;
  gap: 8px;
}

.dist-row {
  display: grid;
  grid-template-columns: 44px 1fr 32px;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.dist-bar {
  height: 8px;
  border-radius: 99px;
  background: #eef2ef;
  overflow: hidden;
}

.dist-bar i {
  display: block;
  height: 100%;
  background: #f0b429;
}

.review-item {
  padding: 22px 0;
  border-bottom: 1px solid var(--line);
}

.review-item:last-child {
  border-bottom: 0;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-head .avatar {
  width: 38px;
  height: 38px;
}

.review-time {
  margin-left: auto;
  font-size: 13px;
}

.stars {
  color: #f0b429;
  letter-spacing: 2px;
}

.review-content {
  margin: 14px 0;
  line-height: 1.7;
}

.review-images {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.review-images span {
  width: 64px;
  height: 64px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #eef2ef;
}

.review-reply {
  padding: 12px 16px;
  border-radius: 10px;
  background: #f4f7f4;
  font-size: 14px;
  line-height: 1.6;
}

.review-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
}

.reply-input {
  max-width: 420px;
}

@media (max-width: 900px) {
  .product-hero,
  .two-col {
    grid-template-columns: 1fr;
  }
  .rating-summary {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
