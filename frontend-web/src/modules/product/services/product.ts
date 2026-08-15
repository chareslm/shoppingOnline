import { http, unwrap } from '@/services/http'
import type { ApiResponse } from '@/types/api'
import type {
  Category,
  CategoryCreateRequest,
  CategoryNode,
  Page,
  Review,
  ReviewStats,
  SearchItem,
  Sku,
  SpuCreateRequest,
  SpuDetail,
  SpuItem,
} from '../types'

export const categoryApi = {
  async tree() {
    return unwrap((await http.get<ApiResponse<CategoryNode[]>>('/api/categories/tree')).data)
  },
  async list() {
    return unwrap((await http.get<ApiResponse<Category[]>>('/api/admin/categories')).data)
  },
  async create(payload: CategoryCreateRequest) {
    return unwrap((await http.post<ApiResponse<Category>>('/api/admin/categories', payload)).data)
  },
}

export const spuApi = {
  async detail(spuId: string) {
    return unwrap((await http.get<ApiResponse<SpuDetail>>(`/api/spu/${spuId}`)).data)
  },
  async page(params: { categoryId?: string; keyword?: string; status?: string; page: number; pageSize: number }) {
    return unwrap(
      (await http.get<ApiResponse<Page<SpuItem>>>('/api/spu/page', { params })).data,
    )
  },
  async adminPage(params: { categoryId?: string; keyword?: string; status?: string; page: number; pageSize: number }) {
    return unwrap(
      (await http.get<ApiResponse<Page<SpuItem>>>('/api/admin/spu/page', { params })).data,
    )
  },
  async create(payload: SpuCreateRequest) {
    return unwrap((await http.post<ApiResponse<SpuDetail>>('/api/merchant/spu', payload)).data)
  },
  async changeStatus(spuId: string, action: 'SUBMIT' | 'PUBLISH' | 'OFF_SHELF', remark?: string) {
    return unwrap(
      (await http.put<ApiResponse<SpuItem>>(`/api/merchant/spu/${spuId}/status`, { action, remark })).data,
    )
  },
  async audit(spuId: string, result: 'APPROVE' | 'REJECT', remark?: string) {
    return unwrap((await http.put<ApiResponse<SpuItem>>(`/api/admin/spu/${spuId}/audit`, { result, remark })).data)
  },
  async adjustStock(skuId: string, change: number, remark?: string) {
    return unwrap((await http.put<ApiResponse<Sku>>(`/api/merchant/sku/${skuId}/stock`, { change, remark })).data)
  },
}

export const searchApi = {
  async search(params: {
    keyword?: string
    categoryId?: string
    brand?: string
    priceMin?: number
    priceMax?: number
    sort?: string
    page: number
    pageSize: number
  }) {
    return unwrap((await http.get<ApiResponse<Page<SearchItem>>>('/api/search', { params })).data)
  },
  async hotWords(limit = 10) {
    return unwrap((await http.get<ApiResponse<{ words: { keyword: string; count: string }[] }>>('/api/search/hot-words', { params: { limit } })).data)
  },
}

export const reviewApi = {
  async listBySpu(spuId: string, page = 1, pageSize = 20) {
    return unwrap((await http.get<ApiResponse<Page<Review>>>(`/api/review/spu/${spuId}`, { params: { page, pageSize } })).data)
  },
  async stats(spuId: string) {
    return unwrap((await http.get<ApiResponse<ReviewStats>>(`/api/review/spu/${spuId}/stats`)).data)
  },
  async create(payload: { orderItemId: string; rating: number; content?: string; images?: string[]; anonymous?: boolean }) {
    return unwrap((await http.post<ApiResponse<Review>>('/api/review', payload)).data)
  },
  async reply(reviewId: string, content: string) {
    return unwrap((await http.put<ApiResponse<Review>>(`/api/merchant/review/${reviewId}/reply`, { content })).data)
  },
  async audit(reviewId: string, action: 'HIDE' | 'DISPLAY') {
    return unwrap((await http.put<ApiResponse<null>>(`/api/admin/review/${reviewId}/audit`, { action })).data)
  },
}
