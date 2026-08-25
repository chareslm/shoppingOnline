export interface UserStatisticsMetrics {
  paidOrderCount: string
  grossPaidAmount: string
  successfulRefundAmount: string
  displayedReviewCount: string
}

export interface UserStatisticsOverview {
  metricVersion: string
  timezone: string
  generatedAt: string
  dataAsOf: string
  range: { startAt: string; endAt: string }
  metrics: UserStatisticsMetrics
}
