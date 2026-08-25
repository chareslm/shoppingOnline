import { redirectToLogin } from '../../core/auth/access'
import { ApiError, errorMessage } from '../../core/models/api'
import { statisticsApi } from '../../features/account/data/statistics-api'

interface MetricCard {
  label: string
  value: string
  description: string
}

const today = new Date()

Page({
  data: {
    startDate: formatDate(addDays(today, -29)),
    endDate: formatDate(today),
    maxDate: formatDate(today),
    loading: false,
    error: '',
    metricVersion: '',
    timezone: '',
    cards: [] as MetricCard[],
  },

  async onLoad() {
    await this.loadStatistics()
  },

  async onPullDownRefresh() {
    await this.loadStatistics()
    wx.stopPullDownRefresh()
  },

  changeStart(event: WechatMiniprogram.CustomEvent<{ value: string }>) {
    this.setData({ startDate: event.detail.value })
  },

  changeEnd(event: WechatMiniprogram.CustomEvent<{ value: string }>) {
    this.setData({ endDate: event.detail.value })
  },

  async loadStatistics() {
    const start = parseDate(this.data.startDate)
    const end = parseDate(this.data.endDate)
    const days = Math.round((end.getTime() - start.getTime()) / 86_400_000) + 1
    if (days < 1 || days > 31) {
      this.setData({ error: '统计时间范围最多覆盖 31 个自然日' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const overview = await statisticsApi.userOverview(
        `${this.data.startDate}T00:00:00`,
        `${formatDate(addDays(end, 1))}T00:00:00`,
      )
      const metrics = overview.metrics
      this.setData({
        metricVersion: overview.metricVersion,
        timezone: overview.timezone,
        cards: [
          { label: '支付订单', value: metrics.paidOrderCount, description: '按支付成功时间统计' },
          { label: '支付总额', value: `¥${metrics.grossPaidAmount}`, description: '保留原始支付金额' },
          { label: '成功退款', value: `¥${metrics.successfulRefundAmount}`, description: '按退款成功时间统计' },
          { label: '有效评价', value: metrics.displayedReviewCount, description: '区间内创建且当前展示' },
        ],
      })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ loading: false })
    }
  },
})

function parseDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function addDays(value: Date, days: number): Date {
  const result = new Date(value)
  result.setDate(result.getDate() + days)
  return result
}

function formatDate(value: Date): string {
  const two = (part: number) => String(part).padStart(2, '0')
  return `${value.getFullYear()}-${two(value.getMonth() + 1)}-${two(value.getDate())}`
}
