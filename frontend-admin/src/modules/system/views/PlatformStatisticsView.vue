<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { readApiError } from '../../../services/http'
import { platformStatisticsApi, type PlatformOverview, type PlatformTrends } from '../statistics'

const loading = ref(false)
const overview = ref<PlatformOverview | null>(null)
const trends = ref<PlatformTrends | null>(null)

function localDateTime(value: Date) {
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

const now = new Date()
const start = new Date(now)
start.setHours(0, 0, 0, 0)
start.setDate(start.getDate() - 6)
const range = ref<[string, string]>([localDateTime(start), localDateTime(now)])

const cards = computed(() => {
  const metrics = overview.value?.metrics
  if (!metrics) return []
  return [
    ['新增注册用户', count(metrics.newUsers)],
    ['有效用户快照', count(metrics.activeUsersSnapshot)],
    ['支付订单', count(metrics.paidOrderCount)],
    ['支付买家', count(metrics.paidBuyerCount)],
    ['支付总额（GMV）', money(metrics.grossPaidAmount)],
    ['成功退款', money(metrics.successfulRefundAmount)],
    ['净收款活动额', money(metrics.netCashflowActivity)],
    ['当前在售商品', count(metrics.onSaleProductSnapshot)],
    ['搜索次数', count(metrics.searchCount)],
    ['有效展示评价', count(metrics.displayedReviewCount)],
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
  if (!range.value?.[0] || !range.value?.[1]) {
    ElMessage.warning('请选择统计时间范围')
    return
  }
  loading.value = true
  try {
    const [overviewResult, trendResult] = await Promise.all([
      platformStatisticsApi.overview(range.value[0], range.value[1]),
      platformStatisticsApi.trends(range.value[0], range.value[1]),
    ])
    overview.value = overviewResult
    trends.value = trendResult
  } catch (error) {
    ElMessage.error(readApiError(error, '平台统计加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section v-loading="loading">
    <div class="page-heading statistics-heading">
      <div>
        <p class="eyebrow">EXACT STATISTICS</p>
        <h1>平台统计</h1>
        <p>来自 MySQL 权威业务表的精确查询；支付总额不是平台收入。</p>
      </div>
      <div class="statistics-filter">
        <el-date-picker
          v-model="range"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
        />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
    </div>

    <el-alert
      v-if="overview"
      class="statistics-alert"
      type="info"
      :closable="false"
      :title="`口径 ${overview.metricVersion} · ${overview.timezone} · 数据截至 ${formatTime(overview.dataAsOf)}`"
    />

    <div class="statistics-card-grid">
      <el-card v-for="card in cards" :key="card[0]" shadow="never" class="statistics-card">
        <span>{{ card[0] }}</span>
        <strong>{{ card[1] }}</strong>
      </el-card>
    </div>

    <el-card shadow="never" class="statistics-table-card">
      <template #header><strong>按日趋势</strong></template>
      <el-table :data="trends?.points ?? []" empty-text="当前区间暂无数据">
        <el-table-column prop="date" label="日期" min-width="112" />
        <el-table-column prop="newUsers" label="新增用户" min-width="100" />
        <el-table-column prop="paidOrderCount" label="支付订单" min-width="100" />
        <el-table-column prop="paidBuyerCount" label="支付买家" min-width="100" />
        <el-table-column label="支付总额" min-width="130"><template #default="scope">{{ money(scope.row.grossPaidAmount) }}</template></el-table-column>
        <el-table-column label="成功退款" min-width="130"><template #default="scope">{{ money(scope.row.successfulRefundAmount) }}</template></el-table-column>
        <el-table-column label="净收款活动额" min-width="140"><template #default="scope">{{ money(scope.row.netCashflowActivity) }}</template></el-table-column>
        <el-table-column prop="searchCount" label="搜索次数" min-width="100" />
      </el-table>
    </el-card>
  </section>
</template>

<style scoped>
.statistics-heading { gap: 20px; }
.statistics-filter { display: flex; align-items: center; gap: 10px; }
.statistics-alert { margin-bottom: 16px; }
.statistics-card-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; margin-bottom: 18px; }
.statistics-card span, .statistics-card strong { display: block; }
.statistics-card span { color: #64748b; font-size: 13px; }
.statistics-card strong { margin-top: 12px; color: #1e3a8a; font-size: 22px; overflow-wrap: anywhere; }
.statistics-table-card :deep(.el-card__body) { padding: 0; }
@media (max-width: 1180px) { .statistics-card-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 760px) {
  .statistics-heading, .statistics-filter { align-items: stretch; flex-direction: column; }
  .statistics-card-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .statistics-filter :deep(.el-date-editor) { width: 100%; }
}
</style>
