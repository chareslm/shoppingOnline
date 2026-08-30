import { apiRequest } from '../../../core/http/api-client'
import type { UserStatisticsOverview } from '../domain/statistics-models'

export const statisticsApi = {
  userOverview(startAt: string, endAt: string): Promise<UserStatisticsOverview> {
    return apiRequest({
      path: '/api/users/me/statistics/overview',
      data: { startAt, endAt, timezone: 'Asia/Shanghai', granularity: 'DAY' },
    })
  },
}
