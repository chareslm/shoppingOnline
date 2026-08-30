<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { readApiError } from '@/services/http'
import { userStatisticsApi, type UserOverview } from '../statistics'

const loading = ref(false)
const message = ref('')
const overview = ref<UserOverview | null>(null)

function localDateTime(value: Date) {
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

const now = new Date()
const start = new Date(now)
start.setHours(0, 0, 0, 0)
start.setDate(start.getDate() - 29)
const startAt = ref(localDateTime(start))
const endAt = ref(localDateTime(now))

const cards = computed(() => {
  const metrics = overview.value?.metrics
  if (!metrics) return []
  return [
    ['支付订单', count(metrics.paidOrderCount), '按支付成功时间统计本人订单'],
    ['支付总额', money(metrics.grossPaidAmount), '保留退款订单的原始支付金额'],
    ['成功退款', money(metrics.successfulRefundAmount), '按退款成功发生时间统计'],
    ['有效评价', count(metrics.displayedReviewCount), '区间内创建且当前仍展示'],
  ]
})

function money(value: string) { return `¥${value}` }
function count(value: string) { return value.replace(/\B(?=(\d{3})+(?!\d))/g, ',') }
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
    overview.value = await userStatisticsApi.overview(startAt.value, endAt.value)
  } catch (error) {
    message.value = readApiError(error, '消费统计加载失败')
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
        <p class="eyebrow">MY STATISTICS</p>
        <h1>消费统计</h1>
        <p>仅查询当前登录账号本人数据，来自 MySQL 权威业务表。</p>
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
    <div v-if="loading && !overview" class="loading-card">正在读取本人统计…</div>
    <div v-else class="statistics-card-grid">
      <article v-for="card in cards" :key="card[0]" class="stat-card compact-stat">
        <span>{{ card[0] }}</span><strong>{{ card[1] }}</strong><small>{{ card[2] }}</small>
      </article>
    </div>

    <div class="section-card scope-note">
      <h2>口径说明</h2>
      <p>支付总额按原支付成功时间统计，不因后续退款回写；成功退款按退款实际成功时间统计，因此两个金额可能来自不同支付周期。本页面不是账单或财务结算凭证。</p>
    </div>
  </section>
</template>

<style scoped>
.statistics-heading { align-items: flex-end; }
.statistics-filter { display: grid; grid-template-columns: minmax(190px, 1fr) minmax(190px, 1fr) auto; gap: 10px; align-items: end; }
.statistics-filter label { display: grid; gap: 7px; color: var(--muted); font-size: 12px; font-weight: 700; }
.statistics-meta { margin: 0; padding: 12px 16px; border: 1px solid #c9ddd3; border-radius: 12px; color: var(--green-dark); background: #eef7f2; }
.statistics-card-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.compact-stat { padding: 22px; }
.compact-stat strong { margin-bottom: 12px; font-size: 26px; overflow-wrap: anywhere; }
.scope-note h2 { margin-top: 0; }
.scope-note p { margin-bottom: 0; color: var(--muted); line-height: 1.8; }
@media (max-width: 980px) { .statistics-card-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 760px) {
  .statistics-heading { align-items: stretch; }
  .statistics-filter { grid-template-columns: 1fr; width: 100%; }
  .statistics-card-grid { grid-template-columns: 1fr; }
}
</style>
