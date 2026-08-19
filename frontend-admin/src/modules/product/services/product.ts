import type { ApiResponse } from '../../../types/auth'
import { http } from '../../../services/http'
import type { AdminCategory, AdminSpuDetail, AdminSpuItem, AdminSpuPage } from '../types'

function unwrap<T>(response: ApiResponse<T>) {
  if (response.code !== 0) throw new Error(response.message)
  return response.data
}

export const productAdminApi = {
  async page(params: { keyword?: string; status?: string; page: number; pageSize: number }) {
    return unwrap(
      (await http.get<ApiResponse<AdminSpuPage>>('/api/admin/spu/page', { params })).data,
    )
  },
  async detail(spuId: string) {
    return unwrap((await http.get<ApiResponse<AdminSpuDetail>>(`/api/admin/spu/${spuId}`)).data)
  },
  async audit(spuId: string, result: 'APPROVE' | 'REJECT' | 'REVOKE', remark?: string) {
    return unwrap(
      (await http.put<ApiResponse<AdminSpuItem>>(`/api/admin/spu/${spuId}/audit`, { result, remark })).data,
    )
  },
}

export const categoryAdminApi = {
  async list() {
    return unwrap((await http.get<ApiResponse<AdminCategory[]>>('/api/admin/categories')).data)
  },
  async create(payload: { parentId: string | number; name: string; sortOrder: number; status: number }) {
    return unwrap((await http.post<ApiResponse<AdminCategory>>('/api/admin/categories', payload)).data)
  },
  async update(categoryId: string, payload: { name: string; sortOrder: number; status: number; icon?: string | null; level?: number }) {
    return unwrap((await http.put<ApiResponse<AdminCategory>>(`/api/admin/categories/${categoryId}`, payload)).data)
  },
  async remove(categoryId: string) {
    return unwrap((await http.delete<ApiResponse<null>>(`/api/admin/categories/${categoryId}`)).data)
  },
}
