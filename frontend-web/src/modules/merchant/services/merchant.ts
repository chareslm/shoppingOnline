import { http, unwrap } from '@/services/http'
import type { ApiResponse } from '@/types/api'
import type { MerchantApplicationReceipt, MerchantApplicationRequest, ShopStaffAccount, ShopSummary } from '../types'

export const merchantApi = {
  async submitApplication(application: MerchantApplicationRequest, files: File[]) {
    const body = new FormData()
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
  async listStaff() {
    return unwrap((await http.get<ApiResponse<ShopStaffAccount[]>>('/api/merchant/staff')).data)
  },
  async currentShop() {
    return unwrap((await http.get<ApiResponse<ShopSummary>>('/api/merchant/shop')).data)
  },
  async createStaff(payload: { email: string; displayName: string; username?: string }) {
    return unwrap((await http.post<ApiResponse<ShopStaffAccount>>('/api/merchant/staff', payload)).data)
  },
  async retryStaffEmail(staffId: string) {
    return unwrap((await http.post<ApiResponse<ShopStaffAccount>>(`/api/merchant/staff/${staffId}/credential-email/retry`)).data)
  },
}
