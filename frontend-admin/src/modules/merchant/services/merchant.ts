import type { ApiResponse } from '../../../types/auth'
import { http } from '../../../services/http'
import type { MerchantApplication, MerchantApplicationPage } from '../types'

function unwrap<T>(response: ApiResponse<T>) {
  if (response.code !== 0) throw new Error(response.message)
  return response.data
}

export const merchantAdminApi = {
  async applications(params: { status: string; page: number; pageSize: number }) {
    return unwrap((await http.get<ApiResponse<MerchantApplicationPage>>('/api/admin/merchant/applications', { params })).data)
  },
  async application(id: string) {
    return unwrap((await http.get<ApiResponse<MerchantApplication>>(`/api/admin/merchant/applications/${id}`)).data)
  },
  async qualificationAudit(id: string, approved: boolean, remark: string) {
    return unwrap(
      (
        await http.post<ApiResponse<null>>(`/api/admin/merchant/applications/${id}/qualification-audit`, {
          approved,
          reason: remark,
        })
      ).data,
    )
  },
  async accountAudit(id: string, approved: boolean, remark: string) {
    return unwrap(
      (
        await http.post<ApiResponse<null>>(`/api/admin/merchant/applications/${id}/account-audit`, {
          approved,
          reason: remark,
        })
      ).data,
    )
  },
  async retryCredentialEmail(id: string) {
    return unwrap(
      (await http.post<ApiResponse<null>>(`/api/admin/merchant/applications/${id}/credential-email/retry`)).data,
    )
  },
  async downloadFile(applicationId: string, fileId: string, filename: string) {
    const response = await http.get(`/api/admin/merchant/applications/${applicationId}/files/${fileId}`, {
      responseType: 'blob',
    })
    // 临时对象 URL 仅用于触发浏览器下载，点击后立即释放，避免连续审核时累积内存。
    const url = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = filename
    anchor.click()
    URL.revokeObjectURL(url)
  },
}
