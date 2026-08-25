<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { readApiError } from '@/services/http'
import { shopStatisticsApi, type ShopOverview, type ShopTrends } from '../statistics'

const loading = ref(false)
const message = ref('')
const overview = ref<ShopOverview | null>(null)
const trends = ref<ShopTrends | null>(null)

function localDateTime(value: Date) {
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

const now = new Date()
const start = new Date(now)
start.setHours(0, 0, 0, 0)
start.setDate(start.getDate() - 6)
const startAt = ref(localDateTime(start))
const endAt = ref(localDateTime(now))

const cards = computed(() => {
  const metrics = overview.value?.metrics
  if (!metrics) return []
  return [
    ['支付订单', count(metrics.paidOrderCount)],
    ['支付买家', count(metrics.paidBuyerCount)],
    ['支付总额', money(metrics.grossPaidAmount)],
    ['成功退款', money(metrics.successfulRefundAmount)],
    ['净收款活动额', money(metrics.netCashflowActivity)],
    ['客单价', metrics.averageOrderValue ? money(metrics.averageOrderValue) : '—'],
    ['支付商品件数', count(metrics.soldQuantity)],
    ['当前在售商品', count(metrics.onSaleProductSnapshot)],
    ['当前有效评价', count(metrics.displayedReviewCount)],
    ['当前平均评分', metrics.averageRating ?? '—'],
  ]
})

function money(value: string) {
  return `¥${value}`
}

function count(value: string) {
  return value.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
}

async function load() {
  if (!startAt.value || !endAt.value) {
    message.value = '请选择统计时间范围'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const [overviewResult, trendResult] = await Promise.all([
      shopStatisticsApi.overview(startAt.value, endAt.value),
      shopStatisticsApi.trends(startAt.value, endAt.value),
    ])
    overview.value = overviewResult
    trends.value = trendResult
  } catch (error) {
    message.value = readApiError(error, '经营统计加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <div class="page-heading statistics-heading">
      <div>
        <p class="eyebrow">EXACT STATISTICS</p>
        <h1>经营统计</h1>
        <p>{{ overview?.shopName ? `${overview.shopName} · ` : '' }}来自 MySQL 权威业务表的精确查询。</p>
      </div>
      <div class="statistics-filter">
        <label>开始时间<input v-model="startAt" type="datetime-local" step="1" /></label>
        <label>结束时间<input v-model="endAt" type="datetime-local" step="1" /></label>
        <button class="primary-button" type="button" :disabled="loading" @click="load">查询</button>
      </div>
    </div>

    <p v-if="message" class="notice error">{{ message }}</p>
    <p v-if="overview" class="statistics-meta">
      口径 {{ overview.metricVersion }} · {{ overview.timezone }} · 数据截至 {{ formatTime(overview.dataAsOf) }}
    </p>
    <div v-if="loading && !overview" class="loading-card">正在读取经营统计…</div>
    <div v-else class="statistics-card-grid">
      <article v-for="card in cards" :key="card[0]" class="stat-card compact-stat">
        <span>{{ card[0] }}</span><strong>{{ card[1] }}</strong>
      </article>
    </div>

    <div class="section-card statistics-table">
      <div class="table-heading">
        <div><p class="eyebrow">DAILY TREND</p><h2>按日趋势</h2></div>
        <small class="muted">缺失日期按 0 补齐，退款按成功发生日统计</small>
      </div>
      <div class="table-scroll">
        <table>
          <thead><tr><th>日期</th><th>支付订单</th><th>支付买家</th><th>支付总额</th><th>成功退款</th><th>净收款活动额</th><th>支付件数</th></tr></thead>
          <tbody>
            <tr v-for="point in trends?.points ?? []" :key="point.date">
              <td>{{ point.date }}</td><td>{{ point.paidOrderCount }}</td><td>{{ point.paidBuyerCount }}</td>
              <td>{{ money(point.grossPaidAmount) }}</td><td>{{ money(point.successfulRefundAmount) }}</td>
              <td>{{ money(point.netCashflowActivity) }}</td><td>{{ point.soldQuantity }}</td>
            </tr>
            <tr v-if="!trends?.points.length"><td colspan="7" class="empty-row">当前区间暂无数据</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<style scoped>
.statistics-heading { align-items: flex-end; }
.statistics-filter { display: grid; grid-template-columns: minmax(190px, 1fr) minmax(190px, 1fr) auto; gap: 10px; align-items: end; }
.statistics-filter label { display: grid; gap: 7px; color: var(--muted); font-size: 12px; font-weight: 700; }
.statistics-meta { margin: 0; padding: 12px 16px; border: 1px solid #c9ddd3; border-radius: 12px; color: var(--green-dark); background: #eef7f2; }
.statistics-card-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; }
.compact-stat { padding: 20px; }
.compact-stat strong { margin-bottom: 0; font-size: 24px; overflow-wrap: anywhere; }
.statistics-table { padding: 0; overflow: hidden; }
.table-heading { display: flex; align-items: end; justify-content: space-between; gap: 20px; padding: 24px; border-bottom: 1px solid var(--line); }
.table-heading h2 { margin: 0; }
.table-scroll { overflow: auto; }
table { width: 100%; min-width: 840px; border-collapse: collapse; }
th, td { padding: 15px 18px; text-align: left; border-bottom: 1px solid var(--line); white-space: nowrap; }
th { color: var(--muted); font-size: 12px; }
.empty-row { padding: 44px; text-align: center; color: var(--muted); }
@media (max-width: 980px) { .statistics-card-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 760px) {
  .statistics-heading { align-items: stretch; }
  .statistics-filter { grid-template-columns: 1fr; width: 100%; }
  .statistics-card-grid { grid-template-columns: 1fr; }
  .table-heading { align-items: flex-start; flex-direction: column; }
}
</style>
