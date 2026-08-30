import { redirectToLogin } from '../../../core/auth/access'
import { ApiError, errorMessage } from '../../../core/models/api'
import { merchantApi } from '../../../features/merchant/data/merchant-api'

function pad(part: number): string {
  return String(part).padStart(2, '0')
}

function localDateTime(value: Date): string {
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

Page({
  data: {
    shopName: '',
    cards: [] as { label: string; value: string }[],
    loading: false,
    error: '',
  },

  async onLoad() {
    await this.load()
  },

  async load() {
    const now = new Date()
    const start = new Date(now)
    start.setHours(0, 0, 0, 0)
    start.setDate(start.getDate() - 6)
    this.setData({ loading: true, error: '' })
    try {
      const overview = await merchantApi.statisticsOverview(localDateTime(start), localDateTime(now))
      const metrics = overview.metrics
      this.setData({
        shopName: overview.shopName,
        cards: [
          { label: '支付订单', value: metrics.paidOrderCount },
          { label: '支付买家', value: metrics.paidBuyerCount },
          { label: '支付总额', value: `¥${metrics.grossPaidAmount}` },
          { label: '成功退款', value: `¥${metrics.successfulRefundAmount}` },
          { label: '净收款活动额', value: `¥${metrics.netCashflowActivity}` },
          { label: '客单价', value: metrics.averageOrderValue ? `¥${metrics.averageOrderValue}` : '—' },
          { label: '支付商品件数', value: metrics.soldQuantity },
          { label: '当前在售商品', value: metrics.onSaleProductSnapshot },
          { label: '当前有效评价', value: metrics.displayedReviewCount },
          { label: '当前平均评分', value: metrics.averageRating ?? '—' },
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
