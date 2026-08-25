import { http, unwrap } from '@/services/http'
import type { ApiResponse } from '@/types/api'

export interface StatisticsRange {
  startAt: string
  endAt: string
}

export interface ShopMetrics {
  paidOrderCount: string
  paidBuyerCount: string
  grossPaidAmount: string
  successfulRefundAmount: string
  netCashflowActivity: string
  averageOrderValue: string | null
  soldQuantity: string
  onSaleProductSnapshot: string
  displayedReviewCount: string
  averageRating: string | null
}

export interface ShopTrendPoint {
  date: string
  paidOrderCount: string
  paidBuyerCount: string
  grossPaidAmount: string
  successfulRefundAmount: string
  netCashflowActivity: string
  soldQuantity: string
}

interface ShopStatisticsMetadata {
  metricVersion: string
  timezone: string
  generatedAt: string
  dataAsOf: string
  range: StatisticsRange
  shopId: string
  shopName: string
}

export interface ShopOverview extends ShopStatisticsMetadata {
  metrics: ShopMetrics
}

export interface ShopTrends extends ShopStatisticsMetadata {
  points: ShopTrendPoint[]
}

export const shopStatisticsApi = {
  async overview(startAt: string, endAt: string) {
    return unwrap((await http.get<ApiResponse<ShopOverview>>('/api/merchant/statistics/overview', {
      params: { startAt, endAt, timezone: 'Asia/Shanghai', granularity: 'DAY' },
    })).data)
  },
  async trends(startAt: string, endAt: string) {
    return unwrap((await http.get<ApiResponse<ShopTrends>>('/api/merchant/statistics/trends', {
      params: { startAt, endAt, timezone: 'Asia/Shanghai', granularity: 'DAY' },
    })).data)
  },
}
