import axios from 'axios'
import type { ApiResponse } from '../../../types/auth'
import { http, readApiError } from '../../../services/http'
import type { AdminShopStaff, MerchantApplication, MerchantApplicationPage } from '../types'

function unwrap<T>(response: ApiResponse<T>) {
  if (response.code !== 0) throw new Error(response.message)
  return response.data
}

async function readBlobPayload(payload: Blob) {
  if (payload.type.includes('json')) {
    const text = await payload.text()
    throw new Error(JSON.parse(text).message || '资质文件加载失败')
  }
  return payload
}

async function readBlobRequestError(error: unknown, fallback: string) {
  if (axios.isAxiosError(error) && error.response?.data instanceof Blob) {
    try {
      const text = await error.response.data.text()
      const message = JSON.parse(text).message
      if (typeof message === 'string' && message) return message
    } catch {
      // Fall through to the generic API error message.
    }
  }
  return readApiError(error, fallback)
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
  async revoke(id: string) {
    return unwrap((await http.post<ApiResponse<null>>(`/api/admin/merchant/applications/${id}/revoke`)).data)
  },
  async restore(id: string) {
    return unwrap((await http.post<ApiResponse<null>>(`/api/admin/merchant/applications/${id}/restore`)).data)
  },
  async filePreviewUrl(applicationId: string, fileId: string) {
    const response = await http.get(`/api/admin/merchant/applications/${applicationId}/files/${fileId}`, {
      responseType: 'blob',
      timeout: 30_000,
    })
    return URL.createObjectURL(await readBlobPayload(response.data))
  },
  async downloadFile(applicationId: string, fileId: string, filename: string) {
    try {
      const response = await http.get(`/api/admin/merchant/applications/${applicationId}/files/${fileId}`, {
        params: { download: true },
        responseType: 'blob',
        timeout: 30_000,
      })
      const payload = await readBlobPayload(response.data)
      const url = URL.createObjectURL(payload)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = filename
      anchor.click()
      URL.revokeObjectURL(url)
    } catch (error) {
      throw new Error(await readBlobRequestError(error, '资质文件下载失败'))
    }
  },
  async staff(status?: string) {
    return unwrap((await http.get<ApiResponse<AdminShopStaff[]>>('/api/admin/merchant/staff', { params: { status } })).data)
  },
  async auditStaff(staffId: string, result: 'APPROVE' | 'REJECT', remark?: string) {
    return unwrap(
      (await http.post<ApiResponse<AdminShopStaff>>(`/api/admin/merchant/staff/${staffId}/audit`, { result, remark })).data,
    )
  },
  async revokeStaff(staffId: string, remark?: string) {
    return unwrap((await http.post<ApiResponse<AdminShopStaff>>(`/api/admin/merchant/staff/${staffId}/revoke`, { remark })).data)
  },
  async restoreStaff(staffId: string, remark?: string) {
    return unwrap((await http.post<ApiResponse<AdminShopStaff>>(`/api/admin/merchant/staff/${staffId}/restore`, { remark })).data)
  },
  async retryStaffEmail(staffId: string) {
    return unwrap(
      (await http.post<ApiResponse<AdminShopStaff>>(`/api/admin/merchant/staff/${staffId}/credential-email/retry`)).data,
    )
  },
}
