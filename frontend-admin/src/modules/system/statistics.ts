import type { ApiResponse } from '../../types/auth'
import { http } from '../../services/http'

export interface StatisticsRange {
  startAt: string
  endAt: string
}

export interface PlatformMetrics {
  newUsers: string
  activeUsersSnapshot: string
  paidOrderCount: string
  paidBuyerCount: string
  grossPaidAmount: string
  successfulRefundAmount: string
  netCashflowActivity: string
  onSaleProductSnapshot: string
  searchCount: string
  displayedReviewCount: string
}

export interface PlatformTrendPoint {
  date: string
  newUsers: string
  paidOrderCount: string
  paidBuyerCount: string
  grossPaidAmount: string
  successfulRefundAmount: string
  netCashflowActivity: string
  searchCount: string
}

export interface StatisticsMetadata {
  metricVersion: string
  timezone: string
  generatedAt: string
  dataAsOf: string
  range: StatisticsRange
}

export interface PlatformOverview extends StatisticsMetadata {
  metrics: PlatformMetrics
}

export interface PlatformTrends extends StatisticsMetadata {
  points: PlatformTrendPoint[]
}

function unwrap<T>(response: ApiResponse<T>) {
  if (response.code !== 0) throw new Error(response.message)
  return response.data
}

export const platformStatisticsApi = {
  async overview(startAt: string, endAt: string) {
    return unwrap((await http.get<ApiResponse<PlatformOverview>>('/api/admin/statistics/platform/overview', {
      params: { startAt, endAt, timezone: 'Asia/Shanghai', granularity: 'DAY' },
    })).data)
  },
  async trends(startAt: string, endAt: string) {
    return unwrap((await http.get<ApiResponse<PlatformTrends>>('/api/admin/statistics/platform/trends', {
      params: { startAt, endAt, timezone: 'Asia/Shanghai', granularity: 'DAY' },
    })).data)
  },
}
