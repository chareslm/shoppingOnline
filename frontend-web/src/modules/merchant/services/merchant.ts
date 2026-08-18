import { http, unwrap } from '@/services/http'
import type { ApiResponse } from '@/types/api'
import type { MerchantApplicationReceipt, MerchantApplicationRequest } from '../types'

export const merchantApi = {
  async submitApplication(application: MerchantApplicationRequest, files: File[]) {
    const body = new FormData()
    // JSON 使用独立 application part，避免后端将其按普通字符串而非结构化请求体解析。
    body.append('application', new Blob([JSON.stringify(application)], { type: 'application/json' }))
    files.forEach((file) => body.append('files', file))
    return unwrap(
      (
        await http.post<ApiResponse<MerchantApplicationReceipt>>('/api/merchant/applications', body, {
          timeout: 30_000,
        })
      ).data,
    )
  },
}
